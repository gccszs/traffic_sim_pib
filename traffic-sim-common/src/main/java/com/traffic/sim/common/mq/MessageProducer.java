package com.traffic.sim.common.mq;

/**
 * 消息生产者接口
 * 提供发送消息、延迟消息、广播消息等功能
 */
public interface MessageProducer {

    /**
     * 发送消息到指定主题
     * @param topic 主题名称
     * @param payload 消息载荷
     * @return 消息ID
     */
    <T> String send(String topic, T payload);

    /**
     * 发送消息到指定主题，并携带回调
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param callback 发送回调
     * @return 消息ID
     */
    <T> String send(String topic, T payload, SendCallback callback);

    /**
     * 发送延迟消息
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param delayMillis 延迟毫秒数
     * @return 消息ID
     */
    <T> String sendDelayed(String topic, T payload, long delayMillis);

    /**
     * 发送延迟消息，并携带回调
     * @param topic 主题名称
     * @param payload 消息载荷
     * @param delayMillis 延迟毫秒数
     * @param callback 发送回调
     * @return 消息ID
     */
    <T> String sendDelayed(String topic, T payload, long delayMillis, SendCallback callback);

    /**
     * 广播消息到指定主题，所有订阅者都会收到
     * @param topic 主题名称
     * @param payload 消息载荷
     */
    <T> void broadcast(String topic, T payload);

    /**
     * 关闭生产者，释放资源
     */
    void shutdown();

    /**
     * 发送回调接口
     */
    interface SendCallback {
        /**
         * 发送成功回调
         * @param messageId 消息ID
         */
        void onSuccess(String messageId);

        /**
         * 发送失败回调
         * @param messageId 消息ID
         * @param throwable 异常信息
         */
        void onFailure(String messageId, Throwable throwable);
    }
}
