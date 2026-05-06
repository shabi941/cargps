package com.project.cargps.ui;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.database.Cursor;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.track.AMapTrackClient;
import com.amap.api.track.ErrorCode;
import com.amap.api.track.OnTrackLifecycleListener;
import com.amap.api.track.TrackParam;
import com.amap.api.track.query.model.AddTerminalRequest;
import com.amap.api.track.query.model.AddTerminalResponse;
import com.amap.api.track.query.model.AddTrackRequest;
import com.amap.api.track.query.model.AddTrackResponse;
import com.amap.api.track.query.model.DistanceResponse;
import com.amap.api.track.query.model.HistoryTrackResponse;
import com.amap.api.track.query.model.LatestPointResponse;
import com.amap.api.track.query.model.OnTrackListener;
import com.amap.api.track.query.model.ParamErrorResponse;
import com.amap.api.track.query.model.QueryTerminalRequest;
import com.amap.api.track.query.model.QueryTerminalResponse;
import com.amap.api.track.query.model.QueryTrackResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.project.cargps.R;
import com.project.cargps.base.BaseApplication;
import com.project.cargps.bean.MyLocation;
import com.project.cargps.logcat.LogManager;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.net.ApiService;
import com.project.cargps.net.OkHttpManage;
import com.project.cargps.net.PostParams;
import com.project.cargps.ui.activity.MainAty;
import com.project.cargps.util.PermissionUtil;
import com.project.cargps.util.ServiceIdManagerUtil;
import com.project.cargps.util.UpdateUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RunAppService extends Service {
    private static final long LOG_UPLOAD_INTERVAL_MS = 30 * 60 * 1000; // 30分钟
    private static final String LogcatTag = "RunAppService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "GpsUploadChannel";

    public static boolean isOpenBoot = false;

    private Handler handler = new Handler();
    private Gson gson = new Gson();
    private Context context;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    //声明AMapLocationClient类对象
    AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    AMapLocationListener mLocationListener;
    //声明AMapLocationClientOption对象
    AMapLocationClientOption mLocationOption = null;

    private TelephonyManager telephonyManager;

    private DisplayManager displayManager;
    private KeyguardManager keyguardManager;
    private DisplayManager.DisplayListener displayListener;

    private String androidId;

    private boolean isForeground = false;
    private boolean isGPSRunning = false;
    private boolean isDownloadApk = false;

    // GPS待上传队列
    private static final String PENDING_GPS_FILE = "pending_gps.json";
    private List<PendingGpsData> pendingGpsList = new ArrayList<>();
    private boolean isUploadingPending = false;

    // 网络状态监听器
    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    LogUtil.e(LogcatTag, ">>> 网络恢复连接，开始上传待发送的GPS数据和日志");
                    uploadPendingGps();
                    uploadDeviceLog();
                } else {
                    LogUtil.e(LogcatTag, ">>> 网络断开");
                }
            }
        }
    };

    // 待发送的GPS数据结构
    static class PendingGpsData {
        double longitude;
        double latitude;
        String device_no;
        long position_time;
        long createTime;
        double speed;

        PendingGpsData(double longitude, double latitude, String device_no, long position_time, double speed) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.device_no = device_no;
            this.position_time = position_time;
            this.createTime = System.currentTimeMillis();
            this.speed = speed;
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.e(LogcatTag, "onCreate");
        context = this;
        initManagers();
        createNotificationChannel();
        registerDisplayListener();

        // 注册网络状态监听器
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
        LogUtil.e(LogcatTag, ">>> 网络监听器已注册");

        // 加载本地待发送的GPS数据
        loadPendingGps();

        //尝试重新创建轨迹
        BaseApplication.tryCreateTrack.observeForever(tryCreate->{
//            upTrack();
        });

        // 启动日志上传定时任务
        startLogUploadSchedule();
    }

    /**
     * 定时上传日志任务
     */
    private void startLogUploadSchedule() {
        LogUtil.e(LogcatTag, ">>> 启动日志上传定时器，间隔=" + LOG_UPLOAD_INTERVAL_MS + "ms");
        // 启动后立即先上传一次，便于验证链路
        uploadDeviceLog();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LogUtil.e(LogcatTag, ">>> 定时器触发，准备上传日志");
                uploadDeviceLog();
                // 继续下一次调度
                handler.postDelayed(this, LOG_UPLOAD_INTERVAL_MS);
            }
        }, LOG_UPLOAD_INTERVAL_MS);
    }

    /**
     * 上传设备日志到服务器
     */
    private void uploadDeviceLog() {
        try {
            LogUtil.e(LogcatTag, ">>> uploadDeviceLog 开始");
            String deviceNo = ServiceIdManagerUtil.getDeviceNo(context);
            if (deviceNo == null || deviceNo.isEmpty()) {
                LogUtil.e(LogcatTag, ">>> deviceNo为空，使用unknown");
                deviceNo = "unknown";
            }
            LogUtil.e(LogcatTag, ">>> 当前deviceNo=" + deviceNo);

            String logFilePath = LogUtil.getLogFilePath();
            LogUtil.e(LogcatTag, ">>> 日志文件路径=" + logFilePath);
            // 获取日志目录并导出ZIP
            File logDir = new File(logFilePath).getParentFile();
            if (logDir == null || !logDir.exists()) {
                LogUtil.e(LogcatTag, ">>> 日志目录不存在，跳过上传");
                return;
            }
            LogUtil.e(LogcatTag, ">>> 日志目录存在=" + logDir.getAbsolutePath());

            File[] logFiles = LogManager.getAllLogFiles();
            if (logFiles == null || logFiles.length == 0) {
                LogUtil.e(LogcatTag, ">>> 没有日志文件可上传");
                return;
            }
            LogUtil.e(LogcatTag, ">>> 找到日志文件数量=" + logFiles.length);

            // 导出日志ZIP
            if (context.getExternalFilesDir("logs") == null) {
                LogUtil.e(LogcatTag, ">>> ExternalFilesDir(logs)为空，跳过上传");
                return;
            }
            String exportPath = context.getExternalFilesDir("logs").getAbsolutePath();
            LogUtil.e(LogcatTag, ">>> 准备导出ZIP到=" + exportPath);
            final String zipFilePath = LogManager.exportLogs(context, exportPath);
            if (zipFilePath == null) {
                LogUtil.e(LogcatTag, ">>> 日志导出失败，跳过上传");
                return;
            }
            LogUtil.e(LogcatTag, ">>> ZIP导出路径=" + zipFilePath);

            final File zipFile = new File(zipFilePath);
            if (!zipFile.exists()) {
                LogUtil.e(LogcatTag, ">>> ZIP文件不存在，跳过上传");
                return;
            }
            LogUtil.e(LogcatTag, ">>> ZIP文件存在，大小=" + zipFile.length());

            // 构建请求：device_no 作为 Query 参数，file 作为 multipart form-field
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), zipFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", zipFile.getName(), fileBody);
            ApiService apiService = OkHttpManage.instance().create(ApiService.class);
            Call<Object> call = apiService.uploadDeviceLog(deviceNo, part);

            LogUtil.e(LogcatTag, ">>> 开始请求 /api/index/uploadDeviceLog, device_no=" + deviceNo + ", file=" + zipFile.getName());

            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    String json = gson.toJson(response.body());
                    LogUtil.e(LogcatTag, ">>> 日志上传onResponse: " + json);
                    // 上传成功后删除ZIP文件
                    if (zipFile.exists()) {
                        zipFile.delete();
                        LogUtil.e(LogcatTag, ">>> 日志ZIP已删除=" + zipFile.getName());
                    }
                    // 上传成功后清理所有日志文件
                    LogUtil.clearLogFile();
                    LogUtil.e(LogcatTag, ">>> 日志文件已清理");
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    LogUtil.e(LogcatTag, ">>> 日志上传onFailure: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            LogUtil.e(LogcatTag, ">>> 上传日志异常: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.e(LogcatTag, "onStartCommand");
        if (!isForeground) {
            isForeground = true;
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, notification);
            }
        }
        handleBootStartup();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "KLZX-GPS";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentTitle("长期运行服务")
                .setContentText("服务正在后台运行")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        return builder.getNotification();
    }

    private void initManagers() {
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    }

    private void registerDisplayListener() {
        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {

            }

            @Override
            public void onDisplayRemoved(int displayId) {

            }

            @Override
            public void onDisplayChanged(int displayId) {
                try {
                    Display display = displayManager.getDisplay(displayId);
                    int state = display.getState();

                    if (displayId == Display.DEFAULT_DISPLAY) {
                        switch (state) {
                            case Display.STATE_ON:
                                LogUtil.e(LogcatTag, "STATE_ON");
                                handleScreenOn();
                                break;
                            //                        case Display.STATE_OFF:
                            //                            break;
                            //                        case Display.STATE_DOZE:
                            //                            break;
                            //                        case Display.STATE_DOZE_SUSPEND:
                            //                            break;
                        }
                    }
                } catch (Exception e) {

                }
            }
        };

        displayManager.registerDisplayListener(displayListener, null);
    }

    private void handleScreenOn() {
        LogUtil.e(LogcatTag, "isDeviceUnlocked:" + isDeviceUnlocked() + ",isKeyguardSecure:" + isKeyguardSecure());
        if (isDeviceUnlocked() || !isKeyguardSecure()) {
            startMainActivity();
        } else {
            scheduleDelayedActivity();
        }
    }

    private void scheduleDelayedActivity() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isDeviceUnlocked()) {
                    startMainActivity();
                }
            }
        }, 3000);
    }

    private boolean isDeviceUnlocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return !keyguardManager.isDeviceLocked();
        } else {
            return !keyguardManager.isKeyguardLocked();
        }
    }

    private boolean isKeyguardSecure() {
        return keyguardManager.isKeyguardSecure();
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    private void handleBootStartup() {
        LogUtil.e(LogcatTag, ">>> handleBootStartup 开始");
        LogUtil.e(LogcatTag, ">>> isDownloadApk=" + isDownloadApk + ", isOpenBoot=" + isOpenBoot);
        LogUtil.e(LogcatTag, ">>> 手机权限: " + PermissionUtil.hasPhonePermission(this));
        LogUtil.e(LogcatTag, ">>> 位置权限: " + PermissionUtil.hasLocationPermission(this));
        /*if (!isDownloadApk && PermissionUtil.hasStoragePermission(context)){
            onUpgradeApk();
        }else {
            LogUtil.e(LogcatTag, "没储存权限，isDownloadApk：" +isDownloadApk);
        }*/
        if (!isDownloadApk && !isOpenBoot) {
            LogUtil.e(LogcatTag, ">>> 准备检查APK更新");
            onUpgradeApk();
        }

        if (PermissionUtil.hasPhonePermission(this) && PermissionUtil.hasLocationPermission(this)) {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            androidId = Settings.Global.getString(context.getContentResolver(), "device_id");
            LogUtil.e(LogcatTag, ">>> androidId=" + androidId);

//            if (locationListener == null) {
//                locationListener = location -> {
//                    //LogUtil.e("okhttp", "location：" + location.getLongitude() + "," + location.getLatitude());
//                    BaseApplication.currentLocation.setValue(location);
//                    onSaveLocation(location.getLongitude(), location.getLatitude(), androidId, location.getTime());
//                };
//            }

            if (!isGPSRunning) {
                isGPSRunning = true;
                LogUtil.e(LogcatTag, ">>> 准备调用requestLocation");
//                upTrack();
                requestLocation();
            } else {
                LogUtil.e(LogcatTag, ">>> GPS已经在运行中，跳过");
            }
        } else {
            LogUtil.e(LogcatTag, ">>> 权限不足，准备跳转MainActivity");
            handler.postDelayed(() -> {
                startMainActivity();
            }, 2000);
        }
        LogUtil.e(LogcatTag, ">>> handleBootStartup 结束");
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainAty.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", 0);
        startActivity(intent);
    }

    private AMapTrackClient aMapTrackClient;

    //上报
    private void upTrack() {
        try {
            final long serviceId = ServiceIdManagerUtil.sId;  // 这里填入前面创建的服务id
            final String terminalName = ServiceIdManagerUtil.getDeviceNo(context);   // 唯一标识某个用户或某台设备的名称，可根据您的业务自行选择

            aMapTrackClient = new AMapTrackClient(getApplicationContext());
            aMapTrackClient.setInterval(2, 5);
            LogUtil.d("猎鹰轨迹", "创建参数 serviceId：" + serviceId + " deviceNo:" + terminalName);

            aMapTrackClient.queryTerminal(new QueryTerminalRequest(serviceId, terminalName), new OnTrackListener() {


                @Override
                public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
                    if (queryTerminalResponse.isSuccess()) {
                        if (queryTerminalResponse.getTid() <= 0) {
                            LogUtil.d("猎鹰轨迹", "创建成功 1，" + queryTerminalResponse.getErrorMsg() + " code:" + queryTerminalResponse.getErrorCode());
                            // terminal还不存在，先创建
                            aMapTrackClient.addTerminal(new AddTerminalRequest(terminalName, serviceId), new OnTrackListener() {


                                @Override
                                public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {

                                }

                                @Override
                                public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {
                                    if (addTerminalResponse.isSuccess()) {
                                        // 创建完成，开启猎鹰服务
                                        long terminalId = addTerminalResponse.getTid();
                                        LogUtil.d("猎鹰轨迹", "创建成功 1，" + queryTerminalResponse.getErrorMsg() + " code:" + queryTerminalResponse.getErrorCode() + " terminalId:" + terminalId);

                                        addTrack(serviceId, terminalId);

                                        BaseApplication.createTrackResult.postValue(true);

                                    } else {
                                        // 请求失败
                                        LogUtil.d("猎鹰轨迹", "请求失败 2，" + addTerminalResponse.getErrorMsg());

                                        Toast.makeText(BaseApplication.mContext,"创建轨迹失败",Toast.LENGTH_SHORT).show();
                                        BaseApplication.createTrackResult.postValue(false);
                                    }
                                }

                                @Override
                                public void onDistanceCallback(DistanceResponse distanceResponse) {

                                }

                                @Override
                                public void onLatestPointCallback(LatestPointResponse latestPointResponse) {

                                }

                                @Override
                                public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {

                                }

                                @Override
                                public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

                                }

                                @Override
                                public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

                                }

                                @Override
                                public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

                                }
                            });
                        } else {
                            // terminal已经存在，直接开启猎鹰服务
                            long terminalId = queryTerminalResponse.getTid();

                            LogUtil.d("猎鹰轨迹", "terminal已经存在，直接开启猎鹰服务");
                            addTrack(serviceId, terminalId);
//                          aMapTrackClient.startTrack(new TrackParam(serviceId, terminalId), onTrackLifecycleListener);
                        }
                    } else {
                        // 请求失败
                        LogUtil.d("猎鹰轨迹", "请求失败 1，" + queryTerminalResponse.getErrorMsg() + " code:" + queryTerminalResponse.getErrorCode());

                        Toast.makeText(BaseApplication.mContext,"创建轨迹失败",Toast.LENGTH_SHORT).show();
                        BaseApplication.createTrackResult.postValue(false);
                    }
                }

                @Override
                public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

                }

                @Override
                public void onDistanceCallback(DistanceResponse distanceResponse) {

                }

                @Override
                public void onLatestPointCallback(LatestPointResponse latestPointResponse) {

                }

                @Override
                public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {

                }

                @Override
                public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

                }

                @Override
                public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

                }

                @Override
                public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

                }
            });
        } catch (Exception e) {

        }
    }

    final OnTrackLifecycleListener onTrackLifecycleListener = new OnTrackLifecycleListener() {


        @Override
        public void onBindServiceCallback(int i, String s) {

        }

        @Override
        public void onStartGatherCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_GATHER_SUCEE ||
                    status == ErrorCode.TrackListen.START_GATHER_ALREADY_STARTED) {
                LogUtil.d("猎鹰轨迹", "定位采集开启成功");
//                    Toast.makeText(MainActivity.this, "定位采集开启成功！", Toast.LENGTH_SHORT).show();
            } else {
                LogUtil.d("猎鹰轨迹", "定位采集启动异常");
//                    Toast.makeText(MainActivity.this, "定位采集启动异常，" + msg, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onStartTrackCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_TRACK_SUCEE ||
                    status == ErrorCode.TrackListen.START_TRACK_SUCEE_NO_NETWORK ||
                    status == ErrorCode.TrackListen.START_TRACK_ALREADY_STARTED) {
                // 服务启动成功，继续开启收集上报
                aMapTrackClient.startGather(this);
            } else {
                LogUtil.d("猎鹰轨迹", "轨迹上报服务服务启动异常");
            }
        }

        @Override
        public void onStopGatherCallback(int i, String s) {

        }

        @Override
        public void onStopTrackCallback(int i, String s) {

        }
    };

    //创建轨迹
    private void addTrack(long serviceId, long terminalId) {
        ServiceIdManagerUtil.terminalId = terminalId;

        LogUtil.d("猎鹰轨迹", "创建轨迹成功:serviceId:" + serviceId + " terminalId:" + terminalId);

        aMapTrackClient.addTrack(new AddTrackRequest(serviceId, terminalId), new OnTrackListener() {

            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
            }

            @Override
            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

            }

            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {
            }

            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {

            }

            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
            }

            @Override
            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

            }

            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {
                if (addTrackResponse.isSuccess()) {
                    long trackId = addTrackResponse.getTrid();
                    LogUtil.d("猎鹰轨迹", "创建轨迹成功:trackId:" + trackId);
//                    LogUtil.d("猎鹰轨迹", "创建轨迹成功: 但是使用固定的历史id:18");
//                    trackId = 18;
                    //创建

                    ServiceIdManagerUtil.trid = trackId;

                    TrackParam trackParam = new TrackParam(serviceId, terminalId);
                    trackParam.setTrackId(trackId);
                    aMapTrackClient.startTrack(trackParam, onTrackLifecycleListener);

                    registerTrace();
//                    aMapTrackClient.startTrack(new TrackParam(serviceId, terminalId), onTrackLifecycleListener);

                    BaseApplication.createTrackResult.postValue(true);
                } else {
                    LogUtil.d("猎鹰轨迹", "创建轨迹失败:ErrorMsg:" + addTrackResponse.getErrorMsg() + " errorCode:" + addTrackResponse.getErrorCode());

                    Toast.makeText(BaseApplication.mContext,"创建轨迹失败",Toast.LENGTH_SHORT).show();

                    BaseApplication.createTrackResult.postValue(false);
                }
            }

            @Override
            public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

            }
        });
    }

    /**
     * 获取定位信息
     */
    @SuppressLint("MissingPermission")
    private void requestLocation() {
        LogUtil.e(LogcatTag, ">>> requestLocation 开始");
        try {
            mLocationListener = new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if (aMapLocation != null && aMapLocation.getLongitude() > 0 && aMapLocation.getLatitude() > 0) {
                        LogUtil.e(LogcatTag, ">>> GPS数据: 经度=" + aMapLocation.getLongitude() + ", 纬度=" + aMapLocation.getLatitude());
                        // 获取速度并转换为km/h（高德返回的是米/秒）
                        double speedKmh = aMapLocation.getSpeed() * 3.6;
                        LogUtil.e(LogcatTag, ">>> 速度=" + speedKmh + " km/h");
                        MyLocation myLocation = new MyLocation();
                        myLocation.latitude = aMapLocation.getLatitude();
                        myLocation.longitude = aMapLocation.getLongitude();
                        myLocation.speeds = speedKmh;

                        BaseApplication.mCurrentLocation.setValue(myLocation);

                        LogUtil.e(LogcatTag, ">>> 准备调用onSaveLocation上报GPS");
                        onSaveLocation(aMapLocation.getLongitude(), aMapLocation.getLatitude(), androidId, aMapLocation.getTime(), speedKmh);
                        LogUtil.e(LogcatTag, ">>> onSaveLocation调用完成");
                    } else {
                        LogUtil.e(LogcatTag, ">>> GPS数据无效: " + (aMapLocation != null ? "经度=" + aMapLocation.getLongitude() + ",纬度=" + aMapLocation.getLatitude() : "aMapLocation为null"));
                    }
                }
            };

            //初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());


            //设置定位回调监听
            mLocationClient.setLocationListener(mLocationListener);


            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
            mLocationOption.setInterval(1000);
            mLocationOption.setOnceLocationLatest(false);
            mLocationOption.setHttpTimeOut(20000);


            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            //启动定位
            mLocationClient.startLocation();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存定位信息
     */
    private void onSaveLocation(double longitude, double latitude, String id, long time, double speed) {
        String device_no = id == null ? "0" : id;
        HashMap<String, Object> paramsHashMap = new HashMap<>();
        paramsHashMap.put("longitude", longitude);
        paramsHashMap.put("latitude", latitude);
        paramsHashMap.put("device_no", device_no);
        paramsHashMap.put("position_time", time);
        // 格式化速度为2位小数，避免GPS原始精度过长
        paramsHashMap.put("speed", String.format("%.2f", speed));
        PostParams postParams = new PostParams();
        RequestBody requestBody = postParams.getGsonRequestBody(paramsHashMap);
        LogUtil.e("okhttp", "requestBody:" + gson.toJson(paramsHashMap));

        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.saveLocationMsg(requestBody);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                String json = gson.toJson(response.body());
                LogUtil.e("okhttp", "onResponse:" + json);
                if (!isResponseSuccess(json)) {
                    LogUtil.e(LogcatTag, ">>> 服务端返回失败，保存到本地待发送队列");
                    savePendingGps(new PendingGpsData(longitude, latitude, device_no, time, speed));
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e("okhttp", "onFailure:" + t.getMessage() + "，保存到本地待发送队列");
                // 网络失败，保存到本地队列
                savePendingGps(new PendingGpsData(longitude, latitude, device_no, time, speed));
            }
        });
    }

    private boolean isResponseSuccess(String json) {
        try {
            JsonObject jsonObject = getJsonObject(json);
            JsonElement codeElement = jsonObject.get("code");
            if (codeElement == null || codeElement.isJsonNull()) {
                return false;
            }
            if (codeElement.getAsInt() == 0) {
                return true;
            }
        } catch (Exception e) {
            LogUtil.e(LogcatTag, ">>> 解析服务端返回失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 保存待发送的GPS数据到本地
     */
    private void savePendingGps(PendingGpsData data) {
        try {
            if (containsPendingGps(data)) {
                LogUtil.e(LogcatTag, ">>> GPS数据已存在本地队列，跳过重复入队: device_no=" + data.device_no + ", position_time=" + data.position_time);
                return;
            }
            pendingGpsList.add(data);
            File file = new File(context.getFilesDir(), PENDING_GPS_FILE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(pendingGpsList, writer);
            writer.close();
            LogUtil.e(LogcatTag, ">>> GPS数据已保存到本地，待发送数量=" + pendingGpsList.size());
        } catch (IOException e) {
            LogUtil.e(LogcatTag, ">>> 保存GPS数据失败: " + e.getMessage());
        }
    }

    private boolean containsPendingGps(PendingGpsData target) {
        for (PendingGpsData item : pendingGpsList) {
            if (item != null
                    && safeEquals(item.device_no, target.device_no)
                    && item.position_time == target.position_time) {
                return true;
            }
        }
        return false;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * 加载本地待发送的GPS数据
     */
    private void loadPendingGps() {
        try {
            File file = new File(context.getFilesDir(), PENDING_GPS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                PendingGpsData[] array = gson.fromJson(reader, PendingGpsData[].class);
                reader.close();
                if (array != null) {
                    for (PendingGpsData data : array) {
                        pendingGpsList.add(data);
                    }
                    LogUtil.e(LogcatTag, ">>> 已加载" + pendingGpsList.size() + "条待发送GPS数据");
                }
            }
        } catch (Exception e) {
            LogUtil.e(LogcatTag, ">>> 加载GPS数据失败: " + e.getMessage());
        }
    }

    /**
     * 上传本地待发送的GPS数据（按顺序）
     */
    private void uploadPendingGps() {
        if (isUploadingPending) {
            LogUtil.e(LogcatTag, ">>> 正在上传中，跳过");
            return;
        }
        if (pendingGpsList.isEmpty()) {
            LogUtil.e(LogcatTag, ">>> 没有待发送的GPS数据");
            return;
        }

        isUploadingPending = true;
        LogUtil.e(LogcatTag, ">>> 开始上传" + pendingGpsList.size() + "条待发送GPS数据");

        // 按顺序上传，每条完成后上传下一条
        uploadNextPending(0);
    }

    private void uploadNextPending(final int index) {
        if (index >= pendingGpsList.size()) {
            // 全部上传完成
            LogUtil.e(LogcatTag, ">>> 全部待发送GPS数据上传完成");
            pendingGpsList.clear();
            isUploadingPending = false;
            // 清空文件
            try {
                File file = new File(context.getFilesDir(), PENDING_GPS_FILE);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                LogUtil.e(LogcatTag, ">>> 删除待发送文件失败: " + e.getMessage());
            }
            return;
        }

        final PendingGpsData data = pendingGpsList.get(index);
        HashMap<String, Object> paramsHashMap = new HashMap<>();
        paramsHashMap.put("longitude", data.longitude);
        paramsHashMap.put("latitude", data.latitude);
        paramsHashMap.put("device_no", data.device_no);
        paramsHashMap.put("position_time", data.position_time);
        // 格式化速度为2位小数，避免GPS原始精度过长
        paramsHashMap.put("speed", String.format("%.2f", data.speed));
        PostParams postParams = new PostParams();
        RequestBody requestBody = postParams.getGsonRequestBody(paramsHashMap);

        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.saveLocationMsg(requestBody);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                String json = gson.toJson(response.body());
                if (isResponseSuccess(json)) {
                    LogUtil.e(LogcatTag, ">>> 待发送GPS[" + (index + 1) + "/" + pendingGpsList.size() + "]上传成功");
                    // 更新本地文件（移除已上传的）
                    pendingGpsList.remove(index);
                    savePendingGpsListToFile();
                    // 上传下一条
                    uploadNextPending(index);
                } else {
                    LogUtil.e(LogcatTag, ">>> 待发送GPS[" + (index + 1) + "]服务端返回失败，停止上传，response=" + json);
                    isUploadingPending = false;
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e(LogcatTag, ">>> 待发送GPS[" + (index + 1) + "]上传失败: " + t.getMessage() + "，停止上传");
                isUploadingPending = false;
            }
        });
    }

    private void savePendingGpsListToFile() {
        try {
            File file = new File(context.getFilesDir(), PENDING_GPS_FILE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(pendingGpsList, writer);
            writer.close();
        } catch (IOException e) {
            LogUtil.e(LogcatTag, ">>> 保存待发送列表失败: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    private void removeLocation() {
//        if (locationManager != null) {
//            locationManager.removeUpdates(locationListener);
//        }
    }

    /**
     * 查询app更新
     */
    private void onUpgradeApk() {
        isDownloadApk = true;
        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.upGradeApk();
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                String json = gson.toJson(response.body());
                LogUtil.e("okhttp", "onResponse:" + json);
                JsonObject jsonObject = getJsonObject(json);
                JsonElement dataElement = jsonObject.get("data");
                if (dataElement.isJsonObject()) {
                    JsonObject dataObject = dataElement.getAsJsonObject();
                    String versions = dataObject.get("versions").getAsString();
                    String apk_url = dataObject.get("apk_url").getAsString();
                    String md5_code = dataObject.get("md5_code").getAsString();
                    if (UpdateUtil.hasNewVersion(context, Integer.parseInt(versions))) {
                        startDownloadApk(apk_url);
                    }
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e("okhttp", "onFailure:" + t.getMessage());
                isDownloadApk = false;
            }
        });
    }

    /**
     * 下载apk
     */
    private String fileName;
    private File downloadedApkFile;

    private void startDownloadApk(String apkUrl) {
        LogUtil.e(LogcatTag, "startDownloadApk");
        isDownloading = false;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle("应用更新");
        request.setDescription("正在下载新版本");

        fileName = UpdateUtil.getFileName();
        // 设置下载路径
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用公共目录
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        } else {
            // Android 10 以下使用传统目录
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            downloadedApkFile = new File(downloadDir, fileName);
            request.setDestinationUri(Uri.fromFile(downloadedApkFile));
        }

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        // 监听下载进度
        listenDownloadProgress(downloadManager, downloadId);
    }

    private boolean isDownloading = false;

    @SuppressLint("Range")
    private void listenDownloadProgress(DownloadManager downloadManager, long downloadId) {
        new Thread(() -> {
            isDownloading = true;
            while (isDownloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);

                Cursor cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    long bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:
                            LogUtil.e(LogcatTag, "下载完成");
                            isDownloading = false;
                            isDownloadApk = false;
                            if (hasInstallPermission()) {
                                handler.post(() -> installApk(context));
                            }
                            break;

                        case DownloadManager.STATUS_FAILED:
                            LogUtil.e(LogcatTag, "下载失败");
                            isDownloading = false;
                            isDownloadApk = false;
                            break;

                        case DownloadManager.STATUS_RUNNING:
                            int progress = 0;
                            if (bytesTotal > 0) {
                                progress = (int) ((bytesDownloaded * 100) / bytesTotal);
                            }
                            break;
                    }
                    cursor.close();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogUtil.e(LogcatTag, e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 安装已下载的APK
     */
    private void installApk(Context context) {
        // 实现安装逻辑
        File apkFile = UpdateUtil.getDownloadedApk(fileName);
        UpdateUtil.installApk(context, apkFile);
    }

    /**
     * 是否有安装权限
     */
    private boolean hasInstallPermission() {
        return XXPermissions.isGrantedPermission(this,
                PermissionLists.getRequestInstallPackagesPermission());
    }

    private JsonObject getJsonObject(String json) {
        JsonParser parser = new JsonParser();
        return parser.parse(json).getAsJsonObject();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        LogUtil.e(LogcatTag, "onDestroy");
        isForeground = false;
        // 注销网络监听器
        try {
            unregisterReceiver(networkReceiver);
        } catch (Exception e) {
            LogUtil.e(LogcatTag, "网络监听器注销失败: " + e.getMessage());
        }
        super.onDestroy();
    }

    /**
     * 注册新的轨迹
     */
    void registerTrace() {
        String device_no = ServiceIdManagerUtil.getDeviceNo(context);
        HashMap<String, Object> paramsHashMap = new HashMap<>();
        paramsHashMap.put("tid", ServiceIdManagerUtil.terminalId);
        paramsHashMap.put("trid", ServiceIdManagerUtil.trid);
        paramsHashMap.put("device_no", device_no);

        PostParams postParams = new PostParams();
        RequestBody requestBody = postParams.getGsonRequestBody(paramsHashMap);
        LogUtil.e("okhttp", "requestBody:" + gson.toJson(paramsHashMap));

        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.registerTrace(requestBody);

        StringBuffer uploadLog = new StringBuffer();
        uploadLog.append("registerTrace记录:\n");
        uploadLog.append("paramsHashMap:").append(gson.toJson(paramsHashMap));


        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                String json = gson.toJson(response.body());
                LogUtil.e("okhttp", "onResponse:" + json);
                uploadLog.append("ok:").append(json);

                ServiceIdManagerUtil.uploadLog.add(uploadLog.toString());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e("okhttp", "onFailure:" + t.getMessage());

                uploadLog.append("onFailure:").append(t.getMessage());

                ServiceIdManagerUtil.uploadLog.add(uploadLog.toString());
            }
        });
    }

}
