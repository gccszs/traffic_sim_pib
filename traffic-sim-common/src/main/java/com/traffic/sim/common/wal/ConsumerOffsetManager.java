package com.traffic.sim.common.wal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消费偏移量管理器
 * <p>
 * 负责管理消费者组的消费进度（偏移量），支持：
 * - 内存缓存消费偏移量
 * - 定时异步持久化到文件
 * - 启动时从文件恢复偏移量
 */
public class ConsumerOffsetManager {

    /** 偏移量文件目录 */
    private final File offsetDirectory;

    /** 内存缓存：key = topic_group, value = 偏移量 */
    private final ConcurrentHashMap<String, AtomicLong> offsets;

    /** 待刷盘的脏标记 */
    private final ConcurrentHashMap<String, Long> dirtyFlags;

    /** 定时刷盘线程池 */
    private final ScheduledExecutorService flushExecutor;

    /**
     * 创建消费偏移量管理器
     *
     * @param baseDirectory 基础目录（会在其下创建 offsets 目录）
     */
    public ConsumerOffsetManager(File baseDirectory) {
        this.offsetDirectory = new File(baseDirectory, "offsets");
        this.offsets = new ConcurrentHashMap<>();
        this.dirtyFlags = new ConcurrentHashMap<>();
        this.flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "offset-flush");
            t.setDaemon(true);
            return t;
        });

        if (!offsetDirectory.exists()) {
            offsetDirectory.mkdirs();
        }

        startFlushScheduler();
    }

    /**
     * 启动定时刷盘调度器（每 5 秒刷盘一次）
     */
    private void startFlushScheduler() {
        flushExecutor.scheduleAtFixedRate(() -> {
            try {
                flushAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 提交消费偏移量
     *
     * @param topic 主题
     * @param group 消费者组
     * @param offset 消费偏移量
     */
    public void commitOffset(String topic, String group, long offset) {
        String key = buildKey(topic, group);
        AtomicLong current = offsets.get(key);
        if (current == null) {
            current = new AtomicLong(offset);
            AtomicLong old = offsets.putIfAbsent(key, current);
            if (old != null) {
                current = old;
            }
        }

        long prev = current.getAndSet(offset);
        if (offset > prev) {
            dirtyFlags.put(key, offset);
        }
    }

    /**
     * 获取已提交的偏移量
     */
    public long getOffset(String topic, String group) {
        String key = buildKey(topic, group);
        AtomicLong offset = offsets.get(key);
        return offset != null ? offset.get() : 0;
    }

    /**
     * 获取下次消费的起始位置（与 getOffset 等价）
     */
    public long getNextOffset(String topic, String group) {
        return getOffset(topic, group);
    }

    /**
     * 构建缓存 key
     */
    private String buildKey(String topic, String group) {
        return topic + "_" + group;
    }

    /**
     * 构建偏移量文件名
     */
    private String buildFileName(String topic, String group) {
        return topic + "." + group + ".offset";
    }

    /**
     * 刷盘所有待刷新的偏移量
     */
    public void flushAll() throws IOException {
        for (String key : dirtyFlags.keySet()) {
            try {
                flush(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将指定 key 的偏移量刷盘到文件
     */
    private void flush(String key) throws IOException {
        Long offset = dirtyFlags.get(key);
        if (offset == null) {
            return;
        }

        String[] parts = key.split("_", 2);
        if (parts.length != 2) {
            return;
        }

        String topic = parts[0];
        String group = parts[1];

        File offsetFile = new File(offsetDirectory, buildFileName(topic, group));

        try (RandomAccessFile raf = new RandomAccessFile(offsetFile, "rw");
             FileChannel channel = raf.getChannel()) {

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 64);
            buffer.putLong(offset);
            buffer.force();
        }

        dirtyFlags.remove(key);
    }

    /**
     * 从文件加载偏移量
     *
     * @param topic 主题
     * @param group 消费者组
     */
    public void loadFromFile(String topic, String group) {
        String key = buildKey(topic, group);
        File offsetFile = new File(offsetDirectory, buildFileName(topic, group));

        if (!offsetFile.exists()) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(offsetFile, "r");
             FileChannel channel = raf.getChannel()) {

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 8);
            long offset = buffer.getLong();
            offsets.put(key, new AtomicLong(offset));
        } catch (Exception e) {
        }
    }

    /**
     * 关闭管理器，释放资源
     */
    public void close() throws IOException {
        flushAll();
        flushExecutor.shutdown();
    }

    public File getOffsetDirectory() {
        return offsetDirectory;
    }
}
