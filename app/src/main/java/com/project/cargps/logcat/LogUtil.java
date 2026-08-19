package com.project.cargps.logcat;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogUtil {
    private static final String TAG = "LogUtil";
    private static final long LOG_KEEP_HOURS = 48; // 保留最近48小时
    private static boolean isLogToFile = false;
    private static String logFilePath;
    private static SimpleDateFormat dateFormat;
    private static SimpleDateFormat logDateFormat;
    private static int logWriteCount = 0;
    private static final ExecutorService FILE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final int MAINTENANCE_WRITE_INTERVAL = 500;

    public static void init(Context context) {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        // 设置日志文件路径
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            logFilePath = context.getExternalFilesDir("logs").getAbsolutePath() + "/app_log.txt";
        } else {
            logFilePath = context.getFilesDir().getAbsolutePath() + "/logs/app_log.txt";
        }

        // 创建日志目录
        File logDir = new File(logFilePath).getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        isLogToFile = true;
        i(TAG, "日志系统初始化完成，日志文件路径: " + logFilePath);
        
        // 启动时清理过期日志（每10次写日志时检查）
        FILE_EXECUTOR.execute(LogUtil::cleanExpiredLogs);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
        writeToFile("V", tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        writeToFile("D", tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        writeToFile("I", tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        writeToFile("W", tag, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
        writeToFile("E", TAG, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        writeToFile("E", tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        String stackTrace = getStackTraceString(tr);
        writeToFile("E", tag, msg + "\n" + stackTrace);
    }

    private static void writeToFile(String level, String tag, String msg) {
        if (!isLogToFile) return;

        final String time;
        synchronized (LogUtil.class) {
            time = dateFormat.format(new Date());
        }
        final String logMsg = String.format("%s %s/%s: %s\n", time, level, tag, msg);
        FILE_EXECUTOR.execute(() -> {
            try (FileOutputStream fos = new FileOutputStream(logFilePath, true)) {
                fos.write(logMsg.getBytes("UTF-8"));
            } catch (IOException e) {
                Log.e(TAG, "写入日志文件失败", e);
            }
            logWriteCount++;
            if (logWriteCount >= MAINTENANCE_WRITE_INTERVAL) {
                logWriteCount = 0;
                LogManager.checkAndRotateLog();
                cleanExpiredLogs();
            }
        });
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取日志文件路径
     */
    public static String getLogFilePath() {
        return logFilePath;
    }

    /**
     * 清理日志文件（所有txt和zip文件）
     */
    public static void clearLogFile() {
        if (logFilePath != null) {
            File logDir = new File(logFilePath).getParentFile();
            if (logDir != null && logDir.exists()) {
                File[] files = logDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName();
                        if (name.endsWith(".txt") || name.endsWith(".zip")) {
                            file.delete();
                            e(TAG, "已删除日志文件: " + name);
                        }
                    }
                }
            }
        }
    }

    /**
     * 清理48小时前的过期日志（公开给LogManager调用）
     */
    public static void cleanExpiredLogs() {
        if (logFilePath == null) return;

        File logDir = new File(logFilePath).getParentFile();
        if (logDir == null || !logDir.exists()) return;

        long expireTime = System.currentTimeMillis() - (LOG_KEEP_HOURS * 60 * 60 * 1000L);
        File[] files = logDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null) return;

        for (File file : files) {
            try {
                String name = file.getName();
                long lastModified = file.lastModified();

                if (name.equals("app_log.txt")) {
                    // 当前日志文件：逐行清理，超过48小时的记录删除
                    cleanExpiredLinesInFile(file, expireTime);
                } else {
                    // 轮转日志文件(.1.txt等)：整体删除
                    if (lastModified > 0 && lastModified < expireTime) {
                        if (file.delete()) {
                            Log.i(TAG, "已清理过期日志: " + name);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "清理过期日志失败: " + e.getMessage());
            }
        }
    }

    /**
     * 清理文件内超过48小时的日志行
     */
    private static void cleanExpiredLinesInFile(File file, long expireTime) {
        if (file == null || !file.exists() || !file.canWrite()) return;

        StringBuilder validLines = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 解析日志行的时间戳（格式：2026-04-29 09:33:24.102）
                long lineTime = parseLogLineTime(line);
                if (lineTime > 0 && lineTime > expireTime) {
                    validLines.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "读取日志文件失败: " + e.getMessage());
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(validLines.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            Log.e(TAG, "写入清理后的日志失败: " + e.getMessage());
        }
    }

    /**
     * 从日志行解析时间戳
     */
    private static long parseLogLineTime(String line) {
        if (line == null || line.length() < 23) return 0;
        try {
            // 日志格式：2026-04-29 09:33:24.102 D/Tag: message
            String timeStr = line.substring(0, 23);
            Date date;
            synchronized (LogUtil.class) {
                date = logDateFormat.parse(timeStr);
            }
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }
}
