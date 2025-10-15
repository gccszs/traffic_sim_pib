package com.simeng.pib.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 */
public class TimeUtils {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_WITH_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * 获取当前时间戳（秒）
     */
    public long getCurrentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    public static long getCurrentTimestampMillis() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * 时间戳转字符串（秒）
     */
    public String timestampToString(long timestampSeconds) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestampSeconds), ZoneId.systemDefault());
        return dateTime.format(FORMATTER);
    }
    
    /**
     * 时间戳转字符串（毫秒）
     */
    public String timestampMillisToString(long timestampMillis) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestampMillis), ZoneId.systemDefault());
        return dateTime.format(FORMATTER_WITH_MILLIS);
    }
    
    /**
     * 字符串转时间戳（秒）
     */
    public long stringToTimestamp(String dateTimeString) {
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, FORMATTER);
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
