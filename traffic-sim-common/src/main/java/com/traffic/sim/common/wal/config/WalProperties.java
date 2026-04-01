package com.traffic.sim.common.wal.config;

import com.traffic.sim.common.wal.WalConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WAL 模块配置属性
 * <p>
 * 可在 application.yml 中通过 wal.* 前缀配置
 */
@ConfigurationProperties(prefix = "wal")
public class WalProperties {

    /** 是否启用 WAL */
    private boolean enabled = true;

    /** WAL 数据存储目录 */
    private String directory = "./wal-data";

    /** 单个日志段大小 */
    private long segmentSize = 0;

    /** 批量刷盘阈值 */
    private int batchSize = 0;

    /** 刷盘时间间隔（毫秒） */
    private long flushIntervalMs = 0;

    /** 稀疏索引间隔 */
    private int indexInterval = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public long getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public int getIndexInterval() {
        return indexInterval;
    }

    public void setIndexInterval(int indexInterval) {
        this.indexInterval = indexInterval;
    }

    public long getResolvedSegmentSize() {
        return segmentSize > 0 ? segmentSize : WalConstants.DEFAULT_SEGMENT_SIZE;
    }

    public int getResolvedBatchSize() {
        return batchSize > 0 ? batchSize : WalConstants.DEFAULT_BATCH_SIZE;
    }

    public long getResolvedFlushIntervalMs() {
        return flushIntervalMs > 0 ? flushIntervalMs : WalConstants.DEFAULT_FLUSH_INTERVAL_MS;
    }

    public int getResolvedIndexInterval() {
        return indexInterval > 0 ? indexInterval : WalConstants.INDEX_INTERVAL;
    }
}
