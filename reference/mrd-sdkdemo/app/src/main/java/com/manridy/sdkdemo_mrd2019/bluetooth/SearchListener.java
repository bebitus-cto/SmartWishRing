package com.manridy.sdkdemo_mrd2019.bluetooth;


import android.bluetooth.BluetoothDevice;

/**
 * Created by Alan on 2018/5/8.
 */

public class SearchListener {
    public interface ScanListener {
        void onLeScan(BluetoothDevice bluetoothDevice, int rssi);
    }
}
