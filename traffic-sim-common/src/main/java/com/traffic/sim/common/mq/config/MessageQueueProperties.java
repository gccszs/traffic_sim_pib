package com.traffic.sim.common.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息队列配置属性
 * 用于配置消息队列的各项参数
 */
@Data
@ConfigurationProperties(prefix = "mq")
public class MessageQueueProperties {

    /** 是否启用消息队列，默认true */
    private boolean enabled = true;

    /** 默认队列容量，默认10000，0表示无限制 */
    private int queueCapacity = 10000;

    /** 消费者线程数，默认4 */
    private int consumerThreads = 4;

    /** 是否启用延迟消息，默认true */
    private boolean delayedMessageEnabled = true;
}
