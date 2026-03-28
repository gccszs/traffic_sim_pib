package com.traffic.sim.common.mq;

import java.io.Serializable;
import java.util.UUID;

/**
 * 消息实体类
 * @param <T> 消息载荷类型
 */
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息唯一标识 */
    private String id;

    /** 消息主题 */
    private String topic;

    /** 消息载荷 */
    private T payload;

    /** 消息时间戳（延迟消息表示可消费时间） */
    private long timestamp;

    /** 消息类型 */
    private MessageType type;

    /** 重试次数 */
    private int retryCount;

    /** 关联ID，用于消息追踪 */
    private String correlationId;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /** 普通消息 */
        NORMAL,
        /** 延迟消息 */
        DELAYED,
        /** 广播消息 */
        BROADCAST
    }

    // 无参构造函数
    public Message() {
    }

    // 全参构造函数
    public Message(String id, String topic, T payload, long timestamp, MessageType type, int retryCount, String correlationId) {
        this.id = id;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = timestamp;
        this.type = type;
        this.retryCount = retryCount;
        this.correlationId = correlationId;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * 创建普通消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @return 消息对象
     */
    public static <T> Message<T> of(String topic, T payload) {
        Message<T> message = new Message<>();
        message.setId(UUID.randomUUID().toString());
        message.setTopic(topic);
        message.setPayload(payload);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(MessageType.NORMAL);
        message.setRetryCount(0);
        return message;
    }

    /**
     * 创建延迟消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param delayMillis 延迟毫秒数
     * @return 消息对象
     */
    public static <T> Message<T> delayed(String topic, T payload, long delayMillis) {
        Message<T> message = new Message<>();
        message.setId(UUID.randomUUID().toString());
        message.setTopic(topic);
        message.setPayload(payload);
        message.setTimestamp(System.currentTimeMillis() + delayMillis);
        message.setType(MessageType.DELAYED);
        message.setRetryCount(0);
        return message;
    }

    /**
     * 创建广播消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @return 消息对象
     */
    public static <T> Message<T> broadcast(String topic, T payload) {
        Message<T> message = new Message<>();
        message.setId(UUID.randomUUID().toString());
        message.setTopic(topic);
        message.setPayload(payload);
        message.setTimestamp(System.currentTimeMillis());
        message.setType(MessageType.BROADCAST);
        message.setRetryCount(0);
        return message;
    }

    /**
     * 判断是否为延迟消息且尚未到达可消费时间
     * @return true表示需要延迟处理
     */
    public boolean isDelayed() {
        return MessageType.DELAYED.equals(type) && timestamp > System.currentTimeMillis();
    }

    /**
     * 判断消息是否到达可消费时间
     * @return true表示可以消费
     */
    public boolean isReady() {
        return timestamp <= System.currentTimeMillis();
    }
}
