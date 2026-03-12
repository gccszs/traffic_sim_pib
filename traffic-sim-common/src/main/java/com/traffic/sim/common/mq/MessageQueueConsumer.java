package com.traffic.sim.common.mq;

import java.util.function.Consumer;

public interface MessageQueueConsumer<T> {

    String getConsumerId();

    String getTopic();

    void consume(Message<T> message);

    default void onSuccess(Message<T> message) {
    }

    default void onFailure(Message<T> message, Throwable throwable) {
    }

    static <T> MessageQueueConsumer<T> of(String consumerId, String topic, java.util.function.Consumer<Message<T>> handler) {
        return new MessageQueueConsumer<T>() {
            @Override
            public String getConsumerId() {
                return consumerId;
            }

            @Override
            public String getTopic() {
                return topic;
            }

            @Override
            public void consume(Message<T> message) {
                handler.accept(message);
            }
        };
    }
}
