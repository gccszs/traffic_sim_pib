package com.traffic.sim.common.wal;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WAL 写入器
 * <p>
 * 负责将消息写入 WAL 日志，支持：
 * - 多主题管理（每个主题对应一个日志段）
 * - 批量刷盘策略（积攒 N 条或 M 毫秒后刷盘）
 * - 异步刷盘（避免阻塞主线程）
 */
public class WALWriter {

    /** WAL 数据目录 */
    private final File walDirectory;

    /** 单个日志段大小 */
    private final long segmentSize;

    /** 异步刷盘线程池 */
    private final ExecutorService flushExecutor;

    /** 当前活跃的日志段（按主题名管理） */
    private final ConcurrentHashMap<String, LogSegment> activeSegments;

    /** 全局下一个可用的偏移量 */
    private final AtomicLong globalNextOffset;

    /** 批量刷盘阈值 */
    private final int batchSize;

    /** 刷盘时间间隔（毫秒） */
    private final long flushIntervalMs;

    /** 自上次刷盘以来的消息数量 */
    private final AtomicLong messagesSinceLastFlush;

    /** 上次刷盘时间 */
    private volatile long lastFlushTime;

    /**
     * 创建 WAL 写入器（默认配置）
     *
     * @param walDirectory WAL 数据目录
     */
    public WALWriter(File walDirectory) {
        this(walDirectory, WalConstants.DEFAULT_SEGMENT_SIZE);
    }

    /**
     * 创建 WAL 写入器（指定段大小）
     */
    public WALWriter(File walDirectory, long segmentSize) {
        this(walDirectory, segmentSize, 100, 100);
    }

    /**
     * 创建 WAL 写入器（完整配置）
     */
    public WALWriter(File walDirectory, long segmentSize, int batchSize, long flushIntervalMs) {
        this.walDirectory = walDirectory;
        this.segmentSize = segmentSize;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.activeSegments = new ConcurrentHashMap<>();
        this.globalNextOffset = new AtomicLong(0);
        this.messagesSinceLastFlush = new AtomicLong(0);
        this.lastFlushTime = System.currentTimeMillis();
        this.flushExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "wal-flush");
            t.setDaemon(true);
            return t;
        });

        if (!walDirectory.exists()) {
            walDirectory.mkdirs();
        }
    }

    /**
     * 写入消息（使用当前时间戳）
     *
     * @param topic   主题
     * @param payload 消息载荷
     * @return 消息偏移量
     */
    public long write(String topic, byte[] payload) throws IOException {
        return write(topic, payload, System.currentTimeMillis());
    }

    /**
     * 写入消息（指定时间戳）
     *
     * @param topic     主题
     * @param payload   消息载荷
     * @param timestamp 时间戳
     * @return 消息偏移量
     */
    public long write(String topic, byte[] payload, long timestamp) throws IOException {
        String segmentKey = getSegmentKey(topic);
        LogSegment segment = activeSegments.get(segmentKey);

        if (segment == null || segment.isFull()) {
            segment = createNewSegment(topic);
        }

        MessageRecord record = new MessageRecord(0, timestamp, payload);
        long offset = segment.write(record);

        messagesSinceLastFlush.incrementAndGet();
        checkAndFlush();

        return offset;
    }

    /**
     * 获取日志段键（目前使用主题名）
     */
    private String getSegmentKey(String topic) {
        return topic;
    }

    /**
     * 创建新的日志段
     */
    private LogSegment createNewSegment(String topic) throws IOException {
        long baseOffset = globalNextOffset.get();
        LogSegment segment = new LogSegment(walDirectory, baseOffset, segmentSize);

        LogSegment old = activeSegments.put(topic, segment);
        if (old != null) {
            old.close();
        }

        return segment;
    }

    /**
     * 检查是否需要刷盘，并触发异步刷盘
     */
    private void checkAndFlush() {
        boolean shouldFlush = messagesSinceLastFlush.get() >= batchSize ||
                (System.currentTimeMillis() - lastFlushTime) >= flushIntervalMs;

        if (shouldFlush) {
            flushExecutor.execute(() -> {
                try {
                    flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 强制刷盘，将所有待写入数据同步到磁盘
     */
    public void flush() throws IOException {
        for (LogSegment segment : activeSegments.values()) {
            segment.flush();
        }
        messagesSinceLastFlush.set(0);
        lastFlushTime = System.currentTimeMillis();
    }

    /**
     * 关闭写入器，释放资源
     */
    public void close() throws IOException {
        flush();
        for (LogSegment segment : activeSegments.values()) {
            segment.close();
        }
        activeSegments.clear();
        flushExecutor.shutdown();
    }

    /**
     * 删除所有 WAL 数据
     */
    public void delete() throws IOException {
        close();
        File[] files = walDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        walDirectory.delete();
    }

    public long getGlobalNextOffset() {
        return globalNextOffset.get();
    }

    public void setGlobalNextOffset(long offset) {
        globalNextOffset.set(offset);
    }
}
