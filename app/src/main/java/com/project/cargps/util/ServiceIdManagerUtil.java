package com.project.cargps.util;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public class ServiceIdManagerUtil {

    public static long sId;
    public static long tId;
    private static String deviceNo;

    public static long trid;
    public static long terminalId;

    public static List<String> uploadLog = new ArrayList();


     public static String getDeviceNo(Context context){
         if (deviceNo == null){
             deviceNo = Settings.Global.getString(context.getContentResolver(), "device_id");
         }
//         deviceNo = "866275035374959";
         return deviceNo;
     }
}
