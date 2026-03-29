package com.traffic.sim.controller;

import com.traffic.sim.common.mq.Message;
import com.traffic.sim.common.mq.MessageProducer;
import com.traffic.sim.common.mq.MessageQueue;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MQ 演示控制器
 * 提供各种接口来演示 MQ 模块的功能
 */
@Slf4j
@RestController
@RequestMapping("/mq-demo")
public class MQDemoController {

    @Autowired
    private MessageQueue messageQueue;

    @Autowired
    private MessageProducer messageProducer;

    private final AtomicLong messageCounter = new AtomicLong(0);
    private final Map<String, List<ReceivedMessage>> receivedMessages = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @PostConstruct
    public void init() {
        subscribeToDemoTopics();
        log.info("===========================================");
        log.info("MQ Demo Controller initialized");
        log.info("===========================================");
    }

    private void subscribeToDemoTopics() {
        messageQueue.subscribe("demo.test", message -> {
            recordReceivedMessage("demo.test", message);
            log.info("🎯 MQ Demo [demo.test] Received: {}", message.getPayload());
        });

        messageQueue.subscribe("demo.delayed", message -> {
            recordReceivedMessage("demo.delayed", message);
            log.info("⏰ MQ Demo [demo.delayed] Delayed message received: {}", message.getPayload());
        });

        messageQueue.subscribe("demo.broadcast", message -> {
            recordReceivedMessage("demo.broadcast", message);
            log.info("📢 MQ Demo [demo.broadcast] Broadcast received: {}", message.getPayload());
        });

        messageQueue.subscribe("demo.statistics", message -> {
            recordReceivedMessage("demo.statistics", message);
            log.info("📊 MQ Demo [demo.statistics] Statistics data received: {}", message.getPayload());
        });

        log.info("Subscribed to demo topics: demo.test, demo.delayed, demo.broadcast, demo.statistics");
    }

    private void recordReceivedMessage(String topic, Message<?> message) {
        ReceivedMessage received = new ReceivedMessage();
        received.setMessageId(message.getId());
        received.setTopic(topic);
        received.setPayload(message.getPayload().toString());
        received.setTimestamp(LocalDateTime.now().format(formatter));
        received.setThreadName(Thread.currentThread().getName());
        
        receivedMessages.computeIfAbsent(topic, k -> new ArrayList<>()).add(received);
        
        if (receivedMessages.get(topic).size() > 100) {
            receivedMessages.get(topic).remove(0);
        }
    }

    /**
     * 发送普通测试消息
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> payload) {
        String content = payload.getOrDefault("content", "Test message " + messageCounter.incrementAndGet()).toString();
        long startTime = System.currentTimeMillis();
        
        String messageId = messageProducer.send("demo.test", content);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("messageId", messageId);
        result.put("content", content);
        result.put("sendTime", LocalDateTime.now().format(formatter));
        result.put("topic", "demo.test");
        result.put("sendDuration", endTime - startTime + "ms");
        
        log.info("📤 Sent message: ID={}, Content={}, Time={}", messageId, content, LocalDateTime.now().format(formatter));
        
        return result;
    }

    /**
     * 发送延迟消息
     */
    @PostMapping("/send-delayed")
    public Map<String, Object> sendDelayedMessage(@RequestBody Map<String, Object> payload) {
        String content = payload.getOrDefault("content", "Delayed message " + messageCounter.incrementAndGet()).toString();
        long delayMs = Long.parseLong(payload.getOrDefault("delay", "3000").toString());
        long startTime = System.currentTimeMillis();
        
        String messageId = messageProducer.sendDelayed("demo.delayed", content, delayMs);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("messageId", messageId);
        result.put("content", content);
        result.put("sendTime", LocalDateTime.now().format(formatter));
        result.put("expectedDelay", delayMs + "ms");
        result.put("topic", "demo.delayed");
        result.put("sendDuration", endTime - startTime + "ms");
        
        log.info("📤 Sent delayed message: ID={}, Content={}, Delay={}ms", messageId, content, delayMs);
        
        return result;
    }

    /**
     * 发送广播消息
     */
    @PostMapping("/broadcast")
    public Map<String, Object> broadcastMessage(@RequestBody Map<String, Object> payload) {
        String content = payload.getOrDefault("content", "Broadcast message " + messageCounter.incrementAndGet()).toString();
        long startTime = System.currentTimeMillis();
        
        messageProducer.broadcast("demo.broadcast", content);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("content", content);
        result.put("broadcastTime", LocalDateTime.now().format(formatter));
        result.put("topic", "demo.broadcast");
        result.put("sendDuration", endTime - startTime + "ms");
        
        log.info("📤 Sent broadcast: Content={}, Time={}", content, LocalDateTime.now().format(formatter));
        
        return result;
    }

    /**
     * 批量发送消息
     */
    @PostMapping("/send-batch")
    public Map<String, Object> sendBatchMessages(@RequestBody Map<String, Object> payload) {
        int count = Integer.parseInt(payload.getOrDefault("count", "10").toString());
        String prefix = payload.getOrDefault("prefix", "Batch").toString();
        
        long startTime = System.currentTimeMillis();
        List<String> messageIds = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String content = prefix + " message " + (messageCounter.incrementAndGet());
            String messageId = messageProducer.send("demo.test", content);
            messageIds.add(messageId);
        }
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", count);
        result.put("messageIds", messageIds);
        result.put("sendTime", LocalDateTime.now().format(formatter));
        result.put("topic", "demo.test");
        result.put("totalDuration", endTime - startTime + "ms");
        result.put("avgDuration", (endTime - startTime) / count + "ms");
        
        log.info("📤 Sent {} batch messages in {}ms", count, endTime - startTime);
        
        return result;
    }

    /**
     * 获取接收到的消息
     */
    @GetMapping("/received")
    public Map<String, Object> getReceivedMessages(@RequestParam(required = false) String topic) {
        Map<String, Object> result = new HashMap<>();
        
        if (topic != null && !topic.isEmpty()) {
            result.put("topic", topic);
            result.put("messages", receivedMessages.getOrDefault(topic, new ArrayList<>()));
            result.put("count", receivedMessages.getOrDefault(topic, new ArrayList<>()).size());
        } else {
            result.put("allTopics", receivedMessages.keySet());
            result.put("totalTopics", receivedMessages.size());
            
            Map<String, Object> topicStats = new HashMap<>();
            receivedMessages.forEach((t, messages) -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("count", messages.size());
                stats.put("messages", messages);
                topicStats.put(t, stats);
            });
            result.put("topics", topicStats);
        }
        
        return result;
    }

    /**
     * 获取 MQ 状态
     */
    @GetMapping("/status")
    public Map<String, Object> getMQStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().format(formatter));
        result.put("messageQueueSize", getQueueSize());
        result.put("delayedQueueSize", getDelayedQueueSize());
        result.put("totalReceived", receivedMessages.values().stream().mapToInt(List::size).sum());
        result.put("topicsWithMessages", receivedMessages.size());
        
        Map<String, Integer> topicCounts = new HashMap<>();
        receivedMessages.forEach((topic, messages) -> topicCounts.put(topic, messages.size()));
        result.put("messageCountsByTopic", topicCounts);
        
        return result;
    }

    /**
     * 清除接收到的消息
     */
    @PostMapping("/clear")
    public Map<String, Object> clearMessages() {
        receivedMessages.clear();
        log.info("🗑️ Cleared all received messages");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "All received messages cleared");
        result.put("timestamp", LocalDateTime.now().format(formatter));
        
        return result;
    }

    /**
     * 发送统计测试数据
     */
    @PostMapping("/send-statistics")
    public Map<String, Object> sendStatisticsData(@RequestBody(required = false) Map<String, Object> customData) {
        Map<String, Object> stats = customData != null ? customData : new HashMap<>();
        
        if (stats.isEmpty()) {
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("cpu_usage", Math.random() * 100);
            stats.put("memory_usage", Math.random() * 100);
            stats.put("active_connections", (int) (Math.random() * 100));
            stats.put("requests_per_second", Math.random() * 1000);
        }
        
        long startTime = System.currentTimeMillis();
        String messageId = messageProducer.send("demo.statistics", stats);
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("messageId", messageId);
        result.put("data", stats);
        result.put("sendTime", LocalDateTime.now().format(formatter));
        result.put("topic", "demo.statistics");
        result.put("sendDuration", endTime - startTime + "ms");
        
        log.info("📤 Sent statistics data: {}", stats);
        
        return result;
    }

    private int getQueueSize() {
        try {
            java.lang.reflect.Field field = messageQueue.getClass().getDeclaredField("queueSize");
            field.setAccessible(true);
            Object queue = field.get(messageQueue);
            if (queue instanceof ConcurrentHashMap) {
                return ((ConcurrentHashMap<?, ?>) queue).size();
            }
        } catch (Exception e) {
            log.debug("Failed to get queue size", e);
        }
        return 0;
    }

    private int getDelayedQueueSize() {
        try {
            java.lang.reflect.Field field = messageQueue.getClass().getDeclaredField("delayedQueueSize");
            field.setAccessible(true);
            Object size = field.get(messageQueue);
            if (size instanceof AtomicLong) {
                return (int) ((AtomicLong) size).get();
            }
        } catch (Exception e) {
            log.debug("Failed to get delayed queue size", e);
        }
        return 0;
    }

    @Data
    public static class ReceivedMessage {
        private String messageId;
        private String topic;
        private String payload;
        private String timestamp;
        private String threadName;
    }
}
