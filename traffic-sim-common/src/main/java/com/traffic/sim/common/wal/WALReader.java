package com.traffic.sim.common.wal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WAL 读取器
 * <p>
 * 负责从 WAL 日志读取消息，支持：
 * - 按偏移量读取单条消息
 * - 批量读取消息
 * - 日志段管理
 */
public class WALReader {

    /** WAL 数据目录 */
    private final File walDirectory;

    /** 主题对应的日志段列表 */
    private final ConcurrentHashMap<String, List<LogSegment>> segmentsMap;

    /**
     * 创建 WAL 读取器
     *
     * @param walDirectory WAL 数据目录
     */
    public WALReader(File walDirectory) {
        this.walDirectory = walDirectory;
        this.segmentsMap = new ConcurrentHashMap<>();
        loadSegments();
    }

    /**
     * 加载目录中的日志段
     */
    private void loadSegments() {
        if (!walDirectory.exists()) {
            walDirectory.mkdirs();
            return;
        }

        File[] files = walDirectory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(WalConstants.LOG_FILE_SUFFIX)) {
                String baseName = file.getName().replace(WalConstants.LOG_FILE_SUFFIX, "");
                long baseOffset = parseOffset(baseName);
                String topic = inferTopic(baseName);
            }
        }
    }

    /**
     * 从文件名解析起始偏移量
     */
    private long parseOffset(String baseName) {
        try {
            return Long.parseLong(baseName, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从文件名推断主题名（暂时简单返回 default）
     */
    private String inferTopic(String baseName) {
        return "default";
    }

    /**
     * 按偏移量读取单条消息
     *
     * @param topic  主题
     * @param offset 消息偏移量
     * @return 消息记录，不存在则返回 null
     */
    public MessageRecord read(String topic, long offset) throws IOException {
        List<LogSegment> segments = segmentsMap.get(topic);
        if (segments == null || segments.isEmpty()) {
            return null;
        }

        for (LogSegment segment : segments) {
            if (offset >= segment.getBaseOffset() && offset < segment.getNextOffset()) {
                return segment.read(offset);
            }
        }

        return null;
    }

    /**
     * 批量读取消息
     *
     * @param topic      主题
     * @param startOffset 起始偏移量
     * @param count       读取数量
     * @return 消息列表
     */
    public List<MessageRecord> readBatch(String topic, long startOffset, int count) throws IOException {
        List<MessageRecord> results = new ArrayList<>();
        List<LogSegment> segments = segmentsMap.get(topic);

        if (segments == null || segments.isEmpty()) {
            return results;
        }

        long currentOffset = startOffset;
        int read = 0;

        for (LogSegment segment : segments) {
            if (currentOffset >= segment.getNextOffset()) {
                continue;
            }

            while (read < count) {
                MessageRecord record = segment.read(currentOffset);
                if (record == null) {
                    break;
                }
                results.add(record);
                currentOffset++;
                read++;
            }

            if (read >= count) {
                break;
            }
        }

        return results;
    }

    /**
     * 获取主题的下一个可用偏移量
     */
    public long getNextOffset(String topic) {
        List<LogSegment> segments = segmentsMap.get(topic);
        if (segments == null || segments.isEmpty()) {
            return 0;
        }

        LogSegment lastSegment = segments.get(segments.size() - 1);
        return lastSegment.getNextOffset();
    }

    /**
     * 关闭读取器，释放资源
     */
    public void close() throws IOException {
        for (List<LogSegment> segments : segmentsMap.values()) {
            for (LogSegment segment : segments) {
                segment.close();
            }
        }
        segmentsMap.clear();
    }

    public File getWalDirectory() {
        return walDirectory;
    }
}
