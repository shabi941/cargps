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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.amap.api.maps.AMapUtils;
//import com.amap.api.maps.model.LatLng;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.project.cargps.R;
import com.project.cargps.base.BaseApplication;
import com.project.cargps.bean.MyLocation;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.logcat.LogViewerActivity;
import com.project.cargps.ui.activity.HistoryTrackActivity;
import com.project.cargps.ui.activity.HistoryUplogActivity;
import com.project.cargps.ui.adapter.GDGpsMessageAdapter;
import com.project.cargps.util.ServiceIdManagerUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;

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
    private final Observer<MyLocation> locationObserver = location -> {
        if (location == null) return;
        getSpeeds(location);
        if (locationMsgList.size() >= 100) {
            locationMsgList.remove(0);
        }
        gpsMessageAdapter.addItem(location);
    };
    private final Observer<Boolean> trackObserver = isSuccess -> { };

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

        BaseApplication.mCurrentLocation.observeForever(locationObserver);
        BaseApplication.createTrackResult.observeForever(trackObserver);

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

                    // GPSDialog只显示本地估算速度，不再调用速度/位置接口。
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

    @Override
    public void dismiss() {
        BaseApplication.mCurrentLocation.removeObserver(locationObserver);
        BaseApplication.createTrackResult.removeObserver(trackObserver);
        super.dismiss();
    }

}
