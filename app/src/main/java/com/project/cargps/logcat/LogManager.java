package com.project.cargps.logcat;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogManager {
    private static final long MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LOG_FILES = 5;

    /**
     * 检查日志文件大小，如果过大则轮转
     */
    public static void checkAndRotateLog() {
        String logPath = LogUtil.getLogFilePath();
        if (logPath == null) return;

        File logFile = new File(logPath);
        if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
            rotateLogFiles(logFile);
        }
    }

    /**
     * 日志文件轮转
     */
    private static void rotateLogFiles(File currentLogFile) {
        File parentDir = currentLogFile.getParentFile();
        String baseName = currentLogFile.getName().replace(".txt", "");

        // 删除最旧的日志文件
        File oldestLog = new File(parentDir, baseName + "." + MAX_LOG_FILES + ".txt");
        if (oldestLog.exists()) {
            oldestLog.delete();
        }

        // 重命名现有日志文件
        for (int i = MAX_LOG_FILES - 1; i >= 1; i--) {
            File oldFile = new File(parentDir, baseName + "." + i + ".txt");
            if (oldFile.exists()) {
                File newFile = new File(parentDir, baseName + "." + (i + 1) + ".txt");
                oldFile.renameTo(newFile);
            }
        }

        // 重命名当前日志文件
        File newFile = new File(parentDir, baseName + ".1.txt");
        currentLogFile.renameTo(newFile);
        
        // 轮转后同时清理48小时前的过期日志
        cleanExpiredLogs();
    }

    /**
     * 清理48小时前的过期日志文件
     */
    public static void cleanExpiredLogs() {
        LogUtil.cleanExpiredLogs(); // 复用 LogUtil 的清理逻辑
    }

    /**
     * 导出日志文件到指定路径
     */
    public static String exportLogs(Context context, String exportPath) {
        File logDir = new File(LogUtil.getLogFilePath()).getParentFile();
        if (!logDir.exists()) return null;

        File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (logFiles == null || logFiles.length == 0) return null;

        String zipFilePath = exportPath + "/logs_" + System.currentTimeMillis() + ".zip";

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            byte[] buffer = new byte[1024];

            for (File logFile : logFiles) {
                ZipEntry ze = new ZipEntry(logFile.getName());
                zos.putNextEntry(ze);

                try (FileInputStream fis = new FileInputStream(logFile)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }

            return zipFilePath;
        } catch (IOException e) {
            LogUtil.e("LogManager", "导出日志失败", e);
            return null;
        }
    }

    /**
     * 获取日志目录中的所有日志文件
     */
    public static File[] getAllLogFiles() {
        File logDir = new File(LogUtil.getLogFilePath()).getParentFile();
        if (logDir.exists()) {
            return logDir.listFiles((dir, name) -> name.endsWith(".txt"));
        }
        return new File[0];
    }
}
