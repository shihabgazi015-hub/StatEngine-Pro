package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegressionCoefficient(
    val variable: String,
    val estimate: Double,
    val stdError: Double,
    val tValue: Double,
    val pValue: Double,
    val vif: Double? = null
)

@JsonClass(generateAdapter = true)
data class MultipleLinearRegressionResult(
    val coefficients: List<RegressionCoefficient>,
    val rSquared: Double,
    val adjustedRSquared: Double,
    val fStatistic: Double,
    val dfRegression: Int,
    val dfResidual: Int,
    val fPValue: Double,
    val shapiroWilkStat: Double,
    val shapiroWilkW: Double, // p-value of residuals normality
    val leveneStat: Double,
    val levenePValue: Double, // homoscedasticity p-value
    val residuals: List<Double>,
    val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class AnovaEffect(
    val source: String,
    val sumOfSquares: Double,
    val df: Int,
    val meanSquare: Double,
    val fStatistic: Double,
    val pValue: Double,
    val partialEtaSquared: Double
)

@JsonClass(generateAdapter = true)
data class MixedAnovaResult(
    val effects: List<AnovaEffect>,
    val leveneStat: Double,
    val levenePValue: Double,
    val mauchlyStat: Double,
    val mauchlyPValue: Double,
    val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class ChiSquareCell(
    val rowValue: String,
    val colValue: String,
    val observed: Int,
    val expected: Double
)

@JsonClass(generateAdapter = true)
data class ChiSquareResult(
    val cells: List<ChiSquareCell>,
    val chiSquareStat: Double,
    val df: Int,
    val pValue: Double,
    val cramersV: Double,
    val rowCategories: List<String>,
    val colCategories: List<String>,
    val contingencyTable: Map<String, Map<String, Int>>,
    val errorMessage: String? = null
)

enum class ModelType {
    REGRESSION,
    ANOVA,
    CHI_SQUARE
}

@JsonClass(generateAdapter = true)
data class ColumnDescriptiveStats(
    val columnName: String,
    val columnType: ColumnType,
    val mean: Double?,
    val median: Double?,
    val stdDev: Double?,
    val min: Double?,
    val max: Double?,
    val count: Int,
    val missingCount: Int
)

@JsonClass(generateAdapter = true)
data class CorrelationItem(
    val columnA: String,
    val columnB: String,
    val coefficient: Double?
)

@JsonClass(generateAdapter = true)
data class OutlierItem(
    val columnName: String,
    val rowIndex: Int,
    val value: Double,
    val lowerFence: Double,
    val upperFence: Double
)

@JsonClass(generateAdapter = true)
data class MissingValueItem(
    val columnName: String,
    val rowIndex: Int,
    val value: String?
)

enum class MissingValueHandling {
    DROP_ROWS,
    IMPUTE_MEAN,
    IMPUTE_MEDIAN
}

enum class SubsetTestType {
    INDEPENDENT_T_TEST,
    PAIRED_T_TEST,
    ONE_WAY_ANOVA
}

@JsonClass(generateAdapter = true)
data class SubsetGroupStats(
    val groupName: String,
    val count: Int,
    val mean: Double,
    val stdDev: Double,
    val sem: Double
)

@JsonClass(generateAdapter = true)
data class SubsetHypothesisResult(
    val testType: SubsetTestType,
    val statistic: Double,
    val df: Double,
    val pValue: Double,
    val confidenceIntervalLower: Double?,
    val confidenceIntervalUpper: Double?,
    val meanDifference: Double?,
    val groupsStats: List<SubsetGroupStats>,
    val explanation: String,
    val errorMessage: String? = null
)

