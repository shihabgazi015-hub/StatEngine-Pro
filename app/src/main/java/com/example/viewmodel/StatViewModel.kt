package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.auth.AuthManager
import com.example.db.AppDatabase
import com.example.db.HistoryDao
import com.example.math.StatsEngine
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.sqrt

data class ColumnFilterRange(
    val columnName: String,
    val min: Double,
    val max: Double,
    val selectedMin: Double,
    val selectedMax: Double
)

data class ColumnFilterCategory(
    val columnName: String,
    val allCategories: List<String>,
    val selectedCategories: Set<String>
)

class StatViewModel(private val historyDao: HistoryDao) : ViewModel() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // --- Template Datasets ---
    val templates = listOf(
        createHousingTemplate(),
        createClinicalTemplate(),
        createAcademicTemplate()
    )

    // --- State Variables ---
    private val _unfilteredDataset = MutableStateFlow<Dataset?>(templates[0])
    val unfilteredDataset: StateFlow<Dataset?> = _unfilteredDataset

    private val _comparisonDataset1 = MutableStateFlow<Dataset?>(templates[0])
    val comparisonDataset1: StateFlow<Dataset?> = _comparisonDataset1

    private val _comparisonDataset2 = MutableStateFlow<Dataset?>(templates[1])
    val comparisonDataset2: StateFlow<Dataset?> = _comparisonDataset2

    fun setComparisonDataset1(dataset: Dataset?) {
        _comparisonDataset1.value = dataset
    }

    fun setComparisonDataset2(dataset: Dataset?) {
        _comparisonDataset2.value = dataset
    }

    fun parseAndLoadComparisonFile(context: Context, uri: Uri, targetSlot: Int) {
        viewModelScope.launch {
            _isCleaning.value = true
            try {
                val (fileName, extension) = getFileNameAndExtension(context, uri)
                val dataset = withContext(Dispatchers.IO) {
                    if (extension == "xlsx") {
                        parseXlsxFile(context, uri, fileName)
                    } else {
                        parseCsvFile(context, uri, fileName)
                    }
                }
                if (dataset != null) {
                    if (targetSlot == 1) {
                        _comparisonDataset1.value = dataset
                    } else {
                        _comparisonDataset2.value = dataset
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCleaning.value = false
            }
        }
    }

    val rangeFilters = MutableStateFlow<List<ColumnFilterRange>>(emptyList())
    val categoryFilters = MutableStateFlow<List<ColumnFilterCategory>>(emptyList())

    val selectedDataset: StateFlow<Dataset?> = combine(
        _unfilteredDataset,
        rangeFilters,
        categoryFilters
    ) { unfiltered, ranges, categories ->
        if (unfiltered == null) return@combine null

        val filteredRows = unfiltered.rows.filter { row ->
            val matchesRanges = ranges.all { rf ->
                val cellVal = row[rf.columnName]?.toDoubleOrNull()
                if (cellVal != null) {
                    cellVal in rf.selectedMin..rf.selectedMax
                } else {
                    true
                }
            }

            val matchesCategories = categories.all { cf ->
                val cellVal = row[cf.columnName] ?: ""
                cf.selectedCategories.contains(cellVal)
            }

            matchesRanges && matchesCategories
        }

        unfiltered.copy(rows = filteredRows)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), templates[0])

    private val _activeTab = MutableStateFlow(0) // 0: Ingestion, 1: Settings, 2: Report
    val activeTab: StateFlow<Int> = _activeTab

    // --- Ingestion States ---
    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning

    // Filter Options
    val dropMissing = MutableStateFlow(true)
    val missingValueHandling = MutableStateFlow(MissingValueHandling.DROP_ROWS)
    val filterOutliers = MutableStateFlow(false)

    // Subset Hypothesis Testing Configuration
    val subsetTestType = MutableStateFlow(SubsetTestType.INDEPENDENT_T_TEST)
    val subsetGroupCol = MutableStateFlow<String?>("")
    val subsetGroupA = MutableStateFlow("")
    val subsetGroupB = MutableStateFlow("")
    val subsetDepVar = MutableStateFlow<String?>("")
    val subsetPairedVar1 = MutableStateFlow<String?>("")
    val subsetPairedVar2 = MutableStateFlow<String?>("")

    private val _subsetHypothesisResult = MutableStateFlow<SubsetHypothesisResult?>(null)
    val subsetHypothesisResult: StateFlow<SubsetHypothesisResult?> = _subsetHypothesisResult

    // --- Analysis Configuration ---
    val selectedModel = MutableStateFlow(ModelType.REGRESSION)
    val dependentVariable = MutableStateFlow<String?>("Price")
    val independentVariables = MutableStateFlow<List<String>>(listOf("SqFt", "Bedrooms", "Age"))
    
    // ANOVA Configuration
    val subjectColumn = MutableStateFlow<String?>("SubjectID")
    val betweenSubjectsFactor = MutableStateFlow<String?>("Group")
    val withinSubjectsFactors = MutableStateFlow<List<String>>(listOf("Time_Pre", "Time_Post"))

    // Chi-Square Configuration
    val chiRowVariable = MutableStateFlow<String?>("Department")
    val chiColVariable = MutableStateFlow<String?>("Admitted")

    // Overrides
    val applyRobustErrors = MutableStateFlow(false)
    val runPostHoc = MutableStateFlow(true)

    // --- Calculations & AI states ---
    private val _isComputing = MutableStateFlow(false)
    val isComputing: StateFlow<Boolean> = _isComputing

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport

    private val _regressionResult = MutableStateFlow<MultipleLinearRegressionResult?>(null)
    val regressionResult: StateFlow<MultipleLinearRegressionResult?> = _regressionResult

    private val _anovaResult = MutableStateFlow<MixedAnovaResult?>(null)
    val anovaResult: StateFlow<MixedAnovaResult?> = _anovaResult

    private val _chiSquareResult = MutableStateFlow<ChiSquareResult?>(null)
    val chiSquareResult: StateFlow<ChiSquareResult?> = _chiSquareResult

    val descriptiveStats: StateFlow<List<ColumnDescriptiveStats>> = selectedDataset
        .map { dataset ->
            if (dataset != null) {
                StatsEngine.calculateDescriptiveStats(dataset)
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val correlationMatrix: StateFlow<List<CorrelationItem>> = selectedDataset
        .map { dataset ->
            if (dataset != null) {
                StatsEngine.calculateCorrelationMatrix(dataset)
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedOutliers: StateFlow<List<OutlierItem>> = selectedDataset
        .map { dataset ->
            if (dataset != null) {
                StatsEngine.detectOutliers(dataset)
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedMissingValues: StateFlow<List<MissingValueItem>> = selectedDataset
        .map { dataset ->
            if (dataset != null) {
                val list = mutableListOf<MissingValueItem>()
                for (rowIndex in dataset.rows.indices) {
                    val row = dataset.rows[rowIndex]
                    for (col in dataset.columns) {
                        val value = row[col]
                        if (value.isNullOrBlank() || value == "NaN" || value == "null") {
                            list.add(MissingValueItem(col, rowIndex, value))
                        }
                    }
                }
                list
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _apaReport = MutableStateFlow<String?>(null)
    val apaReport: StateFlow<String?> = _apaReport

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // --- Visual Configuration ---
    val chartPalette = MutableStateFlow("Geometric Balance") // Geometric Balance, Imperial Navy, Crimson Sunset
    val chartFontScale = MutableStateFlow(1.0f)
    val chartXLabelOverride = MutableStateFlow("")
    val chartYLabelOverride = MutableStateFlow("")

    // --- Distribution Plot Configuration ---
    val selectedDistColumn = MutableStateFlow<String?>(null)
    val histogramBins = MutableStateFlow(10)

    // --- History List ---
    val historyItems: StateFlow<List<AnalysisHistoryItem>> = AuthManager.currentUser
        .flatMapLatest { user ->
            if (user == null || user.isGuest) {
                historyDao.getAllHistory()
            } else {
                historyDao.getHistoryForUser(user.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically default variable names and filters when dataset shifts
        viewModelScope.launch {
            _unfilteredDataset.collect { dataset ->
                resetFiltersForDataset(dataset)
                if (dataset != null) {
                    val numericCols = dataset.columns.filter { dataset.columnTypes[it] == ColumnType.NUMERIC }
                    val categCols = dataset.columns.filter { dataset.columnTypes[it] == ColumnType.CATEGORICAL }
                    
                    if (numericCols.isNotEmpty()) {
                        dependentVariable.value = numericCols.first()
                        selectedDistColumn.value = numericCols.first()
                        subsetDepVar.value = numericCols.first()
                        subsetPairedVar1.value = numericCols.first()
                        if (numericCols.size > 1) {
                            independentVariables.value = numericCols.subList(1, numericCols.size)
                            subsetPairedVar2.value = numericCols[1]
                        } else {
                            independentVariables.value = emptyList()
                            subsetPairedVar2.value = numericCols.first()
                        }
                    } else {
                        selectedDistColumn.value = null
                        subsetDepVar.value = null
                        subsetPairedVar1.value = null
                        subsetPairedVar2.value = null
                    }
                    if (categCols.isNotEmpty()) {
                        betweenSubjectsFactor.value = categCols.first()
                        subsetGroupCol.value = categCols.first()
                        if (categCols.size > 1) {
                            chiRowVariable.value = categCols.first()
                            chiColVariable.value = categCols[1]
                        }
                        
                        val cats = dataset.rows.map { it[categCols.first()] ?: "" }.filter { it.isNotBlank() }.distinct().sorted()
                        if (cats.isNotEmpty()) {
                            subsetGroupA.value = cats.first()
                            if (cats.size > 1) {
                                subsetGroupB.value = cats[1]
                            } else {
                                subsetGroupB.value = cats.first()
                            }
                        }
                    } else {
                        subsetGroupCol.value = null
                        subsetGroupA.value = ""
                        subsetGroupB.value = ""
                    }
                    val ids = dataset.columns.filter { it.contains("id", ignoreCase = true) }
                    if (ids.isNotEmpty()) {
                        subjectColumn.value = ids.first()
                    } else if (dataset.columns.isNotEmpty()) {
                        subjectColumn.value = dataset.columns.first()
                    }
                }
            }
        }

        // Automated recalculation of subset hypothesis test on change of configuration
        viewModelScope.launch {
            combine(
                selectedDataset,
                subsetTestType,
                subsetGroupCol,
                subsetGroupA,
                subsetGroupB,
                subsetDepVar,
                subsetPairedVar1,
                subsetPairedVar2
            ) { _ -> }.collect {
                runSubsetHypothesisTest()
            }
        }
    }

    fun resetFiltersForDataset(dataset: Dataset?) {
        if (dataset == null) {
            rangeFilters.value = emptyList()
            categoryFilters.value = emptyList()
            return
        }

        val newRanges = mutableListOf<ColumnFilterRange>()
        val newCategories = mutableListOf<ColumnFilterCategory>()

        dataset.columns.forEach { colName ->
            val type = dataset.columnTypes[colName]
            if (type == ColumnType.NUMERIC) {
                val values = dataset.rows.mapNotNull { it[colName]?.toDoubleOrNull() }
                if (values.isNotEmpty()) {
                    val minVal = values.minOrNull() ?: 0.0
                    val maxVal = values.maxOrNull() ?: 0.0
                    newRanges.add(
                        ColumnFilterRange(
                            columnName = colName,
                            min = minVal,
                            max = maxVal,
                            selectedMin = minVal,
                            selectedMax = maxVal
                        )
                    )
                }
            } else if (type == ColumnType.CATEGORICAL) {
                val values = dataset.rows.map { it[colName] ?: "" }.filter { it.isNotBlank() }.distinct().sorted()
                newCategories.add(
                    ColumnFilterCategory(
                        columnName = colName,
                        allCategories = values,
                        selectedCategories = values.toSet()
                    )
                )
            }
        }

        rangeFilters.value = newRanges
        categoryFilters.value = newCategories
    }

    fun updateRangeFilter(columnName: String, selectedMin: Double, selectedMax: Double) {
        rangeFilters.value = rangeFilters.value.map {
            if (it.columnName == columnName) {
                it.copy(selectedMin = selectedMin, selectedMax = selectedMax)
            } else {
                it
            }
        }
    }

    fun updateCategoryFilter(columnName: String, selectedCategories: Set<String>) {
        categoryFilters.value = categoryFilters.value.map {
            if (it.columnName == columnName) {
                it.copy(selectedCategories = selectedCategories)
            } else {
                it
            }
        }
    }

    fun resetAllFilters() {
        resetFiltersForDataset(_unfilteredDataset.value)
    }

    fun runSubsetHypothesisTest() {
        val dataset = selectedDataset.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val result = when (subsetTestType.value) {
                    SubsetTestType.INDEPENDENT_T_TEST -> {
                        StatsEngine.runIndependentTTest(
                            dataset = dataset,
                            groupCol = subsetGroupCol.value ?: "",
                            groupA = subsetGroupA.value,
                            groupB = subsetGroupB.value,
                            depVar = subsetDepVar.value ?: ""
                        )
                    }
                    SubsetTestType.PAIRED_T_TEST -> {
                        StatsEngine.runPairedTTest(
                            dataset = dataset,
                            var1 = subsetPairedVar1.value ?: "",
                            var2 = subsetPairedVar2.value ?: ""
                        )
                    }
                    SubsetTestType.ONE_WAY_ANOVA -> {
                        StatsEngine.runSubsetOneWayAnova(
                            dataset = dataset,
                            groupCol = subsetGroupCol.value ?: "",
                            depVar = subsetDepVar.value ?: ""
                        )
                    }
                }
                _subsetHypothesisResult.value = result
            }
        }
    }

    fun setTab(index: Int) {
        _activeTab.value = index
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun loadTemplate(dataset: Dataset) {
        _unfilteredDataset.value = dataset
        clearResults()
    }

    private fun clearResults() {
        _regressionResult.value = null
        _anovaResult.value = null
        _chiSquareResult.value = null
        _subsetHypothesisResult.value = null
        _apaReport.value = null
    }

    // --- File Ingestion ---
    fun parseAndLoadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isCleaning.value = true
            try {
                val (fileName, extension) = getFileNameAndExtension(context, uri)
                val dataset = withContext(Dispatchers.IO) {
                    if (extension == "xlsx") {
                        parseXlsxFile(context, uri, fileName)
                    } else {
                        parseCsvFile(context, uri, fileName)
                    }
                }
                if (dataset != null) {
                    _unfilteredDataset.value = dataset
                    clearResults()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCleaning.value = false
            }
        }
    }

    private fun getFileNameAndExtension(context: Context, uri: Uri): Pair<String, String> {
        var name = "dataset"
        var ext = "csv"
        try {
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = it.getString(nameIndex)
                            if (displayName != null) {
                                name = displayName
                                val dotIdx = displayName.lastIndexOf('.')
                                if (dotIdx != -1) {
                                    ext = displayName.substring(dotIdx + 1).lowercase()
                                }
                            }
                        }
                    }
                }
            } else {
                val path = uri.path
                if (path != null) {
                    val file = java.io.File(path)
                    name = file.name
                    val dotIdx = name.lastIndexOf('.')
                    if (dotIdx != -1) {
                        ext = name.substring(dotIdx + 1).lowercase()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, ext)
    }

    private fun parseCsvFile(context: Context, uri: Uri, fileName: String): Dataset? {
        val contentResolver = context.contentResolver
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            if (lines.isEmpty()) return@use null

            val firstLine = lines[0]
            val delimiter = detectDelimiter(firstLine)
            val rawHeaders = parseCsvLine(firstLine, delimiter)
            
            // Ensure unique headers
            val seen = HashSet<String>()
            val headers = ArrayList<String>()
            for (h in rawHeaders) {
                var uniqueH = h
                var count = 1
                while (seen.contains(uniqueH)) {
                    uniqueH = "${h}_$count"
                    count++
                }
                seen.add(uniqueH)
                headers.add(uniqueH)
            }

            val rows = ArrayList<Map<String, String>>()
            
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue
                val parts = parseCsvLine(line, delimiter)
                val rowMap = HashMap<String, String>()
                for (j in headers.indices) {
                    if (j < parts.size) {
                        rowMap[headers[j]] = parts[j]
                    } else {
                        rowMap[headers[j]] = ""
                    }
                }
                rows.add(rowMap)
            }

            // Deduce column types
            val colTypes = HashMap<String, ColumnType>()
            for (col in headers) {
                var numericCount = 0
                var validCount = 0
                for (row in rows) {
                    val valStr = row[col]
                    if (!valStr.isNullOrBlank()) {
                        validCount++
                        if (valStr.toDoubleOrNull() != null) {
                            numericCount++
                        }
                    }
                }
                if (validCount > 0 && numericCount.toDouble() / validCount > 0.8) {
                    colTypes[col] = ColumnType.NUMERIC
                } else {
                    colTypes[col] = ColumnType.CATEGORICAL
                }
            }

            Dataset(
                name = fileName,
                columns = headers,
                columnTypes = colTypes,
                rows = rows
            )
        }
    }

    private fun parseXlsxFile(context: Context, uri: Uri, fileName: String): Dataset? {
        val contentResolver = context.contentResolver
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            try {
                val zipInputStream = java.util.zip.ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                val sharedStrings = ArrayList<String>()
                var sheetBytes: ByteArray? = null
                var sharedStringsBytes: ByteArray? = null

                while (entry != null) {
                    val name = entry.name
                    if (name.endsWith("sharedStrings.xml")) {
                        sharedStringsBytes = zipInputStream.readBytes()
                    } else if (name.endsWith("sheet1.xml")) {
                        sheetBytes = zipInputStream.readBytes()
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }

                // Parse Shared Strings
                if (sharedStringsBytes != null) {
                    val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(java.io.ByteArrayInputStream(sharedStringsBytes), "UTF-8")
                    var eventType = parser.eventType
                    val currentText = StringBuilder()
                    while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                            if (parser.name == "t") {
                                currentText.setLength(0)
                                eventType = parser.next()
                                while (eventType == org.xmlpull.v1.XmlPullParser.TEXT) {
                                    currentText.append(parser.text)
                                    eventType = parser.next()
                                }
                                sharedStrings.add(currentText.toString())
                            }
                        }
                        eventType = parser.next()
                    }
                }

                // Parse Sheet Rows and Cells
                if (sheetBytes != null) {
                    val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(java.io.ByteArrayInputStream(sheetBytes), "UTF-8")
                    
                    val rows = ArrayList<Map<String, String>>()
                    var currentHeaderNames = ArrayList<String>()
                    
                    var eventType = parser.eventType
                    var isCellValue = false
                    var isCell = false
                    var cellType: String? = null
                    var cellRef: String? = null
                    val cellValue = StringBuilder()

                    var maxColIndex = 0
                    val rowCells = HashMap<Int, String>()
                    
                    while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                            val tagName = parser.name
                            if (tagName == "row") {
                                rowCells.clear()
                            } else if (tagName == "c") {
                                isCell = true
                                cellType = parser.getAttributeValue(null, "t")
                                cellRef = parser.getAttributeValue(null, "r")
                                cellValue.setLength(0)
                            } else if (tagName == "v") {
                                isCellValue = true
                            }
                        } else if (eventType == org.xmlpull.v1.XmlPullParser.TEXT) {
                            if (isCellValue) {
                                cellValue.append(parser.text)
                            }
                        } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                            val tagName = parser.name
                            if (tagName == "v") {
                                isCellValue = false
                            } else if (tagName == "c") {
                                isCell = false
                                val colIndex = cellRefToColIndex(cellRef)
                                if (colIndex >= 0) {
                                    var finalVal = cellValue.toString().trim()
                                    if (cellType == "s") {
                                        val sIndex = finalVal.toIntOrNull()
                                        if (sIndex != null && sIndex in sharedStrings.indices) {
                                            finalVal = sharedStrings[sIndex]
                                        }
                                    }
                                    rowCells[colIndex] = finalVal
                                    if (colIndex > maxColIndex) {
                                        maxColIndex = colIndex
                                    }
                                }
                            } else if (tagName == "row") {
                                if (currentHeaderNames.isEmpty()) {
                                    for (c in 0..maxColIndex) {
                                        val h = rowCells[c]?.takeIf { it.isNotBlank() } ?: "Col${c + 1}"
                                        currentHeaderNames.add(h)
                                    }
                                    // Make headers unique
                                    val seen = HashSet<String>()
                                    val uniqueHeaders = ArrayList<String>()
                                    for (header in currentHeaderNames) {
                                        var h = header
                                        var count = 1
                                        while (seen.contains(h)) {
                                            h = "${header}_$count"
                                            count++
                                        }
                                        seen.add(h)
                                        uniqueHeaders.add(h)
                                    }
                                    currentHeaderNames = uniqueHeaders
                                } else {
                                    val rowMap = HashMap<String, String>()
                                    var hasAnyData = false
                                    for (c in currentHeaderNames.indices) {
                                        val value = rowCells[c] ?: ""
                                        if (value.isNotBlank()) {
                                            hasAnyData = true
                                        }
                                        rowMap[currentHeaderNames[c]] = value
                                    }
                                    if (hasAnyData) {
                                        rows.add(rowMap)
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }

                    // Deduce column types
                    val colTypes = HashMap<String, ColumnType>()
                    for (col in currentHeaderNames) {
                        var numericCount = 0
                        var validCount = 0
                        for (row in rows) {
                            val valStr = row[col]
                            if (!valStr.isNullOrBlank()) {
                                validCount++
                                if (valStr.toDoubleOrNull() != null) {
                                    numericCount++
                                }
                            }
                        }
                        if (validCount > 0 && numericCount.toDouble() / validCount > 0.8) {
                            colTypes[col] = ColumnType.NUMERIC
                        } else {
                            colTypes[col] = ColumnType.CATEGORICAL
                        }
                    }

                    return Dataset(
                        name = fileName,
                        columns = currentHeaderNames,
                        columnTypes = colTypes,
                        rows = rows
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

    internal fun cellRefToColIndex(ref: String?): Int {
        if (ref == null) return -1
        var index = 0
        for (char in ref) {
            if (char in 'A'..'Z') {
                index = index * 26 + (char - 'A' + 1)
            } else {
                break
            }
        }
        return index - 1
    }

    internal fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == delimiter && !inQuotes) {
                result.add(current.toString().trim().replace("\"", ""))
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim().replace("\"", ""))
        return result
    }

    internal fun detectDelimiter(firstLine: String): Char {
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        val tabs = firstLine.count { it == '\t' }
        return when {
            semicolons > commas && semicolons > tabs -> ';'
            tabs > commas && tabs > semicolons -> '\t'
            else -> ','
        }
    }

    // --- Data Cleaning ---
    fun cleanActiveDataset() {
        val current = _unfilteredDataset.value ?: return
        viewModelScope.launch {
            _isCleaning.value = true
            val cleaned = withContext(Dispatchers.IO) {
                var processedRows = current.rows

                // 1. Handle Missing Values
                when (missingValueHandling.value) {
                    MissingValueHandling.DROP_ROWS -> {
                        processedRows = processedRows.filter { row ->
                            current.columns.all { col ->
                                val value = row[col]
                                !value.isNullOrBlank() && value != "NaN" && value != "null"
                            }
                        }
                    }
                    MissingValueHandling.IMPUTE_MEAN -> {
                        val means = current.columns.associateWith { col ->
                            val vals = current.rows.mapNotNull { it[col]?.toDoubleOrNull() }
                            if (vals.isNotEmpty()) vals.average() else 0.0
                        }
                        processedRows = processedRows.map { row ->
                            val updatedMap = row.toMutableMap()
                            current.columns.forEach { col ->
                                val value = row[col]
                                if (value.isNullOrBlank() || value == "NaN" || value == "null") {
                                    if (current.columnTypes[col] == ColumnType.NUMERIC) {
                                        updatedMap[col] = String.format("%.4f", means[col] ?: 0.0)
                                    } else {
                                        updatedMap[col] = "Missing"
                                    }
                                }
                            }
                            updatedMap
                        }
                    }
                    MissingValueHandling.IMPUTE_MEDIAN -> {
                        val medians = current.columns.associateWith { col ->
                            val vals = current.rows.mapNotNull { it[col]?.toDoubleOrNull() }.sorted()
                            if (vals.isNotEmpty()) {
                                if (vals.size % 2 == 1) {
                                    vals[vals.size / 2]
                                } else {
                                    (vals[vals.size / 2 - 1] + vals[vals.size / 2]) / 2.0
                                }
                            } else 0.0
                        }
                        processedRows = processedRows.map { row ->
                            val updatedMap = row.toMutableMap()
                            current.columns.forEach { col ->
                                val value = row[col]
                                if (value.isNullOrBlank() || value == "NaN" || value == "null") {
                                    if (current.columnTypes[col] == ColumnType.NUMERIC) {
                                        updatedMap[col] = String.format("%.4f", medians[col] ?: 0.0)
                                    } else {
                                        updatedMap[col] = "Missing"
                                    }
                                }
                            }
                            updatedMap
                        }
                    }
                }

                // 2. Filter Statistical Outliers (Z-score > ±3 on numeric columns)
                if (filterOutliers.value) {
                    val numericCols = current.columns.filter { current.columnTypes[it] == ColumnType.NUMERIC }
                    for (col in numericCols) {
                        val vals = processedRows.mapNotNull { it[col]?.toDoubleOrNull() }
                        if (vals.size > 2) {
                            val mean = vals.average()
                            val stdDev = sqrt(vals.map { (it - mean).pow2() }.sum() / (vals.size - 1))
                            if (stdDev > 0) {
                                processedRows = processedRows.filter { row ->
                                    val valNum = row[col]?.toDoubleOrNull() ?: return@filter true
                                    val z = (valNum - mean) / stdDev
                                    abs(z) <= 3.0
                                }
                            }
                        }
                    }
                }

                Dataset(
                    name = "${current.name} (Cleaned)",
                    columns = current.columns,
                    columnTypes = current.columnTypes,
                    rows = processedRows
                )
            }
            _unfilteredDataset.value = cleaned
            _isCleaning.value = false
        }
    }

    private fun Double.pow2() = this * this

    // --- Core Computations ---
    fun runStatisticalModeling() {
        val dataset = selectedDataset.value ?: return
        viewModelScope.launch {
            _isComputing.value = true
            _apaReport.value = null

            withContext(Dispatchers.Default) {
                when (selectedModel.value) {
                    ModelType.REGRESSION -> {
                        val dep = dependentVariable.value ?: ""
                        val inds = independentVariables.value
                        val result = StatsEngine.runRegression(
                            dataset = dataset,
                            depVar = dep,
                            indVars = inds,
                            applyRobust = applyRobustErrors.value
                        )
                        _regressionResult.value = result
                        _activeTab.value = 2 // Shift to Report tab immediately
                    }
                    ModelType.ANOVA -> {
                        val sub = subjectColumn.value ?: ""
                        val bFactor = betweenSubjectsFactor.value ?: ""
                        val wFactors = withinSubjectsFactors.value
                        val result = StatsEngine.runMixedAnova(
                            dataset = dataset,
                            subjectCol = sub,
                            betweenCol = bFactor,
                            withinCols = wFactors,
                            runPostHoc = runPostHoc.value
                        )
                        _anovaResult.value = result
                        _activeTab.value = 2
                    }
                    ModelType.CHI_SQUARE -> {
                        val rowVar = chiRowVariable.value ?: ""
                        val colVar = chiColVariable.value ?: ""
                        val result = StatsEngine.runChiSquare(
                            dataset = dataset,
                            rowVar = rowVar,
                            colVar = colVar
                        )
                        _chiSquareResult.value = result
                        _activeTab.value = 2
                    }
                }
            }
            _isComputing.value = false

            // Automatically trigger Gemini report once computed!
            generateAiWriteUp()
        }
    }

    private fun generateAiWriteUp() {
        val dataset = selectedDataset.value ?: return
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val resultsJson = when (selectedModel.value) {
                ModelType.REGRESSION -> {
                    val res = _regressionResult.value ?: return@launch
                    val adapter = moshi.adapter(MultipleLinearRegressionResult::class.java)
                    adapter.toJson(res)
                }
                ModelType.ANOVA -> {
                    val res = _anovaResult.value ?: return@launch
                    val adapter = moshi.adapter(MixedAnovaResult::class.java)
                    adapter.toJson(res)
                }
                ModelType.CHI_SQUARE -> {
                    val res = _chiSquareResult.value ?: return@launch
                    val adapter = moshi.adapter(ChiSquareResult::class.java)
                    adapter.toJson(res)
                }
            }

            val systemPrompt = """
                You are a Senior Academic Methodologist. Your objective is to generate formal APA 7th Edition write-ups based strictly on the provided JSON statistical package. You must interpret the diagnostic metrics (Shapiro-Wilk, Levene's) to provide expert commentary on data integrity. If assumptions are violated, you must articulate the necessary corrective steps taken (e.g., shifting to robust standard errors or non-parametric alternatives). Report all test statistics using standard academic formatting notations exactly as dictated by the APA manual. Do not alter, round, approximate, or hallucinate any mathematical values provided in the JSON payload under any circumstances.
            """.trimIndent()

            val text = GeminiApiClient.getApaWriteUp(systemPrompt, resultsJson)
            _apaReport.value = text
            _isGeneratingReport.value = false

            // Save to database
            saveSessionToHistory(dataset, resultsJson, text)
        }
    }

    private suspend fun saveSessionToHistory(dataset: Dataset, resultsJson: String, writeUp: String) {
        val activeUser = AuthManager.currentUser.value
        val userId = activeUser?.uid ?: ""
        
        val datasetAdapter = moshi.adapter(Dataset::class.java)
        val datasetJson = datasetAdapter.toJson(dataset)

        val settingsMap = mapOf(
            "modelType" to selectedModel.value.name,
            "depVar" to dependentVariable.value,
            "indVars" to independentVariables.value,
            "subCol" to subjectColumn.value,
            "betweenCol" to betweenSubjectsFactor.value,
            "withinCols" to withinSubjectsFactors.value,
            "rowVar" to chiRowVariable.value,
            "colVar" to chiColVariable.value,
            "applyRobust" to applyRobustErrors.value,
            "runPostHoc" to runPostHoc.value
        )
        val settingsJson = moshi.adapter(Map::class.java).toJson(settingsMap)

        val historyItem = AnalysisHistoryItem(
            title = "${dataset.name} - ${selectedModel.value.name.replace("_", " ")}",
            datasetJson = datasetJson,
            modelType = selectedModel.value.name,
            settingsJson = settingsJson,
            resultsJson = resultsJson,
            reportText = writeUp,
            userId = userId
        )

        val insertedId = historyDao.insertHistory(historyItem)
        if (activeUser != null && !activeUser.isGuest) {
            val fullItem = historyItem.copy(id = insertedId.toInt())
            AuthManager.syncToFirestore(fullItem)
        }
    }

    fun deleteHistory(item: AnalysisHistoryItem) {
        viewModelScope.launch {
            historyDao.deleteHistory(item)
        }
    }

    // --- Creation of Preconfigured Datasets ---
    private fun createHousingTemplate(): Dataset {
        val columns = listOf("Price", "SqFt", "Bedrooms", "Age", "CrimeRate")
        val colTypes = columns.associateWith { ColumnType.NUMERIC }
        val rawData = listOf(
            listOf("350000", "1800", "3", "12", "0.02"),
            listOf("420000", "2200", "4", "5", "0.01"),
            listOf("290000", "1500", "2", "20", "0.04"),
            listOf("480000", "2500", "4", "3", "0.01"),
            listOf("380000", "2000", "3", "8", "0.02"),
            listOf("520000", "2800", "5", "2", "0.01"),
            listOf("310000", "1650", "3", "18", "0.03"),
            listOf("450000", "2400", "4", "4", "0.01"),
            listOf("260000", "1300", "2", "25", "0.05"),
            listOf("410000", "2100", "3", "10", "0.02")
        )
        val rows = rawData.map { rowList ->
            columns.zip(rowList).toMap()
        }
        return Dataset("Housing Market Template", columns, colTypes, rows)
    }

    private fun createClinicalTemplate(): Dataset {
        val columns = listOf("SubjectID", "Group", "Time_Pre", "Time_Post")
        val colTypes = mapOf(
            "SubjectID" to ColumnType.CATEGORICAL,
            "Group" to ColumnType.CATEGORICAL,
            "Time_Pre" to ColumnType.NUMERIC,
            "Time_Post" to ColumnType.NUMERIC
        )
        val rawData = listOf(
            listOf("S01", "Treatment", "85", "45"),
            listOf("S02", "Treatment", "92", "50"),
            listOf("S03", "Treatment", "78", "42"),
            listOf("S04", "Treatment", "88", "48"),
            listOf("S05", "Treatment", "90", "52"),
            listOf("S06", "Control", "82", "80"),
            listOf("S07", "Control", "89", "85"),
            listOf("S08", "Control", "79", "78"),
            listOf("S09", "Control", "91", "88"),
            listOf("S10", "Control", "86", "84")
        )
        val rows = rawData.map { rowList ->
            columns.zip(rowList).toMap()
        }
        return Dataset("Clinical Anxiety Trial", columns, colTypes, rows)
    }

    private fun createAcademicTemplate(): Dataset {
        val columns = listOf("StudentID", "Department", "Admitted")
        val colTypes = mapOf(
            "StudentID" to ColumnType.CATEGORICAL,
            "Department" to ColumnType.CATEGORICAL,
            "Admitted" to ColumnType.CATEGORICAL
        )
        val rawData = listOf(
            listOf("ST01", "Engineering", "Yes"),
            listOf("ST02", "Engineering", "Yes"),
            listOf("ST03", "Engineering", "No"),
            listOf("ST04", "Engineering", "Yes"),
            listOf("ST05", "Humanities", "Yes"),
            listOf("ST06", "Humanities", "No"),
            listOf("ST07", "Humanities", "No"),
            listOf("ST08", "Business", "Yes"),
            listOf("ST09", "Business", "No"),
            listOf("ST10", "Business", "Yes"),
            listOf("ST11", "Engineering", "Yes"),
            listOf("ST12", "Humanities", "No")
        )
        val rows = rawData.map { rowList ->
            columns.zip(rowList).toMap()
        }
        return Dataset("Admissions Diversity Data", columns, colTypes, rows)
    }
}

class StatViewModelFactory(private val historyDao: HistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatViewModel(historyDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
