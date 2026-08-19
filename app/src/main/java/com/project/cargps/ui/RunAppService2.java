//package com.project.cargps.ui;
//
//import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
//
//import android.annotation.SuppressLint;
//import android.app.DownloadManager;
//import android.app.KeyguardManager;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.database.Cursor;
//import android.hardware.display.DisplayManager;
//import android.location.Criteria;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.IBinder;
//import android.provider.Settings;
//import android.telephony.TelephonyManager;
//import android.view.Display;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import com.hjq.permissions.XXPermissions;
//import com.hjq.permissions.permission.PermissionLists;
//import com.project.cargps.R;
//import com.project.cargps.base.BaseApplication;
//import com.project.cargps.logcat.LogUtil;
//import com.project.cargps.net.ApiService;
//import com.project.cargps.net.OkHttpManage;
//import com.project.cargps.net.PostParams;
//import com.project.cargps.ui.activity.MainAty;
//import com.project.cargps.util.PermissionUtil;
//import com.project.cargps.util.UpdateUtil;
//
//import java.io.File;
//import java.util.HashMap;
//
//import okhttp3.RequestBody;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class RunAppService2 extends Service {
//    private final String LogcatTag = "RunAppService";
//    private static final int NOTIFICATION_ID = 1;
//    private static final String CHANNEL_ID = "GpsUploadChannel";
//
//    public static boolean isOpenBoot = false;
//
//    private Handler handler = new Handler();
//    private Gson gson = new Gson();
//    private Context context;
//
//    private NotificationManager notificationManager;
//    private NotificationCompat.Builder builder;
//    private LocationManager locationManager;
//    private LocationListener locationListener;
//    private TelephonyManager telephonyManager;
//
//    private DisplayManager displayManager;
//    private KeyguardManager keyguardManager;
//    private DisplayManager.DisplayListener displayListener;
//
//    private String androidId;
//
//    private boolean isForeground = false;
//    private boolean isGPSRunning = false;
//    private boolean isDownloadApk = false;
//
//    @SuppressLint("ForegroundServiceType")
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        LogUtil.e(LogcatTag, "onCreate");
//        context = this;
//        initManagers();
//        createNotificationChannel();
//        registerDisplayListener();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        LogUtil.e(LogcatTag, "onStartCommand");
//        if (!isForeground) {
//            isForeground = true;
//            Notification notification = createNotification();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//                startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
//            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForeground(NOTIFICATION_ID, notification);
//            }
//        }
//        handleBootStartup();
//        return START_STICKY;
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "KLZX-GPS";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    private Notification createNotification() {
//        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
//        builder.setContentTitle("长期运行服务")
//                .setContentText("服务正在后台运行")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setOngoing(true)
//                .setAutoCancel(false)
//                .setShowWhen(false)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .build();
//        return builder.getNotification();
//    }
//
//    private void initManagers() {
//        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
//        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
//    }
//
//    private void registerDisplayListener() {
//        displayListener = new DisplayManager.DisplayListener() {
//            @Override
//            public void onDisplayAdded(int displayId) {
//
//            }
//
//            @Override
//            public void onDisplayRemoved(int displayId) {
//
//            }
//
//            @Override
//            public void onDisplayChanged(int displayId) {
//                Display display = displayManager.getDisplay(displayId);
//                int state = display.getState();
//
//                if (displayId == Display.DEFAULT_DISPLAY) {
//                    switch (state) {
//                        case Display.STATE_ON:
//                            LogUtil.e(LogcatTag, "STATE_ON");
//                            handleScreenOn();
//                            break;
////                        case Display.STATE_OFF:
////                            break;
////                        case Display.STATE_DOZE:
////                            break;
////                        case Display.STATE_DOZE_SUSPEND:
////                            break;
//                    }
//                }
//            }
//        };
//
//        displayManager.registerDisplayListener(displayListener, null);
//    }
//
//    private void handleScreenOn() {
//        LogUtil.e(LogcatTag, "isDeviceUnlocked:" + isDeviceUnlocked() + ",isKeyguardSecure:" + isKeyguardSecure());
//        if (isDeviceUnlocked() || !isKeyguardSecure()) {
//            startMainActivity();
//        } else {
//            scheduleDelayedActivity();
//        }
//    }
//
//    private void scheduleDelayedActivity() {
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (isDeviceUnlocked()) {
//                    startMainActivity();
//                }
//            }
//        }, 3000);
//    }
//
//    private boolean isDeviceUnlocked() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            return !keyguardManager.isDeviceLocked();
//        } else {
//            return !keyguardManager.isKeyguardLocked();
//        }
//    }
//
//    private boolean isKeyguardSecure() {
//        return keyguardManager.isKeyguardSecure();
//    }
//
//    @SuppressLint({"MissingPermission", "NewApi"})
//    private void handleBootStartup() {
//        /*if (!isDownloadApk && PermissionUtil.hasStoragePermission(context)){
//            onUpgradeApk();
//        }else {
//            LogUtil.e(LogcatTag, "没储存权限，isDownloadApk：" +isDownloadApk);
//        }*/
//        if (!isDownloadApk && !isOpenBoot) {
//            onUpgradeApk();
//        }
//
//        if (PermissionUtil.hasPhonePermission(this) && PermissionUtil.hasLocationPermission(this)) {
//            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//            androidId = Settings.Global.getString(context.getContentResolver(), "device_id");
//
//            if (locationListener == null) {
//                locationListener = location -> {
//                    //LogUtil.e("okhttp", "location：" + location.getLongitude() + "," + location.getLatitude());
//                    BaseApplication.currentLocation.setValue(location);
//                    旧LocationManager位置上传已禁用，统一由RunAppService处理。
//                };
//            }
//
//            if (!isGPSRunning) {
//                isGPSRunning = true;
//                requestLocation();
//            }
//        } else {
//            handler.postDelayed(() -> {
//                startMainActivity();
//            }, 2000);
//        }
//    }
//
//    private void startMainActivity() {
//        Intent intent = new Intent(this, MainAty.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra("type", 0);
//        startActivity(intent);
//    }
//
//    /**
//     * 获取定位信息
//     */
//    @SuppressLint("MissingPermission")
//    private void requestLocation() {
//        // 初始化 LocationManager
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        long minTime = 1000L; // 毫秒
//        float minDistance = 0f; // 米
//        Criteria criteria = new Criteria(); // 创建一个定位准则对象
//        // 获取定位管理器的最佳定位提供者
//        String bestProvider = locationManager.getBestProvider(criteria, true);
//        locationManager.requestLocationUpdates(bestProvider, minTime, minDistance, locationListener);
//    }
//
//    /**
//     * 旧定位上传逻辑已禁用
//     */
//    private void disabledLocationUpload(double longitude, double latitude, String id, long time) {
//        String device_no = id == null ? "0" : id;
//        HashMap<String, Object> paramsHashMap = new HashMap<>();
//        paramsHashMap.put("longitude", longitude);
//        paramsHashMap.put("latitude", latitude);
//        paramsHashMap.put("device_no", device_no);
//        paramsHashMap.put("position_time", time);
//        PostParams postParams = new PostParams();
//        RequestBody requestBody = postParams.getGsonRequestBody(paramsHashMap);
//        LogUtil.e("okhttp", "requestBody:" + gson.toJson(paramsHashMap));
//
//        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
//        Call<Object> call = null;
//        call.enqueue(new Callback<Object>() {
//            @Override
//            public void onResponse(Call<Object> call, Response<Object> response) {
//                String json = gson.toJson(response.body());
//                LogUtil.e("okhttp", "onResponse:" + json);
//            }
//
//            @Override
//            public void onFailure(Call<Object> call, Throwable t) {
//                LogUtil.e("okhttp", "onFailure:" + t.getMessage());
//            }
//        });
//    }
//
//    @SuppressLint("MissingPermission")
//    private void removeLocation() {
//        if (locationManager != null) {
//            locationManager.removeUpdates(locationListener);
//        }
//    }
//
//    /**
//     * 查询app更新
//     */
//    private void onUpgradeApk() {
//        isDownloadApk = true;
//        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
//        Call<Object> call = apiService.upGradeApk();
//        call.enqueue(new Callback<Object>() {
//            @Override
//            public void onResponse(Call<Object> call, Response<Object> response) {
//                String json = gson.toJson(response.body());
//                LogUtil.e("okhttp", "onResponse:" + json);
//                JsonObject jsonObject = getJsonObject(json);
//                JsonElement dataElement = jsonObject.get("data");
//                if (dataElement.isJsonObject()) {
//                    JsonObject dataObject = dataElement.getAsJsonObject();
//                    String versions = dataObject.get("versions").getAsString();
//                    String apk_url = dataObject.get("apk_url").getAsString();
//                    String md5_code = dataObject.get("md5_code").getAsString();
//                    if (UpdateUtil.hasNewVersion(context, Integer.parseInt(versions))) {
//                        startDownloadApk(apk_url);
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Object> call, Throwable t) {
//                LogUtil.e("okhttp", "onFailure:" + t.getMessage());
//                isDownloadApk = false;
//            }
//        });
//    }
//
//    /**
//     * 下载apk
//     */
//    private String fileName;
//    private File downloadedApkFile;
//
//    private void startDownloadApk(String apkUrl) {
//        LogUtil.e(LogcatTag, "startDownloadApk");
//        isDownloading = false;
//        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
//        request.setAllowedOverRoaming(false);
//        request.setTitle("应用更新");
//        request.setDescription("正在下载新版本");
//
//        fileName = UpdateUtil.getFileName();
//        // 设置下载路径
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // Android 10+ 使用公共目录
//            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//        } else {
//            // Android 10 以下使用传统目录
//            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            downloadedApkFile = new File(downloadDir, fileName);
//            request.setDestinationUri(Uri.fromFile(downloadedApkFile));
//        }
//
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        request.setMimeType("application/vnd.android.package-archive");
//
//        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//        long downloadId = downloadManager.enqueue(request);
//
//        // 监听下载进度
//        listenDownloadProgress(downloadManager, downloadId);
//    }
//
//    private boolean isDownloading = false;
//
//    @SuppressLint("Range")
//    private void listenDownloadProgress(DownloadManager downloadManager, long downloadId) {
//        new Thread(() -> {
//            isDownloading = true;
//            while (isDownloading) {
//                DownloadManager.Query query = new DownloadManager.Query();
//                query.setFilterById(downloadId);
//
//                Cursor cursor = downloadManager.query(query);
//                if (cursor != null && cursor.moveToFirst()) {
//                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                    long bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
//                    long bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
//
//                    switch (status) {
//                        case DownloadManager.STATUS_SUCCESSFUL:
//                            LogUtil.e(LogcatTag, "下载完成");
//                            isDownloading = false;
//                            isDownloadApk = false;
//                            if (hasInstallPermission()) {
//                                handler.post(() -> installApk(context));
//                            }
//                            break;
//
//                        case DownloadManager.STATUS_FAILED:
//                            LogUtil.e(LogcatTag, "下载失败");
//                            isDownloading = false;
//                            isDownloadApk = false;
//                            break;
//
//                        case DownloadManager.STATUS_RUNNING:
//                            int progress = 0;
//                            if (bytesTotal > 0) {
//                                progress = (int) ((bytesDownloaded * 100) / bytesTotal);
//                            }
//                            break;
//                    }
//                    cursor.close();
//                }
//
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    LogUtil.e(LogcatTag, e.getMessage());
//                }
//            }
//        }).start();
//    }
//
//    /**
//     * 安装已下载的APK
//     */
//    private void installApk(Context context) {
//        // 实现安装逻辑
//        File apkFile = UpdateUtil.getDownloadedApk(fileName);
//        UpdateUtil.installApk(context, apkFile);
//    }
//
//    /**
//     * 是否有安装权限
//     */
//    private boolean hasInstallPermission() {
//        return XXPermissions.isGrantedPermission(this,
//                PermissionLists.getRequestInstallPackagesPermission());
//    }
//
//    private JsonObject getJsonObject(String json) {
//        JsonParser parser = new JsonParser();
//        return parser.parse(json).getAsJsonObject();
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public void onDestroy() {
//        LogUtil.e(LogcatTag, "onDestroy");
//        isForeground = false;
//        super.onDestroy();
//    }
//}
