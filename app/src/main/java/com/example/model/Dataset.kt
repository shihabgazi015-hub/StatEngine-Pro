package com.example.model

import com.squareup.moshi.JsonClass

enum class ColumnType {
    NUMERIC,
    CATEGORICAL
}

@JsonClass(generateAdapter = true)
data class Dataset(
    val name: String,
    val columns: List<String>,
    val columnTypes: Map<String, ColumnType>,
    val rows: List<Map<String, String>>
) {
    fun getNumericValues(column: String): List<Double> {
        return rows.mapNotNull { it[column]?.toDoubleOrNull() }
    }

    fun getCategoricalValues(column: String): List<String> {
        return rows.map { it[column] ?: "" }
    }
}
