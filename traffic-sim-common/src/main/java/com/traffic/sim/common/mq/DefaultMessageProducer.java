package com.traffic.sim.common.mq;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 默认消息生产者实现
 * 负责将消息发布到消息队列，支持同步发送、异步回调、延迟消息和广播
 */
@Slf4j
public class DefaultMessageProducer implements MessageProducer {

    private final MessageQueue messageQueue;
    private final ExecutorService callbackExecutor;

    public DefaultMessageProducer(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
        this.callbackExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "mq-producer-callback");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public <T> String send(String topic, T payload) {
        Message<T> message = Message.of(topic, payload);
        String messageId = message.getId();
        messageQueue.publish(message);
        return messageId;
    }

    @Override
    public <T> String send(String topic, T payload, SendCallback callback) {
        Message<T> message = Message.of(topic, payload);
        String messageId = message.getId();
        try {
            messageQueue.publish(message);
            callbackExecutor.submit(() -> {
                try {
                    callback.onSuccess(messageId);
                } catch (Exception e) {
                    log.error("Error in callback for message: {}", messageId, e);
                }
            });
        } catch (Exception e) {
            callbackExecutor.submit(() -> {
                try {
                    callback.onFailure(messageId, e);
                } catch (Exception ex) {
                    log.error("Error in callback for message: {}", messageId, ex);
                }
            });
            throw e;
        }
        return messageId;
    }

    @Override
    public <T> String sendDelayed(String topic, T payload, long delayMillis) {
        Message<T> message = Message.delayed(topic, payload, delayMillis);
        String messageId = message.getId();
        messageQueue.publish(message);
        return messageId;
    }

    @Override
    public <T> String sendDelayed(String topic, T payload, long delayMillis, SendCallback callback) {
        Message<T> message = Message.delayed(topic, payload, delayMillis);
        String messageId = message.getId();
        try {
            messageQueue.publish(message);
            callbackExecutor.submit(() -> {
                try {
                    callback.onSuccess(messageId);
                } catch (Exception e) {
                    log.error("Error in callback for message: {}", messageId, e);
                }
            });
        } catch (Exception e) {
            callbackExecutor.submit(() -> {
                try {
                    callback.onFailure(messageId, e);
                } catch (Exception ex) {
                    log.error("Error in callback for message: {}", messageId, ex);
                }
            });
            throw e;
        }
        return messageId;
    }

    @Override
    public <T> void broadcast(String topic, T payload) {
        Message<T> message = Message.broadcast(topic, payload);
        messageQueue.publish(message);
    }

    @Override
    public void shutdown() {
        try {
            callbackExecutor.shutdown();
            if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                callbackExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            callbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
