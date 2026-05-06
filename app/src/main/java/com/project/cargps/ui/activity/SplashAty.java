package com.project.cargps.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.cargps.R;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.net.ApiService;
import com.project.cargps.net.OkHttpManage;
import com.project.cargps.ui.RunAppService;
import com.project.cargps.util.ServiceIdManagerUtil;
import com.project.cargps.util.UpdateUtil;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashAty extends Activity {

    private Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper()));

    Context mContext;
    Button btnGetDeviceNo;
//    Button btnRetry;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_splash);

        btnGetDeviceNo = findViewById(R.id.btnGetDeviceNo);
//        btnRetry = findViewById(R.id.btnRetry);
        RunAppService.isOpenBoot = false;

        LogUtil.e("SplashAty", "onCreate");

        btnGetDeviceNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceNo();

            }
        });
//
//        btnRetry.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getServiceId();
//            }
//        });

        getDeviceNo();


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

            Intent intent = new Intent(SplashAty.this, MainAty.class);
            intent.putExtra("type", 1);
            startActivity(intent);
            finish();
        }

    }

   /* private void getServiceId(){
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

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(SplashAty.this, MainAty.class);
                                intent.putExtra("type", 1);
                                startActivity(intent);
                                finish();
                            }
                        });
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
    }*/

    private JsonObject getJsonObject(String json) {
        JsonParser parser = new JsonParser();
        return parser.parse(json).getAsJsonObject();
    }
}
