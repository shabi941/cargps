package com.project.cargps.logcat;

import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.project.cargps.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LogViewerActivity extends AppCompatActivity {
    private TextView tvLogContent;
    private Button btnRefresh, btnExport, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        initViews();
        loadLogContent();
    }

    private void initViews() {
        tvLogContent = findViewById(R.id.tv_log_content);
        tvLogContent.setMovementMethod(new ScrollingMovementMethod());

        btnRefresh = findViewById(R.id.btn_refresh);
        btnExport = findViewById(R.id.btn_export);
        btnClear = findViewById(R.id.btn_clear);

        btnRefresh.setOnClickListener(v -> loadLogContent());
        btnExport.setOnClickListener(v -> exportLogs());
        btnClear.setOnClickListener(v -> clearLogs());
    }

    private void loadLogContent() {
        new Thread(() -> {
            String logContent = readLogFile();
            runOnUiThread(() -> tvLogContent.setText(logContent));
        }).start();
    }

    private String readLogFile() {
        StringBuilder sb = new StringBuilder();
        File logFile = new File(LogUtil.getLogFilePath());

        if (!logFile.exists()) {
            return "日志文件不存在";
        }

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "读取日志文件失败: " + e.getMessage();
        }

        return sb.toString();
    }

    private void exportLogs() {
        new Thread(() -> {
            String exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String zipPath = LogManager.exportLogs(this, exportDir);

            runOnUiThread(() -> {
                if (zipPath != null) {
                    // 显示导出成功消息
                    tvLogContent.append("\n\n日志已导出到: " + zipPath);
                } else {
                    tvLogContent.append("\n\n日志导出失败");
                }
            });
        }).start();
    }

    private void clearLogs() {
        LogUtil.clearLogFile();
        loadLogContent();
    }
}
