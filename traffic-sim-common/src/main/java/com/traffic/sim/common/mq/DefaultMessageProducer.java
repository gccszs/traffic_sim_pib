package com.traffic.sim.common.mq;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class DefaultMessageProducer implements MessageProducer {

    private final MessageQueue messageQueue;
    private final ExecutorService callbackExecutor;
    private final ConcurrentHashMap<String, Message<?>> pendingMessages;

    public DefaultMessageProducer(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
        this.callbackExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "mq-producer-callback");
            t.setDaemon(true);
            return t;
        });
        this.pendingMessages = new ConcurrentHashMap<>();
    }

    @Override
    public <T> String send(String topic, T payload) {
        Message<T> message = Message.of(topic, payload);
        String messageId = message.getId();
        pendingMessages.put(messageId, message);
        try {
            messageQueue.publish(message);
            return messageId;
        } finally {
            pendingMessages.remove(messageId);
        }
    }

    @Override
    public <T> String send(String topic, T payload, SendCallback callback) {
        Message<T> message = Message.of(topic, payload);
        String messageId = message.getId();
        pendingMessages.put(messageId, message);
        try {
            messageQueue.publish(message);
            callback.onSuccess(messageId);
            return messageId;
        } catch (Exception e) {
            callback.onFailure(messageId, e);
            throw e;
        } finally {
            pendingMessages.remove(messageId);
        }
    }

    @Override
    public <T> String sendDelayed(String topic, T payload, long delayMillis) {
        Message<T> message = Message.delayed(topic, payload, delayMillis);
        String messageId = message.getId();
        pendingMessages.put(messageId, message);
        try {
            messageQueue.publish(message);
            return messageId;
        } finally {
            pendingMessages.remove(messageId);
        }
    }

    @Override
    public <T> String sendDelayed(String topic, T payload, long delayMillis, SendCallback callback) {
        Message<T> message = Message.delayed(topic, payload, delayMillis);
        String messageId = message.getId();
        pendingMessages.put(messageId, message);
        try {
            messageQueue.publish(message);
            callback.onSuccess(messageId);
            return messageId;
        } catch (Exception e) {
            callback.onFailure(messageId, e);
            throw e;
        } finally {
            pendingMessages.remove(messageId);
        }
    }

    @Override
    public <T> void broadcast(String topic, T payload) {
        Message<T> message = Message.broadcast(topic, payload);
        messageQueue.publish(message);
    }

    @Override
    public void shutdown() {
        callbackExecutor.shutdown();
    }
}
