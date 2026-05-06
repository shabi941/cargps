package com.project.cargps.ui;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.project.cargps.logcat.LogUtil;
import com.project.cargps.ui.activity.MainAty;
import com.project.cargps.util.PermissionUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RunAppReceiver extends BroadcastReceiver {
    private final String LogcatTag = "RunAppReceiver";

    public static final String PermissionAction = "com.project.cargps.PERMISSION_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            String action = intent.getAction();
            // 开机/解锁时打印关键日志，便于排查自启问题
            LogUtil.e(LogcatTag, "===== APP启动 =====");
            LogUtil.e(LogcatTag, "时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date()));
            LogUtil.e(LogcatTag, "触发Action: " + action);
            LogUtil.e(LogcatTag, "进程名: " + getProcessName(context));
            LogUtil.e(LogcatTag, "进程ID: " + android.os.Process.myPid());
            LogUtil.e(LogcatTag, "网络状态: " + getNetworkInfo(context));
            LogUtil.e(LogcatTag, "Gps开关: " + isGpsEnabled(context));
            LogUtil.e(LogcatTag, "位置权限: " + PermissionUtil.hasLocationPermission(context));
            LogUtil.e(LogcatTag, "服务是否运行: " + isServiceRunning(context));
            LogUtil.e(LogcatTag, "===== 开始启动 =====");
            switch (action) {
                case Intent.ACTION_USER_PRESENT:
                    RunAppService.isOpenBoot = false;
                    startMainActivity(context);
                    break;
                case PermissionAction:
                    startRunAppService(context,0);
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    RunAppService.isOpenBoot = true;
                    startRunAppService(context,1);
                    break;
            }
        }
    }

    private void startMainActivity(Context context) {
        Intent intent = new Intent(context, MainAty.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", 1);
        context.startActivity(intent);
    }

    @SuppressLint("NewApi")
    private void startRunAppService(Context context,int type) {
        LogUtil.e(LogcatTag, ">>> startRunAppService 开始执行, type=" + type);
        LogUtil.e(LogcatTag, ">>> 位置权限检查: " + PermissionUtil.hasLocationPermission(context));
        LogUtil.e(LogcatTag, ">>> 手机权限检查: " + PermissionUtil.hasPhonePermission(context));
        LogUtil.e(LogcatTag, ">>> 网络状态: " + getNetworkInfo(context));
        LogUtil.e(LogcatTag, ">>> GPS开关: " + isGpsEnabled(context));

        if (PermissionUtil.hasLocationPermission(context)) {
            Intent serviceIntent = new Intent(context, RunAppService.class);
            serviceIntent.putExtra("type", type);
            LogUtil.e(LogcatTag, ">>> 准备启动RunAppService");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                LogUtil.e(LogcatTag, ">>> 已调用startForegroundService");
            } else {
                context.startService(serviceIntent);
                LogUtil.e(LogcatTag, ">>> 已调用startService");
            }
            LogUtil.e(LogcatTag, ">>> startRunAppService 执行完成");
        } else {
            LogUtil.e(LogcatTag, ">>> 位置权限不足，无法启动Service");
        }
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RunAppService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前进程名
     */
    private String getProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) {
            LogUtil.e(LogcatTag, "getRunningAppProcesses() 返回null，使用PackageName作为进程名");
            return context.getPackageName();
        }
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info.pid == pid) {
                return info.processName;
            }
        }
        return "unknown";
    }

    /**
     * 获取网络连接状态
     */
    private String getNetworkInfo(Context context) {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            String type = activeNetwork.getTypeName();
            boolean wifi = activeNetwork.getType() == android.net.ConnectivityManager.TYPE_WIFI;
            return type + (wifi ? "(WiFi)" : "(移动网络)");
        }
        return "无网络";
    }

    /**
     * 检查GPS是否开启
     */
    private boolean isGpsEnabled(Context context) {
        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }
}
