package com.traffic.sim.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 消息实体类
 * @param <T> 消息载荷类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * 创建普通消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @return 消息对象
     */
    public static <T> Message<T> of(String topic, T payload) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .type(MessageType.NORMAL)
                .retryCount(0)
                .build();
    }

    /**
     * 创建延迟消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param delayMillis 延迟毫秒数
     * @return 消息对象
     */
    public static <T> Message<T> delayed(String topic, T payload, long delayMillis) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .timestamp(System.currentTimeMillis() + delayMillis)
                .type(MessageType.DELAYED)
                .retryCount(0)
                .build();
    }

    /**
     * 创建广播消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @return 消息对象
     */
    public static <T> Message<T> broadcast(String topic, T payload) {
        return Message.<T>builder()
                .id(UUID.randomUUID().toString())
                .topic(topic)
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .type(MessageType.BROADCAST)
                .retryCount(0)
                .build();
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
