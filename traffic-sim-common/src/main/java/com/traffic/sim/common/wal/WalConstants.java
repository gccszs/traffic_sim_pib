package com.traffic.sim.common.wal;

public interface WalConstants {
    /** WAL 文件魔数，用于识别 WAL 格式文件 */
    int MAGIC = 0x57414C31;

    /** 魔数字节数组，用于快速写入 */
    byte[] MAGIC_BYTES = new byte[] {0x57, 0x41, 0x4C, 0x31};

    /** 消息头大小：Magic(4) + CRC(4) + Offset(8) + Timestamp(8) + Length(4) = 28 字节 */
    int HEADER_SIZE = 28;

    /** 魔数占用字节数 */
    int MAGIC_SIZE = 4;

    /** CRC 校验和占用字节数 */
    int CRC_SIZE = 4;

    /** 消息偏移量占用字节数 */
    int OFFSET_SIZE = 8;

    /** 时间戳占用字节数 */
    int TIMESTAMP_SIZE = 8;

    /** 消息长度占用字节数 */
    int LENGTH_SIZE = 4;

    /** 稀疏索引间隔：每 1000 条消息建立一个索引项 */
    int INDEX_INTERVAL = 1000;

    /** 默认日志段大小：100MB */
    long DEFAULT_SEGMENT_SIZE = 1024 * 1024 * 100L;

    /** 默认批量刷盘阈值 */
    int DEFAULT_BATCH_SIZE = 100;

    /** 默认刷盘时间间隔（毫秒） */
    long DEFAULT_FLUSH_INTERVAL_MS = 100;

    /** 日志文件后缀 */
    String LOG_FILE_SUFFIX = ".log";

    /** 索引文件后缀 */
    String INDEX_FILE_SUFFIX = ".index";

    /** 偏移量文件后缀 */
    String OFFSET_FILE_SUFFIX = ".offset";
}
