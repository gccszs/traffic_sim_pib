package com.traffic.sim.common.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mq")
public class MessageQueueProperties {

    private boolean enabled = true;

    private int queueCapacity = 10000;

    private int consumerThreads = 4;

    private boolean delayedMessageEnabled = true;
}
