package com.manridy.sdkdemo_mrd2019.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

/**
 * Created by Alan on 2018/5/31.
 */

public class BleTool {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Context context;
    public static final int requestCode = 8989;

    public BleTool(Context context) {
        this.context = context;
        initialize(context);
    }

    public BluetoothAdapter GetAdapter() {
        return mBluetoothAdapter;
    }

    public boolean initialize(Context context) {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }


    /**
     * 是否支持蓝牙
     *
     * @return
     */
    public boolean hasBleOpen() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 蓝牙是否打开
     *
     * @return
     */
    public boolean isBleOpen() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public boolean openBLE() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        try {
            return mBluetoothAdapter.enable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 系统打开蓝牙
     */
    public void sysOpenBLE(AppCompatActivity activity) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 系统打开蓝牙
     */
    public void sysOpenBLE(FragmentActivity activity) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    public static String ByteToString(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data) {
            stringBuilder.append(String.format("%02X ", byteChar));
        }
        return stringBuilder.toString();
    }

    public static String ByteToString2(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data) {
            stringBuilder.append(String.format("%02X", byteChar));
        }
        return stringBuilder.toString();
    }

    public static byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (i * 8));
        }
        return b;
    }

    /**
     * 将byte数组转换为int数据
     *
     * @param b 字节数组
     * @return 生成的int数据
     */
    public static int byteToInt(byte[] b) {
        int data = 0;
        for (int i = 0; i < b.length; i++) {
            data += (b[i] & 0x0ff) << (i * 8);
        }
        return data;
    }


    public static int[] b_or_int(byte[] data) {
        int[] data_i = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            data_i[i] = data[i] & 0x0ff;
        }
        return data_i;
    }

}
