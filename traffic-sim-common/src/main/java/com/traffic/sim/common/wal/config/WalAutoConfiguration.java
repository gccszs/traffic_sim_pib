package com.traffic.sim.common.wal.config;

import com.traffic.sim.common.wal.ConsumerOffsetManager;
import com.traffic.sim.common.wal.WALReader;
import com.traffic.sim.common.wal.WALWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.File;

/**
 * WAL 模块自动配置
 * <p>
 * 当 wal.enabled=true 时自动配置 WAL 相关的 Bean
 */
@AutoConfiguration
@EnableConfigurationProperties(WalProperties.class)
@ConditionalOnProperty(prefix = "wal", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WalAutoConfiguration {

    /**
     * 配置 WALWriter Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public WALWriter walWriter(WalProperties properties) {
        File walDirectory = new File(properties.getDirectory());
        return new WALWriter(walDirectory, properties.getResolvedSegmentSize(),
                properties.getResolvedBatchSize(), properties.getResolvedFlushIntervalMs());
    }

    /**
     * 配置 WALReader Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public WALReader walReader(WalProperties properties) {
        File walDirectory = new File(properties.getDirectory());
        return new WALReader(walDirectory);
    }

    /**
     * 配置 ConsumerOffsetManager Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public ConsumerOffsetManager consumerOffsetManager(WalProperties properties) {
        File baseDirectory = new File(properties.getDirectory());
        return new ConsumerOffsetManager(baseDirectory);
    }
}
