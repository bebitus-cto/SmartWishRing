package com.wishring.app.presentation.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.presentation.home.HomeScreenContent
import com.wishring.app.presentation.home.HomeViewState
import com.wishring.app.presentation.home.PageInfo
import com.wishring.app.presentation.main.BlePhase
import com.wishring.app.ui.theme.WishRingTheme
import java.time.LocalDate

@Preview(showBackground = true, name = "Zero Wishes State")
@Composable
private fun HomeScreenZeroWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedNoWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = emptyList(),
                deviceBatteryLevel = 15,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 0)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedFullWishes(
                isLoading = false,
                todayWish = WishUiState(
                    date = "2024-01-15",
                    targetCount = 1000,
                    wishText = "매일 운동하기",
                    currentCount = 700,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                wishHistory = generateDummyRecords(),
                deviceBatteryLevel = 76,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = true, totalItems = 10)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "Connected No Wishes - Registration Prompt")
@Composable
private fun HomeScreenConnectedNoWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedNoWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = emptyList(),
                deviceBatteryLevel = 85,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 0)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "One Wish With Button")
@Composable
private fun HomeScreenOneWishWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedPartialWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = generateDummyRecords().take(1),
                deviceBatteryLevel = 80,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 1)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "Two Wishes With Button")
@Composable
private fun HomeScreenTwoWishesWithButtonPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedPartialWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = generateDummyRecords().take(2),
                deviceBatteryLevel = 60,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 2)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "One Wish 100% Complete")
@Composable
private fun HomeScreenOneWishCompletePreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedPartialWishes(
                isLoading = false,
                todayWish = WishUiState(
                    date = "2024-01-15",
                    targetCount = 1000,
                    wishText = "매일 운동하기",
                    currentCount = 1000,
                    isCompleted = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                wishHistory = generateDummyRecords().take(1),
                deviceBatteryLevel = 85,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 1)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "Long Wish Text")
@Composable
private fun HomeScreenLongWishTextPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedPartialWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = listOf(
                    WishDayUiState(
                        date = LocalDate.now(),
                        completedCount = 750,
                        wishText = "나는 매일 아침 일찍 일어나서 운동을 하고, 건강한 아침 식사를 먹고, 독서를 통해 새로운 지식을 습득하며, 가족과 소중한 시간을 보내고, 일에서도 최선을 다하여 더 나은 내가 되기 위해 끊임없이 노력하고 성장하는 사람이 되고 싶다.",
                        targetCount = 1000,
                        isCompleted = false
                    )
                ),
                deviceBatteryLevel = 70,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = false, totalItems = 1)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "Three Wishes State")
@Composable
private fun HomeScreenThreeWishesPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.ConnectedFullWishes(
                isLoading = false,
                todayWish = null,
                wishHistory = generateDummyRecords().take(3),
                deviceBatteryLevel = 90,
                pageInfo = PageInfo(currentPage = 0, hasNextPage = true, totalItems = 10)
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun HomeScreenLoadingPreview() {
    WishRingTheme {
        HomeScreenContent(
            uiState = HomeViewState.BluetoothDisconnected(
                isLoading = false,
                todayWish = WishUiState(
                    date = "2024-01-15",
                    targetCount = 10,
                    wishText = "매일 운동하기",
                    currentCount = 5,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                deviceBatteryLevel = 50,
                isAttemptingConnection = true
            ),
            onEvent = { /* Preview - no action */ },
            scannedDevices = emptyList(),
            showDevicePicker = false,
            blePhase = BlePhase.Idle,
            activity = null,
        )
    }
}

// Dummy data function for previews
private fun generateDummyRecords(): List<WishDayUiState> {
    return listOf(
        WishDayUiState(
            date = LocalDate.now().minusDays(1),
            completedCount = 1000,
            wishText = "매일 아침 운동하기",
            targetCount = 1000,
            isCompleted = true
        ),
        WishDayUiState(
            date = LocalDate.now().minusDays(2),
            completedCount = 750,
            wishText = "독서하며 성장하기",
            targetCount = 1000,
            isCompleted = false
        ),
        WishDayUiState(
            date = LocalDate.now().minusDays(3),
            completedCount = 500,
            wishText = "가족과 시간 보내기",
            targetCount = 800,
            isCompleted = false
        )
    )
}