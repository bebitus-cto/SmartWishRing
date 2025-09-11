package com.manridy.sdkdemo_mrd2019.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothStateListener {
    void onChange(BluetoothDevice device, BleState state);
    void onReadChange(BluetoothDevice device, byte[] datas);
}
