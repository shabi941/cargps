package com.project.cargps.bean;

public class MyLocation {
    public double  latitude; //纬度
    public double  longitude; //经度


    public long time;
    public double speeds; //速度 km/h

    // GPS质量字段（用于过滤）
    public float accuracy; // GPS精度（米），越小越准
    public int locationType; // 定位类型：1=GPS, 2=基站, 3=WIFI, 4=失败
}
