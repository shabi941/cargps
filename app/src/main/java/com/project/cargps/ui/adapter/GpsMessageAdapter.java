package com.project.cargps.ui.adapter;

import android.location.Location;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.cargps.R;

import java.util.ArrayList;

public class GpsMessageAdapter extends RecyclerView.Adapter {
    private ArrayList<Location> locationMsgList;

    public GpsMessageAdapter(ArrayList<Location> locationMsgList) {
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
        Location item = locationMsgList.get(position);
        ((MyViewHolder) holder).bind(item);
    }

    @Override
    public int getItemCount() {
        return locationMsgList.size();
    }

    public void addItem(Location location){
        locationMsgList.add(location);
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tvGps;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGps = itemView.findViewById(R.id.tvGps);
        }

        public void bind(Location location) {
            String message = "纬度: " + location.getLatitude() +
                    ", 经度: " + location.getLongitude();
            tvGps.setText(message);
            tvGps.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
        }
    }
}
