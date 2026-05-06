package com.project.cargps.logcat;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context context;
    private SimpleDateFormat dateFormat;

    public void init(Context context) {
        this.context = context.getApplicationContext();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 保存崩溃日志
        //saveCrashLog(ex);

        // 显示错误提示
        showErrorToast();

        // 让系统默认处理器处理
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private void saveCrashLog(Throwable ex) {
        String crashLog = buildCrashLog(ex);

        // 保存到文件
        String crashLogPath = getCrashLogPath();
        try (FileOutputStream fos = new FileOutputStream(crashLogPath, true)) {
            fos.write(crashLog.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 同时写入应用日志
        LogUtil.e("CrashHandler", "应用崩溃", ex);
    }

    private String buildCrashLog(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("/********** CRASH LOG **********/\n");
        sb.append("Time: ").append(dateFormat.format(new Date())).append("\n");

        // 设备信息
        sb.append("Device: ").append(android.os.Build.MANUFACTURER)
                .append(" ").append(android.os.Build.MODEL).append("\n");
        sb.append("Android: ").append(android.os.Build.VERSION.RELEASE).append("\n");

        // 异常信息
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        sb.append("Exception: ").append(sw.toString()).append("\n");
        sb.append("/********** CRASH LOG END **********/\n\n");

        return sb.toString();
    }

    private String getCrashLogPath() {
        File crashDir = new File(context.getExternalFilesDir(null), "crash");
        if (!crashDir.exists()) {
            crashDir.mkdirs();
        }
        return crashDir.getAbsolutePath() + "/crash_log.txt";
    }

    private void showErrorToast() {
        // 可以在UI线程显示Toast提示用户
        // 注意：这里需要在UI线程执行
    }
}
