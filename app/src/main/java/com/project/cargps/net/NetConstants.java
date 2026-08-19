package com.project.cargps.net;

public class NetConstants {
    public static final String BASE_URL1 = "https://cargps.zdeps.com/";
    public static final String BASE_URL2 = "https://zdyban.zdeps.com/";

    public static final String LOCATION_SAVE = BASE_URL1+"api/index/positionSave";//保存定位数据
    public static final String UPGRADE_APK = BASE_URL2+"Upgrade/gpsApk";//查询apk更新
    public static final String TERMINAL_INFO = "/api/index/getTerminalInfo"; //获取设备终端信息

    public static final String UPLOAD_SPEED = "/api/index/uploadSpeed"; //上传设备速度
    public static final String UPLOAD_DEVICE_LOG = "/api/index/uploadDeviceLog"; //上传设备日志


}
