package com.wishring.app.ble

import com.wishring.app.data.ble.MrdProtocolAdapter
import com.wishring.app.data.model.HealthData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.experimental.xor
import kotlin.random.Random

/**
 * MRD SDK Protocol Adapter 테스트
 * 
 * MRD SDK와 앱 간의 프로토콜 변환 및 데이터 무결성을 검증합니다.
 * 
 * 테스트 전략:
 * 1. Packet Structure - 패킷 구조 검증
 * 2. Command Encoding - 명령 인코딩
 * 3. Response Parsing - 응답 파싱
 * 4. Error Detection - 오류 감지
 * 5. Protocol Versioning - 프로토콜 버전 관리
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("MRD Protocol Adapter 테스트 - Protocol Compliance")
class MrdProtocolAdapterTest {
    
    private lateinit var adapter: MrdProtocolAdapter
    
    @BeforeEach
    fun setup() {
        adapter = MrdProtocolAdapter()
    }
    
    @Nested
    @DisplayName("1. Packet Structure - 패킷 구조")
    inner class PacketStructureTest {
        
        @Test
        @DisplayName("기본 패킷 구조 검증")
        fun testBasicPacketStructure() {
            // Given
            val command = MrdProtocolAdapter.Command.INCREMENT_COUNT
            
            // When
            val packet = adapter.createCommand(command)
            
            // Then
            packet shouldHaveSize 8 // 최소 패킷 크기
            packet[0] shouldBe PACKET_START_BYTE
            packet[1] shouldBe PROTOCOL_VERSION
            packet[2] shouldBe command.code
            packet[3] shouldBe 0x00 // 길이 상위 바이트
            packet[4] shouldBe 0x00 // 길이 하위 바이트
            packet[packet.size - 2] shouldBe calculateChecksum(packet.sliceArray(0..packet.size - 3))
            packet[packet.size - 1] shouldBe PACKET_END_BYTE
        }
        
        @Test
        @DisplayName("페이로드 포함 패킷 구조")
        fun testPacketWithPayload() {
            // Given
            val command = MrdProtocolAdapter.Command.SET_LED_COLOR
            val payload = byteArrayOf(0xFF.toByte(), 0x00, 0x00) // Red color
            
            // When
            val packet = adapter.createCommand(command, payload)
            
            // Then
            packet shouldHaveSize (8 + payload.size)
            
            // 길이 필드 검증
            val length = (packet[3].toInt() shl 8) or (packet[4].toInt() and 0xFF)
            length shouldBe payload.size
            
            // 페이로드 검증
            val extractedPayload = packet.sliceArray(5 until 5 + payload.size)
            extractedPayload shouldContainExactly payload.toList()
        }
        
        @ParameterizedTest
        @ValueSource(ints = [0, 1, 10, 100, 255, 512, 1024])
        @DisplayName("다양한 페이로드 크기 처리")
        fun testVariousPayloadSizes(size: Int) {
            // Given
            val payload = ByteArray(size) { it.toByte() }
            
            // When
            val packet = adapter.createCommand(MrdProtocolAdapter.Command.CUSTOM_DATA, payload)
            
            // Then
            val extractedLength = (packet[3].toInt() shl 8) or (packet[4].toInt() and 0xFF)
            extractedLength shouldBe size
            
            if (size > 0) {
                val extractedPayload = packet.sliceArray(5 until 5 + size)
                extractedPayload shouldContainExactly payload.toList()
            }
        }
    }
    
    @Nested
    @DisplayName("2. Command Encoding - 명령 인코딩")
    inner class CommandEncodingTest {
        
        @Test
        @DisplayName("모든 명령 타입 인코딩")
        fun testAllCommandTypes() {
            // Given
            val commands = MrdProtocolAdapter.Command.values()
            
            // When & Then
            commands.forEach { command ->
                val packet = adapter.createCommand(command)
                
                packet[2] shouldBe command.code
                validatePacketIntegrity(packet) shouldBe true
            }
        }
        
        @Test
        @DisplayName("위시 카운트 증가 명령")
        fun testIncrementWishCountCommand() {
            // Given & When
            val packet = adapter.createIncrementCommand()
            
            // Then
            packet[2] shouldBe MrdProtocolAdapter.Command.INCREMENT_COUNT.code
            
            // 타임스탬프 검증 (페이로드에 포함)
            val timestamp = ByteBuffer.wrap(packet.sliceArray(5..8))
                .order(ByteOrder.BIG_ENDIAN)
                .int
            
            val currentTime = System.currentTimeMillis() / 1000
            Math.abs(timestamp - currentTime) < 5 shouldBe true // 5초 이내
        }
        
        @Test
        @DisplayName("LED 색상 설정 명령")
        fun testSetLedColorCommand() {
            // Given
            val colors = listOf(
                Triple(255, 0, 0),    // Red
                Triple(0, 255, 0),    // Green
                Triple(0, 0, 255),    // Blue
                Triple(255, 255, 255), // White
                Triple(128, 64, 192)  // Custom
            )
            
            // When & Then
            colors.forEach { (r, g, b) ->
                val packet = adapter.setLedColor(r, g, b)
                
                packet[2] shouldBe MrdProtocolAdapter.Command.SET_LED_COLOR.code
                packet[5] shouldBe r.toByte()
                packet[6] shouldBe g.toByte()
                packet[7] shouldBe b.toByte()
            }
        }
        
        @Test
        @DisplayName("진동 패턴 설정 명령")
        fun testVibrationPatternCommand() {
            // Given
            val patterns = listOf(
                listOf(100, 50, 100, 50, 100), // Triple pulse
                listOf(500),                    // Single long
                listOf(50, 50, 50, 50),        // Quick pulses
                List(10) { 100 }                // 10 pulses
            )
            
            // When & Then
            patterns.forEach { pattern ->
                val packet = adapter.setVibrationPattern(pattern)
                
                packet[2] shouldBe MrdProtocolAdapter.Command.VIBRATE.code
                
                // 패턴 데이터 검증
                val patternCount = packet[5].toInt()
                patternCount shouldBe pattern.size
                
                pattern.forEachIndexed { index, duration ->
                    val extractedDuration = ByteBuffer.wrap(
                        packet.sliceArray(6 + index * 2 until 8 + index * 2)
                    ).order(ByteOrder.BIG_ENDIAN).short.toInt()
                    
                    extractedDuration shouldBe duration
                }
            }
        }
    }
    
    @Nested
    @DisplayName("3. Response Parsing - 응답 파싱")
    inner class ResponseParsingTest {
        
        @Test
        @DisplayName("건강 데이터 응답 파싱")
        fun testHealthDataParsing() {
            // Given - MRD SDK 건강 데이터 형식
            val healthPacket = createHealthDataPacket(
                heartRate = 75,
                steps = 10000,
                sleepScore = 85,
                stress = 40,
                spo2 = 98
            )
            
            // When
            val healthData = adapter.parseHealthData(healthPacket)
            
            // Then
            healthData shouldNotBe null
            healthData?.heartRate shouldBe 75
            healthData?.steps shouldBe 10000
            healthData?.sleepScore shouldBe 85
            healthData?.stress shouldBe 40
            healthData?.spo2 shouldBe 98
        }
        
        @Test
        @DisplayName("상태 응답 파싱")
        fun testStatusResponseParsing() {
            // Given
            val statusPacket = createStatusPacket(
                batteryLevel = 85,
                firmwareVersion = "1.2.3",
                isCharging = false,
                wishCount = 42
            )
            
            // When
            val status = adapter.parseStatusResponse(statusPacket)
            
            // Then
            status.batteryLevel shouldBe 85
            status.firmwareVersion shouldBe "1.2.3"
            status.isCharging shouldBe false
            status.wishCount shouldBe 42
        }
        
        @Test
        @DisplayName("ACK/NACK 응답 파싱")
        fun testAckNackParsing() {
            // Given
            val ackPacket = createAckPacket(MrdProtocolAdapter.Command.INCREMENT_COUNT)
            val nackPacket = createNackPacket(
                MrdProtocolAdapter.Command.SET_LED_COLOR,
                ErrorCode.INVALID_PARAMETER
            )
            
            // When
            val ackResult = adapter.parseResponse(ackPacket)
            val nackResult = adapter.parseResponse(nackPacket)
            
            // Then
            ackResult shouldBe MrdProtocolAdapter.Response.ACK
            nackResult shouldBe MrdProtocolAdapter.Response.NACK(ErrorCode.INVALID_PARAMETER)
        }
        
        @ParameterizedTest
        @CsvSource(
            "0,0,0,POOR",
            "50,30,20,FAIR",
            "80,70,60,GOOD",
            "95,90,85,EXCELLENT"
        )
        @DisplayName("수면 품질 계산")
        fun testSleepQualityCalculation(
            deepSleep: Int,
            remSleep: Int,
            lightSleep: Int,
            expectedQuality: String
        ) {
            // Given
            val sleepPacket = createSleepDataPacket(deepSleep, remSleep, lightSleep)
            
            // When
            val sleepData = adapter.parseSleepData(sleepPacket)
            
            // Then
            sleepData.quality shouldBe expectedQuality
            sleepData.deepSleepPercent shouldBe deepSleep
            sleepData.remSleepPercent shouldBe remSleep
            sleepData.lightSleepPercent shouldBe lightSleep
        }
    }
    
    @Nested
    @DisplayName("4. Error Detection - 오류 감지")
    inner class ErrorDetectionTest {
        
        @Test
        @DisplayName("체크섬 오류 감지")
        fun testChecksumErrorDetection() {
            // Given
            val validPacket = adapter.createCommand(MrdProtocolAdapter.Command.GET_STATUS)
            val corruptedPacket = validPacket.clone()
            corruptedPacket[corruptedPacket.size - 2] = 0xFF.toByte() // 잘못된 체크섬
            
            // When
            val validResult = adapter.validatePacket(validPacket)
            val corruptedResult = adapter.validatePacket(corruptedPacket)
            
            // Then
            validResult shouldBe true
            corruptedResult shouldBe false
        }
        
        @Test
        @DisplayName("패킷 경계 오류 감지")
        fun testPacketBoundaryErrors() {
            // Given
            val invalidPackets = listOf(
                byteArrayOf(0xFF.toByte(), 0x01, 0x02), // 잘못된 시작 바이트
                byteArrayOf(PACKET_START_BYTE, 0x01, 0x02), // 종료 바이트 없음
                byteArrayOf(PACKET_START_BYTE), // 너무 짧은 패킷
                ByteArray(2048) { 0xFF.toByte() } // 너무 긴 패킷
            )
            
            // When & Then
            invalidPackets.forEach { packet ->
                adapter.validatePacket(packet) shouldBe false
            }
        }
        
        @Test
        @DisplayName("프로토콜 버전 불일치 감지")
        fun testProtocolVersionMismatch() {
            // Given
            val oldVersionPacket = byteArrayOf(
                PACKET_START_BYTE,
                0x01, // 구 버전
                0x10, // 명령
                0x00, 0x00, // 길이
                0x00, // 체크섬 (임시)
                PACKET_END_BYTE
            )
            
            // When
            val result = adapter.isVersionCompatible(oldVersionPacket)
            
            // Then
            result shouldBe false
        }
        
        @Test
        @DisplayName("비트 플립 오류 시뮬레이션")
        fun testBitFlipErrorSimulation() {
            // Given
            val originalPacket = adapter.createCommand(
                MrdProtocolAdapter.Command.INCREMENT_COUNT,
                byteArrayOf(0x12, 0x34, 0x56, 0x78)
            )
            
            var detectedErrors = 0
            val iterations = 1000
            
            // When - 랜덤 비트 플립 시뮬레이션
            repeat(iterations) {
                val corruptedPacket = originalPacket.clone()
                val byteIndex = Random.nextInt(corruptedPacket.size)
                val bitIndex = Random.nextInt(8)
                
                corruptedPacket[byteIndex] = 
                    (corruptedPacket[byteIndex].toInt() xor (1 shl bitIndex)).toByte()
                
                if (!adapter.validatePacket(corruptedPacket)) {
                    detectedErrors++
                }
            }
            
            // Then - 99% 이상의 오류 감지율
            val detectionRate = detectedErrors.toFloat() / iterations
            detectionRate > 0.99f shouldBe true
        }
    }
    
    @Nested
    @DisplayName("5. Protocol State Machine - 프로토콜 상태 머신")
    inner class ProtocolStateMachineTest {
        
        @Test
        @DisplayName("명령-응답 시퀀스 추적")
        fun testCommandResponseSequence() {
            // Given
            val sequenceTracker = adapter.createSequenceTracker()
            
            // When - 명령 전송
            val command1 = adapter.createCommand(MrdProtocolAdapter.Command.GET_STATUS)
            val seq1 = sequenceTracker.sendCommand(command1)
            
            val command2 = adapter.createCommand(MrdProtocolAdapter.Command.INCREMENT_COUNT)
            val seq2 = sequenceTracker.sendCommand(command2)
            
            // 응답 수신
            val response1 = createAckPacket(MrdProtocolAdapter.Command.GET_STATUS, seq1)
            val response2 = createAckPacket(MrdProtocolAdapter.Command.INCREMENT_COUNT, seq2)
            
            // Then
            sequenceTracker.matchResponse(response1) shouldBe seq1
            sequenceTracker.matchResponse(response2) shouldBe seq2
            sequenceTracker.getPendingCommands() shouldHaveSize 0
        }
        
        @Test
        @DisplayName("타임아웃 명령 처리")
        fun testTimeoutHandling() {
            // Given
            val sequenceTracker = adapter.createSequenceTracker()
            sequenceTracker.setTimeout(1000) // 1초 타임아웃
            
            // When
            val command = adapter.createCommand(MrdProtocolAdapter.Command.GET_STATUS)
            val seq = sequenceTracker.sendCommand(command)
            
            Thread.sleep(1500) // 타임아웃 대기
            
            // Then
            sequenceTracker.getTimedOutCommands() shouldContain seq
            sequenceTracker.shouldRetry(seq) shouldBe true
        }
        
        @Test
        @DisplayName("중복 응답 필터링")
        fun testDuplicateResponseFiltering() {
            // Given
            val sequenceTracker = adapter.createSequenceTracker()
            
            // When
            val command = adapter.createCommand(MrdProtocolAdapter.Command.INCREMENT_COUNT)
            val seq = sequenceTracker.sendCommand(command)
            
            val response = createAckPacket(MrdProtocolAdapter.Command.INCREMENT_COUNT, seq)
            
            // 동일 응답 2번 수신
            val firstMatch = sequenceTracker.matchResponse(response)
            val secondMatch = sequenceTracker.matchResponse(response)
            
            // Then
            firstMatch shouldBe seq
            secondMatch shouldBe null // 중복 응답은 무시
        }
    }
    
    @Nested
    @DisplayName("6. Data Compression - 데이터 압축")
    inner class DataCompressionTest {
        
        @Test
        @DisplayName("건강 데이터 압축/해제")
        fun testHealthDataCompression() {
            // Given
            val healthData = HealthData(
                heartRate = 75,
                steps = 10000,
                sleepScore = 85,
                stress = 40,
                spo2 = 98,
                temperature = 36.5f
            )
            
            // When
            val compressed = adapter.compressHealthData(healthData)
            val decompressed = adapter.decompressHealthData(compressed)
            
            // Then
            compressed.size < 20 shouldBe true // 압축된 크기
            decompressed shouldBe healthData
        }
        
        @Test
        @DisplayName("배치 데이터 전송")
        fun testBatchDataTransmission() {
            // Given
            val batchData = List(100) { index ->
                HealthData(
                    heartRate = 60 + index % 40,
                    steps = index * 100,
                    sleepScore = 70 + index % 30
                )
            }
            
            // When
            val batches = adapter.createBatches(batchData, maxBatchSize = 10)
            
            // Then
            batches shouldHaveSize 10
            batches.forEach { batch ->
                batch.size shouldBe 10
            }
            
            // 배치 복원 검증
            val restored = batches.flatMap { adapter.parseBatch(it) }
            restored shouldBe batchData
        }
    }
    
    @Nested
    @DisplayName("7. Concurrent Protocol Operations - 동시성 프로토콜 작업")
    inner class ConcurrentProtocolTest {
        
        @Test
        @DisplayName("멀티스레드 패킷 생성 안전성")
        fun testMultithreadedPacketGeneration() {
            // Given
            val threadCount = 10
            val commandsPerThread = 100
            val packets = ConcurrentHashMap<Int, ByteArray>()
            val latch = CountDownLatch(threadCount)
            
            // When
            val executor = Executors.newFixedThreadPool(threadCount)
            
            repeat(threadCount) { threadId ->
                executor.submit {
                    repeat(commandsPerThread) { commandId ->
                        val packet = adapter.createCommand(
                            MrdProtocolAdapter.Command.values().random(),
                            ByteArray(Random.nextInt(0, 100))
                        )
                        packets[threadId * commandsPerThread + commandId] = packet
                    }
                    latch.countDown()
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            
            // Then
            packets.size shouldBe threadCount * commandsPerThread
            
            // 모든 패킷 유효성 검증
            packets.values.forEach { packet ->
                validatePacketIntegrity(packet) shouldBe true
            }
        }
        
        @Test
        @DisplayName("동시 응답 파싱 정확성")
        fun testConcurrentResponseParsing() {
            // Given
            val responses = List(1000) { index ->
                when (index % 3) {
                    0 -> createHealthDataPacket(heartRate = 60 + index % 40)
                    1 -> createStatusPacket(batteryLevel = index % 100)
                    else -> createAckPacket(MrdProtocolAdapter.Command.INCREMENT_COUNT)
                }
            }
            
            val parsedResults = ConcurrentHashMap<Int, Any>()
            
            // When
            responses.parallelStream().forEach { response ->
                val result = adapter.parseResponse(response)
                parsedResults[response.hashCode()] = result
            }
            
            // Then
            parsedResults.size shouldBe responses.size
            parsedResults.values.none { it == null } shouldBe true
        }
    }
    
    // Helper functions
    
    private fun createHealthDataPacket(
        heartRate: Int = 75,
        steps: Int = 0,
        sleepScore: Int = 0,
        stress: Int = 0,
        spo2: Int = 98
    ): ByteArray {
        val payload = ByteBuffer.allocate(10).apply {
            order(ByteOrder.BIG_ENDIAN)
            putShort(heartRate.toShort())
            putInt(steps)
            put(sleepScore.toByte())
            put(stress.toByte())
            putShort(spo2.toShort())
        }.array()
        
        return createPacket(ResponseType.HEALTH_DATA, payload)
    }
    
    private fun createStatusPacket(
        batteryLevel: Int,
        firmwareVersion: String,
        isCharging: Boolean,
        wishCount: Int
    ): ByteArray {
        val versionBytes = firmwareVersion.toByteArray()
        val payload = ByteBuffer.allocate(7 + versionBytes.size).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(batteryLevel.toByte())
            put(if (isCharging) 1 else 0)
            putInt(wishCount)
            put(versionBytes.size.toByte())
            put(versionBytes)
        }.array()
        
        return createPacket(ResponseType.STATUS, payload)
    }
    
    private fun createAckPacket(
        command: MrdProtocolAdapter.Command,
        sequence: Int = 0
    ): ByteArray {
        val payload = ByteBuffer.allocate(3).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(command.code)
            putShort(sequence.toShort())
        }.array()
        
        return createPacket(ResponseType.ACK, payload)
    }
    
    private fun createNackPacket(
        command: MrdProtocolAdapter.Command,
        errorCode: ErrorCode
    ): ByteArray {
        val payload = ByteBuffer.allocate(4).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(command.code)
            put(errorCode.code)
            putShort(0) // sequence
        }.array()
        
        return createPacket(ResponseType.NACK, payload)
    }
    
    private fun createSleepDataPacket(
        deepSleep: Int,
        remSleep: Int,
        lightSleep: Int
    ): ByteArray {
        val payload = ByteBuffer.allocate(3).apply {
            put(deepSleep.toByte())
            put(remSleep.toByte())
            put(lightSleep.toByte())
        }.array()
        
        return createPacket(ResponseType.SLEEP_DATA, payload)
    }
    
    private fun createPacket(type: ResponseType, payload: ByteArray): ByteArray {
        val packet = ByteBuffer.allocate(8 + payload.size).apply {
            put(PACKET_START_BYTE)
            put(PROTOCOL_VERSION)
            put(type.code)
            putShort(payload.size.toShort())
            put(payload)
            put(0) // 체크섬 (계산 예정)
            put(PACKET_END_BYTE)
        }.array()
        
        // 체크섬 계산
        packet[packet.size - 2] = calculateChecksum(packet.sliceArray(0..packet.size - 3))
        
        return packet
    }
    
    private fun calculateChecksum(data: ByteArray): Byte {
        return data.fold(0) { acc, byte -> acc xor byte.toInt() }.toByte()
    }
    
    private fun validatePacketIntegrity(packet: ByteArray): Boolean {
        if (packet.size < 8) return false
        if (packet[0] != PACKET_START_BYTE) return false
        if (packet[packet.size - 1] != PACKET_END_BYTE) return false
        
        val calculatedChecksum = calculateChecksum(packet.sliceArray(0..packet.size - 3))
        return packet[packet.size - 2] == calculatedChecksum
    }
    
    companion object {
        private const val PACKET_START_BYTE: Byte = 0xAA.toByte()
        private const val PACKET_END_BYTE: Byte = 0x55.toByte()
        private const val PROTOCOL_VERSION: Byte = 0x02
        
        @JvmStatic
        fun protocolVersionProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(0x01, false), // 구 버전
            Arguments.of(0x02, true),  // 현재 버전
            Arguments.of(0x03, false)  // 미래 버전
        )
    }
    
    enum class ResponseType(val code: Byte) {
        ACK(0x01),
        NACK(0x02),
        HEALTH_DATA(0x10),
        STATUS(0x11),
        SLEEP_DATA(0x12)
    }
    
    enum class ErrorCode(val code: Byte) {
        NONE(0x00),
        INVALID_COMMAND(0x01),
        INVALID_PARAMETER(0x02),
        CHECKSUM_ERROR(0x03),
        TIMEOUT(0x04),
        BUSY(0x05),
        NOT_READY(0x06)
    }
}

// Property-based testing for protocol invariants
class MrdProtocolPropertyTest : FunSpec({
    
    test("패킷 크기는 항상 페이로드 크기 + 8") {
        checkAll(
            Arb.byteArray(Arb.int(0..1000), Arb.byte())
        ) { payload ->
            val adapter = MrdProtocolAdapter()
            val packet = adapter.createCommand(
                MrdProtocolAdapter.Command.CUSTOM_DATA,
                payload
            )
            
            packet.size shouldBe payload.size + 8
        }
    }
    
    test("체크섬은 데이터 변경 감지") {
        checkAll(
            Arb.byteArray(Arb.int(10..100), Arb.byte()),
            Arb.int(0..99)
        ) { payload, corruptIndex ->
            val adapter = MrdProtocolAdapter()
            val packet = adapter.createCommand(
                MrdProtocolAdapter.Command.CUSTOM_DATA,
                payload
            )
            
            // 원본 유효성
            adapter.validatePacket(packet) shouldBe true
            
            // 데이터 변조
            val corrupted = packet.clone()
            if (corruptIndex < corrupted.size - 2) {
                corrupted[corruptIndex] = (corrupted[corruptIndex] + 1).toByte()
                
                // 변조 감지
                adapter.validatePacket(corrupted) shouldBe false
            }
        }
    }
    
    test("압축/해제 무손실 보장") {
        checkAll(
            Arb.int(40..200), // heartRate
            Arb.int(0..50000), // steps
            Arb.int(0..100) // sleepScore
        ) { heartRate, steps, sleepScore ->
            val adapter = MrdProtocolAdapter()
            val original = HealthData(
                heartRate = heartRate,
                steps = steps,
                sleepScore = sleepScore
            )
            
            val compressed = adapter.compressHealthData(original)
            val decompressed = adapter.decompressHealthData(compressed)
            
            decompressed shouldBe original
        }
    }
})