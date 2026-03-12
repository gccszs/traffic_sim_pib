package com.traffic.sim.common.mq;

public interface MessageProducer {

    <T> String send(String topic, T payload);

    <T> String send(String topic, T payload, SendCallback callback);

    <T> String sendDelayed(String topic, T payload, long delayMillis);

    <T> String sendDelayed(String topic, T payload, long delayMillis, SendCallback callback);

    <T> void broadcast(String topic, T payload);

    void shutdown();

    interface SendCallback {
        void onSuccess(String messageId);
        void onFailure(String messageId, Throwable throwable);
    }
}
