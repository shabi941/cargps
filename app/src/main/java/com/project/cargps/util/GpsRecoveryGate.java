package com.project.cargps.util;

/**
 * 冷启动或长时间无定位后的恢复点确认器。
 * 第一个点仅作为候选，连续第二个物理可达的点到达后才恢复上传，
 * 防止一次错误首点成为过滤锚点并持续误杀后续正常定位。
 */
public class GpsRecoveryGate {
    private static final long RECOVERY_GAP_MS = 5 * 60 * 1000L;
    private static final long CANDIDATE_MAX_GAP_MS = 30 * 1000L;
    private static final double MAX_RECOVERY_SPEED_KMH = 200.0;

    private boolean waiting;
    private double candidateLat;
    private double candidateLon;
    private long candidateTime;

    public boolean shouldHold(double lat, double lon, long time,
                              boolean hasAcceptedPoint, long acceptedPointGapMs) {
        boolean needsRecovery = !hasAcceptedPoint || acceptedPointGapMs > RECOVERY_GAP_MS;
        if (!needsRecovery) {
            reset();
            return false;
        }
        if (!waiting) {
            setCandidate(lat, lon, time);
            return true;
        }
        long gap = time - candidateTime;
        double distance = GpsFilter.calculateDistance(candidateLat, candidateLon, lat, lon);
        double speed = gap > 0 ? distance / (gap / 1000.0) * 3.6 : Double.MAX_VALUE;
        if (gap <= 0 || gap > CANDIDATE_MAX_GAP_MS || speed > MAX_RECOVERY_SPEED_KMH) {
            setCandidate(lat, lon, time);
            return true;
        }
        reset();
        return false;
    }

    private void setCandidate(double lat, double lon, long time) {
        waiting = true;
        candidateLat = lat;
        candidateLon = lon;
        candidateTime = time;
    }

    private void reset() {
        waiting = false;
    }
}
