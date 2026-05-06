package com.project.cargps.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.hjq.permissions.permission.base.IPermission;
import com.project.cargps.R;
import com.project.cargps.base.BaseApplication;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.net.ApiService;
import com.project.cargps.net.OkHttpManage;
import com.project.cargps.ui.dialog.GPSDialog;
import com.project.cargps.util.AppSignUtil;
import com.project.cargps.util.PermissionUtil;
import com.project.cargps.util.ServiceIdManagerUtil;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainAty extends AppCompatActivity {
    private final String LogcatTag = "MainAty";

    private int PERMISSION_REQUEST_CODE = 1088;

    private TextView tvLogcat;
    Context mContext;
    private Gson gson = new Gson();
    private int type = 0;//0启动，1显示弹窗,下载更新

    ArrayList<IPermission> permissionList = new ArrayList<>();

    Button btnGetDeviceNo;
    Button btnRetry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //tvLogcat = findViewById(R.id.tvLogcat);

        mContext = this;
        ServiceIdManagerUtil.getDeviceNo(mContext);

        btnGetDeviceNo = findViewById(R.id.btnGetDeviceNo);
        btnRetry = findViewById(R.id.btnRetry);

        btnGetDeviceNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceNo();

            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getServiceId();
            }
        });

        if (ServiceIdManagerUtil.sId != 0){
            toRequestPermission();
        }else {
            getDeviceNo();
        }




        /*tvLogcat.setOnClickListener(v -> {
            Intent intent = new Intent(MainAty.this, LogViewerActivity.class);
            startActivity(intent);
        });*/
    }

    private void toRequestPermission(){
        LogUtil.e("MainAty", "onCreate");

        LogUtil.e("SHA1", AppSignUtil.getSingInfo(this,getPackageName(),AppSignUtil.SHA1));

        type = getIntent().getIntExtra("type", 0);

        permissionList.clear();
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getRequestInstallPackagesPermission())) {
            permissionList.add(PermissionLists.getRequestInstallPackagesPermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getAccessFineLocationPermission())) {
            permissionList.add(PermissionLists.getAccessFineLocationPermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getAccessCoarseLocationPermission())) {
            permissionList.add(PermissionLists.getAccessCoarseLocationPermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getAccessBackgroundLocationPermission())) {
            permissionList.add(PermissionLists.getAccessBackgroundLocationPermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getReadPhoneStatePermission())) {
            permissionList.add(PermissionLists.getReadPhoneStatePermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getPostNotificationsPermission())) {
            permissionList.add(PermissionLists.getPostNotificationsPermission());
        }
        if (!XXPermissions.isGrantedPermission(this,
                PermissionLists.getRequestIgnoreBatteryOptimizationsPermission())) {
            permissionList.add(PermissionLists.getRequestIgnoreBatteryOptimizationsPermission());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!XXPermissions.isGrantedPermission(this,
                    PermissionLists.getReadMediaImagesPermission())) {
                permissionList.add(PermissionLists.getReadMediaImagesPermission());
            }
            if (!XXPermissions.isGrantedPermission(this,
                    PermissionLists.getReadMediaVideoPermission())) {
                permissionList.add(PermissionLists.getReadMediaVideoPermission());
            }
            if (!XXPermissions.isGrantedPermission(this,
                    PermissionLists.getReadMediaAudioPermission())) {
                permissionList.add(PermissionLists.getReadMediaAudioPermission());
            }
        } else {
            if (!XXPermissions.isGrantedPermission(this,
                    PermissionLists.getWriteExternalStoragePermission())) {
                permissionList.add(PermissionLists.getWriteExternalStoragePermission());
            }
        }

        onRequestPermission();
    }
    /**
     * 请求权限
     */
    @SuppressLint({"MissingPermission", "NewApi"})
    private void onRequestPermission() {
        if (!permissionList.isEmpty()) {
            LogUtil.e("RequestPermission");
            XXPermissions.with(MainAty.this)
                    .permissions(permissionList)
                    .request((grantedList, deniedList) -> {
                        onSendBroadcast();
                    });
        } else {
            LogUtil.e("HasPermission");
            onSendBroadcast();
        }
    }

    private void  onSendBroadcast(){

        LogUtil.e("onSendBroadcast");

        if (PermissionUtil.hasPhonePermission(this) &&
                PermissionUtil.hasLocationPermission(this)) {
            BaseApplication.onSendBroadcast();
            if (type == 1) {
                onShowDialog();
            }
        }
        if (type == 0) {
            finish();
        }
    }

    private GPSDialog gpsDialog;

    private void onShowDialog() {
        gpsDialog = new GPSDialog(this);
        gpsDialog.setOnDismissListener(dialog -> {
            finish();
        });
        gpsDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
















    private void getDeviceNo(){
        String deviceNo =  ServiceIdManagerUtil.getDeviceNo(this);
        if (deviceNo == null || deviceNo.isEmpty()){
            btnGetDeviceNo.setVisibility(View.VISIBLE);
            Toast.makeText(mContext,"获取设备Id失败，请手动获取",Toast.LENGTH_SHORT).show();
        }else {
            //先获取serviceId
            btnGetDeviceNo.setVisibility(View.GONE);
//            getServiceId();

            toRequestPermission();
        }

    }

    private void getServiceId(){
        ApiService apiService = OkHttpManage.instance().create(ApiService.class);
        Call<Object> call = apiService.getTerminalInfo( ServiceIdManagerUtil.getDeviceNo(this));
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(response.body());
                    LogUtil.d("okhttp", "onResponse:" + json);
                    JsonObject jsonObject = getJsonObject(json);
                    JsonElement dataElement = jsonObject.get("data");
                    if (dataElement.isJsonObject()) {
                        JsonObject dataObject = dataElement.getAsJsonObject();
                        String sid = dataObject.get("sid").getAsString();
                        long tid = dataObject.get("tid").getAsLong();

//                        ServiceIdManagerUtil.sid = tid;
                        ServiceIdManagerUtil.sId = Long.parseLong(sid);
//                        ServiceIdManagerUtil.sId = 1058095;
                        ServiceIdManagerUtil.tId = tid;
//                        ServiceIdManagerUtil.tId = 1818782043;

//                        mHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Intent intent = new Intent(SplashAty.this, MainAty.class);
//                                intent.putExtra("type", 1);
//                                startActivity(intent);
//                                finish();
//                            }
//                        });

                        if (ServiceIdManagerUtil.sId != 0){
                            toRequestPermission();
                        }else {
                            btnRetry.setVisibility(View.VISIBLE);
                            Toast.makeText(mContext,"网络请求失败，请手动获取",Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {

                    btnRetry.setVisibility(View.VISIBLE);
                    Toast.makeText(mContext,"网络请求失败，请手动获取",Toast.LENGTH_SHORT).show();
                    LogUtil.d("解析失败",e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                LogUtil.d("获取失败",t.getMessage());
                btnRetry.setVisibility(View.VISIBLE);
                Toast.makeText(mContext,"网络请求异常，请手动获取",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private JsonObject getJsonObject(String json) {
        JsonParser parser = new JsonParser();
        return parser.parse(json).getAsJsonObject();
    }
}
