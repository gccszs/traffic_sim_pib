package com.traffic.sim.common.wal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WAL 日志段管理
 * <p>
 * 管理单个日志段（.log 文件）和对应的索引文件（.index 文件）
 * 支持追加写入、稀疏索引、快速读取
 */
public class LogSegment {

    /** 日志文件 */
    private final File logFile;

    /** 索引文件 */
    private final File indexFile;

    /** 日志段最大大小 */
    private final long segmentSize;

    /** 日志文件随机访问 */
    private final RandomAccessFile logRaf;

    /** 日志文件通道 */
    private final FileChannel logChannel;

    /** 索引文件内存映射 */
    private final MappedByteBuffer indexBuffer;

    /** 日志段起始偏移量 */
    private final AtomicLong baseOffset;

    /** 下一个可用的偏移量 */
    private final AtomicLong nextOffset;

    /** 当前写入位置 */
    private final AtomicLong writePosition;

    /** 已写入消息数量 */
    private final AtomicLong messagesWritten;

    /**
     * 创建日志段
     *
     * @param directory  目录
     * @param baseOffset 起始偏移量
     * @param segmentSize 日志段大小
     */
    public LogSegment(File directory, long baseOffset, long segmentSize) throws IOException {
        this.segmentSize = segmentSize;
        this.baseOffset = new AtomicLong(baseOffset);
        this.nextOffset = new AtomicLong(baseOffset);
        this.writePosition = new AtomicLong(0);
        this.messagesWritten = new AtomicLong(0);

        String baseName = String.format("%016d", baseOffset);
        this.logFile = new File(directory, baseName + WalConstants.LOG_FILE_SUFFIX);
        this.indexFile = new File(directory, baseName + WalConstants.INDEX_FILE_SUFFIX);

        if (!directory.exists()) {
            Files.createDirectories(directory);
        }

        this.logRaf = new RandomAccessFile(logFile, "rw");
        this.logChannel = logRaf.getChannel();

        long indexSize = WalConstants.INDEX_INTERVAL * (WalConstants.OFFSET_SIZE + WalConstants.OFFSET_SIZE);
        this.indexBuffer = mapIndexFile(indexSize);
    }

    /**
     * 创建索引文件的内存映射
     */
    private MappedByteBuffer mapIndexFile(long size) throws IOException {
        RandomAccessFile indexRaf = new RandomAccessFile(indexFile, "rw");
        FileChannel indexChannel = indexRaf.getChannel();
        return indexChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
    }

    /**
     * 写入消息记录
     *
     * @param record 消息记录
     * @return 写入后的偏移量
     */
    public long write(MessageRecord record) throws IOException {
        if (writePosition.get() + record.getTotalSize() > segmentSize) {
            throw new IllegalStateException("Segment is full");
        }

        record.setOffset(nextOffset.getAndIncrement());

        ByteBuffer buffer = record.toByteBuffer();
        long writtenBytes = logChannel.write(buffer);

        writePosition.addAndGet(writtenBytes);
        long count = messagesWritten.incrementAndGet();

        if (count % WalConstants.INDEX_INTERVAL == 0) {
            writeIndex(record.getOffset(), writePosition.get());
        }

        return record.getOffset();
    }

    /**
     * 写入稀疏索引
     */
    private void writeIndex(long offset, long position) {
        long index = (offset - baseOffset.get()) / WalConstants.INDEX_INTERVAL;
        long pos = index * (WalConstants.OFFSET_SIZE + WalConstants.OFFSET_SIZE);

        if (pos < indexBuffer.limit()) {
            indexBuffer.putLong(pos, offset);
            indexBuffer.putLong(pos + WalConstants.OFFSET_SIZE, position);
        }
    }

    /**
     * 按偏移量读取消息记录
     *
     * @param offset 消息偏移量
     * @return 消息记录，不存在则返回 null
     */
    public MessageRecord read(long offset) throws IOException {
        long relativeOffset = offset - baseOffset.get();
        if (relativeOffset < 0) {
            return null;
        }

        long index = relativeOffset / WalConstants.INDEX_INTERVAL;
        long pos = index * (WalConstants.OFFSET_SIZE + WalConstants.OFFSET_SIZE);

        long filePosition = 0;
        if (pos < indexBuffer.limit()) {
            long indexedOffset = indexBuffer.getLong(pos);
            if (indexedOffset <= offset) {
                filePosition = indexBuffer.getLong(pos + WalConstants.OFFSET_SIZE);
            }
        }

        if (filePosition <= 0) {
            filePosition = 0;
        }

        logChannel.position(filePosition);

        while (logChannel.position() < writePosition.get()) {
            ByteBuffer headerBuffer = ByteBuffer.allocate(WalConstants.HEADER_SIZE);
            int read = logChannel.read(headerBuffer);
            if (read <= 0) {
                break;
            }

            headerBuffer.flip();
            int magic = headerBuffer.getInt();
            if (magic != WalConstants.MAGIC) {
                continue;
            }

            long crc = headerBuffer.getLong();
            long recOffset = headerBuffer.getLong();
            long timestamp = headerBuffer.getLong();
            int length = headerBuffer.getInt();

            if (recOffset == offset) {
                ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
                logChannel.read(payloadBuffer);
                payloadBuffer.flip();

                byte[] payload = new byte[length];
                payloadBuffer.get(payload);

                MessageRecord record = new MessageRecord(offset, timestamp, payload);
                record.setCrc(crc);

                if (!record.verifyCrc()) {
                    throw new IllegalStateException("CRC verification failed at offset " + offset);
                }

                return record;
            } else if (recOffset > offset) {
                break;
            }

            if (logChannel.position() + length < logChannel.size()) {
                logChannel.position(logChannel.position() + length);
            }
        }

        return null;
    }

    /**
     * 强制刷盘，确保数据落盘
     */
    public void flush() throws IOException {
        logChannel.force(true);
    }

    /**
     * 关闭日志段
     */
    public void close() throws IOException {
        flush();
        logChannel.close();
        logRaf.close();
    }

    /**
     * 删除日志段文件
     */
    public void delete() throws IOException {
        close();
        Files.deleteIfExists(logFile);
        Files.deleteIfExists(indexFile);
    }

    /**
     * 判断日志段是否已满
     */
    public boolean isFull() {
        return writePosition.get() + WalConstants.HEADER_SIZE >= segmentSize;
    }

    public long getBaseOffset() {
        return baseOffset.get();
    }

    public long getNextOffset() {
        return nextOffset.get();
    }

    public long getWritePosition() {
        return writePosition.get();
    }

    public long getMessagesWritten() {
        return messagesWritten.get();
    }

    public File getLogFile() {
        return logFile;
    }

    public File getIndexFile() {
        return indexFile;
    }
}
