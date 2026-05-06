package com.project.cargps.util;

public class TimeUtils {

    /**
     * 判断两个时间戳是否相差超过一分钟
     */
    public static boolean isMoreThanOneMinute(long time1, long time2) {
        return Math.abs(time1 - time2) > 60000;
    }

    /**
     * 判断与当前时间是否相差超过一分钟
     */
    public static boolean isMoreThanOneMinuteFromNow(long timestamp) {
        return Math.abs(System.currentTimeMillis() - timestamp) > 60000;
    }

    /**
     * 获取时间差（毫秒）
     */
    public static long getTimeDifference(long time1, long time2) {
        return Math.abs(time1 - time2);
    }

    /**
     * 获取时间差描述
     */
    public static String getTimeDifferenceDesc(long time1, long time2) {
        long diff = getTimeDifference(time1, time2);
        if (diff < 1000) {
            return diff + "毫秒";
        } else if (diff < 60000) {
            return (diff / 1000) + "秒";
        } else {
            return (diff / 60000) + "分钟";
        }
    }
}
