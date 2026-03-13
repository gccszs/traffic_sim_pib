package com.traffic.sim.common.mq;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 内存消息队列实现
 * 基于JVM内存的消息队列，支持主题订阅、消息发布、延迟消息、广播等功能
 * 采用BlockingQueue实现消费者阻塞等待，避免CPU空转
 */
@Slf4j
public class MemoryMessageQueue implements MessageQueue {

    /** 普通消息队列：主题 -> 消息队列 */
    private final Map<String, BlockingQueue<Message<?>>> normalQueues;

    /** 延迟消息队列：按可消费时间排序的优先级队列 */
    private final PriorityBlockingQueue<DelayedMessageWrapper> delayedQueue;

    /** 消费者列表：主题 -> 消费者列表 */
    private final Map<String, List<ConsumerHolder<?>>> consumers;

    /** 消费者线程池：主题 -> 线程池 */
    private final Map<String, ExecutorService> consumerExecutors;

    /** 队列统计：主题 -> 消息计数 */
    private final Map<String, AtomicLong> queueStats;

    /** 延迟消息处理器线程池 */
    private final ScheduledExecutorService delayedMessageExecutor;

    /** 消息发布线程池 */
    private final ExecutorService publishExecutor;

    /** 默认队列容量，0表示无限制 */
    private final int defaultQueueCapacity;

    /** 关闭标志 */
    private volatile boolean isShutdown;

    /** 每次扫描延迟消息的最大数量 */
    private static final int MAX_DELAYED_MESSAGE_SCAN = 100;

    /**
     * 创建默认配置的内存消息队列（容量10000）
     */
    public MemoryMessageQueue() {
        this(10000);
    }

    /**
     * 创建指定容量的内存消息队列
     * @param defaultQueueCapacity 默认队列容量，0表示无限制
     */
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

    /**
     * 启动延迟消息处理器
     * 每10ms扫描一次延迟队列，将到达可消费时间的消息投递到目标队列
     */
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

        BlockingQueue<Message<?>> queue = normalQueues.computeIfAbsent(topic, k -> {
            queueStats.put(k, new AtomicLong(0));
            return new LinkedBlockingQueue<>();
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
                        BlockingQueue<Message<?>> queue = normalQueues.get(topic);
                        if (queue != null) {
                            Message<?> message = queue.poll(100, TimeUnit.MILLISECONDS);
                            if (message != null) {
                                dispatchToConsumer(holder, message);
                            }
                        }
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

    /**
     * 分发消息给消费者
     * @param holder 消费者持有者
     * @param message 消息对象
     */
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

    /**
     * 将消息分发给主题的消费者
     * 广播消息会分发给所有消费者，普通消息使用哈希分发到单个消费者
     * @param topic 主题名称
     * @param message 消息对象
     */
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
        BlockingQueue<Message<?>> queue = normalQueues.get(topic);
        long normalSize = queue != null ? queue.size() : 0;
        long delayedSize = delayedQueue.stream()
                .filter(w -> w.getMessage().getTopic().equals(topic))
                .count();
        return normalSize + delayedSize;
    }

    @Override
    public void clear(String topic) {
        BlockingQueue<Message<?>> queue = normalQueues.remove(topic);
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

    /**
     * 获取延迟队列大小
     * @return 延迟队列中的消息数量
     */
    public long getDelayedQueueSize() {
        return delayedQueue.size();
    }

    /**
     * 获取队列统计信息
     * @return 主题 -> 消息计数 的映射
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : queueStats.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().get());
        }
        return stats;
    }

    /**
     * 延迟消息包装器
     * 用于延迟队列按时间排序
     */
    private static class DelayedMessageWrapper {
        /** 消息可消费时间 */
        private final long readyTime;
        /** 消息对象 */
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

    /**
     * 消费者持有者
     * 封装消费者相关信息
     */
    private static class ConsumerHolder<T> {
        /** 消费者ID */
        final String consumerId;
        /** 订阅主题 */
        final String topic;
        /** 消费者实例 */
        final MessageConsumer<T> consumer;
        /** 消费者索引 */
        final int index;
        /** 消费者总数 */
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
