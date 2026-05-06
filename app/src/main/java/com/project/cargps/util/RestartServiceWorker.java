package com.project.cargps.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.project.cargps.ui.RunAppService;

public class RestartServiceWorker extends Worker {

    public RestartServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 检查服务是否在运行，如果不在运行则启动服务
        if (!isServiceRunning() && PermissionUtil.hasLocationPermission(getApplicationContext())) {
            Intent serviceIntent = new Intent(getApplicationContext(), RunAppService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(serviceIntent);
            } else {
                getApplicationContext().startService(serviceIntent);
            }
        }
        return Result.success();
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RunAppService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

