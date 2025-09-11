package com.manridy.sdkdemo_mrd2019.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.manridy.sdkdemo_mrd2019.R;
import com.manridy.sdkdemo_mrd2019.bluetooth.BleAdapter;

import java.util.ArrayList;

public class SendDialog extends Dialog {
    private boolean isBack = true;

    public SendDialog(Context context) {
        super(context, R.style.MyDialog);
        setContentView(R.layout.dialog_send);
        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = window.getWindowManager().getDefaultDisplay().getWidth();
        lp.height = window.getWindowManager().getDefaultDisplay().getHeight();
        window.setAttributes(lp);
    }

    public void setBack(boolean back) {
        isBack = back;
        setCanceledOnTouchOutside(isBack);
    }

    @Override
    public void onBackPressed() {
        if (isBack) {
            super.onBackPressed();
        }
    }

    public void showDelayed(int time) {
        super.show();
        handler.sendEmptyMessageDelayed(0, time);
    }


    public void show(final BleAdapter adapter, final ArrayList<byte[]> list) {
        super.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (byte[] bytes : list) {
                    adapter.LostWriteData(bytes);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.sendEmptyMessage(0);
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            dismiss();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }
}
