package com.traffic.sim.service;

import com.traffic.sim.common.mq.Message;
import com.traffic.sim.common.mq.MessageProducer;
import com.traffic.sim.common.mq.MessageQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 消息队列使用示例服务
 * 展示如何在Spring Boot环境中使用消息队列
 */
@Slf4j
@Service
public class MessageQueueExampleService {

    private final MessageQueue messageQueue;
    private final MessageProducer messageProducer;

    public MessageQueueExampleService(MessageQueue messageQueue, MessageProducer messageProducer) {
        this.messageQueue = messageQueue;
        this.messageProducer = messageProducer;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing MessageQueueExampleService...");
        subscribeToTopics();
        log.info("MessageQueueExampleService initialized successfully");
    }

    private void subscribeToTopics() {
        messageQueue.subscribe("simulation.events", message -> {
            log.info("Received simulation event: {}", message.getPayload());
        });

        messageQueue.subscribe("statistics.data", message -> {
            log.info("Received statistics data: {}", message.getPayload());
        }, 2);

        messageQueue.subscribe("broadcast.test", message -> {
            log.info("Received broadcast message: {}", message.getPayload());
        });

        log.info("Subscribed to topics: simulation.events, statistics.data, broadcast.test");
    }

    public void sendSimulationEvent(Object eventData) {
        String messageId = messageProducer.send("simulation.events", eventData);
        log.info("Sent simulation event with ID: {}", messageId);
    }

    public void sendStatisticsData(Object statistics) {
        String messageId = messageProducer.send("statistics.data", statistics);
        log.info("Sent statistics data with ID: {}", messageId);
    }

    public void sendDelayedEvent(Object eventData, long delayMillis) {
        String messageId = messageProducer.sendDelayed("simulation.events", eventData, delayMillis);
        log.info("Sent delayed simulation event with ID: {}, delay: {}ms", messageId, delayMillis);
    }

    public void broadcastMessage(Object messageData) {
        messageProducer.broadcast("broadcast.test", messageData);
        log.info("Broadcast message sent");
    }

    public void sendWithCallback(Object data) {
        messageProducer.send("simulation.events", data, new MessageProducer.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                log.info("Message sent successfully: {}", messageId);
            }

            @Override
            public void onFailure(String messageId, Throwable throwable) {
                log.error("Failed to send message: {}", messageId, throwable);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up MessageQueueExampleService...");
        messageQueue.unsubscribe("simulation.events");
        messageQueue.unsubscribe("statistics.data");
        messageQueue.unsubscribe("broadcast.test");
        log.info("MessageQueueExampleService cleanup completed");
    }
}
