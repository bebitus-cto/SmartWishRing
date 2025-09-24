package com.wishring.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.wishring.app.data.repository.PreferencesRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import app.cash.turbine.test
import java.time.LocalDateTime
import java.time.LocalDate

@DisplayName("PreferencesRepository 구현체 테스트")
class PreferencesRepositoryImplTest {

    @MockK
    private lateinit var dataStore: DataStore<Preferences>
    
    private lateinit var repository: PreferencesRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        repository = PreferencesRepositoryImpl(
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    @Nested
    @DisplayName("사용자 설정 읽기 테스트")
    inner class ReadPreferencesTests {
        
        @Test
        @DisplayName("일일 목표 조회")
        fun `should get daily goal`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.DAILY_GOAL to 150
            )
            val flow = flowOf(preferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getDailyGoalFlow().test {
                // Then
                val result = awaitItem()
                result shouldBe 150
                awaitComplete()
            }
            
            verify { dataStore.data }
        }
        
        @Test
        @DisplayName("기본값으로 일일 목표 조회")
        fun `should return default daily goal when not set`() = runTest(testDispatcher) {
            // Given
            val emptyPreferences = createPreferences()
            val flow = flowOf(emptyPreferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getDailyGoalFlow().test {
                // Then
                val result = awaitItem()
                result shouldBe 100 // Default value
                awaitComplete()
            }
        }
        
        @Test
        @DisplayName("알림 설정 조회")
        fun `should get notification settings`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.NOTIFICATIONS_ENABLED to true,
                PreferencesKeys.NOTIFICATION_TIME to "09:00"
            )
            val flow = flowOf(preferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getNotificationSettingsFlow().test {
                // Then
                val result = awaitItem()
                result.enabled shouldBe true
                result.time shouldBe "09:00"
                awaitComplete()
            }
        }
        
        @Test
        @DisplayName("테마 설정 조회")
        fun `should get theme preference`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.THEME_MODE to "DARK"
            )
            val flow = flowOf(preferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getThemeFlow().test {
                // Then
                val result = awaitItem()
                result shouldBe ThemeMode.DARK
                awaitComplete()
            }
        }
        
        @Test
        @DisplayName("첫 실행 여부 조회")
        fun `should get first launch status`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.IS_FIRST_LAUNCH to false
            )
            
            coEvery { dataStore.data.first() } returns preferences
            
            // When
            val result = repository.isFirstLaunch()
            
            // Then
            result shouldBe false
            
            coVerify { dataStore.data.first() }
        }
    }
    
    @Nested
    @DisplayName("사용자 설정 저장 테스트")
    inner class SavePreferencesTests {
        
        @ParameterizedTest
        @ValueSource(ints = [10, 50, 100, 200, 500])
        @DisplayName("일일 목표 저장")
        fun `should save daily goal`(goal: Int) = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setDailyGoal(goal)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("잘못된 일일 목표 거부")
        fun `should reject invalid daily goal`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When/Then
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    repository.setDailyGoal(0)
                }
            }
            
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    repository.setDailyGoal(-10)
                }
            }
            
            assertThrows<IllegalArgumentException> {
                runBlocking {
                    repository.setDailyGoal(10001)
                }
            }
            
            coVerify(exactly = 0) { dataStore.edit(any()) }
        }
        
        @Test
        @DisplayName("알림 설정 저장")
        fun `should save notification settings`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setNotificationEnabled(true)
            repository.setNotificationTime("10:30")
            
            // Then
            coVerify(exactly = 2) {
                dataStore.edit(any())
            }
        }
        
        @ParameterizedTest
        @CsvSource(
            "LIGHT,LIGHT",
            "DARK,DARK",
            "SYSTEM,SYSTEM"
        )
        @DisplayName("테마 설정 저장")
        fun `should save theme preference`(input: String, expected: String) = runTest(testDispatcher) {
            // Given
            val theme = ThemeMode.valueOf(input)
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setTheme(theme)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("첫 실행 상태 업데이트")
        fun `should update first launch status`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setFirstLaunchCompleted()
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
    }
    
    @Nested
    @DisplayName("온보딩 설정 테스트")
    inner class OnboardingTests {
        
        @Test
        @DisplayName("온보딩 완료 상태 저장")
        fun `should save onboarding completion`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setOnboardingCompleted()
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("온보딩 단계 저장")
        fun `should save onboarding step`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setOnboardingStep(3)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("온보딩 상태 조회")
        fun `should get onboarding status`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.ONBOARDING_COMPLETED to true,
                PreferencesKeys.ONBOARDING_STEP to 5
            )
            
            coEvery { dataStore.data.first() } returns preferences
            
            // When
            val completed = repository.isOnboardingCompleted()
            val step = repository.getOnboardingStep()
            
            // Then
            completed shouldBe true
            step shouldBe 5
        }
    }
    
    @Nested
    @DisplayName("BLE 설정 테스트")
    inner class BleSettingsTests {
        
        @Test
        @DisplayName("자동 연결 설정 저장")
        fun `should save auto connect preference`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setAutoConnect(true)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("마지막 연결 디바이스 저장")
        fun `should save last connected device`() = runTest(testDispatcher) {
            // Given
            val deviceAddress = "00:11:22:33:44:55"
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setLastConnectedDevice(deviceAddress)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("BLE 설정 조회")
        fun `should get BLE settings`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.AUTO_CONNECT to true,
                PreferencesKeys.LAST_CONNECTED_DEVICE to "AA:BB:CC:DD:EE:FF"
            )
            val flow = flowOf(preferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getBleSettingsFlow().test {
                // Then
                val result = awaitItem()
                result.autoConnect shouldBe true
                result.lastDeviceAddress shouldBe "AA:BB:CC:DD:EE:FF"
                awaitComplete()
            }
        }
    }
    
    @Nested
    @DisplayName("백업 및 복원 테스트")
    inner class BackupRestoreTests {
        
        @Test
        @DisplayName("마지막 백업 시간 저장")
        fun `should save last backup time`() = runTest(testDispatcher) {
            // Given
            val backupTime = LocalDateTime.now()
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setLastBackupTime(backupTime)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("백업 설정 조회")
        fun `should get backup settings`() = runTest(testDispatcher) {
            // Given
            val lastBackup = "2025-01-06T10:00:00"
            val preferences = createPreferences(
                PreferencesKeys.LAST_BACKUP_TIME to lastBackup,
                PreferencesKeys.AUTO_BACKUP_ENABLED to true
            )
            
            coEvery { dataStore.data.first() } returns preferences
            
            // When
            val backupTime = repository.getLastBackupTime()
            val autoBackup = repository.isAutoBackupEnabled()
            
            // Then
            backupTime shouldNotBe null
            autoBackup shouldBe true
        }
        
        @Test
        @DisplayName("모든 설정 초기화")
        fun `should clear all preferences`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.clearAllPreferences()
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("설정 내보내기")
        fun `should export all settings`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.DAILY_GOAL to 200,
                PreferencesKeys.THEME_MODE to "DARK",
                PreferencesKeys.NOTIFICATIONS_ENABLED to true
            )
            
            coEvery { dataStore.data.first() } returns preferences
            
            // When
            val exported = repository.exportSettings()
            
            // Then
            exported.size shouldBe 3
            exported["daily_goal"] shouldBe 200
            exported["theme_mode"] shouldBe "DARK"
            exported["notifications_enabled"] shouldBe true
        }
        
        @Test
        @DisplayName("설정 가져오기")
        fun `should import settings`() = runTest(testDispatcher) {
            // Given
            val settings = mapOf(
                "daily_goal" to 150,
                "theme_mode" to "LIGHT",
                "notifications_enabled" to false
            )
            
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.importSettings(settings)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
    }
    
    @Nested
    @DisplayName("통계 설정 테스트")
    inner class StatisticsSettingsTests {
        
        @Test
        @DisplayName("통계 기간 설정 저장")
        fun `should save statistics period`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setStatisticsPeriod(StatisticsPeriod.MONTHLY)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("그래프 타입 설정 저장")
        fun `should save graph type preference`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.setGraphType(GraphType.BAR)
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("통계 설정 조회")
        fun `should get statistics settings`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.STATS_PERIOD to "WEEKLY",
                PreferencesKeys.GRAPH_TYPE to "LINE"
            )
            val flow = flowOf(preferences)
            
            every { dataStore.data } returns flow
            
            // When
            repository.getStatisticsSettingsFlow().test {
                // Then
                val result = awaitItem()
                result.period shouldBe StatisticsPeriod.WEEKLY
                result.graphType shouldBe GraphType.LINE
                awaitComplete()
            }
        }
    }
    
    @Nested
    @DisplayName("마이그레이션 테스트")
    inner class MigrationTests {
        
        @Test
        @DisplayName("버전 1에서 2로 마이그레이션")
        fun `should migrate from version 1 to 2`() = runTest(testDispatcher) {
            // Given
            val oldPreferences = createPreferences(
                PreferencesKeys.PREFERENCES_VERSION to 1,
                PreferencesKeys.DAILY_GOAL to 100
            )
            
            coEvery { dataStore.data.first() } returns oldPreferences
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When
            repository.migrateIfNeeded()
            
            // Then
            coVerify {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("최신 버전은 마이그레이션 불필요")
        fun `should skip migration for latest version`() = runTest(testDispatcher) {
            // Given
            val preferences = createPreferences(
                PreferencesKeys.PREFERENCES_VERSION to 2
            )
            
            coEvery { dataStore.data.first() } returns preferences
            
            // When
            repository.migrateIfNeeded()
            
            // Then
            coVerify(exactly = 0) {
                dataStore.edit(any())
            }
        }
    }
    
    @Nested
    @DisplayName("동시성 테스트")
    inner class ConcurrencyTests {
        
        @Test
        @DisplayName("동시 쓰기 작업 처리")
        fun `should handle concurrent write operations`() = runTest(testDispatcher) {
            // Given
            coEvery { dataStore.edit(any()) } returns mockk()
            
            // When - 동시에 여러 설정 변경
            val jobs = listOf(
                launch { repository.setDailyGoal(150) },
                launch { repository.setNotificationEnabled(true) },
                launch { repository.setTheme(ThemeMode.DARK) },
                launch { repository.setAutoConnect(true) }
            )
            
            jobs.forEach { it.join() }
            
            // Then
            coVerify(exactly = 4) {
                dataStore.edit(any())
            }
        }
        
        @Test
        @DisplayName("읽기 중 쓰기 작업 처리")
        fun `should handle write during read`() = runTest(testDispatcher) {
            // Given
            val mutableFlow = MutableStateFlow(
                createPreferences(PreferencesKeys.DAILY_GOAL to 100)
            )
            
            every { dataStore.data } returns mutableFlow
            coEvery { dataStore.edit(any()) } answers {
                mutableFlow.update {
                    createPreferences(PreferencesKeys.DAILY_GOAL to 200)
                }
                mockk()
            }
            
            // When
            repository.getDailyGoalFlow().test {
                val initial = awaitItem()
                initial shouldBe 100
                
                repository.setDailyGoal(200)
                
                val updated = awaitItem()
                updated shouldBe 200
                
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    
    @Nested
    @DisplayName("Property-based 테스트")
    inner class PropertyBasedTests {
        
        @Test
        @DisplayName("일일 목표는 항상 유효 범위 내")
        fun `daily goal should always be within valid range`() = runTest {
            checkAll(Arb.int()) { goal ->
                if (goal in 1..10000) {
                    // Valid range - should not throw
                    coEvery { dataStore.edit(any()) } returns mockk()
                    repository.setDailyGoal(goal)
                } else {
                    // Invalid range - should throw
                    assertThrows<IllegalArgumentException> {
                        runBlocking {
                            repository.setDailyGoal(goal)
                        }
                    }
                }
            }
        }
        
        @Test
        @DisplayName("설정 내보내기/가져오기 일관성")
        fun `export and import should be consistent`() = runTest {
            checkAll(
                Arb.int(1..1000),
                Arb.boolean(),
                Arb.enum<ThemeMode>()
            ) { goal, notifications, theme ->
                // Given
                val originalSettings = mapOf(
                    "daily_goal" to goal,
                    "notifications_enabled" to notifications,
                    "theme_mode" to theme.name
                )
                
                coEvery { dataStore.edit(any()) } returns mockk()
                coEvery { dataStore.data.first() } returns createPreferences(
                    PreferencesKeys.DAILY_GOAL to goal,
                    PreferencesKeys.NOTIFICATIONS_ENABLED to notifications,
                    PreferencesKeys.THEME_MODE to theme.name
                )
                
                // When
                repository.importSettings(originalSettings)
                val exported = repository.exportSettings()
                
                // Then
                exported["daily_goal"] shouldBe goal
                exported["notifications_enabled"] shouldBe notifications
                exported["theme_mode"] shouldBe theme.name
            }
        }
    }
    
    // Helper functions
    private fun createPreferences(vararg pairs: Pair<Preferences.Key<*>, Any>): Preferences {
        return mutablePreferencesOf(*pairs)
    }
}

// Mock implementation
class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: TestDispatcher
) : PreferencesRepository {
    
    override fun getDailyGoalFlow(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL] ?: 100
        }
    }
    
    override suspend fun setDailyGoal(goal: Int) {
        require(goal in 1..10000) { "Daily goal must be between 1 and 10000" }
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL] = goal
        }
    }
    
    override fun getNotificationSettingsFlow(): Flow<NotificationSettings> {
        return dataStore.data.map { preferences ->
            NotificationSettings(
                enabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false,
                time = preferences[PreferencesKeys.NOTIFICATION_TIME] ?: "09:00"
            )
        }
    }
    
    override suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    override suspend fun setNotificationTime(time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_TIME] = time
        }
    }
    
    override fun getThemeFlow(): Flow<ThemeMode> {
        return dataStore.data.map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
            ThemeMode.valueOf(themeName)
        }
    }
    
    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme.name
        }
    }
    
    override suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.first()[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
    }
    
    override suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }
    
    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }
    
    override suspend fun setOnboardingStep(step: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_STEP] = step
        }
    }
    
    override suspend fun isOnboardingCompleted(): Boolean {
        return dataStore.data.first()[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }
    
    override suspend fun getOnboardingStep(): Int {
        return dataStore.data.first()[PreferencesKeys.ONBOARDING_STEP] ?: 0
    }
    
    override suspend fun setAutoConnect(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CONNECT] = enabled
        }
    }
    
    override suspend fun setLastConnectedDevice(address: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CONNECTED_DEVICE] = address
        }
    }
    
    override fun getBleSettingsFlow(): Flow<BleSettings> {
        return dataStore.data.map { preferences ->
            BleSettings(
                autoConnect = preferences[PreferencesKeys.AUTO_CONNECT] ?: false,
                lastDeviceAddress = preferences[PreferencesKeys.LAST_CONNECTED_DEVICE]
            )
        }
    }
    
    override suspend fun setLastBackupTime(time: LocalDateTime) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = time.toString()
        }
    }
    
    override suspend fun getLastBackupTime(): LocalDateTime? {
        val timeString = dataStore.data.first()[PreferencesKeys.LAST_BACKUP_TIME]
        return timeString?.let { LocalDateTime.parse(it) }
    }
    
    override suspend fun isAutoBackupEnabled(): Boolean {
        return dataStore.data.first()[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
    }
    
    override suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    override suspend fun exportSettings(): Map<String, Any> {
        val preferences = dataStore.data.first()
        val settings = mutableMapOf<String, Any>()
        
        preferences[PreferencesKeys.DAILY_GOAL]?.let { settings["daily_goal"] = it }
        preferences[PreferencesKeys.THEME_MODE]?.let { settings["theme_mode"] = it }
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED]?.let { settings["notifications_enabled"] = it }
        
        return settings
    }
    
    override suspend fun importSettings(settings: Map<String, Any>) {
        dataStore.edit { preferences ->
            settings["daily_goal"]?.let { 
                preferences[PreferencesKeys.DAILY_GOAL] = it as Int 
            }
            settings["theme_mode"]?.let { 
                preferences[PreferencesKeys.THEME_MODE] = it as String 
            }
            settings["notifications_enabled"]?.let { 
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = it as Boolean 
            }
        }
    }
    
    override suspend fun setStatisticsPeriod(period: StatisticsPeriod) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.STATS_PERIOD] = period.name
        }
    }
    
    override suspend fun setGraphType(type: GraphType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRAPH_TYPE] = type.name
        }
    }
    
    override fun getStatisticsSettingsFlow(): Flow<StatisticsSettings> {
        return dataStore.data.map { preferences ->
            StatisticsSettings(
                period = preferences[PreferencesKeys.STATS_PERIOD]?.let { 
                    StatisticsPeriod.valueOf(it) 
                } ?: StatisticsPeriod.WEEKLY,
                graphType = preferences[PreferencesKeys.GRAPH_TYPE]?.let { 
                    GraphType.valueOf(it) 
                } ?: GraphType.LINE
            )
        }
    }
    
    suspend fun migrateIfNeeded() {
        val currentVersion = dataStore.data.first()[PreferencesKeys.PREFERENCES_VERSION] ?: 1
        
        if (currentVersion < 2) {
            dataStore.edit { preferences ->
                // Perform migration
                preferences[PreferencesKeys.PREFERENCES_VERSION] = 2
            }
        }
    }
}

// Supporting classes
object PreferencesKeys {
    val DAILY_GOAL = intPreferencesKey("daily_goal")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val NOTIFICATION_TIME = stringPreferencesKey("notification_time")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
    val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    val LAST_CONNECTED_DEVICE = stringPreferencesKey("last_connected_device")
    val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")
    val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    val STATS_PERIOD = stringPreferencesKey("stats_period")
    val GRAPH_TYPE = stringPreferencesKey("graph_type")
    val PREFERENCES_VERSION = intPreferencesKey("preferences_version")
}

data class NotificationSettings(
    val enabled: Boolean,
    val time: String
)

data class BleSettings(
    val autoConnect: Boolean,
    val lastDeviceAddress: String?
)

data class StatisticsSettings(
    val period: StatisticsPeriod,
    val graphType: GraphType
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class StatisticsPeriod {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

enum class GraphType {
    LINE, BAR, PIE
}