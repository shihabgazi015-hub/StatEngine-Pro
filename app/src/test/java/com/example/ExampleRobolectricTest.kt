package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.StatViewModel
import com.example.db.HistoryDao
import com.example.model.AnalysisHistoryItem
import com.example.math.StatsEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private val mockHistoryDao = object : HistoryDao {
    override fun getAllHistory(): Flow<List<AnalysisHistoryItem>> = flowOf(emptyList())
    override fun getHistoryForUser(userId: String): Flow<List<AnalysisHistoryItem>> = flowOf(emptyList())
    override suspend fun getHistoryById(id: Int): AnalysisHistoryItem? = null
    override suspend fun insertHistory(item: AnalysisHistoryItem): Long = 0L
    override suspend fun deleteHistory(item: AnalysisHistoryItem) {}
    override suspend fun clearAll() {}
    override suspend fun getUnsyncedHistory(): List<AnalysisHistoryItem> = emptyList()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("StatEngine Pro", appName)
  }

  @Test
  fun testCellRefToColIndex() {
    val viewModel = StatViewModel(mockHistoryDao)
    assertEquals(0, viewModel.cellRefToColIndex("A1"))
    assertEquals(1, viewModel.cellRefToColIndex("B12"))
    assertEquals(25, viewModel.cellRefToColIndex("Z100"))
    assertEquals(26, viewModel.cellRefToColIndex("AA1"))
    assertEquals(-1, viewModel.cellRefToColIndex(null))
  }

  @Test
  fun testParseCsvLineWithQuotes() {
    val viewModel = StatViewModel(mockHistoryDao)
    val line = "1,\"Doe, John\",25"
    val parsed = viewModel.parseCsvLine(line, ',')
    assertEquals(3, parsed.size)
    assertEquals("1", parsed[0])
    assertEquals("Doe, John", parsed[1])
    assertEquals("25", parsed[2])
  }

  @Test
  fun testDetectDelimiter() {
    val viewModel = StatViewModel(mockHistoryDao)
    assertEquals(',', viewModel.detectDelimiter("header1,header2,header3"))
    assertEquals(';', viewModel.detectDelimiter("header1;header2;header3"))
    assertEquals('\t', viewModel.detectDelimiter("header1\theader2\theader3"))
  }

  @Test
  fun testCalculateDescriptiveStats() {
    val dataset = com.example.model.Dataset(
      name = "Test",
      columns = listOf("col1", "col2"),
      columnTypes = mapOf("col1" to com.example.model.ColumnType.NUMERIC, "col2" to com.example.model.ColumnType.CATEGORICAL),
      rows = listOf(
        mapOf("col1" to "10.0", "col2" to "A"),
        mapOf("col1" to "20.0", "col2" to "B"),
        mapOf("col1" to "30.0", "col2" to "A"),
        mapOf("col1" to "", "col2" to "")
      )
    )
    val stats = StatsEngine.calculateDescriptiveStats(dataset)
    assertEquals(2, stats.size)
    
    val stat1 = stats[0]
    assertEquals("col1", stat1.columnName)
    assertEquals(com.example.model.ColumnType.NUMERIC, stat1.columnType)
    assertEquals(20.0, stat1.mean!!, 1e-5)
    assertEquals(20.0, stat1.median!!, 1e-5)
    assertEquals(10.0, stat1.stdDev!!, 1e-5)
    assertEquals(10.0, stat1.min!!, 1e-5)
    assertEquals(30.0, stat1.max!!, 1e-5)
    assertEquals(3, stat1.count)
    assertEquals(1, stat1.missingCount)
    
    val stat2 = stats[1]
    assertEquals("col2", stat2.columnName)
    assertEquals(com.example.model.ColumnType.CATEGORICAL, stat2.columnType)
    assertEquals(null, stat2.mean)
    assertEquals(3, stat2.count)
    assertEquals(1, stat2.missingCount)
  }
}
