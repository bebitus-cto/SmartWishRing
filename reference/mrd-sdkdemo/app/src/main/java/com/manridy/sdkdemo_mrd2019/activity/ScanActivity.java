package com.manridy.sdkdemo_mrd2019.activity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.manridy.sdkdemo_mrd2019.MyApplication;
import com.manridy.sdkdemo_mrd2019.R;
import com.manridy.sdkdemo_mrd2019.adapter.DeviceAdapter;
import com.manridy.sdkdemo_mrd2019.bluetooth.SearchBle;
import com.manridy.sdkdemo_mrd2019.bluetooth.SearchListener;

public class ScanActivity extends AppCompatActivity implements SearchListener.ScanListener, View.OnClickListener {
    private final static String TAG = ScanActivity.class.getSimpleName();
    private RecyclerView rv_device;
    private DeviceAdapter mDeviceAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        init();
    }

    private void init() {
        SearchBle.getInstance(getApplicationContext()).addListener(this);
        findViewById(R.id.button).setOnClickListener(this);
        rv_device = findViewById(R.id.rv_device);
        mDeviceAdapter = new DeviceAdapter();
        rv_device.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_device.setAdapter(mDeviceAdapter);
        mDeviceAdapter.setOnItemClickListener(position -> {
            ((MyApplication) getApplication()).getAdapter().connect(mDeviceAdapter.getItem(position));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SearchBle.getInstance(getApplicationContext()).removeListener(this);
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi) {
        Log.e(TAG, "bluetoothDevice=" + bluetoothDevice.getName());
        mDeviceAdapter.addDevice(bluetoothDevice);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            if (!SearchBle.getInstance(this).search()) {
                Toast.makeText(ScanActivity.this, "请检查蓝牙是否有开启", Toast.LENGTH_LONG).show();
            }
            mDeviceAdapter.clear();
        }
    }
}
