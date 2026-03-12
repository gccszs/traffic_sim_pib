package com.traffic.sim.common.mq.config;

import com.traffic.sim.common.mq.MemoryMessageQueue;
import com.traffic.sim.common.mq.MessageQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageQueueConfig {

    @Bean
    @ConditionalOnMissingBean
    public MessageQueue messageQueue(MessageQueueProperties properties) {
        MemoryMessageQueue queue = new MemoryMessageQueue(properties.getQueueCapacity());
        return queue;
    }
}
