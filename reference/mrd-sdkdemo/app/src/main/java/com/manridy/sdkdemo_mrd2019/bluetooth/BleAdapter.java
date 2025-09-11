package com.manridy.sdkdemo_mrd2019.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.manridy.sdk_mrd2019.install.MrdPushCore;


public class BleAdapter extends BluetoothGattCallback {
    private final static String TAG = BleAdapter.class.getSimpleName();
    private BleTool mBleTool;
    private BluetoothGatt gatt;
    private BluetoothDevice device;
    private Context context;
    private BleState state = BleState.DISCONNECTED;
    private BluetoothStateListener listener;

    public BleAdapter(Context context) {
        this.context = context;
        mBleTool = new BleTool(context);
    }

    public boolean connect() {
        return connect(device);
    }

    private void showToast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public boolean connect(BluetoothDevice device) {
        this.device = device;
        if (mBleTool.GetAdapter() == null) {
            showToast("未获取到蓝牙");
            Log.e(TAG, " mBleTool.GetAdapter() == null");
            return false;
        }
        if (device == null) {
            showToast("未绑定蓝牙");
            Log.e(TAG, "mBase == null");
            return false;
        }
        putHandler(BleState.CONNECTING, device);
        gatt = device.connectGatt(context, false, this);
        MrdPushCore.getInstance().init(gatt);
        return true;
    }

    public void close() {
        Log.e(TAG, "close");
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        state = BleState.DISCONNECTED;
        if (listener != null) {
            listener.onChange(device, state);
        }
        han.removeCallbacksAndMessages(null);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "mtu is " + mtu + " status is " + status);
        }
        MrdPushCore.getInstance().onMtuChanged(mtu, status);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.e(TAG, "STATE_CONNECTED");
                putHandler(BleState.CONNECTED, gatt.getDevice());
                gatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.e(TAG, "STATE_DISCONNECTED");
                putHandler(BleState.DISCONNECTED, gatt.getDevice());
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.e(TAG, "onServicesDiscovered");
        putHandler(BleState.SERVICES_DISCOVERED, gatt.getDevice());
        enableLostNoti();
    }

    /**
     * 打开读服务
     */
    public Boolean enableLostNoti() {
        BluetoothGattService nableService = gatt.getService(SampleGattAttributes.NotifyServiceUUID);
        if (nableService == null) {
            return false;
        }
        BluetoothGattCharacteristic TxPowerLevel = nableService.getCharacteristic(SampleGattAttributes.NotifyCharacteristicUUID);
        if (TxPowerLevel == null) {
            return false;
        }
        Boolean isNotification = gatt.setCharacteristicNotification(TxPowerLevel, true);
        if (SampleGattAttributes.NotifyCharacteristicUUID.equals(TxPowerLevel.getUuid())) {
            BluetoothGattDescriptor descriptor = TxPowerLevel.getDescriptor(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
            if ((TxPowerLevel.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else if ((TxPowerLevel.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            gatt.writeDescriptor(descriptor);
        }
        return isNotification;
    }


    public boolean LostWriteData(byte[] pwm_data_buf) {
        try {
            if (pwm_data_buf == null) {
                Toast.makeText(context, "参数或秘钥错误！", Toast.LENGTH_LONG).show();
                return false;
            }
            if (gatt == null)
                return false;
            BluetoothGattService alertService = gatt.getService(SampleGattAttributes.WriteServiceUUID);
            if (alertService == null) {
                return false;
            }
            BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(SampleGattAttributes.WriteCharacteristicUUID);
            if (alertLevel == null) {
                return false;
            }
            boolean status = false;
            int storedLevel = alertLevel.getWriteType();
            alertLevel.setValue(pwm_data_buf);
            alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            status = gatt.writeCharacteristic(alertLevel);
            Log.i(TAG, status + "-data=" + BleTool.ByteToString(pwm_data_buf));
            return status;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        byte[] value = characteristic.getValue();
        MrdPushCore.getInstance().readData(value);
        putHandler(value, gatt.getDevice());
        Log.i(TAG, "read=" + BleTool.ByteToString(value));
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        MrdPushCore.getInstance().onCharacteristicWrite(status, characteristic);
        Log.w(TAG, "write=" + BleTool.ByteToString(characteristic.getValue()));
    }

    private String hanKey_Device = "Device";
    private String hanKey_State = "State";
    private String hanKey_Datas = "Datas";
    @SuppressLint("HandlerLeak")
    Handler han = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: {
                    Bundle bundle = msg.getData();
                    BleState newState = (BleState) bundle.getSerializable(hanKey_State);
                    BluetoothDevice device = bundle.getParcelable(hanKey_Device);
                    switch (newState) {
                        case CONNECTING:
                            showToast("设备开始连接");
                            break;
                        case CONNECTED:
                            showToast("蓝牙已连接");
                            break;
                        case DISCONNECTED:
                            showToast("蓝牙已断开");
                            if (gatt != null) {
                                gatt.disconnect();
                                gatt.close();
                            }
                            gatt = null;
                            break;
                        case SERVICES_DISCOVERED:
                            showToast("蓝牙已发现服务");
                            break;
                    }
                    if (listener != null) {
                        listener.onChange(device, newState);
                    }
                    state = newState;
                }
                break;
                case 2: {
                    Bundle bundle = msg.getData();
                    byte[] datas = bundle.getByteArray(hanKey_Datas);
                    BluetoothDevice device = bundle.getParcelable(hanKey_Device);
                    if (listener != null) {
                        listener.onReadChange(device, datas);
                    }
                }
                break;
            }
        }

    };

    private void putHandler(BleState bleState, BluetoothDevice device) {
        Bundle bundle = new Bundle();
        Message message = new Message();
        bundle.putSerializable(hanKey_State, bleState);
        bundle.putParcelable(hanKey_Device, device);
        message.setData(bundle);
        message.what = 1;
        han.sendMessage(message);
    }

    private void putHandler(byte[] datas, BluetoothDevice device) {
        Bundle bundle = new Bundle();
        Message message = new Message();
        bundle.putByteArray(hanKey_Datas, datas);
        bundle.putParcelable(hanKey_Device, device);
        message.setData(bundle);
        message.what = 2;
        han.sendMessage(message);
    }


    public BleState getState() {
        return state;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setListener(BluetoothStateListener listener) {
        this.listener = listener;
    }
}
