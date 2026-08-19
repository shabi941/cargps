package com.project.cargps.net;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST(NetConstants.LOCATION_SAVE)
    Call<Object> saveLocationMsg(@Body RequestBody requestBody);

    @GET(NetConstants.UPGRADE_APK)
    Call<Object> upGradeApk();

    // 获取设备终端信息
    @GET(NetConstants.TERMINAL_INFO)
    Call<Object> getTerminalInfo(
        @Query("device_no") String deviceNo
    );

    // 上传设备速度
    @POST(NetConstants.UPLOAD_SPEED)
    Call<Object> uploadSpeed(@Body RequestBody requestBody);

    // 上传设备日志（文件二进制）
    @Multipart
    @POST(NetConstants.UPLOAD_DEVICE_LOG)
    Call<Object> uploadDeviceLog(
        @Query("device_no") String deviceNo,
        @Part MultipartBody.Part file
    );

}
