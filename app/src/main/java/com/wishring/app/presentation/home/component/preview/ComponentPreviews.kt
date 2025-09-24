package com.wishring.app.presentation.home.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.presentation.home.component.CircularGauge
import com.wishring.app.presentation.home.component.WishCountCard
import com.wishring.app.presentation.home.component.WishReportItem
import com.wishring.app.presentation.home.components.BleStatusCard
import com.wishring.app.ui.theme.WishRingTheme
import com.wishring.app.data.model.BatteryStatus

// CircularGauge Previews
@Preview(showBackground = true)
@Composable
fun CircularGaugePreview() {
    WishRingTheme {
        CircularGauge(
            currentCount = 700,
            targetCount = 1000
        )
    }
}

@Preview(showBackground = true, name = "CircularGauge Completed")
@Composable
fun CircularGaugeCompletedPreview() {
    WishRingTheme {
        CircularGauge(
            currentCount = 1000,
            targetCount = 1000
        )
    }
}

@Preview(showBackground = true, name = "CircularGauge Zero Progress")
@Composable
fun CircularGaugeZeroPreview() {
    WishRingTheme {
        CircularGauge(
            currentCount = 0,
            targetCount = 1000
        )
    }
}

// WishCountCard Previews
@Preview(showBackground = true)
@Composable
fun WishCountCardPreview() {
    WishRingTheme {
        WishCountCard(
            currentCount = 700,
            targetCount = 1000,
            wishText = "나는 매일 성장하고 있다"
        )
    }
}

@Preview(showBackground = true, name = "WishCountCard Completed")
@Composable
fun WishCountCardCompletedPreview() {
    WishRingTheme {
        WishCountCard(
            currentCount = 1000,
            targetCount = 1000,
            wishText = "매일 운동하기 - 완료!"
        )
    }
}

@Preview(showBackground = true, name = "WishCountCard Low Progress")
@Composable
fun WishCountCardLowProgressPreview() {
    WishRingTheme {
        WishCountCard(
            currentCount = 50,
            targetCount = 1000,
            wishText = "꾸준한 독서 습관 만들기"
        )
    }
}

// WishReportItem Preview
@Preview(showBackground = true)
@Composable
fun WishReportItemPreview() {
    WishRingTheme {
        WishReportItem(
            wishText = "매일 아침 운동하기",
            date = "2024-01-15",
            count = 850,
            isCompleted = false,
            onClick = { }
        )
    }
}

@Preview(showBackground = true, name = "WishReportItem Completed")
@Composable
fun WishReportItemCompletedPreview() {
    WishRingTheme {
        WishReportItem(
            wishText = "건강한 식습관 유지하기",
            date = "2024-01-14",
            count = 1000,
            isCompleted = true,
            onClick = { }
        )
    }
}

@Preview(showBackground = true, name = "WishReportItem Long Text")
@Composable
fun WishReportItemLongTextPreview() {
    WishRingTheme {
        WishReportItem(
            wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며 성장하는 사람이 되고 싶다",
            date = "2024-01-13",
            count = 750,
            isCompleted = false,
            onClick = { }
        )
    }
}

// BleStatusCard Previews
@Preview
@Composable
private fun BleStatusCardPreview() {
    WishRingTheme {
        BleStatusCard(
            connectionState = BleConnectionState.CONNECTED,
            batteryLevel = 85,
            batteryStatus = BatteryStatus.GOOD,
            onRetryConnection = { },
            onOpenSettings = { }
        )
    }
}

@Preview(name = "BLE Disconnected")
@Composable
private fun BleStatusCardDisconnectedPreview() {
    WishRingTheme {
        BleStatusCard(
            connectionState = BleConnectionState.DISCONNECTED,
            batteryLevel = 0,
            batteryStatus = BatteryStatus.LOW,
            onRetryConnection = { },
            onOpenSettings = { }
        )
    }
}

@Preview(name = "BLE Connecting")
@Composable
private fun BleStatusCardConnectingPreview() {
    WishRingTheme {
        BleStatusCard(
            connectionState = BleConnectionState.CONNECTING,
            batteryLevel = 45,
            batteryStatus = BatteryStatus.MEDIUM,
            onRetryConnection = { },
            onOpenSettings = { }
        )
    }
}

@Preview(name = "BLE Error")
@Composable
private fun BleStatusCardErrorPreview() {
    WishRingTheme {
        BleStatusCard(
            connectionState = BleConnectionState.ERROR,
            batteryLevel = 20,
            batteryStatus = BatteryStatus.LOW,
            onRetryConnection = { },
            onOpenSettings = { }
        )
    }
}