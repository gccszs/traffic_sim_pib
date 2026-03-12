package com.traffic.sim.common.mq;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class MemoryMessageQueue implements MessageQueue {

    private final Map<String, ConcurrentLinkedQueue<Message<?>>> normalQueues;
    private final PriorityBlockingQueue<DelayedMessageWrapper> delayedQueue;
    private final Map<String, List<ConsumerHolder<?>>> consumers;
    private final Map<String, ExecutorService> consumerExecutors;
    private final Map<String, AtomicLong> queueStats;
    private final ScheduledExecutorService delayedMessageExecutor;
    private final ExecutorService publishExecutor;
    private final int defaultQueueCapacity;
    private volatile boolean isShutdown;

    private static final int MAX_DELAYED_MESSAGE_SCAN = 100;

    public MemoryMessageQueue() {
        this(10000);
    }

    public MemoryMessageQueue(int defaultQueueCapacity) {
        this.normalQueues = new ConcurrentHashMap<>();
        this.delayedQueue = new PriorityBlockingQueue<>(100,
            Comparator.comparingLong(DelayedMessageWrapper::getReadyTime));
        this.consumers = new ConcurrentHashMap<>();
        this.consumerExecutors = new ConcurrentHashMap<>();
        this.queueStats = new ConcurrentHashMap<>();
        this.defaultQueueCapacity = defaultQueueCapacity;
        this.isShutdown = false;
        this.delayedMessageExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mq-delayed-message-executor");
            t.setDaemon(true);
            return t;
        });
        this.publishExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "mq-publish-executor");
            t.setDaemon(true);
            return t;
        });

        startDelayedMessageProcessor();
    }

    private void startDelayedMessageProcessor() {
        delayedMessageExecutor.scheduleAtFixedRate(() -> {
            if (isShutdown) {
                return;
            }
            try {
                long now = System.currentTimeMillis();
                int processed = 0;

                while (processed < MAX_DELAYED_MESSAGE_SCAN) {
                    DelayedMessageWrapper wrapper = delayedQueue.peek();
                    if (wrapper == null || wrapper.getReadyTime() > now) {
                        break;
                    }

                    wrapper = delayedQueue.poll();
                    if (wrapper != null) {
                        Message<?> message = wrapper.getMessage();
                        dispatchMessage(message.getTopic(), message);
                        processed++;
                    }
                }

                if (processed > 0) {
                    log.debug("Processed {} delayed messages", processed);
                }
            } catch (Exception e) {
                log.error("Error processing delayed messages", e);
            }
        }, 10, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void publish(String topic, T payload) {
        publish(Message.of(topic, payload));
    }

    @Override
    public <T> void publish(Message<T> message) {
        if (isShutdown) {
            throw new IllegalStateException("MessageQueue is shutdown");
        }

        String topic = message.getTopic();

        if (message.isDelayed()) {
            long delay = message.getTimestamp() - System.currentTimeMillis();
            if (delay > 0) {
                delayedQueue.offer(new DelayedMessageWrapper(message.getTimestamp(), message));
                queueStats.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
                return;
            } else {
                message.setTimestamp(System.currentTimeMillis());
                message.setType(Message.MessageType.NORMAL);
            }
        }

        ConcurrentLinkedQueue<Message<?>> queue = normalQueues.computeIfAbsent(topic, k -> {
            queueStats.put(k, new AtomicLong(0));
            return new ConcurrentLinkedQueue<>();
        });

        if (defaultQueueCapacity > 0 && queue.size() >= defaultQueueCapacity) {
            throw new IllegalStateException("Queue [" + topic + "] is full, capacity: " + defaultQueueCapacity);
        }

        queue.offer(message);
        queueStats.get(topic).incrementAndGet();

        dispatchMessage(topic, message);
    }

    @Override
    public <T> void publishDelayed(String topic, T payload, long delayMillis) {
        publish(Message.delayed(topic, payload, delayMillis));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, MessageConsumer<T> consumer) {
        subscribe(topic, consumer, 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, MessageConsumer<T> consumer, int threads) {
        if (threads <= 0) {
            threads = 1;
        }

        consumers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>());

        List<ConsumerHolder<?>> topicConsumers = consumers.get(topic);

        ExecutorService executor = consumerExecutors.computeIfAbsent(topic,
                k -> {
                    ThreadFactory factory = new ThreadFactory() {
                        private int count = 0;
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "mq-consumer-" + topic + "-" + count++);
                            t.setDaemon(true);
                            return t;
                        }
                    };
                    return Executors.newFixedThreadPool(threads, factory);
                });

        for (int i = 0; i < threads; i++) {
            String consumerId = consumer.getClass().getSimpleName() + "-" + topic + "-" + i;
            ConsumerHolder<T> holder = new ConsumerHolder<>(consumerId, topic, consumer, i, threads);
            topicConsumers.add(holder);

            final int threadIndex = i;
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted() && !isShutdown) {
                    try {
                        ConcurrentLinkedQueue<Message<?>> queue = normalQueues.get(topic);
                        if (queue != null) {
                            Message<?> message = queue.poll();
                            if (message != null) {
                                dispatchToConsumer(holder, message);
                            }
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Error consuming message from topic: {}", topic, e);
                    }
                }
            });
        }

        log.info("Subscribed {} threads to topic: {}", threads, topic);
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatchToConsumer(ConsumerHolder<T> holder, Message<T> message) {
        try {
            holder.consumer.consume(message);
            holder.consumer.onSuccess(message);
        } catch (Exception e) {
            log.error("Error in consumer: {}", holder.consumerId, e);
            holder.consumer.onFailure(message, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchMessage(String topic, Message<?> message) {
        List<ConsumerHolder<?>> topicConsumers = consumers.get(topic);
        if (topicConsumers == null || topicConsumers.isEmpty()) {
            return;
        }

        if (message.getType() == Message.MessageType.BROADCAST) {
            for (ConsumerHolder<?> holder : topicConsumers) {
                dispatchToConsumer(holder, message);
            }
        } else {
            int size = topicConsumers.size();
            if (size == 1) {
                dispatchToConsumer(topicConsumers.get(0), message);
            } else {
                int index = message.getId().hashCode() % size;
                if (index < 0) {
                    index += size;
                }
                dispatchToConsumer(topicConsumers.get(index), message);
            }
        }
    }

    @Override
    public void unsubscribe(String topic) {
        consumers.remove(topic);
        ExecutorService executor = consumerExecutors.remove(topic);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void unsubscribe(String topic, MessageConsumer<?> consumer) {
        List<ConsumerHolder<?>> topicConsumers = consumers.get(topic);
        if (topicConsumers != null) {
            String targetId = consumer.getClass().getSimpleName();
            topicConsumers.removeIf(h -> h.consumerId.startsWith(targetId));
        }
    }

    @Override
    public Collection<String> getTopics() {
        Set<String> topics = new HashSet<>(normalQueues.keySet());
        for (DelayedMessageWrapper wrapper : delayedQueue) {
            topics.add(wrapper.getMessage().getTopic());
        }
        return topics;
    }

    @Override
    public long getQueueSize(String topic) {
        ConcurrentLinkedQueue<Message<?>> queue = normalQueues.get(topic);
        long normalSize = queue != null ? queue.size() : 0;
        long delayedSize = delayedQueue.stream()
                .filter(w -> w.getMessage().getTopic().equals(topic))
                .count();
        return normalSize + delayedSize;
    }

    @Override
    public void clear(String topic) {
        ConcurrentLinkedQueue<Message<?>> queue = normalQueues.remove(topic);
        if (queue != null) {
            queue.clear();
        }
        delayedQueue.removeIf(w -> w.getMessage().getTopic().equals(topic));
        queueStats.remove(topic);
    }

    @Override
    public void clearAll() {
        for (String topic : new ArrayList<>(normalQueues.keySet())) {
            clear(topic);
        }
    }

    @Override
    public void shutdown() {
        this.isShutdown = true;
        clearAll();

        for (ExecutorService executor : consumerExecutors.values()) {
            executor.shutdownNow();
        }
        consumerExecutors.clear();

        delayedMessageExecutor.shutdownNow();
        publishExecutor.shutdownNow();

        log.info("MemoryMessageQueue shutdown completed");
    }

    public long getDelayedQueueSize() {
        return delayedQueue.size();
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : queueStats.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().get());
        }
        return stats;
    }

    private static class DelayedMessageWrapper {
        private final long readyTime;
        private final Message<?> message;

        public DelayedMessageWrapper(long readyTime, Message<?> message) {
            this.readyTime = readyTime;
            this.message = message;
        }

        public long getReadyTime() {
            return readyTime;
        }

        public Message<?> getMessage() {
            return message;
        }
    }

    private static class ConsumerHolder<T> {
        final String consumerId;
        final String topic;
        final MessageConsumer<T> consumer;
        final int index;
        final int total;

        ConsumerHolder(String consumerId, String topic, MessageConsumer<T> consumer, int index, int total) {
            this.consumerId = consumerId;
            this.topic = topic;
            this.consumer = consumer;
            this.index = index;
            this.total = total;
        }
    }
}
