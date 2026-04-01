package com.traffic.sim.common.wal;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * WAL 日志消息记录
 * <p>
 * 磁盘存储格式：
 * | Magic(4) | CRC(4) | Offset(8) | Timestamp(8) | Length(4) | Payload(N) |
 */
public class MessageRecord {

    /** 消息全局偏移量，用于唯一标识和检索 */
    private long offset;

    /** 消息时间戳（毫秒） */
    private long timestamp;

    /** 消息载荷 */
    private byte[] payload;

    /** CRC32 校验和（transient，不参与序列化） */
    private transient long crc;

    public MessageRecord() {
    }

    public MessageRecord(long offset, long timestamp, byte[] payload) {
        this.offset = offset;
        this.timestamp = timestamp;
        this.payload = payload;
        this.crc = computeCrc();
    }

    /**
     * 计算载荷的 CRC32 校验和
     */
    public long computeCrc() {
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        return crc32.getValue();
    }

    /**
     * 验证 CRC 校验和是否正确
     */
    public boolean verifyCrc() {
        return crc == computeCrc();
    }

    /**
     * 获取消息头大小
     */
    public int getHeaderSize() {
        return WalConstants.HEADER_SIZE;
    }

    /**
     * 获取消息总大小（头 + 载荷）
     */
    public int getTotalSize() {
        return getHeaderSize() + payload.length;
    }

    /**
     * 将消息序列化为 ByteBuffer（用于写入磁盘）
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(getTotalSize());

        buffer.putInt(WalConstants.MAGIC);
        buffer.putLong(crc);
        buffer.putLong(offset);
        buffer.putLong(timestamp);
        buffer.putInt(payload.length);
        buffer.put(payload);

        buffer.flip();
        return buffer;
    }

    /**
     * 从 ByteBuffer 反序列化（用于从磁盘读取）
     */
    public static MessageRecord fromByteBuffer(ByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != WalConstants.MAGIC) {
            throw new IllegalStateException("Invalid WAL file magic: " + Integer.toHexString(magic));
        }

        MessageRecord record = new MessageRecord();
        record.crc = buffer.getLong();
        record.offset = buffer.getLong();
        record.timestamp = buffer.getLong();

        int length = buffer.getInt();
        record.payload = new byte[length];
        buffer.get(record.payload);

        if (!record.verifyCrc()) {
            throw new IllegalStateException("CRC check failed at offset " + record.offset);
        }

        return record;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public long getCrc() {
        return crc;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }
}
