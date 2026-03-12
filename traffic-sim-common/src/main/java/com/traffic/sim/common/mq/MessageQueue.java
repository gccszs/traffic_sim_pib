package com.traffic.sim.common.mq;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface MessageQueue {

    <T> void publish(String topic, T payload);

    <T> void publish(Message<T> message);

    <T> void publishDelayed(String topic, T payload, long delayMillis);

    <T> void subscribe(String topic, MessageConsumer<T> consumer);

    <T> void subscribe(String topic, MessageConsumer<T> consumer, int threads);

    void unsubscribe(String topic);

    void unsubscribe(String topic, MessageConsumer<?> consumer);

    Collection<String> getTopics();

    long getQueueSize(String topic);

    void clear(String topic);

    void clearAll();

    void shutdown();

    interface MessageConsumer<T> {
        void consume(Message<T> message);
    }

    interface MessageProcessor<T> {
        void process(Message<T> message, Consumer<Message<T>> ack, Consumer<Message<T>> nack);
    }
}
