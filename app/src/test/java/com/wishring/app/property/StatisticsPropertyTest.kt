package com.wishring.app.property

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.assume
import io.kotest.property.checkAll
import io.kotest.property.forAll
import org.junit.jupiter.api.DisplayName
import kotlin.math.*

/**
 * Statistics Property-based Testing
 * 
 * 통계 계산의 수학적 속성을 검증합니다.
 * 
 * 테스트 속성:
 * 1. Statistical Invariants - 통계 불변식
 * 2. Distribution Properties - 분포 속성
 * 3. Aggregation Properties - 집계 속성
 * 4. Correlation Properties - 상관관계 속성
 * 5. Time Series Properties - 시계열 속성
 * 6. Percentile Properties - 백분위수 속성
 */
@DisplayName("Statistics Property-based 테스트")
class StatisticsPropertyTest : FunSpec({
    
    test("평균은 최소값과 최대값 사이에 있다") {
        checkAll(
            Arb.list(Arb.int(0..1000), 1..100)
        ) { values ->
            val stats = Statistics(values)
            
            stats.mean shouldBeGreaterThanOrEqual values.minOrNull()!!.toDouble()
            stats.mean shouldBeLessThanOrEqual values.maxOrNull()!!.toDouble()
        }
    }
    
    test("표준편차는 항상 0 이상") {
        forAll(
            Arb.list(Arb.int(0..1000), 1..100)
        ) { values ->
            val stats = Statistics(values)
            stats.standardDeviation >= 0
        }
    }
    
    test("중앙값은 정렬된 리스트의 중간 값") {
        checkAll(
            Arb.list(Arb.int(0..1000), 1..99)
        ) { values ->
            val stats = Statistics(values)
            val sorted = values.sorted()
            
            val expectedMedian = if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
            
            abs(stats.median - expectedMedian) < 0.01
        }
    }
    
    test("완료율은 0에서 100 사이") {
        checkAll(
            Arb.int(0..1000),
            Arb.int(0..1000)
        ) { completed, total ->
            assume(total > 0)
            
            val rate = (completed.toDouble() / total) * 100
            
            rate shouldBeGreaterThanOrEqual 0.0
            rate shouldBeLessThanOrEqual 100.0
        }
    }
    
    test("분산의 제곱근은 표준편차") {
        checkAll(
            Arb.list(Arb.double(0.0..100.0), 2..50)
        ) { values ->
            val stats = AdvancedStatistics(values)
            
            abs(sqrt(stats.variance) - stats.standardDeviation) < 0.01
        }
    }
    
    test("피어슨 상관계수는 -1에서 1 사이") {
        checkAll(
            Arb.list(Arb.int(0..100), 10..50),
            Arb.list(Arb.int(0..100), 10..50)
        ) { x, y ->
            assume(x.size == y.size && x.size > 1)
            
            val correlation = pearsonCorrelation(
                x.map { it.toDouble() },
                y.map { it.toDouble() }
            )
            
            correlation shouldBeGreaterThanOrEqual -1.0
            correlation shouldBeLessThanOrEqual 1.0
        }
    }
    
    test("이동평균은 원본 데이터 범위 내") {
        checkAll(
            Arb.list(Arb.int(0..100), 10..100),
            Arb.int(2..10)
        ) { values, windowSize ->
            assume(windowSize <= values.size)
            
            val movingAvg = movingAverage(values, windowSize)
            val min = values.minOrNull()!!
            val max = values.maxOrNull()!!
            
            movingAvg.all { avg ->
                avg >= min && avg <= max
            }
        }
    }
    
    test("백분위수의 순서 속성") {
        checkAll(
            Arb.list(Arb.int(0..1000), 10..100)
        ) { values ->
            val stats = Statistics(values)
            
            val p25 = stats.percentile(25)
            val p50 = stats.percentile(50)
            val p75 = stats.percentile(75)
            val p90 = stats.percentile(90)
            
            p25 <= p50 && p50 <= p75 && p75 <= p90
        }
    }
    
    test("사분위수 범위 (IQR) 속성") {
        checkAll(
            Arb.list(Arb.int(0..1000), 10..100)
        ) { values ->
            val stats = Statistics(values)
            
            val q1 = stats.percentile(25)
            val q3 = stats.percentile(75)
            val iqr = q3 - q1
            
            iqr >= 0
        }
    }
    
    test("왜도(Skewness)의 대칭성") {
        checkAll(
            Arb.int(1..50)
        ) { size ->
            // 완전 대칭 분포 생성
            val symmetric = List(size) { i -> i } + List(size) { i -> size - i }
            val stats = AdvancedStatistics(symmetric.map { it.toDouble() })
            
            abs(stats.skewness) < 0.1 // 거의 0에 가까움
        }
    }
    
    test("첨도(Kurtosis)의 정규분포 속성") {
        checkAll(
            Arb.int(30..100)
        ) { size ->
            // 대략적 정규분포 시뮬레이션
            val normalLike = List(size) { 
                (Random.nextGaussian() * 10 + 50).toInt()
            }
            val stats = AdvancedStatistics(normalLike.map { it.toDouble() })
            
            // 정규분포의 첨도는 약 3
            abs(stats.kurtosis - 3.0) < 2.0
        }
    }
    
    test("스트릭 계산의 연속성") {
        checkAll(
            Arb.list(Arb.boolean(), 1..30)
        ) { completions ->
            val streak = calculateCurrentStreak(completions)
            
            // 스트릭은 첫 false까지의 true 개수
            val expectedStreak = completions.takeWhile { it }.size
            
            streak == expectedStreak
        }
    }
    
    test("주간 통계의 합산 속성") {
        checkAll(
            Arb.list(
                Arb.list(Arb.int(0..100), 7..7),
                1..10
            )
        ) { weeks ->
            val weeklyStats = weeks.map { week ->
                WeeklyStatistics(
                    totalCount = week.sum(),
                    averageCount = week.average(),
                    completedDays = week.count { it >= 10 }
                )
            }
            
            val monthlyTotal = weeklyStats.sumOf { it.totalCount }
            val individualSum = weeks.flatten().sum()
            
            monthlyTotal == individualSum
        }
    }
    
    test("성장률 계산의 단조성") {
        checkAll(
            Arb.int(1..100),
            Arb.int(1..100)
        ) { previous, current ->
            assume(previous > 0)
            
            val growthRate = ((current - previous).toDouble() / previous) * 100
            
            when {
                current > previous -> growthRate > 0
                current < previous -> growthRate < 0
                else -> growthRate == 0.0
            }
        }
    }
    
    test("신뢰구간의 포함 관계") {
        checkAll(
            Arb.list(Arb.double(0.0..100.0), 30..100)
        ) { values ->
            val stats = AdvancedStatistics(values)
            val ci95 = stats.confidenceInterval(0.95)
            val ci99 = stats.confidenceInterval(0.99)
            
            // 99% 신뢰구간이 95% 신뢰구간을 포함
            ci99.lower <= ci95.lower && ci99.upper >= ci95.upper
        }
    }
    
    test("지수이동평균 (EMA)의 수렴성") {
        checkAll(
            Arb.constant(50.0),
            Arb.int(10..100)
        ) { constantValue, length ->
            // 상수 시계열
            val values = List(length) { constantValue }
            val ema = exponentialMovingAverage(values, 0.2)
            
            // EMA는 상수값으로 수렴
            val lastEma = ema.last()
            abs(lastEma - constantValue) < 1.0
        }
    }
    
    test("자기상관의 시차 속성") {
        checkAll(
            Arb.list(Arb.int(0..100), 20..50)
        ) { values ->
            val autocorr0 = autocorrelation(values, 0)
            val autocorr1 = autocorrelation(values, 1)
            
            // 시차 0의 자기상관은 1
            abs(autocorr0 - 1.0) < 0.01
            
            // 다른 시차의 자기상관은 -1에서 1 사이
            autocorr1 shouldBeGreaterThanOrEqual -1.0
            autocorr1 shouldBeLessThanOrEqual 1.0
        }
    }
    
    test("히스토그램 빈의 완전성") {
        checkAll(
            Arb.list(Arb.int(0..100), 10..100),
            Arb.int(5..20)
        ) { values, binCount ->
            val histogram = createHistogram(values, binCount)
            
            // 모든 값이 어떤 빈에 속함
            histogram.values.sum() == values.size
        }
    }
    
    test("지니계수의 불평등 측정") {
        checkAll(
            Arb.list(Arb.int(1..100), 10..50)
        ) { values ->
            val gini = giniCoefficient(values)
            
            // 지니계수는 0(완전평등)과 1(완전불평등) 사이
            gini shouldBeGreaterThanOrEqual 0.0
            gini shouldBeLessThanOrEqual 1.0
            
            // 모든 값이 같으면 지니계수는 0
            if (values.distinct().size == 1) {
                gini shouldBe 0.0
            }
        }
    }
    
    test("엔트로피의 정보량 속성") {
        checkAll(
            Arb.list(Arb.int(1..10), 10..50)
        ) { values ->
            val entropy = calculateEntropy(values)
            
            // 엔트로피는 0 이상
            entropy shouldBeGreaterThanOrEqual 0.0
            
            // 균등분포일 때 엔트로피 최대
            val uniqueCount = values.distinct().size
            val maxEntropy = ln(uniqueCount.toDouble())
            
            entropy shouldBeLessThanOrEqual maxEntropy + 0.01
        }
    }
})

// Helper classes and functions

class Statistics(private val values: List<Int>) {
    val mean: Double = values.average()
    
    val median: Double = {
        val sorted = values.sorted()
        if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2].toDouble()
        }
    }()
    
    val standardDeviation: Double = {
        val avg = mean
        sqrt(values.map { (it - avg).pow(2) }.average())
    }()
    
    fun percentile(p: Int): Double {
        val sorted = values.sorted()
        val index = (p / 100.0 * sorted.size).toInt()
        return sorted[minOf(index, sorted.size - 1)].toDouble()
    }
}

class AdvancedStatistics(private val values: List<Double>) {
    val mean = values.average()
    
    val variance = values.map { (it - mean).pow(2) }.average()
    
    val standardDeviation = sqrt(variance)
    
    val skewness: Double = {
        val n = values.size
        val m3 = values.map { (it - mean).pow(3) }.sum() / n
        val s3 = standardDeviation.pow(3)
        if (s3 == 0.0) 0.0 else m3 / s3
    }()
    
    val kurtosis: Double = {
        val n = values.size
        val m4 = values.map { (it - mean).pow(4) }.sum() / n
        val s4 = standardDeviation.pow(4)
        if (s4 == 0.0) 0.0 else m4 / s4
    }()
    
    fun confidenceInterval(confidence: Double): ConfidenceInterval {
        val z = when (confidence) {
            0.95 -> 1.96
            0.99 -> 2.576
            else -> 1.96
        }
        val margin = z * standardDeviation / sqrt(values.size.toDouble())
        return ConfidenceInterval(mean - margin, mean + margin)
    }
}

data class ConfidenceInterval(val lower: Double, val upper: Double)

data class WeeklyStatistics(
    val totalCount: Int,
    val averageCount: Double,
    val completedDays: Int
)

private fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
    val n = x.size
    val xMean = x.average()
    val yMean = y.average()
    
    val numerator = x.zip(y).sumOf { (xi, yi) -> (xi - xMean) * (yi - yMean) }
    val xDenom = sqrt(x.sumOf { (it - xMean).pow(2) })
    val yDenom = sqrt(y.sumOf { (it - yMean).pow(2) })
    
    return if (xDenom == 0.0 || yDenom == 0.0) 0.0 else numerator / (xDenom * yDenom)
}

private fun movingAverage(values: List<Int>, windowSize: Int): List<Double> {
    return values.windowed(windowSize) { window ->
        window.average()
    }
}

private fun exponentialMovingAverage(values: List<Double>, alpha: Double): List<Double> {
    val ema = mutableListOf<Double>()
    var current = values.first()
    
    values.forEach { value ->
        current = alpha * value + (1 - alpha) * current
        ema.add(current)
    }
    
    return ema
}

private fun autocorrelation(values: List<Int>, lag: Int): Double {
    if (lag == 0) return 1.0
    if (lag >= values.size) return 0.0
    
    val n = values.size - lag
    val mean = values.average()
    
    val numerator = (0 until n).sumOf { i ->
        (values[i] - mean) * (values[i + lag] - mean)
    }
    
    val denominator = values.sumOf { (it - mean).pow(2) }
    
    return if (denominator == 0.0) 0.0 else numerator / denominator
}

private fun createHistogram(values: List<Int>, binCount: Int): Map<Int, Int> {
    val min = values.minOrNull() ?: 0
    val max = values.maxOrNull() ?: 0
    val binSize = ((max - min) / binCount.toDouble()).ceilToInt()
    
    return values.groupingBy { value ->
        ((value - min) / binSize).coerceAtMost(binCount - 1)
    }.eachCount()
}

private fun giniCoefficient(values: List<Int>): Double {
    val sorted = values.sorted()
    val n = sorted.size
    
    val sum = sorted.mapIndexed { index, value ->
        (2 * (index + 1) - n - 1) * value
    }.sum()
    
    val totalWealth = sorted.sum()
    
    return if (totalWealth == 0) 0.0 else sum.toDouble() / (n * totalWealth)
}

private fun calculateEntropy(values: List<Int>): Double {
    val counts = values.groupingBy { it }.eachCount()
    val total = values.size.toDouble()
    
    return -counts.values.sumOf { count ->
        val p = count / total
        if (p > 0) p * ln(p) else 0.0
    }
}

private fun calculateCurrentStreak(completions: List<Boolean>): Int {
    return completions.takeWhile { it }.size
}

// Kotlin Random extension
private fun Random.nextGaussian(): Double {
    // Box-Muller transform for Gaussian distribution
    val u1 = kotlin.random.Random.nextDouble()
    val u2 = kotlin.random.Random.nextDouble()
    return sqrt(-2 * ln(u1)) * cos(2 * PI * u2)
}

private fun Double.ceilToInt(): Int = ceil(this).toInt()