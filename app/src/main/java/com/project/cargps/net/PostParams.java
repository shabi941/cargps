package com.project.cargps.net;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
public class PostParams {
    private Gson gson;
    private HashMap hashMap;

    public PostParams() {
        gson = new Gson();
        this.hashMap = new HashMap<>();
    }

    public PostParams add(String key, Object value) {
        if (key != null && key != "" && value != null && value != "") {
            hashMap.put(key, value);
        }
        return this;
    }

    public HashMap getHashMap() {
        return hashMap;
    }

    public void clearPostParams() {
        hashMap.clear();
    }


    public RequestBody getGsonRequestBody(File file) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        return requestBody;
    }

    public RequestBody getGsonRequestBody(Object obj) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), gson.toJson(obj));
        return requestBody;
    }

    public RequestBody getGsonRequestBody() {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(hashMap));
        return requestBody;
    }

    public static RequestBody requestBodyJson(String[] keys, Object[] values) {
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            Object value = values[i];
            try {//减小数据中某一项对于所有数据的影响
                jsonObject.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=UTF-8"), jsonObject.toString());
        return requestBody;
    }
}
