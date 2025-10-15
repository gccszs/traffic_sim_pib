package edu.uestc.iscssl.itsbackend.utils;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

/**
 * 邮件工具类
 */
public class MailUtil {

    private static final String CODE = "0123456789";

    private static final Random RANDOM = new SecureRandom();

    public static String getCode() {
        char[] chars = new char[6];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = CODE.charAt(RANDOM.nextInt(chars.length));
        }
        return new String(chars);
    }

    /**
     *计算两个日期的分钟差
     */
    public static int getMinute(Date fromDate, Date toDate) {
        return (int) (toDate.getTime() - fromDate.getTime()) / (60 * 1000);
    }

}
