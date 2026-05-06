package com.project.cargps.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtil {
    final private static String FLIE_COMMOM = "TheSharedPreferences";

    private static SharedPreferences.Editor getEditor(Context context) {
        return context.getSharedPreferences(FLIE_COMMOM, Context.MODE_PRIVATE).edit();
    }

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences(FLIE_COMMOM, Context.MODE_PRIVATE);
    }

    public static void putString(Context context, String key, String value) {
        SPUtil.getEditor(context).putString(key, value).commit();
    }

    public static String getString(Context context, String key) {
        return SPUtil.getSP(context).getString(key, "");
    }

    public static void putFloat(Context context, String key, float value) {
        SPUtil.getEditor(context).putFloat(key, value).commit();
    }

    public static float getFloat(Context context, String key) {
        return SPUtil.getSP(context).getFloat(key, 0);
    }

    public static void putLong(Context context, String key, long value) {
        SPUtil.getEditor(context).putLong(key, value).commit();
    }

    public static long getLong(Context context, String key) {
        return SPUtil.getSP(context).getLong(key, 0);
    }

    public static void putInt(Context context, String key, int value) {
        SPUtil.getEditor(context).putInt(key, value).commit();
    }

    public static int getInt(Context context, String key) {
        return SPUtil.getSP(context).getInt(key, 0);
    }

    public static void putBoolean(Context context, String key, Boolean value) {
        SPUtil.getEditor(context).putBoolean(key, value).commit();
    }

    public static Boolean getBoolean(Context context, String key) {
        return SPUtil.getSP(context).getBoolean(key, false);
    }
}
