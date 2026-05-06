package com.project.cargps.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.cargps.R;
import com.project.cargps.bean.MyLocation;

import java.util.ArrayList;

public class GDGpsMessageAdapter extends RecyclerView.Adapter {
    private final ArrayList<MyLocation> locationMsgList;

    public GDGpsMessageAdapter(ArrayList<MyLocation> locationMsgList) {
        this.locationMsgList = locationMsgList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_gps, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyLocation item = locationMsgList.get(position);
        ((MyViewHolder) holder).bind(item,position+1);
    }

    @Override
    public int getItemCount() {
        return locationMsgList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addItem(MyLocation location){
        locationMsgList.add(location);
//        notifyItemInserted(locationMsgList.size());

        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGps;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGps = itemView.findViewById(R.id.tvGps);
        }

        public void bind(MyLocation location,int no) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(no).append(":").append("纬度: ").append(location.latitude).append(";")
                    .append(" 经度: ").append(location.longitude).append("\n")
                    .append("速度：").append(location.speeds).append(";")
                    .append("时间差：").append(location.time);


            tvGps.setText(stringBuffer);
        }
    }
}
