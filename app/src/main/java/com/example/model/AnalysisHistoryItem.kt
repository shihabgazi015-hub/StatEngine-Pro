package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "analysis_history")
@JsonClass(generateAdapter = true)
data class AnalysisHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val datasetJson: String, // Dataset serialized
    val modelType: String,   // REGRESSION, ANOVA, CHI_SQUARE
    val settingsJson: String, // Inputs: depVar, indVars, etc.
    val resultsJson: String,  // Strict structured JSON analytical results
    val reportText: String,   // Gemini generated APA Write-up
    val isSynced: Boolean = false,
    val userId: String = ""   // Associated authenticated user ID
)
