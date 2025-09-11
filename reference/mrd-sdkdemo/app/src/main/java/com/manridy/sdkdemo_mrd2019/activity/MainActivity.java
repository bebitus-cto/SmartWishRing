package com.manridy.sdkdemo_mrd2019.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.manridy.sdk_mrd2019.Manridy;
import com.manridy.sdk_mrd2019.bean.common.DrinkReminderBean;
import com.manridy.sdk_mrd2019.bean.send.AppPush;
import com.manridy.sdk_mrd2019.bean.send.AppPushEnum;
import com.manridy.sdk_mrd2019.bean.send.DayRepeatFlag;
import com.manridy.sdk_mrd2019.bean.send.MrdClock;
import com.manridy.sdk_mrd2019.bean.send.MrdEventClock;
import com.manridy.sdk_mrd2019.bean.send.MrdHeartBloodAlert;
import com.manridy.sdk_mrd2019.bean.send.MrdNotDisturb;
import com.manridy.sdk_mrd2019.bean.send.MrdSedentary;
import com.manridy.sdk_mrd2019.bean.send.MrdUserInfo;
import com.manridy.sdk_mrd2019.bean.send.MrdWeather;
import com.manridy.sdk_mrd2019.bean.send.SystemEnum;
import com.manridy.sdk_mrd2019.create.CompareBinStreamCore;
import com.manridy.sdk_mrd2019.install.MrdPushConst;
import com.manridy.sdk_mrd2019.install.MrdPushCore;
import com.manridy.sdk_mrd2019.install.MrdPushParse;
import com.manridy.sdk_mrd2019.ota.OTAEnum;
import com.manridy.sdk_mrd2019.ota.OTAListener;
import com.manridy.sdk_mrd2019.ota.syd.SYDManager;
import com.manridy.sdk_mrd2019.read.MrdReadEnum;
import com.manridy.sdk_mrd2019.read.MrdReadRequest;
import com.manridy.sdk_mrd2019.send.MrdSendListRequest;
import com.manridy.sdkdemo_mrd2019.MyApplication;
import com.manridy.sdkdemo_mrd2019.R;
import com.manridy.sdkdemo_mrd2019.adapter.ParseDataAdapter;
import com.manridy.sdkdemo_mrd2019.bluetooth.BleState;
import com.manridy.sdkdemo_mrd2019.bluetooth.BluetoothStateListener;
import com.manridy.sdkdemo_mrd2019.dialog.SendDialog;
import com.manridy.sdkdemo_mrd2019.dialog.WaitDialog;
import com.realsil.sdk.core.RtkConfigure;
import com.realsil.sdk.core.RtkCore;
import com.realsil.sdk.dfu.DfuConstants;
import com.realsil.sdk.dfu.RtkDfu;
import com.realsil.sdk.dfu.model.DfuConfig;
import com.realsil.sdk.dfu.model.DfuProgressInfo;
import com.realsil.sdk.dfu.model.OtaDeviceInfo;
import com.realsil.sdk.dfu.model.OtaModeInfo;
import com.realsil.sdk.dfu.model.Throughput;
import com.realsil.sdk.dfu.utils.ConnectParams;
import com.realsil.sdk.dfu.utils.DfuAdapter;
import com.realsil.sdk.dfu.utils.GattDfuAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity
        extends AppCompatActivity
        implements View.OnClickListener, BluetoothStateListener {

    private MyApplication application;
    private TextView tv_device_name, tv_device_state, text;
    private RecyclerView rv_body;
    private ParseDataAdapter dataAdapter;
    private StringBuffer appendBuffer = new StringBuffer();
    private SendDialog sendDialog;
    private WaitDialog waitDialog;
    private int index = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this, new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1000);
        }
    }

    private void initView() {
        sendDialog = new SendDialog(this);
        waitDialog = new WaitDialog(this);
        application = (MyApplication) getApplication();
        application.getAdapter().setListener(this);
        findViewById(R.id.lin_scan).setOnClickListener(this);
        findViewById(R.id.lin_device).setOnClickListener(this);
        tv_device_name = findViewById(R.id.tv_device_name);
        tv_device_state = findViewById(R.id.tv_device_state);
        text = findViewById(R.id.text);
        rv_body = findViewById(R.id.rv_body);
        dataAdapter = new ParseDataAdapter();
        rv_body.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_body.setAdapter(dataAdapter);
        onChange(application.getAdapter().getDevice(), application.getAdapter().getState());
    }

    
    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.lin_device:
                if (application.getAdapter().getDevice() == null) {
                    application.getAdapter().close();
                    intent = new Intent(this, ScanActivity.class);
                    startActivity(intent);
                } else {
                    switch (application.getAdapter().getState()) {
                        case CONNECTING:
                            break;
                        case CONNECTED:
                        case SERVICES_DISCOVERED:
                            application.getAdapter().close();
                            break;
                        case DISCONNECTED:
                            application.getAdapter().connect();
                            break;
                    }
                }
                return;
            case R.id.lin_scan:
                application.getAdapter().close();
                intent = new Intent(this, ScanActivity.class);
                startActivity(intent);
                return;
            case R.id.bt_clean_text:
                appendBuffer.setLength(0);
                text.setText("暂无数据");
                dataAdapter.clear();
                return;
        }
        if (application.getAdapter().getState() != BleState.SERVICES_DISCOVERED) {
            Toast.makeText(MainActivity.this, "请先连接设备！", Toast.LENGTH_LONG).show();
            return;
        }
        switch (v.getId()) {
            case R.id.bt_setTimingTempTest:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setTimingTempTest(true, 30).getDatas());
                break;
            case R.id.bt_getTimingTempTest:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getTimingTempTest().getDatas());
                break;
            case R.id.bt_algorithm_parameters_modification:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setAlgorithmParameters(120, 100, 1850, 75, 75, 22).getDatas());
                break;
            case R.id.bt_get_algorithm_parameters_modification:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getAlgorithmParameters().getDatas());
                break;
            case R.id.bt_algorithm_parameters_modification2:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setAlgorithmParameters2(10, 20, 10, 20, 10, 20, 10, 20).getDatas());
                break;
            case R.id.bt_get_algorithm_parameters_modification2:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getAlgorithmParameters2().getDatas());
                break;
            case R.id.bt_algorithm_parameters_modification3:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setAlgorithmParameters3(10, 20, 10, 20).getDatas());
                break;
            case R.id.bt_get_algorithm_parameters_modification3:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getAlgorithmParameters3().getDatas());
                break;
            case R.id.broadcastVitalSignsData:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().broadcastVitalSignsData().getDatas());
                break;
            case R.id.broadcastAlgorithmDebugData:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().broadcastAlgorithmDebugData().getDatas());
                break;
            case R.id.getVitalsOverrideParams:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getVitalsOverrideParams().getDatas());
                break;
            case R.id.bt_algorithm_parameters_acquisition:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getAlgorithmParameters().getDatas());
                break;
            case R.id.bt_advertisement_acquisition:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getAdvertisement().getDatas());
                break;
            case R.id.bt_advertisement_modification:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setAdvertisement(100, 1).getDatas());
                break;
            case R.id.bt_firmware:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSystem(SystemEnum.version).getDatas());
                break;
            case R.id.bt_battery:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSystem(SystemEnum.battery).getDatas());
                break;
            case R.id.bt_brightness:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSystem(SystemEnum.brightness, 255).getDatas());
                break;
            case R.id.bt_restore:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSystem(SystemEnum.restore).getDatas());
                break;
            case R.id.bt_get_screen_info:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSystem(SystemEnum.WatchScreenInfo).getDatas());
                break;
            case R.id.bt_set_time:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setTime().getDatas());
                break;
            case R.id.bt_set_clock:
                ArrayList<MrdEventClock> clocks = new ArrayList<>();
                clocks.add(new MrdEventClock(true, 10, 38, "测试1", DayRepeatFlag.values()));
                long date = YMDHM2Long("2022-12-17 10:39");
                Log.i("mrd", "date is " + date);
                clocks.add(new MrdEventClock(true, date, "测试2"));
                MrdSendListRequest listRequest = Manridy.getMrdSend().setEventClock(clocks);
                if (listRequest.getStatus() == 1) {
                    sendDialog.show(application.getAdapter(), listRequest.getDataList());
                }
                break;
            case R.id.bt_get_clock:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getClock().getDatas());
                break;
            case R.id.bt_get_history:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHistoryData(index, true, 21, 9, 6).getDatas());
                index += 1;
                if (index > 0x0b) {
                    index = 1;
                }
//                for (int i = 1; i <= 7; i++) {
//                    for (int j = 1; j <= 11; j++) {
//                        application.getAdapter().LostWriteData(Manridy.getMrdSend().getHistoryData(j, true, 21, 9, i).getDatas());
//                    }
//                }
                break;
            case R.id.bt_get_Step:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStep(3).getDatas());
                break;
            case R.id.bt_get_StepHistoryNum:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStepHistoryNum().getDatas());
                break;
            case R.id.bt_get_StepHistoryData:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStepHistoryData().getDatas());
                break;
            case R.id.bt_get_step_history_time:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStepHistoryData(21, 7, 19).getDatas());
                break;
            case R.id.bt_set_userinfo:
                MrdUserInfo userInfo = new MrdUserInfo();
                userInfo.setHeight(170);
                userInfo.setWeight(60);
                userInfo.setSex(1);
                userInfo.setAge(20);
                userInfo.setWalk(100);
                userInfo.setRun(100);
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setUserInfo(userInfo).getDatas());
                break;
            case R.id.bt_load_userinfo:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getUserInfo().getDatas());
                break;
            case R.id.bt_set_SportTarget:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setSportTarget(10000, 6000).getDatas());
                break;
            case R.id.bt_load_sport_target:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSportTarget().getDatas());
                break;
            case R.id.bt_set_EcgHrTest:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setHrTest(4).getDatas());
                break;
            case R.id.bt_set_HrTest:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setHrTest(7).getDatas());
                break;
            case R.id.bt_get_HrData_0:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHrData(0).getDatas());
                break;
            case R.id.bt_get_HrData_1:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHrData(1).getDatas());
                break;
            case R.id.bt_get_HrData_2:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHrData(2).getDatas());
                break;
            case R.id.bt_set_hrv_test:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setHrTest(9).getDatas());
                break;
            case R.id.bt_get_hrv:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHRVHistory(2).getDatas());
                break;
            case R.id.bt_get_EcgHrData_0:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getEcgHrData(0).getDatas());
                break;
            case R.id.bt_get_EcgHrData_1:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getEcgHrData(1).getDatas());
                break;
            case R.id.bt_get_EcgHrData_2:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getEcgHrData(2).getDatas());
                break;
            case R.id.bt_get_bp:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getBpData(0).getDatas());
                break;
            case R.id.bt_get_bo:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getBoData(0).getDatas());
                break;
            case R.id.bt_get_Sleep_0:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSleep(0).getDatas());
                break;
            case R.id.bt_get_Sleep_1:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSleep(1).getDatas());
                break;
            case R.id.bt_get_Sleep_2:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSleep(2).getDatas());
                break;
            case R.id.bt_app_push:
                ArrayList<byte[]> list = new ArrayList<>();
                AppPush appPush = new AppPush();
                appPush.setPushEnum(AppPushEnum.WeChat);
                appPush.setContent("测试");
                appPush.setPushName("曼瑞德");
                appPush.setInfoId(1);
                list.addAll(Manridy.getMrdSend().appPush(appPush).getDataList());
                sendDialog.show(application.getAdapter(), list);
                break;
            case R.id.bt_findDevice:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().findDevice(2).getDatas());
                break;
            case R.id.bt_Wrist:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setWristOnOff(true).getDatas());
                break;
            case R.id.bt_load_wrist:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().loadWristStatus().getDatas());
                break;
            case R.id.bt_Sedentary:
                MrdSedentary mrdSedentary = new MrdSedentary(true, true, "9:00", "19:00", "22:00", "8:00", 30);
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setSedentary(mrdSedentary).getDatas());
                break;
            case R.id.bt_load_sedentary:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSedentaryInfo().getDatas());
                break;
            case R.id.bt_set_drink_info:
                DrinkReminderBean reminderBean = new DrinkReminderBean(true, "9:00", "20:00", true, "12:30", "14:00", 20);
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setDrinkReminderInfo(reminderBean).getDatas());
                break;
            case R.id.bt_get_drink_info:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getDrinkReminderInfo().getDatas());
                break;
            case R.id.bt_setUnit:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setUnit(1, 1).getDatas());
                break;
            case R.id.bt_load_unit:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getUnit().getDatas());
                break;
            case R.id.bt_load_hour_unit:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getHourSelect().getDatas());
                break;
            case R.id.bt_setHourSelect:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setHourSelect(1).getDatas());
                break;
            case R.id.bt_setCamera:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setCameraViewOnOff(1).getDatas());
                break;
            case R.id.bt_setTimingHrTest:
//                application.getAdapter().LostWriteData(Manridy.getMrdSend().setTimingHrTest(true, 30).getDatas());
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getTimingHrTest().getDatas());
                break;
            case R.id.bt_setLightTime:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setLightTime(30).getDatas());
//                application.getAdapter().LostWriteData(Manridy.getMrdSend().getLightTime().getDatas());
                break;
            case R.id.bt_setWeather:
                ArrayList<MrdWeather> weathers = new ArrayList<>();
                MrdWeather weather = new MrdWeather(0, 30, 20, 28);
                weathers.add(weather);
                MrdWeather weathe = new MrdWeather(1, 30, 20, 25);
                weathers.add(weathe);
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setWeather(weathers).getDatas());
                break;
            case R.id.bt_setDoNotDisturbCmd:
                MrdNotDisturb notDisturb = new MrdNotDisturb(true, "20:00", "09:00");
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setDoNotDisturbCmd(notDisturb).getDatas());
                break;
            case R.id.bt_getDoNotDisturbCmd:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getDoNotDisturbCmd().getDatas());
                break;
            case R.id.bt_setHeartBloodAlert:
                MrdHeartBloodAlert mrdHeartBloodAlert = new MrdHeartBloodAlert(true, true, 180, 200);
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setHeartBloodAlert(mrdHeartBloodAlert).getDatas());
                break;
            case R.id.bt_setlanguage:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setlanguage(index++).getDatas());
                if (index >= 14) {
                    index = 1;
                }
                break;
            case R.id.bt_getStepSectionHistroy:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStepSectionHistroy().getDatas());
                break;
            case R.id.bt_getStepSectionNum:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getStepSectionNum().getDatas());
                break;
            case R.id.bt_get_sport:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSport().getDatas());
                break;
            case R.id.bt_get_SportHistoryNum:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSportHistoryNum().getDatas());
                break;
            case R.id.bt_get_SportHistoryData:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getSportHistoryData().getDatas());
                break;
            case R.id.bt_setLostDeviceAlert:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setLostDeviceAlert(true, 0).getDatas());
                break;
            case R.id.bt_query_lost_alert:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().queryLostDeviceAlert().getDatas());
                break;
            case R.id.bt_getTemp:
                ArrayList<byte[]> list_temp = new ArrayList<>();
                list_temp.add(Manridy.getMrdSend().getTempData(0).getDatas());
                list_temp.add(Manridy.getMrdSend().getTempData(2).getDatas());
                list_temp.add(Manridy.getMrdSend().getTempData(1).getDatas());

                sendDialog.show(application.getAdapter(), list_temp);
                break;
            case R.id.bt_set_due_day:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setDueDate(21, 8, 8).getDatas());
                break;
            case R.id.bt_set_menstruation_day:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().setMenstruation(21, 8, 12, 5, 6).getDatas());
                break;
            case R.id.bt_get_female_info:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getDueInfo().getDatas());
                break;
            case R.id.bt_inquiry_dial_index:
                application.getAdapter().LostWriteData(Manridy.getMrdSend().getInstalledDialIndex().getDatas());
                break;
            case R.id.bt_refresh_ui:
                try {
                    InputStream nowStream = getAssets().open("UI_1.0.0.bin");
                    InputStream newStream = getAssets().open("UI_1.0.7.bin");
                    CompareBinStreamCore.getInstance().compareFile(nowStream, newStream, (status, bean) -> {
                        Log.i("REFRESH_UI", "status is " + status);
                        if (status == 1) {
                            runOnUiThread(() -> waitDialog.show("对比文件成功"));
                            MrdPushParse mrdPushParse = MrdPushCore.getInstance().getMrdPushParse();
                            mrdPushParse.refreshUI(bean, false, new MrdPushParse.ProgressStatusCallback() {
                                @Override
                                public void onProgress(double progress) {
                                    Log.i("REFRESH_UI", "progress is " + progress);
                                    runOnUiThread(() -> waitDialog.show("当前进度:" + String.format("%.2f", progress) + "%"));
                                }

                                @Override
                                public void onStatus(int status) {
                                    switch (status) {
                                        case MrdPushConst.CALL_PREPARE_SUCCESS:
                                            runOnUiThread(() -> waitDialog.show("更新UI初始化成功"));
                                            break;
                                        case MrdPushConst.CALL_FINAL_SUCCESS:
                                            runOnUiThread(() -> waitDialog.show("更新UI成功"));
                                            new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                                            break;
                                        case MrdPushConst.CALL_FINAL_FAILURE:
                                            runOnUiThread(() -> waitDialog.show("更新UI失败"));
                                            new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                                            break;
                                        case MrdPushConst.CALL_TIME_OUT:
                                            runOnUiThread(() -> waitDialog.show("更新UI超时"));
                                            new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                                            break;
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(() -> waitDialog.show("对比文件失败"));
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_install_dial_starmax_one:
                InputStream open1;
                try {
                    open1 = getAssets().open("SYD8810_240.bin");
                    MrdPushParse mrdPushParse = MrdPushCore.getInstance().getMrdPushParse();
                    mrdPushParse.startPush(open1, 101, true, new MrdPushParse.ProgressStatusCallback() {
                        @Override
                        public void onProgress(final double progress) {
                            runOnUiThread(() -> waitDialog.show("当前进度:" + String.format("%.2f", progress) + "%"));
                        }

                        @Override
                        public void onStatus(int status) {
                            if (status == MrdPushConst.CALL_FINAL_SUCCESS) {
                                runOnUiThread(() -> waitDialog.show("当前进度:100%"));
                                new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_ota_rtk:
                RtkCore.initialize(this, new RtkConfigure.Builder()
                        .debugEnabled(true)
                        .printLog(true)
                        .logTag("OTA_RTK")
                        .build());
                RtkDfu.initialize(this, true);

                dfuAdapter = GattDfuAdapter.getInstance(this);
                dfuAdapter.initialize(new DfuAdapter.DfuHelperCallback() {
                    @Override
                    public void onStateChanged(int state) {
                        super.onStateChanged(state);
                        switch (state) {
                            case DfuAdapter.STATE_INIT_OK:
                            case DfuAdapter.STATE_DISCONNECTING:
                            case DfuAdapter.STATE_DISCONNECTED:
                                connectDevice(
                                        application.getAdapter().getDevice().getAddress(),
                                        dfuAdapter
                                );
                                break;
                            case DfuAdapter.STATE_PREPARED:
                                startRtkOTA(
                                        application.getAdapter().getDevice().getAddress(),
                                        dfuAdapter
                                );
                                break;
                        }
                    }

                    @Override
                    public void onProgressChanged(DfuProgressInfo dfuProgressInfo) {
                        super.onProgressChanged(dfuProgressInfo);
                        Log.i("OTA_RTK", "ota progress is " + dfuProgressInfo.getProgress() + "%");
                    }

                    @Override
                    public void onProcessStateChanged(int state, Throughput throughput) {
                        super.onProcessStateChanged(state, throughput);
                        switch (state) {
                            case DfuConstants.PROGRESS_IMAGE_ACTIVE_SUCCESS:
                                break;
                            case DfuConstants.PROGRESS_PROCESSING_ERROR:
                                break;
                        }
                    }

                    @Override
                    public void onTargetInfoChanged(OtaDeviceInfo otaDeviceInfo) {
                        super.onTargetInfoChanged(otaDeviceInfo);
                    }

                    @Override
                    public void onError(int type, int code) {
                        super.onError(type, code);
                    }
                });
                break;
            case R.id.bt_ota_nordic:
                Toast.makeText(this, "暂未开放", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bt_ota_syd:
                SYDManager sydManager = new SYDManager(this, new OTAListener() {
                    @Override
                    public void onProgress(float progress) {
                        Log.d("sydManager", "progress=" + progress);
                        runOnUiThread(() -> waitDialog.show("当前进度:" + String.format("%.2f", progress) + "%"));
                    }

                    @Override
                    public void onState(OTAEnum otaEnum) {
                        switch (otaEnum) {
                            case OTAError:
                                runOnUiThread(() -> waitDialog.show("固件升级失败"));
                                new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                                break;
                            case DeviceConnecting:
                            case DfuProcessStarting:
                                runOnUiThread(() -> waitDialog.show("当前进度:" + "0%"));
                                break;
                            case OTAComplete:
                                runOnUiThread(() -> waitDialog.show("固件升级成功"));
                                new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
                                break;
                        }
                        Log.d("sydManager", "otaEnum=" + otaEnum.name());
                    }
                });

                sydManager.startAssets(application.getAdapter().getDevice().getAddress(), "HB220-4_8502_R1_V2.0.8_20231021144500.bin", 0x961CD046);
//                InputStream open_syd;
//                try {
//                    //0x0107CA83
//                    //0x558FA4D7
//                    open_syd = getAssets().open("GTS1_1.6.4.bin");
////                    open_syd = getAssets().open("GTS1_1.5.9.bin");
//                    MrdPushParse mrdPushParse = MrdPushCore.getInstance().getMrdPushParse();
//                    mrdPushParse.startOTA(open_syd, 0x0107CA83, true, new MrdPushParse.ProgressStatusCallback() {
//                        @Override
//                        public void onProgress(double progress) {
//                            runOnUiThread(() -> waitDialog.show("当前进度:" + String.format("%.2f", progress) + "%"));
//                        }
//
//                        @Override
//                        public void onStatus(int status) {
//                            switch (status) {
//                                case MrdPushConst.CALL_OTA_SUCCESS:
//                                    runOnUiThread(() -> waitDialog.show("固件升级成功"));
//                                    new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
//                                    break;
//                                case MrdPushConst.CALL_OTA_FAILURE:
//                                    runOnUiThread(() -> waitDialog.show("固件升级失败"));
//                                    new Handler(getMainLooper()).postDelayed(() -> waitDialog.cancel(), 2000);
//                                    break;
//                            }
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                break;
        }
    }

    private GattDfuAdapter dfuAdapter;

    private void startRtkOTA(String address, GattDfuAdapter dfuAdapter) {
        DfuConfig dfuConfig = new DfuConfig();
        //传入文件绝对路径
        //dfuConfig.setFilePath(binPath);

        //传入asses资源目录下的文件
        dfuConfig.setFilePath("mioya_1.1.1.bin");
        dfuConfig.setFileLocation(DfuConfig.FILE_LOCATION_ASSETS);

        dfuConfig.setAddress(address);
        dfuConfig.setChannelType(DfuConfig.CHANNEL_TYPE_GATT);
        OtaModeInfo modeInfo = dfuAdapter.getPriorityWorkMode(DfuConstants.OTA_MODE_SILENT_FUNCTION);
        dfuConfig.setOtaWorkMode(modeInfo.getWorkmode());
        List<OtaModeInfo> otaModeInfos = dfuAdapter.getSupportedModes();
        if (dfuAdapter.getOtaDeviceInfo() != null) {
            dfuConfig.setProtocolType(dfuAdapter.getOtaDeviceInfo().getProtocolType());
        } else {
            dfuConfig.setProtocolType(0);
        }
        dfuAdapter.startOtaProcess(dfuConfig);
    }

    private void connectDevice(String address, GattDfuAdapter dfuAdapter) {
        ConnectParams.Builder connectParamsBuilder = new ConnectParams.Builder()
                .address(address)
                .reconnectTimes(3);
        dfuAdapter.connectDevice(connectParamsBuilder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onChange(BluetoothDevice device, BleState state) {
        if (application.getAdapter().getDevice() != null) {
            tv_device_name.setText(application.getAdapter().getDevice().getName()
                    + ","
                    + application.getAdapter().getDevice().getAddress()
            );
            switch (state) {
                case CONNECTING:
                    tv_device_state.setText("设备连接中");
                    break;
                case CONNECTED:
                    tv_device_state.setText("已连接,点击断开设备");
                    break;
                case DISCONNECTED:
                    tv_device_state.setText("已断开,点击连接设备");
                    break;
                case SERVICES_DISCOVERED:
                    tv_device_state.setText("已发现服务,点击断开设备");
                    break;
            }
        } else {
            tv_device_name.setText("未绑定设备");
            tv_device_state.setText("点击搜索设备");
        }
    }

    @Override
    public void onReadChange(BluetoothDevice device, byte[] datas) {
        MrdReadRequest readRequest = Manridy.getMrdRead().read(datas);
        String body;
        Log.i("MrdRead", "read enum type is : "
                + readRequest.getMrdReadEnum()
                + " body is : "
                + readRequest.getJson()
        );
        if (readRequest.getMrdReadEnum() == MrdReadEnum.Failure) {
            body = "不支持解析的指令";
        } else if (readRequest.getStatus() == 0) {
            body = "解析数据出错";
        } else {
            body = readRequest.getMrdReadEnum().name();
            if (!TextUtils.isEmpty(readRequest.getJson())) {
                body += "=" + formatJson(readRequest.getJson());
            }
        }
        Log.i("MrdRead", "body is " + body);
        dataAdapter.emit(body);

//        if (readRequest.getMrdReadEnum() == MrdReadEnum.Failure) {
//            return;
//        }
//        if (readRequest.getMrdReadEnum() == MrdReadEnum.EcgTest) {
//            Log.e(getClass().getName(), "EcgTest;" + readRequest.getJson());
//            return;
//        }
//        if (appendBuffer.length() > 0) {
//            appendBuffer.insert(0, "\n");
//            appendBuffer.insert(0, "----------------");
//            appendBuffer.insert(0, "\n");
//        }
//        if (readRequest.getStatus() == 0) {
//            appendBuffer.insert(0, "数据错误，或该设备不支持次功能");
//        } else {
//            String text = readRequest.getMrdReadEnum().name();
//            if (!TextUtils.isEmpty(readRequest.getJson())) {
//                text += "=" + formatJson(readRequest.getJson());
//            }
//            appendBuffer.insert(0, text);
//        }
//        text.setText(appendBuffer);
    }

    public String formatJson(String json) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonObject);
    }

    public static long YMDHM2Long(String date) {
        try {
            if (TextUtils.isEmpty(date)) {
                return 0;
            }
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return dateFormat.parse(date).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
