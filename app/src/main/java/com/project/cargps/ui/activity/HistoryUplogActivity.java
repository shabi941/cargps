package com.project.cargps.ui.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.track.AMapTrackClient;
import com.amap.api.track.query.entity.HistoryTrack;
import com.amap.api.track.query.entity.Point;
import com.amap.api.track.query.model.AddTerminalResponse;
import com.amap.api.track.query.model.AddTrackResponse;
import com.amap.api.track.query.model.DistanceRequest;
import com.amap.api.track.query.model.DistanceResponse;
import com.amap.api.track.query.model.HistoryTrackRequest;
import com.amap.api.track.query.model.HistoryTrackResponse;
import com.amap.api.track.query.model.LatestPointRequest;
import com.amap.api.track.query.model.LatestPointResponse;
import com.amap.api.track.query.model.OnTrackListener;
import com.amap.api.track.query.model.ParamErrorResponse;
import com.amap.api.track.query.model.QueryTerminalResponse;
import com.amap.api.track.query.model.QueryTrackResponse;
import com.google.gson.Gson;
import com.project.cargps.R;
import com.project.cargps.logcat.LogUtil;
import com.project.cargps.ui.adapter.HistoryTrackAdapter;
import com.project.cargps.ui.adapter.HistoryUplogAdapter;
import com.project.cargps.util.ServiceIdManagerUtil;

import java.util.ArrayList;

public class HistoryUplogActivity extends AppCompatActivity {


    ArrayList<String> pointList = new ArrayList<>();
    HistoryUplogAdapter trackAdapter;


    RecyclerView  rvHistoryTrack;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_uplog_list);

        rvHistoryTrack = findViewById(R.id.rvHistoryTrack);

        initAdapter();



    }

    private void initAdapter(){
        pointList.addAll(ServiceIdManagerUtil.uploadLog);
        trackAdapter = new HistoryUplogAdapter(pointList);

        rvHistoryTrack.setAdapter(trackAdapter);
        rvHistoryTrack.setLayoutManager(new LinearLayoutManager(HistoryUplogActivity.this));
    }


}
