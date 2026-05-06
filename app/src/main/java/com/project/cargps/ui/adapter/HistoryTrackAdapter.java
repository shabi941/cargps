package com.project.cargps.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.track.query.entity.Point;
import com.project.cargps.R;

import java.util.ArrayList;

public class HistoryTrackAdapter extends RecyclerView.Adapter {
    private ArrayList<Point> locationMsgList;

    public HistoryTrackAdapter(ArrayList<Point> locationMsgList) {
        this.locationMsgList = locationMsgList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_track, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Point item = locationMsgList.get(position);
        ((MyViewHolder) holder).bind(item);
    }

    @Override
    public int getItemCount() {
        return locationMsgList.size();
    }

    public void addItem(Point location){
        locationMsgList.add(location);
        notifyDataSetChanged();
    }

    public  class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvContent;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
        }

        public void bind(Point point) {

            StringBuffer sb = new StringBuffer();
            sb.append("Lat:")
                    .append(point.getLat())
                    .append("Lng:")
                    .append(point.getLng())
                    .append("速度")
                    .append(point.getSpeed());

            tvContent.setText(sb);
        }
    }
}
