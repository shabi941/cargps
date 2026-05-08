package com.project.cargps.util;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

/**
 * GPS数据过滤器 - 用于过滤漂移、跳变、精度不足的GPS数据
 * 解决经纬度乱穿/乱跳问题
 */
public class GpsFilter {

    // 中国有效经纬度范围
    private static final double CHINA_MIN_LAT = 3.0;   // 最南端约3°N
    private static final double CHINA_MAX_LAT = 54.0;   // 最北端约54°N
    private static final double CHINA_MIN_LON = 73.0;   // 最西端约73°E
    private static final double CHINA_MAX_LON = 135.0;  // 最东端约135°E

    // 过滤阈值
    private static final float MAX_ACCURACY_METERS = 100.0f;   // GPS精度超过100米视为不可靠
    private static final double MAX_JUMP_DISTANCE_METERS = 500.0; // 相邻点超过500米视为跳变（约180km/h的1秒移动）
    private static final float MAX_SPEED_KMH = 300.0f;          // 超过300km/h视为异常（物理不可能）

    /**
     * 检查经纬度是否在中国有效范围内
     */
    public static boolean isValidChinaRange(double latitude, double longitude) {
        return latitude >= CHINA_MIN_LAT && latitude <= CHINA_MAX_LAT
                && longitude >= CHINA_MIN_LON && longitude <= CHINA_MAX_LON;
    }

    /**
     * 检查GPS精度是否可接受
     * @param accuracy 精度值（米），accuracy <= 0 表示不可用
     */
    public static boolean isAccuracyAcceptable(float accuracy) {
        if (accuracy <= 0) {
            return false; // 精度不可用
        }
        return accuracy <= MAX_ACCURACY_METERS;
    }

    /**
     * 计算两点之间的距离（米）
     */
    public static float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        LatLng p1 = new LatLng(lat1, lon1);
        LatLng p2 = new LatLng(lat2, lon2);
        return AMapUtils.calculateLineDistance(p1, p2);
    }

    /**
     * 检查从上一点到新点的跳变是否过大
     * @return true = 跳变过大（应丢弃），false = 跳变正常
     */
    public static boolean isJumpTooLarge(double prevLat, double prevLon,
                                         double currLat, double currLon) {
        float distance = calculateDistance(prevLat, prevLon, currLat, currLon);
        return distance > MAX_JUMP_DISTANCE_METERS;
    }

    /**
     * 检查速度是否物理合理
     * @param speedKmh 速度（km/h）
     */
    public static boolean isSpeedReasonable(double speedKmh) {
        return speedKmh >= 0 && speedKmh <= MAX_SPEED_KMH;
    }

    /**
     * 综合过滤：判断AMapLocation是否应被过滤掉
     *
     * @param location  当前定位结果
     * @param prevLat  上一次有效纬度（首次可为0）
     * @param prevLon  上一次有效经度（首次可为0）
     * @param hasPrev  是否有上一次有效坐标
     * @return true = 应过滤（丢弃），false = 有效数据
     */
    public static boolean shouldFilter(AMapLocation location,
                                      double prevLat, double prevLon,
                                      boolean hasPrev) {
        if (location == null) {
            return true;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float accuracy = location.getAccuracy();
        float speedMs = location.getSpeed(); // 米/秒
        double speedKmh = speedMs * 3.6;

        // 1. 基础有效性检查：经纬度不能为0或负数
        if (lat <= 0 || lon <= 0) {
            return true;
        }

        // 2. 中国范围检查
        if (!isValidChinaRange(lat, lon)) {
            return true;
        }

        // 3. GPS精度检查
        if (!isAccuracyAcceptable(accuracy)) {
            return true;
        }

        // 4. 速度合理性检查
        if (!isSpeedReasonable(speedKmh)) {
            return true;
        }

        // 5. 跳变检测（仅当有历史数据时）
        if (hasPrev && prevLat != 0 && prevLon != 0) {
            if (isJumpTooLarge(prevLat, prevLon, lat, lon)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成过滤原因描述（用于日志）
     */
    public static String getFilterReason(AMapLocation location,
                                        double prevLat, double prevLon,
                                        boolean hasPrev) {
        if (location == null) {
            return "location为null";
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float accuracy = location.getAccuracy();
        double speedKmh = location.getSpeed() * 3.6;

        if (lat <= 0 || lon <= 0) {
            return String.format("经纬度<=0: lat=%.6f, lon=%.6f", lat, lon);
        }
        if (!isValidChinaRange(lat, lon)) {
            return String.format("超出中国范围: lat=%.6f, lon=%.6f", lat, lon);
        }
        if (!isAccuracyAcceptable(accuracy)) {
            return String.format("GPS精度不足: accuracy=%.1fm", accuracy);
        }
        if (!isSpeedReasonable(speedKmh)) {
            return String.format("速度异常: %.1f km/h", speedKmh);
        }
        if (hasPrev && prevLat != 0 && prevLon != 0) {
            if (isJumpTooLarge(prevLat, prevLon, lat, lon)) {
                float jumpDist = calculateDistance(prevLat, prevLon, lat, lon);
                return String.format("跳变过大: 距离=%.1fm, 上点(%.6f,%.6f), 新点(%.6f,%.6f)",
                        jumpDist, prevLat, prevLon, lat, lon);
            }
        }
        return "有效数据";
    }
}
