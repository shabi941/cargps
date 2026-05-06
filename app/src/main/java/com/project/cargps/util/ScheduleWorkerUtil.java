package com.project.cargps.util;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ScheduleWorkerUtil {
    public static void scheduleRestartWorker(Context context) {
        PeriodicWorkRequest restartWorkRequest =
                new PeriodicWorkRequest.Builder(RestartServiceWorker.class, 30, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RestartServiceWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                restartWorkRequest);
    }
}
