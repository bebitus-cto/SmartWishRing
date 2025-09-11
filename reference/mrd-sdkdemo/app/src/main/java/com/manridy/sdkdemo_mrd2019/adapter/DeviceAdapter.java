package com.manridy.sdkdemo_mrd2019.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.manridy.sdkdemo_mrd2019.R;

import java.util.ArrayList;
import java.util.HashMap;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {
    private HashMap<String, BluetoothDevice> deviceMap = new HashMap<>();
    private ArrayList<String> deviceList = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    public void addDevice(BluetoothDevice bleBase) {
        if (deviceMap.containsKey(bleBase.getAddress())) {
            return;
        }
        deviceList.add(bleBase.getAddress());
        deviceMap.put(bleBase.getAddress(), bleBase);
        notifyDataSetChanged();
    }

    public void clear() {
        deviceMap.clear();
        deviceList.clear();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener OnItemClickListener) {
        this.mOnItemClickListener = OnItemClickListener;
    }

    @Override
    @NonNull
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(
                LayoutInflater.from(
                        parent.getContext()).inflate(
                        R.layout.item_binding,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.bindData(getItem(position), position);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public BluetoothDevice getItem(int position) {
        if (deviceList.size() > position) {
            return deviceMap.get(deviceList.get(position));
        }
        return null;
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView tvDeviceName;
        private TextView tvDeviceMac;
        private int position = 0;


        public MyViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceMac = itemView.findViewById(R.id.tv_device_mac);
            itemView.setOnClickListener(this);
        }

        public void bindData(BluetoothDevice device, int position) {
            this.position = position;
            String name = device.getName();
            if (name == null) {
                name = "UNKNOW DEVICE";
            } else if (name.isEmpty()) {
                name = "UNKNOW DEVICE";
            }
            tvDeviceName.setText(name);
            tvDeviceMac.setText(device.getAddress());
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(position);
            }
        }
    }

}
