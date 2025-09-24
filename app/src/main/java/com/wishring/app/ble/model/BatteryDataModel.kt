package com.wishring.app.ble.model

import kotlinx.serialization.Serializable

@Serializable
data class BatteryDataModel(
    val battery: Int,
    val batteryState: Int = 0
)