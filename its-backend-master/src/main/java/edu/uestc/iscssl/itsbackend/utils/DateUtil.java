package edu.uestc.iscssl.itsbackend.utils;

import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间变化 工具
 */
@Component
public class DateUtil {

    private DateUtil() {

    }

    public final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public final static String UI_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 将时间戳转换为时间
     * @param lt
     * @param pattern
     * @return
     */
    public static String stampToDate(long lt, String pattern){
        if(pattern == null || pattern.equals("")){
            pattern = DEFAULT_DATE_PATTERN;
        }
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    /**
     * 自定义格式的时间字符串转换为时间戳
     * @param date
     * @param pattern
     * @return
     */
    public static long dateToStamp(String date, String pattern){
        if(pattern == null || pattern.equals("")){
            pattern = DEFAULT_DATE_PATTERN;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        long stamp = 0L;
        try {
            stamp = simpleDateFormat.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return stamp;
    }

    /**
     * 根据之前的时间和当前时间判断是否为一天
     * @param update
     * @param nowDay
     * @return
     */
    public static boolean isSameDay(long update, long nowDay){
        double now = (nowDay - update) / (1000 * 60 * 60 * 24.0);
        String updateDayEnd = DateUtil.stampToDate(update, DateUtil.UI_DATE_PATTERN) + " 23:59:59";
        long dayendStamp = DateUtil.dateToStamp(updateDayEnd, DateUtil.DEFAULT_DATE_PATTERN);
        double difference = now - (dayendStamp - update) / (1000 * 60 * 60 * 24.0);
        if(difference > 0) return false;
        return true;
    }

    /**
     * 获取小时
     * @param time
     * @return
     */
    public static int getHourByStamp(long time){
        return getCalendarByStamp(time).get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 根据时间戳获取时间
     * @param time
     * @return
     */
    public static Calendar getCalendarByStamp(long time){
        Calendar calendar = Calendar.getInstance();
        Date date = new Date(time);
        calendar.setTime(date);
        return calendar;
    }

}
