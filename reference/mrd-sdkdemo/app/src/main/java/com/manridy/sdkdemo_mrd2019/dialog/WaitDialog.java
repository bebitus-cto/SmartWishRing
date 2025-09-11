package com.manridy.sdkdemo_mrd2019.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.manridy.sdkdemo_mrd2019.R;

public class WaitDialog extends Dialog {
    private TextView tv_text;
    private boolean isBack = true;
    private boolean success = false;

    public WaitDialog(Context context) {
        super(context, R.style.MyDialog);
        setContentView(R.layout.dialog_wait);
        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = window.getWindowManager().getDefaultDisplay().getWidth();
        lp.height = window.getWindowManager().getDefaultDisplay().getHeight();
        window.setAttributes(lp);
        init();
    }

    public boolean isSuccess() {
        return success;
    }

    public void cancel(boolean success) {
        this.success = success;
        super.cancel();
    }

    public void setBack(boolean back) {
        isBack = back;
        setCanceledOnTouchOutside(isBack);
    }

    @Override
    public void onBackPressed() {
        if (isBack) {
            success = false;
            super.onBackPressed();
        }
    }

    public void showDelayed(int time) {
        super.show();
        handler.sendEmptyMessageDelayed(0, time);
    }

    public void showDelayed(int textId, int time) {
        super.show();
        tv_text.setText(textId);
        handler.sendEmptyMessageDelayed(0, time);
    }

    public void showDelayed(String text, int time) {
        super.show();
        tv_text.setText(text);
        handler.sendEmptyMessageDelayed(0, time);
    }

    public void show(int textId) {
        tv_text.setText(textId);
        super.show();
    }

    public void show(String text) {
        tv_text.setText(text);
        super.show();
    }

    public void setText(String text) {
        tv_text.setText(text);
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

    public void init() {
        tv_text = findViewById(R.id.tv_text);
    }
}
