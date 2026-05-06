package com.project.cargps.ui.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import com.project.cargps.util.ServiceIdManagerUtil;

import java.util.ArrayList;
import java.util.Objects;

public class HistoryTrackActivity extends AppCompatActivity {

    private AMapTrackClient aMapTrackClient;

    ArrayList<Point> pointList = new ArrayList<>();
    HistoryTrackAdapter trackAdapter;
    TextView tvDistance;
    TextView tvHistoryTrack;
//    TextView tvSeep;

    RecyclerView  rvHistoryTrack;

    StringBuffer stringBuffer = new StringBuffer();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history_track_list);

        tvDistance = findViewById(R.id.tvDistance);
        tvHistoryTrack = findViewById(R.id.tvHistoryTrack);
        rvHistoryTrack = findViewById(R.id.rvHistoryTrack);
//        tvSeep = findViewById(R.id.tvSeep);
        initAdapter();


        try {
            aMapTrackClient = new AMapTrackClient(getApplicationContext());

            queryHistory();

            queryDistance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        getNowSeep();
    }

    private void initAdapter(){
        trackAdapter = new HistoryTrackAdapter(pointList);

        rvHistoryTrack.setAdapter(trackAdapter);
        rvHistoryTrack.setLayoutManager(new LinearLayoutManager(HistoryTrackActivity.this));
    }

    // 搜索最近12小时以内上报的轨迹
    HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(
            ServiceIdManagerUtil.sId,
            ServiceIdManagerUtil.terminalId,
            System.currentTimeMillis() - 24 * 60 * 60 * 1000,
            System.currentTimeMillis(),
            0,      // 不绑路
            0,      // 不做距离补偿
            5000,   // 距离补偿阈值，只有超过5km的点才启用距离补偿
            0,  // 由旧到新排序
            1,  // 返回第1页数据
            100,    // 一页不超过100条
            ""  // 暂未实现，该参数无意义，请留空
    );

    Gson mGson = new Gson();
    private void queryHistory(){

        aMapTrackClient.queryHistoryTrack(historyTrackRequest, new OnTrackListener() {

            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {

            }

            @Override
            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

            }

            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {

            }

            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {

            }


            @Override
            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

            }

            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

            }

            @Override
            public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

            }

            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                if (historyTrackResponse.isSuccess()) {
                    HistoryTrack historyTrack = historyTrackResponse.getHistoryTrack();
                    // historyTrack中包含终端轨迹信息
                    int count = historyTrack.getCount();

                    String logInfo = "查询成功 数量："+ count;
                    LogUtil.d("猎鹰轨迹",logInfo);
                    tvHistoryTrack.setText(logInfo);

                    pointList.clear();
                    pointList.addAll(historyTrack.getPoints());
                    trackAdapter.notifyDataSetChanged();


                } else {
                    // 查询失败

                    LogUtil.d("猎鹰轨迹","查询失败 1，" + historyTrackResponse.getErrorMsg()+" code:"+historyTrackResponse.getErrorCode());


                    stringBuffer.append("查询失败 1 ：").append(historyTrackResponse.getErrorMsg()).append(" code:").append(historyTrackResponse.getErrorCode())
                            .append("\n");

                    tvDistance.setText(stringBuffer);
                }
            }
        });
    }



    private void queryDistance(){
        long curr = System.currentTimeMillis();
        DistanceRequest distanceRequest = new DistanceRequest(
                ServiceIdManagerUtil.sId,
                ServiceIdManagerUtil.terminalId,
                curr - 24 * 60 * 60 * 1000, // 开始时间
                curr,   // 结束时间
                -1  // 轨迹id，传-1表示包含散点在内的所有轨迹点
        );
        aMapTrackClient.queryDistance(distanceRequest, new OnTrackListener() {

            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {

            }

            @Override
            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

            }

            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {
                if (distanceResponse.isSuccess()) {
                    double meters = distanceResponse.getDistance();
                    // 行驶里程查询成功，行驶了meters米
                    LogUtil.d("猎鹰轨迹","里程："+meters+"米");
                    tvDistance.setText("里程："+meters+"米");
                } else {
                    // 行驶里程查询失败
                    stringBuffer.append("查询失败 2 ：").append(distanceResponse.getErrorMsg()).append(" code:").append(distanceResponse.getErrorCode())
                            .append("\n");

                    tvDistance.setText(stringBuffer);
                }
            }

            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {

            }

            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {

            }

            @Override
            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

            }

            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

            }

            @Override
            public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mCountDownTimer.cancel();
//        mCountDownTimer = null;
    }

//    private CountDownTimer mCountDownTimer;
//    private void getNowSeep(){
//        // 使用CountDownTimer（更精确）
//        mCountDownTimer=  new CountDownTimer(Long.MAX_VALUE, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//
//                LogUtil.d("猎鹰轨迹","执行查询");
//                nowCount = (nowCount+1)%3;
//                // 每秒执行一次
//                aMapTrackClient.queryLatestPoint(new LatestPointRequest(
//                        ServiceIdManagerUtil.sId,
//                        ServiceIdManagerUtil.terminalId
//                ), nowOnTrackListener);
//            }
//
//            @Override
//            public void onFinish() {
//                // 不会自动结束
//            }
//        };
//
//        mCountDownTimer.start();
//
//    }

//    int nowCount = 1;
//    private OnTrackListener nowOnTrackListener = new OnTrackListener() {
//        @Override
//        public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
//
//        }
//
//        @Override
//        public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {
//
//        }
//
//        @Override
//        public void onDistanceCallback(DistanceResponse distanceResponse) {
//
//        }
//
//        @Override
//        public void onLatestPointCallback(LatestPointResponse latestPointResponse) {
//            if (latestPointResponse.isSuccess()) {
//                Point point = latestPointResponse.getLatestPoint().getPoint();
//                // 查询实时位置成功，point为实时位置信息
//
//                StringBuffer stringBuffer = new StringBuffer();
//                stringBuffer.append("当前车速:").append(+point.getSpeed()).append("KM/小时");
//                if (nowCount ==1){
//                    stringBuffer.append(",");
//                }else if (nowCount ==2){
//                    stringBuffer.append(",,");
//                }else if (nowCount ==0){
//                    stringBuffer.append(",,,");
//                }
//                tvSeep.setText(stringBuffer);
//            } else {
//                // 查询实时位置失败
//            }
//        }
//
//        @Override
//        public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
//
//        }
//
//        @Override
//        public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {
//
//        }
//
//        @Override
//        public void onAddTrackCallback(AddTrackResponse addTrackResponse) {
//
//        }
//
//        @Override
//        public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {
//
//        }
//    };
}
