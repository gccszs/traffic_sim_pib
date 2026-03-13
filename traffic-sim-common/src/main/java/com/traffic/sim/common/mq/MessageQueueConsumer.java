package com.traffic.sim.common.mq;

import java.util.function.Consumer;

/**
 * 消息消费者接口
 * 定义消息消费者的基本行为，包括消费成功和失败的回调处理
 * @param <T> 消息载荷类型
 */
public interface MessageQueueConsumer<T> {

    /**
     * 获取消费者ID
     * @return 消费者唯一标识
     */
    String getConsumerId();

    /**
     * 获取订阅的主题
     * @return 主题名称
     */
    String getTopic();

    /**
     * 消费消息
     * @param message 消息对象
     */
    void consume(Message<T> message);

    /**
     * 消费成功回调（可选实现）
     * @param message 消息对象
     */
    default void onSuccess(Message<T> message) {
    }

    /**
     * 消费失败回调（可选实现）
     * @param message 消息对象
     * @param throwable 异常信息
     */
    default void onFailure(Message<T> message, Throwable throwable) {
    }

    /**
     * 创建一个简单的消息消费者
     * @param consumerId 消费者ID
     * @param topic 主题名称
     * @param handler 消息处理函数
     * @param <T> 消息载荷类型
     * @return 消息消费者实例
     */
    static <T> MessageQueueConsumer<T> of(String consumerId, String topic, Consumer<Message<T>> handler) {
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
