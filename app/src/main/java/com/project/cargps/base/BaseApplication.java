package com.project.cargps.base;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.MutableLiveData;

import com.amap.api.location.AMapLocationClient;
import com.project.cargps.bean.MyLocation;
import com.project.cargps.logcat.CrashHandler;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.net.OkHttpManage;
import com.project.cargps.ui.RunAppReceiver;
import com.tencent.bugly.crashreport.CrashReport;

public class BaseApplication extends Application {
    public static Context mContext;

    public static MutableLiveData<MyLocation> mCurrentLocation = new MutableLiveData<>();
    public static MutableLiveData<Boolean> createTrackResult = new MutableLiveData<>();
    public static MutableLiveData<Boolean> tryCreateTrack = new MutableLiveData<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        OkHttpManage.setSSLMode(OkHttpManage.SSLMode.LENIENT);
        // 初始化日志系统
        LogUtil.init(this);
        // 初始化崩溃捕获
        CrashHandler crashHandler = new CrashHandler();
        crashHandler.init(this);

        //高德隐私合规
        AMapLocationClient.updatePrivacyShow(this,true,true);
        AMapLocationClient.updatePrivacyAgree(this,true);
        //ScheduleWorkerUtil.scheduleRestartWorker(this);

        //bugly
        CrashReport.initCrashReport(getApplicationContext(), "7b94e84bca", false);

    }

    public static void onSendBroadcast(){
        Intent intent = new Intent(RunAppReceiver.PermissionAction);
        intent.setComponent(new ComponentName(mContext.getPackageName(),
                RunAppReceiver.class.getName()));
        mContext.sendBroadcast(intent);
    }
}
