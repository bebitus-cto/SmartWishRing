package com.manridy.sdkdemo_mrd2019;

import android.app.Application;

import com.manridy.sdk_mrd2019.Manridy;
import com.manridy.sdkdemo_mrd2019.bluetooth.BleAdapter;

public class MyApplication extends Application {
    private BleAdapter adapter;

    @Override
    public void onCreate() {
        super.onCreate();
        adapter = new BleAdapter(this);
        Manridy.init(getApplicationContext());
    }

    public BleAdapter getAdapter() {
        return adapter;
    }
}
