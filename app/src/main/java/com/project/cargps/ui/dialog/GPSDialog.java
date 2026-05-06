package com.project.cargps.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.amap.api.maps.AMapUtils;
//import com.amap.api.maps.model.LatLng;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.google.gson.Gson;
import com.project.cargps.R;
import com.project.cargps.base.BaseApplication;
import com.project.cargps.bean.MyLocation;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.logcat.LogViewerActivity;
import com.project.cargps.ui.activity.HistoryTrackActivity;
import com.project.cargps.net.ApiService;
import com.project.cargps.net.OkHttpManage;
import com.project.cargps.net.PostParams;
import com.project.cargps.ui.activity.HistoryUplogActivity;
import com.project.cargps.ui.adapter.GDGpsMessageAdapter;
import com.project.cargps.util.ServiceIdManagerUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GPSDialog extends Dialog {

    private TextView tvSpeeds;
    private TextView tvMessage;
    private TextView tvLogcat;
    private TextView tvClose;
    private RecyclerView rvGps;

    private TelephonyManager telephonyManager;
    private String androidId;
    private String versionCode;
    private String versionName;

//    private Button btnRetryCreateTrack;
    private ArrayList<MyLocation> locationMsgList;
    private GDGpsMessageAdapter gpsMessageAdapter;

    private long clickTryCreateTrachTime;
    @SuppressLint({"MissingPermission", "NewApi"})
    public GPSDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_gps);

        // 设置窗口尺寸和位置
        int width = WindowManager.LayoutParams.MATCH_PARENT;
        int height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setLayout(width, height);
        // 设置重力为居中
        getWindow().setGravity(Gravity.CENTER);

        //setCancelable(true);
        //setCanceledOnTouchOutside(true);

        tvClose = findViewById(R.id.tvClose);
        tvMessage = findViewById(R.id.tvMessage);
        tvSpeeds = findViewById(R.id.tvSpeeds);
        tvLogcat = findViewById(R.id.tvLogcat);
        rvGps = findViewById(R.id.rvGps);
//        btnRetryCreateTrack = findViewById(R.id.btnRetryCreateTrack);

        findViewById(R.id.tvLogcatReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, HistoryUplogActivity.class);
                context.startActivity(intent);

            }
        });
//        TextView tvHistoryTrack = findViewById(R.id.tvHistoryTrack);
//        tvHistoryTrack.setOnClickListener(v -> {
//
//            Intent intent = new Intent(context, HistoryTrackActivity.class);
//            context.startActivity(intent);
//        });
        tvClose.setOnClickListener(v -> {
            dismiss();
        });

//        btnRetryCreateTrack.setOnClickListener(v->{
//            if (System.currentTimeMillis() - clickTryCreateTrachTime >2000){
//                clickTryCreateTrachTime = System.currentTimeMillis();
//                BaseApplication.tryCreateTrack.postValue(true);
//            }
//
//        });

        locationMsgList = new ArrayList();
        gpsMessageAdapter = new GDGpsMessageAdapter(locationMsgList);
        rvGps.setLayoutManager(new LinearLayoutManager(context));
        rvGps.setAdapter(gpsMessageAdapter);

        // 观察位置变化
//        BaseApplication.currentLocation.observeForever(location -> {
//            gpsMessageAdapter.addItem(location);
//        });

        BaseApplication.mCurrentLocation.observeForever(location -> {

            getSpeeds(location);
            gpsMessageAdapter.addItem(location);

//            rvGps.smoothScrollToPosition(gpsMessageAdapter.getItemCount());
        });

        BaseApplication.createTrackResult.observeForever(isSuccess->{
//            btnRetryCreateTrack.setVisibility(isSuccess?View.GONE:View.VISIBLE);
        });

//        MyLocation myLocation = new MyLocation();
//        myLocation.latitude =  23.219884;
//        myLocation.longitude= 113.599994;
//
//        lastLatLng = new LatLng(23.218205,113.599509);
//        getSpeeds(myLocation);

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        androidId = ServiceIdManagerUtil.getDeviceNo(context);

        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionCode = String.valueOf(packageInfo.versionCode);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        String message = "id:" + androidId + "\n" + "vCode:" + versionCode + "； vName:" + versionName;
        tvMessage.setText(message);

        tvLogcat.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LogViewerActivity.class);
            getContext().startActivity(intent);
        });
    }


    private long firstTime;
    private long lastTime;
    private LatLng lastLatLng;
    void getSpeeds(MyLocation myLocation){

        if (myLocation.latitude <=0  || myLocation.longitude <=0) {
            return;
        }
        if (lastLatLng != null){

            LatLng nowLatLng = new LatLng(myLocation.latitude,myLocation.longitude);
            float distance = AMapUtils.calculateLineDistance(lastLatLng,nowLatLng);

            try {


                long ttt= SystemClock.elapsedRealtime()-lastTime;

                LogUtil.d("猎鹰轨迹","ttt:"+ttt);
//                if (SystemClock.elapsedRealtime()-firstTime>=2000){ //避免开启的一瞬间计算错
                    //计算出每小时 xxKM 的速度
                    //时间差
                    long time = SystemClock.elapsedRealtime()-lastTime;
//                     long time = 960;
                    myLocation.time = time;

                // 1. 将毫秒转换为小时
                double timeHours = time / 1000.0 / 3600.0;
                // 2. 将米转换为公里
                double distanceKilometers = distance / 1000.0;
                // 3. 计算速度 (公里/小时)
                double speeds = distanceKilometers / timeHours;


                    DecimalFormat df = new DecimalFormat("#0.00");
                    String speedsStr = df.format(speeds);
                    float speedsWithTwoDecimals = Float.parseFloat(speedsStr);
                    myLocation.speeds = speedsWithTwoDecimals;

                    uploadSpeed(speedsWithTwoDecimals);

                    //SystemClock.elapsedRealtime()
                    onSaveLocation(myLocation.longitude,myLocation.latitude,androidId,System.currentTimeMillis()/1000,speedsWithTwoDecimals);
                    tvSpeeds.setText("速度："+speedsWithTwoDecimals+"Km/h");
//                }


            } catch (Exception e) {
                tvSpeeds.setText("速度：-- Km/h");
            }

        }

        lastLatLng = new LatLng(myLocation.latitude,myLocation.longitude);
        lastTime = SystemClock.elapsedRealtime(); //毫秒

        if (firstTime<=0){
            firstTime = SystemClock.elapsedRealtime();
        }
    }

    private Gson gson = new Gson();
    /**
     * 注册新的轨迹
     */
    void uploadSpeed(float speed) {

        String deviceNo = ServiceIdManagerUtil.getDeviceNo(getContext());

        HashMap<String, Object> paramsHashMap = new HashMap<>();
        paramsHashMap.put("speed", speed);
        paramsHashMap.put("device_no", deviceNo );

        PostParams postParams = new PostParams();
        RequestBody requestBody = postParams.getGsonRequestBody(paramsHashMap);
        LogUtil.e("okhttp", "requestBody:" + gson.toJson(paramsHashMap));

        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.uploadSpeed(requestBody);

        StringBuffer uploadLog = new StringBuffer();
        uploadLog.append("speed:").append(speed);
        uploadLog.append("device_no:").append( deviceNo);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                String json = gson.toJson(response.body());
                LogUtil.e("okhttp", "onResponse:" + json);

                uploadLog.append("ok:").append( json);

                ServiceIdManagerUtil.uploadLog.add(uploadLog.toString());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e("okhttp", "onFailure:" + t.getMessage());
                uploadLog.append("onFailure:").append( t.getMessage());

                ServiceIdManagerUtil.uploadLog.add(uploadLog.toString());
            }
        });
    }


    /**
     * 保存定位信息
     */
    private void onSaveLocation(double longitude, double latitude, String id, long time,float speed) {
        String device_no = id == null ? "0" : id;
        HashMap<String, Object> paramsHashMap = new HashMap<>();
        paramsHashMap.put("longitude", longitude);
        paramsHashMap.put("latitude", latitude);
        paramsHashMap.put("device_no", device_no);
        paramsHashMap.put("position_time", time);
        paramsHashMap.put("speed", speed); //速度
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
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.e("okhttp", "onFailure:" + t.getMessage());
            }
        });
    }

}
