package com.example.db

import androidx.room.*
import com.example.model.AnalysisHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AnalysisHistoryItem>>

    @Query("SELECT * FROM analysis_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHistoryForUser(userId: String): Flow<List<AnalysisHistoryItem>>

    @Query("SELECT * FROM analysis_history WHERE id = :id")
    suspend fun getHistoryById(id: Int): AnalysisHistoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: AnalysisHistoryItem): Long

    @Delete
    suspend fun deleteHistory(item: AnalysisHistoryItem)

    @Query("DELETE FROM analysis_history")
    suspend fun clearAll()

    @Query("SELECT * FROM analysis_history WHERE isSynced = 0")
    suspend fun getUnsyncedHistory(): List<AnalysisHistoryItem>
}
