package com.traffic.sim.common.mq;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 消息队列接口，定义消息队列的核心操作
 * 支持主题订阅、消息发布、延迟消息、广播消息等功能
 */
public interface MessageQueue {

    /**
     * 发布消息到指定主题
     * @param topic 主题名称
     * @param payload 消息载荷
     */
    <T> void publish(String topic, T payload);

    /**
     * 发布消息对象到队列
     * @param message 消息对象
     */
    <T> void publish(Message<T> message);

    /**
     * 发布延迟消息，消息将在指定延迟时间后可用
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param delayMillis 延迟毫秒数
     */
    <T> void publishDelayed(String topic, T payload, long delayMillis);

    /**
     * 订阅主题消息，使用单线程消费
     * @param topic 主题名称
     * @param consumer 消息消费者
     */
    <T> void subscribe(String topic, MessageConsumer<T> consumer);

    /**
     * 订阅主题消息，使用指定线程数消费
     * @param topic 主题名称
     * @param consumer 消息消费者
     * @param threads 消费线程数
     */
    <T> void subscribe(String topic, MessageConsumer<T> consumer, int threads);

    /**
     * 取消订阅指定主题的所有消费者
     * @param topic 主题名称
     */
    void unsubscribe(String topic);

    /**
     * 取消订阅指定主题的特定消费者
     * @param topic 主题名称
     * @param consumer 消息消费者
     */
    void unsubscribe(String topic, MessageConsumer<?> consumer);

    /**
     * 获取所有已存在的主题列表
     * @return 主题集合
     */
    Collection<String> getTopics();

    /**
     * 获取指定主题的队列大小（包含普通队列和延迟队列）
     * @param topic 主题名称
     * @return 队列消息数量
     */
    long getQueueSize(String topic);

    /**
     * 清空指定主题的队列
     * @param topic 主题名称
     */
    void clear(String topic);

    /**
     * 清空所有主题的队列
     */
    void clearAll();

    /**
     * 关闭消息队列，释放所有资源
     */
    void shutdown();

    /**
     * 消息消费者函数式接口
     * @param <T> 消息载荷类型
     */
    interface MessageConsumer<T> {
        /**
         * 消费消息
         * @param message 消息对象
         */
        void consume(Message<T> message);
    }

    /**
     * 消息处理器接口，支持手动确认
     * @param <T> 消息载荷类型
     */
    interface MessageProcessor<T> {
        /**
         * 处理消息
         * @param message 消息对象
         * @param ack 确认回调
         * @param nack 拒绝回调
         */
        void process(Message<T> message, Consumer<Message<T>> ack, Consumer<Message<T>> nack);
    }
}
