package com.traffic.sim.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String topic;

    private T payload;

    private long timestamp;

    private MessageType type;

    private int retryCount;

    private String correlationId;

    public enum MessageType {
        NORMAL,
        DELAYED,
        BROADCAST
    }

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

    public boolean isDelayed() {
        return MessageType.DELAYED.equals(type) && timestamp > System.currentTimeMillis();
    }

    public boolean isReady() {
        return timestamp <= System.currentTimeMillis();
    }
}
