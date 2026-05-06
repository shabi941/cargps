package com.project.cargps.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.project.cargps.logcat.LogUtil;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

public class UpdateUtil {
    private static final String LogcatTag = "UpdateUtil";

    /**
     * 检查是否有新版本
     */
    public static boolean hasNewVersion(Context context, int versionCode) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return versionCode > packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 安装 APK 文件
     */
    public static void installApk(Context context, File apkFile) {
        LogUtil.e(LogcatTag, "跳转安装");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean canInstall = context.getPackageManager().canRequestPackageInstalls();
            if (!canInstall) {
                Intent installIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                installIntent.setData(Uri.parse("package:" + context.getPackageName()));
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(installIntent);
                return;
            }
        }
        context.startActivity(intent);
    }

    /**
     * 获取下载的 APK 文件
     */
    public static File getDownloadedApk(String fileName) {
        File apkFile;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 从公共目录获取
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            apkFile = new File(downloadDir, fileName);
        } else {
            // Android 10 以下从传统目录获取
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            apkFile = new File(downloadDir, fileName);
        }
        return apkFile.exists() ? apkFile : null;
    }

    /**
     * 从URL中提取文件名
     */
    public static String getFileName() {
        return "klzx_" + generateFiveDigitString() + ".apk";
    }

    public static String generateFiveDigitString() {
        Random random = new Random();
        return String.format("%05d", random.nextInt(1000));
    }

    /**
     * 检查 APK 文件是否有效
     */
    public static boolean isApkFileValid(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }

        // 检查文件大小
        if (apkFile.length() <= 0) {
            return false;
        }

        // 检查包信息
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
        return packageInfo != null;
    }

    public static Boolean isSameTime(int time) {
        int hour = 0;
        int minute = 0;
        try {
            long currentTimeMillis = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentTimeMillis);
            hour = calendar.get(Calendar.HOUR_OF_DAY); // 24小时制
            minute = calendar.get(Calendar.MINUTE);
            LogUtil.e(LogcatTag, "hour:" + hour + ",minute:" + minute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //精确到小时
        return hour == time && minute < 1;
    }
}
