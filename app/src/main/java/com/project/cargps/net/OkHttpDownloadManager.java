package com.project.cargps.net;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpDownloadManager {
    private Context context;
    private okhttp3.OkHttpClient client;
    private Call currentCall;
    private DownloadListener listener;

    public interface DownloadListener {
        void onDownloadStart();
        void onProgress(int progress);
        void onDownloadSuccess(File file);
        void onDownloadFailed(String error);
    }

    public OkHttpDownloadManager(Context context) {
        this.context = context;
        this.client = new okhttp3.OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
    }

    public void setDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 开始下载APK
     */
    public void downloadApk(String url, String fileName) {
        if (listener != null) {
            listener.onDownloadStart();
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.onDownloadFailed("下载失败: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (listener != null) {
                        listener.onDownloadFailed("服务器错误: " + response.code());
                    }
                    return;
                }

                // 获取文件总长度
                long contentLength = response.body().contentLength();
                InputStream is = null;
                FileOutputStream fos = null;

                try {
                    is = response.body().byteStream();
                    File apkFile = new File(getDownloadDirectory(), fileName);

                    // 删除已存在的文件
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }

                    fos = new FileOutputStream(apkFile);
                    byte[] buffer = new byte[8192];
                    long downloadedLength = 0;
                    int read;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloadedLength += read;

                        // 计算并回调进度
                        if (contentLength > 0) {
                            final int progress = (int) (downloadedLength * 100 / contentLength);
                            if (listener != null) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        listener.onProgress(progress));
                            }
                        }
                    }

                    fos.flush();

                    // 下载成功
                    if (listener != null) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                listener.onDownloadSuccess(apkFile));
                    }

                } catch (Exception e) {
                    if (listener != null) {
                        listener.onDownloadFailed("文件保存失败: " + e.getMessage());
                    }
                } finally {
                    try {
                        if (is != null) is.close();
                        if (fos != null) fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 获取下载目录
     */
    private File getDownloadDirectory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 及以上使用应用私有目录
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        } else {
            // Android 10 以下使用公共下载目录
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            return downloadDir;
        }
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    /**
     * 检查APK文件是否有效
     */
    public static boolean isApkFileValid(Context context, File apkFile) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            return info != null;
        } catch (Exception e) {
            return false;
        }
    }
}