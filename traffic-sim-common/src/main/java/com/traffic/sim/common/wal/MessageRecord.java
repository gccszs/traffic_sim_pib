package com.traffic.sim.common.wal;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * WAL 日志消息记录
 * <p>
 * 磁盘存储格式：
 * | Magic(4) | CRC(4) | Offset(8) | Timestamp(8) | Length(4) | Payload(N) |
 * <p>
 * CRC32计算范围：Offset + Timestamp + Length + Payload
 * CRC32存储格式：4字节（int），内存中使用long（通过&amp;0xFFFFFFFFL转换）
 */
public class MessageRecord {

    /** 消息全局偏移量，用于唯一标识和检索 */
    private long offset;

    /** 消息时间戳（毫秒） */
    private long timestamp;

    /** 消息载荷 */
    private byte[] payload;

    /** CRC32 校验和（transient，不参与序列化），内存中使用long类型 */
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
     * 计算CRC32校验和
     * <p>
     * 计算范围：Offset + Timestamp + Length + Payload
     * 不包含：Magic（全局常量）、CRC自身（校验和本身）
     */
    public long computeCrc() {
        CRC32 crc32 = new CRC32();
        ByteBuffer header = ByteBuffer.allocate(
            WalConstants.OFFSET_SIZE +
            WalConstants.TIMESTAMP_SIZE +
            WalConstants.LENGTH_SIZE
        );
        header.putLong(offset);
        header.putLong(timestamp);
        header.putInt(payload.length);
        header.flip();
        crc32.update(header);
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
     * <p>
     * 序列化顺序：Magic(4) -> CRC(4) -> Offset(8) -> Timestamp(8) -> Length(4) -> Payload
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(getTotalSize());

        buffer.putInt(WalConstants.MAGIC);
        buffer.putInt((int) crc);
        buffer.putLong(offset);
        buffer.putLong(timestamp);
        buffer.putInt(payload.length);
        buffer.put(payload);

        buffer.flip();
        return buffer;
    }

    /**
     * 从 ByteBuffer 反序列化（用于从磁盘读取）
     * <p>
     * CRC读取：4字节int转8字节long（&amp;0xFFFFFFFFL避免符号扩展）
     */
    public static MessageRecord fromByteBuffer(ByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != WalConstants.MAGIC) {
            throw new IllegalStateException("Invalid WAL file magic: " + Integer.toHexString(magic));
        }

        MessageRecord record = new MessageRecord();
        record.crc = buffer.getInt() & 0xFFFFFFFFL;
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
