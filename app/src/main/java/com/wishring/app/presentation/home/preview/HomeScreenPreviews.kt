package com.wishring.app.presentation.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.model.DailyRecord
import com.wishring.app.data.model.WishUiState
import com.wishring.app.presentation.home.HomeScreenContent
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = WishUiState(
                    date = "2024-01-15",
                    targetCount = 700,
                    wishText = "매일 운동하기",
                    currentCount = 1000,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                recentRecords = generateDummyRecords(),
                deviceBatteryLevel = 76,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "Zero Wishes State")
@Composable
private fun HomeScreenZeroWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = null,
                recentRecords = emptyList(),
                deviceBatteryLevel = 15,
                bleConnectionState = BleConnectionState.DISCONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = false,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "One Wish With Button")
@Composable
private fun HomeScreenOneWishWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = null,
                recentRecords = generateDummyRecords().take(1),
                deviceBatteryLevel = 80,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "Two Wishes With Button")
@Composable
private fun HomeScreenTwoWishesWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = null,
                recentRecords = generateDummyRecords().take(2),
                deviceBatteryLevel = 60,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "One Wish 100% Complete")
@Composable
private fun HomeScreenOneWishCompletePreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = WishUiState(
                    date = "2024-01-15",
                    targetCount = 1000, // 목표 달성
                    wishText = "매일 운동하기",
                    currentCount = 1000,
                    isCompleted = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                recentRecords = generateDummyRecords().take(1),
                deviceBatteryLevel = 85,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "Long Wish Text")
@Composable
private fun HomeScreenLongWishTextPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = null,
                recentRecords = listOf(
                    DailyRecord(
                        date = LocalDate.now(),
                        completedCount = 750,
                        wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
                        targetCount = 1000,
                        isCompleted = false
                    )
                ),
                deviceBatteryLevel = 70,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "Three Wishes State")
@Composable
private fun HomeScreenThreeWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = false,
                todayWishUiState = null,
                recentRecords = generateDummyRecords().take(3),
                deviceBatteryLevel = 90,
                bleConnectionState = BleConnectionState.CONNECTED
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = true,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun HomeScreenLoadingPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState(
                isLoading = true,
                todayWishUiState = WishUiState(
                    date = "2024-01-15",
                    targetCount = 5,
                    wishText = "매일 운동하기",
                    currentCount = 10,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                deviceBatteryLevel = 50,
                bleConnectionState = BleConnectionState.CONNECTING
            ),
            onEvent = { /* Preview - no action */ },
            isConnected = false,
            scannedDevices = emptyList(),
            showDevicePicker = false,
            activity = null,
            mainViewModel = null,
            isAutoConnecting = false
        )
    }
}

// Dummy data function for previews
private fun generateDummyRecords(): List<DailyRecord> {
    return listOf(
        DailyRecord(
            date = LocalDate.now().minusDays(1),
            completedCount = 1000,
            wishText = "매일 아침 운동하기",
            targetCount = 1000,
            isCompleted = true
        ),
        DailyRecord(
            date = LocalDate.now().minusDays(2),
            completedCount = 750,
            wishText = "독서하며 성장하기",
            targetCount = 1000,
            isCompleted = false
        ),
        DailyRecord(
            date = LocalDate.now().minusDays(3),
            completedCount = 500,
            wishText = "가족과 시간 보내기",
            targetCount = 800,
            isCompleted = false
        )
    )
}