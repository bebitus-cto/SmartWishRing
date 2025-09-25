package com.wishring.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.manridy.sdk_mrd2019.Manridy
import com.manridy.sdk_mrd2019.bean.send.SystemEnum
import com.manridy.sdk_mrd2019.install.MrdPushCore
import com.wishring.app.ble.model.BatteryDataModel
import com.wishring.app.core.util.SimpleBlePermissionManager
import com.wishring.app.data.ble.model.BleConstants
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.presentation.main.BlePhase
import com.wishring.app.presentation.main.MainViewModel
import com.wishring.app.presentation.navigation.WishRingNavGraph
import com.wishring.app.ui.theme.WishRingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {

    // BLE 관련 필드
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var discoveryReceiver: BroadcastReceiver? = null
    private var discoveryStartTime: Long = 0L
    private var discoveredDevicesCount = 0
    private var bluetoothGatt: BluetoothGatt? = null

    // BLE Scan fields

    private val discoveredDevices = mutableSetOf<String>() // 중복 방지
    private var h13Device: BluetoothDevice? = null
    private var batteryPollingJob: Job? = null // 배터리 폴링 Job 관리
    private var isH13Connected = false // H13 기기 연결 상태

    private lateinit var blePermissionManager: SimpleBlePermissionManager
    private val mainViewModel: MainViewModel by viewModels()


    @Inject
    lateinit var preferencesRepository: PreferencesRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        blePermissionManager = SimpleBlePermissionManager(this)

        initBluetooth()

        observeBleStateChanges()

        setContent {
            WishRingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    WishRingNavGraph(
                        navController = navController,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }

    private fun initBluetooth() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.i(WR_EVENT, "[MainActivity] Bluetooth 초기화 완료")
    }

    fun startClassicDiscovery() {
        Log.i(WR_EVENT, "[MainActivity] ========== Classic Bluetooth Discovery 시작 ==========")

        // 이미 Discovery 진행 중이면 스킵
        if (discoveryReceiver != null || bluetoothAdapter?.isDiscovering == true) {
            Log.w(WR_EVENT, "[MainActivity] Discovery가 이미 진행 중입니다 - 스킵")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(WR_EVENT, "[MainActivity] 블루투스가 비활성화됨")
            return
        }

        // 중복 방지 초기화
        discoveredDevices.clear()
        discoveredDevicesCount = 0
        discoveryStartTime = System.currentTimeMillis()
        Log.i(WR_EVENT, "[MainActivity] Discovery 시작 시간: $discoveryStartTime")

        // BroadcastReceiver 생성
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        val rssi = intent
                            .getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .toInt()

                        device?.let { bluetoothDevice ->
                            if (!discoveredDevices.contains(bluetoothDevice.address)) {
                                discoveredDevices.add(bluetoothDevice.address)
                                discoveredDevicesCount++

                                val deviceName = bluetoothDevice.name ?: "Unknown"

                                // H13 기기 확인 (이름으로 판단)
                                val isH13Device =
                                    deviceName.contains("H13", ignoreCase = true) ||
                                            deviceName.contains("WISH", ignoreCase = true)

                                if (isH13Device) {
                                    Log.i(
                                        WR_EVENT,
                                        "[MainActivity] ✅ H13 기기로 추정됨 - MainViewModel에 전달"
                                    )
                                    // Service UUID는 연결 후 확인 가능하므로 가짜 UUID 전달
                                    val serviceUuids = listOf(BleConstants.SERVICE_UUID.toString())
                                    mainViewModel.addScannedDevice(
                                        deviceName,
                                        bluetoothDevice.address,
                                        rssi,
                                        serviceUuids
                                    )

                                    Log.i(WR_EVENT, "[MainActivity] 🎯 H13 발견! Discovery 즉시 중지")
                                    bluetoothAdapter?.cancelDiscovery()
                                }
                            }
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Log.i(WR_EVENT, "[MainActivity] Classic Discovery 시작됨")
                        // Discovery 시작 시간 갱신
                        discoveryStartTime = System.currentTimeMillis()
                        discoveredDevicesCount = 0
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        val duration = System.currentTimeMillis() - discoveryStartTime
                        Log.i(WR_EVENT, "[MainActivity] Classic Discovery 완료됨")
                        Log.i(WR_EVENT, "[MainActivity] - 소요 시간: ${duration}ms")
                        Log.i(WR_EVENT, "[MainActivity] - 발견된 기기 수: $discoveredDevicesCount")
                        Log.i(
                            WR_EVENT,
                            "[MainActivity] - 스캔된 기기 목록: ${discoveredDevices.joinToString()}"
                        )

                        // 너무 빨리 종료되면 무시 (정상적인 Discovery는 최소 10초)
                        if (duration < 1000) {
                            Log.w(WR_EVENT, "[MainActivity] Discovery가 너무 빨리 종료됨 - 무시")
                            return
                        }

                        // Discovery 완료 시 UI 상태 업데이트
                        mainViewModel.onDiscoveryFinished()
                    }
                }
            }
        }

        // BroadcastReceiver 등록
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            registerReceiver(discoveryReceiver, filter)
            Log.i(WR_EVENT, "[MainActivity] Discovery BroadcastReceiver 등록됨")
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[MainActivity] BroadcastReceiver 등록 실패", e)
            discoveryReceiver = null
            return
        }

        // 이미 진행 중인 Discovery 취소
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
            Log.i(WR_EVENT, "[MainActivity] 기존 Discovery 취소")
            // 취소 후 잠시 대기
            Thread.sleep(100)
        }

        // Classic Bluetooth Discovery 시작
        val discoveryStarted = bluetoothAdapter?.startDiscovery() ?: false
        Log.i(WR_EVENT, "[MainActivity] Classic Discovery 시작 결과: $discoveryStarted")
        Log.i(WR_EVENT, "[MainActivity] isDiscovering 상태: ${bluetoothAdapter?.isDiscovering}")

        // 3초 후 자동 중지 타이머
        lifecycleScope.launch {
            delay(30000) // 최대 10초만 스캔
            if (bluetoothAdapter?.isDiscovering == true) {
                Log.i(WR_EVENT, "[MainActivity] Discovery 타임아웃 (30초) - 자동 중지")
                bluetoothAdapter?.cancelDiscovery()
            }
        }

        if (!discoveryStarted) {
            Log.e(WR_EVENT, "[MainActivity] Discovery 시작 실패")
            stopClassicDiscovery()
        }
    }

    fun stopClassicDiscovery() {
        Log.i(WR_EVENT, "[MainActivity] Classic Discovery 중지 요청")

        try {
            discoveryReceiver?.let {
                try {
                    unregisterReceiver(it)
                    Log.i(WR_EVENT, "[MainActivity] Discovery BroadcastReceiver 해제됨")
                } catch (e: Exception) {
                    Log.e(WR_EVENT, "[MainActivity] BroadcastReceiver 해제 중 오류", e)
                }
                discoveryReceiver = null
            }

            // 권한 체크 후 isDiscovering 확인
            val hasPermission = try {
                bluetoothAdapter?.isDiscovering
                true
            } catch (e: SecurityException) {
                false
            }
            
            if (hasPermission) {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                    Log.i(WR_EVENT, "[MainActivity] Classic Discovery 중지됨")
                }
            } else {
                Log.i(WR_EVENT, "[MainActivity] BLUETOOTH_SCAN 권한 없음 - Discovery 체크 스킵")
            }
        } catch (e: SecurityException) {
            Log.e(WR_EVENT, "[MainActivity] Discovery 중지 중 권한 오류", e)
        }

        Log.i(WR_EVENT, "[MainActivity] 현재 discoveredDevices: ${discoveredDevices.joinToString()}")
    }

    // BLE 스캔 시작
    fun startBleScan() {
        Log.i(WR_EVENT, "[MainActivity] ========== BLE 스캔 시작 요청 ==========")

        blePermissionManager.requestBluetoothSetup(
            onPermissionsGranted = {
                Log.i(WR_EVENT, "[MainActivity] 모든 권한 및 설정 완료, BLE 스캔 시작")
                // MainViewModel 상태 변경 → 이것이 startClassicDiscovery()를 트리거함
                mainViewModel.startBleScan()
            },
            onPermissionsDenied = {
                Log.e(WR_EVENT, "[MainActivity] 권한 거부됨")
            }
        )
    }

    private fun actuallyConnectToDevice(address: String) {

        // 스캔 중지 - MainViewModel을 통해 처리
        mainViewModel.stopBleScan()

        // BluetoothAdapter로 device 찾기
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                Log.i(WR_EVENT, "[MainActivity] 기기 찾음: ${device.name ?: "Unknown"} ($address)")
                connectToDevice(device)
            } else {
                Log.e(WR_EVENT, "[MainActivity] 기기를 찾을 수 없음: $address")
            }
        } catch (e: IllegalArgumentException) {
            Log.e(WR_EVENT, "[MainActivity] 잘못된 블루투스 주소: $address", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            // status 코드 디버깅
            Log.i(WR_EVENT, "[MainActivity] onConnectionStateChange - status: $status, newState: $newState")
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> Log.i(WR_EVENT, "[MainActivity] GATT 작업 성공")
                133 -> Log.e(WR_EVENT, "[MainActivity] ❌ GATT ERROR 133: 연결 실패 - 기기 재시작 또는 페어링 필요")
                8 -> Log.e(WR_EVENT, "[MainActivity] ❌ GATT ERROR 8: 연결 시간 초과")
                19 -> Log.e(WR_EVENT, "[MainActivity] ❌ GATT ERROR 19: 기기에서 연결 거부")
                22 -> Log.e(WR_EVENT, "[MainActivity] ❌ GATT ERROR 22: 기기가 연결 종료")
                else -> Log.e(WR_EVENT, "[MainActivity] ❌ GATT ERROR $status")
            }
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(WR_EVENT, "[MainActivity] GATT 연결됨")

                    // 연결된 기기 정보 저장
                    gatt?.device?.let { device ->
                        lifecycleScope.launch {
                            mainViewModel.onDeviceConnected(
                                deviceAddress = device.address,
                                deviceName = device.name ?: "Unknown"
                            )
                        }
                    }

                    runOnUiThread {
                        mainViewModel.updateConnectionState(true)
                    }
                    // MRD SDK에 GATT 전달
                    gatt?.let {
                        // TODO: 실제 MRD SDK 통합시 주석 해제
                        // mrdManager?.setBluetoothGatt(it)
                        it.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(WR_EVENT, "[MainActivity] GATT 연결 끊김 (status: $status)")
                    
                    // 연결 실패 원인별 처리
                    when (status) {
                        133 -> {
                            Log.e(WR_EVENT, "[MainActivity] 📱 ERROR 133 - 1초 후 재시도합니다...")
                            lifecycleScope.launch {
                                delay(1000)
                                h13Device?.let {
                                    Log.i(WR_EVENT, "[MainActivity] 재연결 시도...")
                                    connectToDevice(it)
                                }
                            }
                        }
                        else -> Log.i(WR_EVENT, "[MainActivity] 연결 종료 (status: $status)")
                    }
                    
                    // H13 연결 상태 초기화 및 배터리 폴링 중지
                    isH13Connected = false
                    batteryPollingJob?.cancel()
                    batteryPollingJob = null
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] 배터리 폴링 중지됨")

                    runOnUiThread {
                        mainViewModel.updateConnectionState(false)
                    }
                    gatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value

            // 상세 데이터 수신 로깅
            Log.i(WR_EVENT, "[BATTERY_DEBUG] ======= BLE 데이터 수신 =======")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] 시간: ${System.currentTimeMillis()}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] UUID: ${characteristic.uuid}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] HEX: ${data.toHexString()}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] 크기: ${data.size} bytes")

            // 배터리 데이터 패턴 체크
            if (data.size >= 9 && data[0] == 0x0F.toByte() && data[1] == 0x06.toByte()) {
                Log.i(WR_EVENT, "[BATTERY_DEBUG] *** 배터리 데이터 패턴 감지! ***")
            }

            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    // MRD SDK에 데이터 전달
                    MrdPushCore.getInstance().readData(data)

                    // MRD SDK로 파싱된 데이터 획득
                    val readRequest = Manridy.getMrdRead().read(data)
                    val readEnum = readRequest?.mrdReadEnum
                    val jsonData = readRequest?.json

                    Log.i(WR_EVENT, "[MainActivity] MRD SDK 파싱 결과")
                    Log.i(WR_EVENT, "[MainActivity] - Type: $readEnum")
                    Log.i(WR_EVENT, "[MainActivity] - Data: $jsonData")

                    // Enum 값을 문자열로 비교 (SDK enum 이슈 회피)
                    when (readEnum?.toString()) {
                        "BATTERY", "battery" -> {
                            val json = Json { ignoreUnknownKeys = true }
                            val batteryLevel = try {
                                json.decodeFromString<BatteryDataModel>(jsonData.orEmpty()).battery
                            } catch (e: Exception) {
                                Log.e(WR_EVENT, "[BATTERY_DEBUG] JSON 파싱 실패: $jsonData", e)
                                0
                            }
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] ======= 배터리 데이터 수신 시작 =======")
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] 1. MRD SDK readEnum: $readEnum")
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] 2. MRD SDK jsonData: $jsonData")
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] 3. 파싱된 배터리 레벨: ${batteryLevel}%")
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] 4. MainViewModel 업데이트 호출...")
                            mainViewModel.updateBatteryLevel(batteryLevel)
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] 5. MainViewModel 업데이트 완료")
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] ======= 배터리 데이터 수신 완료 =======")
                        }

                        "KEY", "key", "button" -> {
                            // 버튼 이벤트
                            Log.i(WR_EVENT, "[MainActivity] 🔘 버튼 이벤트 감지")
                            Log.i(WR_EVENT, "[MainActivity] - 버튼 데이터: $jsonData")
                        }

                        "HEART", "heart" -> {
                            Log.i(WR_EVENT, "[MainActivity] ❤️ 심박 데이터: $jsonData")
                        }

                        "STEP", "step" -> {
                            Log.i(WR_EVENT, "[MainActivity] 👟 걸음수 데이터: $jsonData")
                        }

                        "RESET", "reset" -> {
                            Log.i(WR_EVENT, "[MainActivity] 🔄 리셋 이벤트 감지")
                        }

                        else -> {
                            Log.d(WR_EVENT, "[MainActivity] ❓ 기타 이벤트: $readEnum - $jsonData")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(WR_EVENT, "[MainActivity] MRD SDK 처리 실패", e)

                    if (data.size >= 9 && data[0] == 0x0F.toByte() && data[1] == 0x06.toByte()) {
                        val batteryLevel = data[8].toInt() and 0xFF
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ======= 폴백 배터리 파싱 =======")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 폴백 1. 원시 데이터 크기: ${data.size}")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 폴백 2. 헤더 확인: 0x0F, 0x06")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 폴백 3. 배터리 값 위치 [8]: ${data[8]}")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 폴백 4. 파싱된 배터리: $batteryLevel%")
                        mainViewModel.updateBatteryLevel(batteryLevel)
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ======= 폴백 파싱 완료 =======")
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(WR_EVENT, "[BATTERY_DEBUG] ✅ Descriptor 쓰기 성공: ${descriptor.uuid}")

                if (descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] Notification 설정 완료!")

                    lifecycleScope.launch {
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 2. 초기 배터리 요청")
                        requestBatteryLevel()

                        Log.i(WR_EVENT, "[TIME_SYNC] 시간 동기화 테스트 시작")
                        syncDeviceTime()
                        
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 4. 배터리 폴링 시작")
                        startBatteryPolling()
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== 초기화 완료 =====")
                    }
                }
            } else {
                Log.e(WR_EVENT, "[BATTERY_DEBUG] ❌ Descriptor 쓰기 실패: status=$status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                Log.i(WR_EVENT, "[MainActivity] 서비스 발견 완료")

                // H13 기기인지 확인
                if (isH13Device(gatt)) {
                    Log.i(WR_EVENT, "[MainActivity] ✅ H13 기기 확인됨 - 배터리 관련 기능 시작")
                    isH13Connected = true

                    // H13 기기일 때만 초기화 작업 시작
                    lifecycleScope.launch {
                        // Notification 설정만 하고, 나머지는 onDescriptorWrite 콜백에서 처리
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== H13 서비스 발견 =====")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 1. Notification 설정 시작")
                        setupNotifications(gatt)
                        // 나머지 작업은 onDescriptorWrite 콜백에서 자동 진행됨
                    }
                } else {
                    Log.i(WR_EVENT, "[MainActivity] ❌ H13 기기가 아님 - 배터리 기능 비활성화")
                    isH13Connected = false
                }
            }
        }
    }

    // 디바이스 연결 (개선 버전)
    private fun connectToDevice(device: BluetoothDevice) {
        h13Device = device
        Log.i(WR_EVENT, "[MainActivity] 기기 연결 시작: ${device.address} - ${device.name ?: "Unknown"}")
        
        // 이전 연결 정리
        bluetoothGatt?.let {
            Log.i(WR_EVENT, "[MainActivity] 이전 GATT 연결 정리")
            it.close()
            bluetoothGatt = null
        }

        try {
            Log.i(WR_EVENT, "[MainActivity] connectGatt 호출 - autoConnect: false (즉시 연결)")
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e(WR_EVENT, "[MainActivity] 블루투스 연결 권한 없음", e)
            // 권한이 없으면 다시 권한 요청 flow 시작
            blePermissionManager.requestBluetoothSetup(
                onPermissionsGranted = {
                    // 예외처리 권한 승인 시 플래그 리셋 (안전장치)
                    connectToDevice(device)
                },
                onPermissionsDenied = {
                    Log.e(WR_EVENT, "[MainActivity] 권한 거부됨")
                    // 예외처리 권한 거부 시 플래그 리셋 (안전장치)
                }
            )
        }
    }

    private suspend fun setupNotifications(gatt: BluetoothGatt) = withContext(Dispatchers.IO) {
        try {
            Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== setupNotifications 시작 =====")

            val service = gatt.getService(BleConstants.SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BleConstants.COUNTER_CHAR_UUID)

            Log.i(WR_EVENT, "[BATTERY_DEBUG] Service UUID: ${BleConstants.SERVICE_UUID}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] Counter UUID: ${BleConstants.COUNTER_CHAR_UUID}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] 서비스 찾음: ${service != null}")
            Log.i(WR_EVENT, "[BATTERY_DEBUG] 특성 찾음: ${characteristic != null}")

            characteristic?.let {
                // 1. Android 앱에 notification 받을 준비
                val result = gatt.setCharacteristicNotification(it, true)
                Log.i(WR_EVENT, "[BATTERY_DEBUG] 1. setCharacteristicNotification 결과: $result")

                // 2. CCCD descriptor에 ENABLE_NOTIFICATION 설정
                val descriptor =
                    it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                Log.i(WR_EVENT, "[BATTERY_DEBUG] 2. CCCD Descriptor 찾음: ${descriptor != null}")

                descriptor?.let { desc ->
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] 3. ENABLE_NOTIFICATION_VALUE 설정")

                    val writeResult = gatt.writeDescriptor(desc)
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] 4. writeDescriptor 결과: $writeResult")

                    if (writeResult) {
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ✅ Notification 설정 성공")
                    } else {
                        Log.e(WR_EVENT, "[BATTERY_DEBUG] ❌ Notification 설정 실패")
                    }
                } ?: Log.e(WR_EVENT, "[BATTERY_DEBUG] CCCD Descriptor를 찾을 수 없음")
            } ?: Log.e(WR_EVENT, "[BATTERY_DEBUG] Characteristic을 찾을 수 없음")

            Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== setupNotifications 완료 =====")
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[BATTERY_DEBUG] Notification 설정 예외 발생", e)
        }
    }

    /**
     * BLE 상태 변화 관찰 및 처리
     */
    private fun observeBleStateChanges() {
        Log.i(WR_EVENT, "[MainActivity] observeBleStateChanges() 시작됨")
        lifecycleScope.launch {
            Log.i(WR_EVENT, "[MainActivity] Coroutine 시작 - bleUiState collect 시작")
            mainViewModel.bleCommand.collect { bleState ->
                Log.i(WR_EVENT, "[MainActivity] BLE 상태 변화 감지: ${bleState.phase}")

                when (bleState.phase) {
                    BlePhase.AutoConnecting -> {
                        if (!bleState.isConnected) {
                            Log.i(WR_EVENT, "[MainActivity] 자동 연결 시도 시작")
                            performAutoConnect()
                        }
                    }

                    BlePhase.Connecting -> {
                        Log.i(WR_EVENT, "[MainActivity] 수동 연결 시도 시작")
                        // 수동 연결은 별도 처리 (기기 선택 후)
                    }

                    BlePhase.Scanning -> {
                        Log.i(WR_EVENT, "[MainActivity] 스캔 상태 감지 - Classic Discovery 시작")
                        startClassicDiscovery()
                    }
                    
                    BlePhase.DeviceSelected -> {
                        Log.i(WR_EVENT, "[MainActivity] 기기 선택됨 - 연결 준비중")
                        // 연결 준비 중 - Classic Discovery 중지
                        stopClassicDiscovery()
                    }

                    BlePhase.Idle -> {
                        Log.i(WR_EVENT, "[MainActivity] BLE 대기 상태 - Classic Discovery 중지")
                        stopClassicDiscovery()
                    }
                    
                    BlePhase.Connected -> {
                        Log.i(WR_EVENT, "[MainActivity] BLE 연결 완료 - 초기화 시작")
                        // 초기화 작업 시작
                    }
                    
                    BlePhase.Initializing -> {
                        Log.i(WR_EVENT, "[MainActivity] BLE 초기화 중")
                        // 초기화 진행 중
                    }
                    
                    BlePhase.ReadingSettings -> {
                        Log.i(WR_EVENT, "[MainActivity] 기기 설정 읽는 중")
                        // 설정 읽기 중
                    }
                    
                    BlePhase.WritingTime -> {
                        Log.i(WR_EVENT, "[MainActivity] 시간 동기화 중")
                        // 시간 동기화 중
                    }
                    
                    BlePhase.Ready -> {
                        Log.i(WR_EVENT, "[MainActivity] BLE 준비 완료 - Classic Discovery 중지")
                        stopClassicDiscovery()
                        // 완전히 준비됨
                    }
                }
            }
        }
    }

    /**
     * 자동 연결 수행
     */
    private suspend fun performAutoConnect() {
        try {
            val lastDevice = preferencesRepository.getLastConnectedDevice()
            if (lastDevice != null) {
                Log.i(
                    WR_EVENT,
                    "[MainActivity] 저장된 기기로 자동 연결: ${lastDevice.name} (${lastDevice.address})"
                )

                // MAC 주소 유효성 검증 (XX:XX:XX:XX:XX:XX 형식)
                val isValidMac =
                    lastDevice.address.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))
                if (!isValidMac) {
                    Log.e(WR_EVENT, "[MainActivity] 잘못된 MAC 주소 형식: ${lastDevice.address}")
                    // 잘못된 데이터 삭제
                    preferencesRepository.clearConnectedDevice()
                    mainViewModel.updateConnectionState(false)
                    return
                }

                withContext(Dispatchers.Main) {
                    connectToDeviceByAddress(lastDevice.address)
                }
            } else {
                Log.w(WR_EVENT, "[MainActivity] 저장된 기기 정보 없음")
                mainViewModel.updateConnectionState(false)
            }
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[MainActivity] 자동 연결 실패", e)
            mainViewModel.updateConnectionState(false)
        }
    }

    /**
     * H13 기기인지 확인
     * Service UUID f000efe0-0451-4000-0000-00000000b000 존재 여부로 판단
     */
    private fun isH13Device(gatt: BluetoothGatt): Boolean {
        val h13Service = gatt.getService(BleConstants.SERVICE_UUID)
        return h13Service != null
    }

    // 60초 배터리 폴링
    private fun startBatteryPolling() {
        if (batteryPollingJob?.isActive == true || !isH13Connected) {
            Log.i(WR_EVENT, "[BATTERY_DEBUG] 배터리 폴링이 이미 실행 중")
            return
        }

        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== H13 배터리 폴링 시작 =====")
        batteryPollingJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive && isH13Connected) {
                    if (bluetoothGatt?.device != null) {
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ========================================")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 60초 주기 배터리 요청")
                        Log.i(
                            WR_EVENT,
                            "[BATTERY_DEBUG] 시각: ${
                                SimpleDateFormat(
                                    "HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date())
                            }"
                        )
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] GATT 연결 상태: ${bluetoothGatt != null}")
                        Log.i(
                            WR_EVENT,
                            "[BATTERY_DEBUG] 디바이스: ${bluetoothGatt?.device?.name} (${bluetoothGatt?.device?.address})"
                        )
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] ========================================")

                        requestBatteryLevel()
                    }

                    // 다음 요청까지 60초 대기
                    if (isH13Connected) {
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 다음 배터리 요청까지 60초 대기...")
                        delay(60_000) // 60초
                    }
                }

                Log.i(
                    WR_EVENT,
                    "[BATTERY_DEBUG] 배터리 폴링 루프 종료 (isActive=$isActive, isH13Connected=$isH13Connected)"
                )
            }
        }
        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== 배터리 폴링 Job 생성됨 =====")
    }

    // 배터리 요청
    private suspend fun requestBatteryLevel() = withContext(Dispatchers.IO) {
        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== requestBatteryLevel 시작 =====")

        bluetoothGatt?.let { gatt ->
            try {
                // MRD SDK를 통한 배터리 요청 (매개변수 없음!)
                val commandData = Manridy.getMrdSend().getSystem(SystemEnum.battery)
                Log.i(WR_EVENT, "[BATTERY_DEBUG] MRD SDK 명령 생성: ${commandData != null}")

                if (commandData?.datas != null) {
                    val service = gatt.getService(BleConstants.SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(BleConstants.WRITE_CHAR_UUID)

                    Log.i(WR_EVENT, "[BATTERY_DEBUG] Service UUID: ${BleConstants.SERVICE_UUID}")
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] Write UUID: ${BleConstants.WRITE_CHAR_UUID}")
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] Service 찾음: ${service != null}")
                    Log.i(WR_EVENT, "[BATTERY_DEBUG] Characteristic 찾음: ${characteristic != null}")

                    characteristic?.let {
                        val command = commandData.datas
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 명령어 크기: ${command?.size}")
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] 명령어 HEX: ${command?.toHexString()}")

                        it.value = command
                        val writeResult = gatt.writeCharacteristic(it)
                        Log.i(WR_EVENT, "[BATTERY_DEBUG] Write 결과: $writeResult")

                        if (writeResult) {
                            Log.i(WR_EVENT, "[BATTERY_DEBUG] ✅ 배터리 명령 전송 성공")
                        } else {
                            Log.e(WR_EVENT, "[BATTERY_DEBUG] ❌ 배터리 명령 전송 실패")
                        }
                    } ?: Log.e(WR_EVENT, "[BATTERY_DEBUG] Characteristic이 null임")
                } else {
                    Log.e(WR_EVENT, "[BATTERY_DEBUG] MRD SDK 명령 생성 실패")
                    if (commandData == null) {
                        Log.e(WR_EVENT, "[BATTERY_DEBUG] commandData가 null")
                    } else {
                        Log.e(WR_EVENT, "[BATTERY_DEBUG] commandData.datas가 null")
                    }
                }
            } catch (e: Exception) {
                Log.e(WR_EVENT, "[BATTERY_DEBUG] 배터리 요청 예외 발생", e)
            }
        } ?: Log.e(WR_EVENT, "[BATTERY_DEBUG] bluetoothGatt이 null임")

        Log.i(WR_EVENT, "[BATTERY_DEBUG] ===== requestBatteryLevel 완료 =====")
    }

    // ByteArray를 16진수 문자열로 변환
    private fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }

    /**
     * 외부에서 배터리 레벨 요청할 수 있는 public 함수
     * HomeScreen에서 화면 재진입 시 사용
     */
    fun refreshBatteryLevel() {
        if (bluetoothGatt != null) {
            Log.i(WR_EVENT, "[MainActivity] 배터리 레벨 새로고침 요청")
            lifecycleScope.launch {
                requestBatteryLevel()
            }
        } else {
            Log.w(WR_EVENT, "[MainActivity] BLE 연결되지 않음 - 배터리 요청 불가")
        }
    }

    // 연결 해제
    fun disconnectDevice() {
        Log.i(WR_EVENT, "[MainActivity] 디바이스 연결 해제")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        h13Device = null
    }

    /**
     * 외부에서 호출 가능한 기기 연결 메서드
     * HomeScreen 등에서 사용
     */
    fun connectToDeviceByAddress(deviceAddress: String) {
        Log.i(WR_EVENT, "[MainActivity] 외부 요청 기기 연결: $deviceAddress")
        actuallyConnectToDevice(deviceAddress)
    }

    override fun onDestroy() {
        mainViewModel.stopBleScan()
        disconnectDevice()
        super.onDestroy()
    }

    // 디바이스 시간 동기화 (테스트용 시간 버전)
    private suspend fun syncDeviceTime() {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastSyncDate = preferencesRepository.getLastTimeSyncDate()

            if (lastSyncDate != today) {
                Log.i(WR_EVENT, "[TIME_SYNC] ===== 시간 동기화 시작 =====")

                // 테스트용 시간 설정: 2025년 3월 3일 오후 3시 33분 33초
                val testCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
                    set(Calendar.YEAR, 2025)
                    set(Calendar.MONTH, 2)  // 3월 (0부터 시작)
                    set(Calendar.DAY_OF_MONTH, 3)
                    set(Calendar.HOUR_OF_DAY, 15)  // 24시간 형식
                    set(Calendar.MINUTE, 33)
                    set(Calendar.SECOND, 33)
                }

                val testTimeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(testCalendar.time)
                Log.i(WR_EVENT, "[TIME_SYNC] 🕐 테스트 시간 설정: $testTimeString")
                Log.i(
                    WR_EVENT, "[TIME_SYNC] Year: ${testCalendar.get(Calendar.YEAR)}, " +
                            "Month: ${testCalendar.get(Calendar.MONTH) + 1}, " +
                            "Day: ${testCalendar.get(Calendar.DAY_OF_MONTH)}, " +
                            "Hour: ${testCalendar.get(Calendar.HOUR_OF_DAY)}, " +
                            "Minute: ${testCalendar.get(Calendar.MINUTE)}, " +
                            "Second: ${testCalendar.get(Calendar.SECOND)}"
                )

                // 2. 디바이스에 시간 설정
                val timeRequest = Manridy.getMrdSend().setTime(testCalendar)
                if (timeRequest != null) {
                    val dataSize = timeRequest.datas?.size ?: 0
                    Log.i(WR_EVENT, "[TIME_SYNC] 시간 명령 데이터 크기: $dataSize bytes")
                    if (dataSize > 0) {
                        Log.d(WR_EVENT, "[TIME_SYNC] 데이터: ${timeRequest.datas.contentToString()}")
                    }
                }
                val timeSuccess = sendTimeData(timeRequest, "시간 설정")

                // 3. 24시간 형식 설정
                val formatRequest = Manridy.getMrdSend().setHourSelect(0)
                if (formatRequest != null) {
                    val formatDataSize = formatRequest.datas?.size ?: 0
                    Log.i(WR_EVENT, "[TIME_SYNC] 24시간 형식 명령 데이터 크기: $formatDataSize bytes")
                }
                val formatSuccess = sendTimeData(formatRequest, "24시간 형식 설정")

                // 4. 설정 검증
                if (timeSuccess && formatSuccess) {
                    verifyTimeSettings(testTimeString)
                    preferencesRepository.setLastTimeSyncDate(today)
                    Log.i(WR_EVENT, "[TIME_SYNC] ✅ 시간 동기화 완료! 기기 시간을 확인하세요.")
                    Log.i(
                        WR_EVENT,
                        "[TIME_SYNC] 예상 표시: 2025-03-03 15:33:33 또는 2025년 3월 3일 오후 3:33:33"
                    )
                } else {
                    Log.e(
                        WR_EVENT,
                        "[TIME_SYNC] ❌ 시간 동기화 실패 (시간: $timeSuccess, 형식: $formatSuccess)"
                    )
                }

                Log.i(WR_EVENT, "[TIME_SYNC] ===========================")
            } else {
                Log.i(WR_EVENT, "[TIME_SYNC] 오늘 이미 동기화 완료됨 ($today)")
            }
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[TIME_SYNC] 시간 동기화 오류", e)
        }
    }

    // BLE 데이터 전송 헬퍼 메서드 (개선 버전)
    private suspend fun sendTimeData(request: Any?, operation: String): Boolean {
        return try {
            if (request == null) {
                Log.w(WR_EVENT, "[TIME_SYNC] ⚠️ $operation - request가 null입니다")
                return false
            }

            // MrdSendRequest 타입 체크
            val dataBytes = when (request) {
                is com.manridy.sdk_mrd2019.send.MrdSendRequest -> {
                    Log.d(WR_EVENT, "[TIME_SYNC] MrdSendRequest 타입 확인됨")
                    request.datas
                }

                else -> {
                    // 폴백: 리플렉션 사용 (호환성을 위해 유지)
                    Log.d(WR_EVENT, "[TIME_SYNC] 리플렉션 사용 (타입: ${request.javaClass.simpleName})")
                    try {
                        request.javaClass.getMethod("getDatas").invoke(request) as ByteArray
                    } catch (e: Exception) {
                        Log.e(WR_EVENT, "[TIME_SYNC] getDatas() 호출 실패", e)
                        return false
                    }
                }
            }

            if (dataBytes == null || dataBytes.isEmpty()) {
                Log.e(WR_EVENT, "[TIME_SYNC] ❌ $operation - 데이터가 비어있습니다")
                return false
            }

            bluetoothGatt?.let { gatt ->
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                if (service == null) {
                    Log.e(WR_EVENT, "[TIME_SYNC] ❌ BLE 서비스를 찾을 수 없습니다")
                    return false
                }

                val writeChar = service.getCharacteristic(BleConstants.WRITE_CHAR_UUID)
                if (writeChar == null) {
                    Log.e(WR_EVENT, "[TIME_SYNC] ❌ Write characteristic을 찾을 수 없습니다")
                    return false
                }

                // 데이터 전송
                writeChar.value = dataBytes
                Log.d(WR_EVENT, "[TIME_SYNC] $operation - 전송할 데이터 크기: ${dataBytes.size} bytes")

                val success = gatt.writeCharacteristic(writeChar)

                if (success) {
                    delay(200) // BLE 안정성을 위한 딜레이
                    Log.i(WR_EVENT, "[TIME_SYNC] ✅ $operation 전송 성공")
                    return true
                } else {
                    Log.w(WR_EVENT, "[TIME_SYNC] ❌ $operation 전송 실패")
                    return false
                }
            } ?: run {
                Log.e(WR_EVENT, "[TIME_SYNC] ❌ BluetoothGatt가 null입니다")
                return false
            }
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[TIME_SYNC] $operation 전송 중 예외 발생", e)
            false
        }
    }

    // 시간 설정 검증
    private suspend fun verifyTimeSettings(sentTime: String) {
        try {
            delay(500) // 설정 반영 대기

            // 24시간 형식 설정 확인 요청
            Manridy.getMrdSend().hourSelect
            // getHourSelect() 응답 처리는 MRD SDK 콜백에서 확인

            Log.i(WR_EVENT, "[TIME_VERIFY] 전송한 시간: $sentTime")
            Log.i(WR_EVENT, "[TIME_VERIFY] 24시간 형식 설정 요청 완료")
            Log.i(WR_EVENT, "[TIME_VERIFY] 💡 실제 디바이스 시간은 화면에서 확인 필요")

        } catch (e: Exception) {
            Log.e(WR_EVENT, "[TIME_VERIFY] 검증 오류", e)
        }
    }


    companion object {
        private const val WR_EVENT = "WR_EVENT"
    }
}