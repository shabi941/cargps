package com.project.cargps.net;

import com.google.gson.JsonSyntaxException;
import com.project.cargps.logcat.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class OkHttpManage {
    private static OkHttpManage instance;
    private Retrofit retrofit;

    // SSL 验证模式
    public enum SSLMode {
        STRICT,     // 严格验证（生产环境）
        LENIENT,    // 宽松验证（测试环境）
        UNSAFE      // 忽略验证（仅开发环境）
    }

    private static SSLMode currentSSLMode = SSLMode.STRICT;

    public static void setSSLMode(SSLMode mode) {
        currentSSLMode = mode;
        // 重置实例以便重新创建配置
        instance = null;
    }

    public static OkHttpManage instance() {
        if (instance == null) {
            instance = new OkHttpManage();
        }
        return instance;
    }

    private OkHttpManage() {
        initNetwork();
    }

    private void initNetwork() {
        // 创建日志拦截器
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 创建响应拦截器，用于处理空响应
        Interceptor responseInterceptor = chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);

            // 复制响应体以便多次读取
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String bodyString = responseBody.string();
                // 记录响应体内容
                //LogUtil.e("okhttp", "Response Body: " + bodyString);
                // 检查是否为空响应
                if (bodyString.isEmpty()) {
                    //LogUtil.e("okhttp", "Empty response body for URL: " + request.url());
                }
                // 重新创建响应体
                MediaType contentType = responseBody.contentType();
                ResponseBody newResponseBody = ResponseBody.create(contentType, bodyString);

                return response.newBuilder()
                        .body(newResponseBody)
                        .build();
            }
            return response;
        };

        okhttp3.OkHttpClient.Builder okHttpClientBuilder = new okhttp3.OkHttpClient
                .Builder()
                .addInterceptor(interceptor)
                .addInterceptor(responseInterceptor) // 添加响应拦截器
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        // 根据SSL模式配置
        configureSSL(okHttpClientBuilder);
        okhttp3.OkHttpClient okHttpClient = okHttpClientBuilder.build();

        retrofit = new Retrofit
                .Builder()
                .baseUrl(NetConstants.BASE_URL1)
                // 响应结果的解析器，包含gson，xml，protobuf
                .addConverterFactory(GsonConverterFactory.create())
                // 使用Rxjava对回调数据进行处理
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
    }

    /**
     * 配置SSL验证
     */
    private void configureSSL(okhttp3.OkHttpClient.Builder builder) {
        switch (currentSSLMode) {
            case STRICT:
                // 使用系统默认的SSL验证
                break;

            case LENIENT:

            case UNSAFE:
                // 不安全模式：完全忽略SSL验证（仅用于开发）
                // 宽松模式：信任所有证书但验证主机名
                try {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                }

                                @Override
                                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                }

                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[]{};
                                }
                            }
                    };

                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                    builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
                    builder.hostnameVerifier((hostname, session) -> true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public <T> T create(final Class<T> service) {
        return retrofit.create(service);
    }

    /**
     * 重新创建网络客户端（当SSL模式改变时）
     */
    public void recreate() {
        instance = new OkHttpManage();
    }

    /**
     * 处理网络请求错误
     */
    public static String handleError(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return "连接超时，请检查网络后重试";
        } else if (throwable instanceof ConnectException) {
            return "网络连接失败，请检查网络设置";
        } else if (throwable instanceof UnknownHostException) {
            return "无法连接到服务器，请检查网络";
        } else if (throwable instanceof JsonSyntaxException) {
            return "数据解析失败，请稍后重试";
        } else if (throwable instanceof IOException) {
            return "网络异常，请检查网络连接";
        } else {
            return "请求失败: " + throwable.getMessage();
        }
    }

    /**
     * 检查响应是否有效
     */
    public static boolean isValidResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            LogUtil.e("okhttp", "Response is null or empty");
            return false;
        }

        // 检查是否是有效的JSON
        try {
            new JSONObject(response);
            return true;
        } catch (JSONException e) {
            LogUtil.e("okhttp", "Response is not valid JSON: " + response);

            // 如果不是JSON，检查是否是其他有效内容
            return !response.trim().isEmpty();
        }
    }

    /**
     * 安全解析JSON响应
     */
    public static String safeParseResponse(String response) {
        if (!isValidResponse(response)) {
            return "{}"; // 返回空JSON对象
        }

        try {
            // 尝试解析为JSON
            JSONObject jsonObject = new JSONObject(response);
            return jsonObject.toString();
        } catch (JSONException e) {
            LogUtil.e("okhttp", "Failed to parse response as JSON, returning raw response");
            return response;
        }
    }
}