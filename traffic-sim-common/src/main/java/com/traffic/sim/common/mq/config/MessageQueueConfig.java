package com.traffic.sim.common.mq.config;

import com.traffic.sim.common.mq.DefaultMessageProducer;
import com.traffic.sim.common.mq.MemoryMessageQueue;
import com.traffic.sim.common.mq.MessageProducer;
import com.traffic.sim.common.mq.MessageQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息队列自动配置类
 * 在Spring Boot环境中自动配置消息队列Bean
 */
@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageQueueConfig {

    /**
     * 创建消息队列Bean
     * 如果已存在MessageQueue Bean则不会创建
     * @param properties 消息队列配置属性
     * @return 消息队列实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageQueue messageQueue(MessageQueueProperties properties) {
        MemoryMessageQueue queue = new MemoryMessageQueue(properties.getQueueCapacity());
        return queue;
    }

    /**
     * 创建消息生产者Bean
     * 如果已存在MessageProducer Bean则不会创建
     * @param messageQueue 消息队列实例
     * @return 消息生产者实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageProducer messageProducer(MessageQueue messageQueue) {
        return new DefaultMessageProducer(messageQueue);
    }
}
