package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.content.FileProvider

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.viewmodel.StatViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportView(
    viewModel: StatViewModel,
    onTriggerHelp: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedDataset by viewModel.selectedDataset.collectAsStateWithLifecycle()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()

    val regressionResult by viewModel.regressionResult.collectAsStateWithLifecycle()
    val anovaResult by viewModel.anovaResult.collectAsStateWithLifecycle()
    val chiSquareResult by viewModel.chiSquareResult.collectAsStateWithLifecycle()
    val apaReport by viewModel.apaReport.collectAsStateWithLifecycle()

    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()

    // Visual configurations
    val chartPalette by viewModel.chartPalette.collectAsStateWithLifecycle()
    val chartFontScale by viewModel.chartFontScale.collectAsStateWithLifecycle()
    val chartXLabelOverride by viewModel.chartXLabelOverride.collectAsStateWithLifecycle()
    val chartYLabelOverride by viewModel.chartYLabelOverride.collectAsStateWithLifecycle()

    val selectedDistColumn by viewModel.selectedDistColumn.collectAsStateWithLifecycle()
    val histogramBins by viewModel.histogramBins.collectAsStateWithLifecycle()
    val correlationMatrix by viewModel.correlationMatrix.collectAsStateWithLifecycle()

    val independentVars by viewModel.independentVariables.collectAsStateWithLifecycle()
    var selectedScatterIndependentVar by remember(independentVars) {
        mutableStateOf(independentVars.firstOrNull())
    }


    val paletteColors = when (chartPalette) {
        "Imperial Navy" -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF93C5FD))
        "Crimson Sunset" -> listOf(Color(0xFF991B1B), Color(0xFFEF4444), Color(0xFFFCA5A5))
        else -> listOf(Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF)) // Geometric Balance (Default)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("report_view"),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Column Distribution Plots Section (Available as soon as dataset is loaded)
        selectedDataset?.let { dataset ->
            val numericCols = dataset.columns.filter { dataset.columnTypes[it] == ColumnType.NUMERIC }
            if (numericCols.isNotEmpty()) {
                item {
                    ColumnDistributionSection(
                        dataset = dataset,
                        numericCols = numericCols,
                        selectedDistColumn = selectedDistColumn ?: numericCols.first(),
                        histogramBins = histogramBins,
                        onColumnSelected = { viewModel.selectedDistColumn.value = it },
                        onBinsChanged = { viewModel.histogramBins.value = it },
                        paletteColors = paletteColors,
                        fontScale = chartFontScale
                    )
                }

                item {
                    CorrelationHeatmapSection(
                        numericCols = numericCols,
                        correlationItems = correlationMatrix,
                        paletteColors = paletteColors,
                        fontScale = chartFontScale
                    )
                }
            }
        }


        // Validation: Ensure computations are run first
        val hasResults = (selectedModel == ModelType.REGRESSION && regressionResult != null) ||
                (selectedModel == ModelType.ANOVA && anovaResult != null) ||
                (selectedModel == ModelType.CHI_SQUARE && chiSquareResult != null)

        if (!hasResults) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Laboratory Report Empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please configure your parameters and click 'Run Diagnostics' in the 'Analyze' tab to compute statistical metrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Interactive help tooltips block
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Interactive Academic Help Lexicon",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap on any complex methodological term below to inspect standard academic definitions and interpretations:",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("VIF", "p-value", "Levene's test", "Homoscedasticity").forEach { term ->
                                AssistChip(
                                    onClick = { onTriggerHelp(term) },
                                    label = { Text(term) },
                                    leadingIcon = { Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    modifier = Modifier.testTag("help_chip_$term")
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 1: DYNAMIC CANVAS VECTOR PLOTS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Publication-Ready Vector Visualizations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedModel == ModelType.REGRESSION && independentVars.size > 1) {
                            var xExpanded by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Plot predictor:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box {
                                    OutlinedButton(
                                        onClick = { xExpanded = true },
                                        modifier = Modifier.testTag("scatter_x_dropdown")
                                    ) {
                                        Text(selectedScatterIndependentVar ?: "Select Variable")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = xExpanded, onDismissRequest = { xExpanded = false }) {
                                        independentVars.forEach { col ->
                                            DropdownMenuItem(
                                                text = { Text(col) },
                                                onClick = {
                                                    selectedScatterIndependentVar = col
                                                    xExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Box: Custom Native Canvas Drawing
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().testTag("custom_vector_chart")) {
                                val width = size.width
                                val height = size.height

                                when (selectedModel) {
                                    ModelType.REGRESSION -> {
                                        val res = regressionResult
                                        val dataset = selectedDataset
                                        if (res != null && dataset != null) {
                                            val currentIndVar = selectedScatterIndependentVar ?: independentVars.firstOrNull() ?: ""
                                            drawRegressionScatter(
                                                dataset = dataset,
                                                depVar = viewModel.dependentVariable.value ?: "",
                                                indVar = currentIndVar,
                                                res = res,
                                                paletteColors = paletteColors,
                                                fontScale = chartFontScale,
                                                xLabel = chartXLabelOverride.ifEmpty { currentIndVar },
                                                yLabel = chartYLabelOverride.ifEmpty { viewModel.dependentVariable.value ?: "Outcome" }
                                            )
                                        }
                                    }
                                    ModelType.ANOVA -> {
                                        val res = anovaResult
                                        val dataset = selectedDataset
                                        if (res != null && dataset != null) {
                                            drawAnovaProfile(
                                                dataset = dataset,
                                                withinCols = viewModel.withinSubjectsFactors.value,
                                                betweenCol = viewModel.betweenSubjectsFactor.value ?: "",
                                                paletteColors = paletteColors,
                                                fontScale = chartFontScale,
                                                xLabel = chartXLabelOverride.ifEmpty { "Factors" },
                                                yLabel = chartYLabelOverride.ifEmpty { "Mean Estimates" }
                                            )
                                        }
                                    }
                                    ModelType.CHI_SQUARE -> {
                                        val res = chiSquareResult
                                        if (res != null) {
                                            drawChiSquareBar(
                                                res = res,
                                                paletteColors = paletteColors,
                                                fontScale = chartFontScale,
                                                xLabel = chartXLabelOverride.ifEmpty { viewModel.chiRowVariable.value ?: "Categories" },
                                                yLabel = chartYLabelOverride.ifEmpty { "Observed Counts" }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visualizer configuration options
                        Text("Visualization Configuration Control Panel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Color Palette Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Color Palette", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Geometric Balance", "Imperial Navy", "Crimson Sunset").forEach { palette ->
                                    val isSelected = chartPalette == palette || (chartPalette != "Imperial Navy" && chartPalette != "Crimson Sunset" && palette == "Geometric Balance")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { viewModel.chartPalette.value = palette }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = palette,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font Scaling Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Scale", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(90.dp))
                            Slider(
                                value = chartFontScale,
                                onValueChange = { viewModel.chartFontScale.value = it },
                                valueRange = 0.8f..1.5f,
                                modifier = Modifier.weight(1f).testTag("chart_font_scale_slider")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Axis Overrides
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = chartXLabelOverride,
                                onValueChange = { viewModel.chartXLabelOverride.value = it },
                                label = { Text("X-Axis Custom Label") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                modifier = Modifier.weight(1f).testTag("chart_x_label_input")
                            )
                            OutlinedTextField(
                                value = chartYLabelOverride,
                                onValueChange = { viewModel.chartYLabelOverride.value = it },
                                label = { Text("Y-Axis Custom Label") },
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                modifier = Modifier.weight(1f).testTag("chart_y_label_input")
                            )
                        }
                    }
                }
            }

            // SECTION 2: FORMATED DATA TABLES
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Deterministic Diagnostic Summary Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            var isCopied by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    val textToCopy = when (selectedModel) {
                                        ModelType.REGRESSION -> regressionResult?.let { formatRegressionResult(it) }
                                        ModelType.ANOVA -> anovaResult?.let { formatAnovaResult(it) }
                                        ModelType.CHI_SQUARE -> chiSquareResult?.let { formatChiSquareResult(it) }
                                    } ?: ""
                                    if (textToCopy.isNotEmpty()) {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(textToCopy))
                                        isCopied = true
                                    }
                                },
                                modifier = Modifier.testTag("copy_summary_table_button")
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Done else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Summary Table",
                                    tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isCopied) {
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    isCopied = false
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedModel) {
                            ModelType.REGRESSION -> {
                                val res = regressionResult!!
                                if (res.errorMessage != null) {
                                    Text(res.errorMessage, color = MaterialTheme.colorScheme.error)
                                } else {
                                    // General Regression stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("R-Squared: %.4f".format(res.rSquared), fontWeight = FontWeight.Bold)
                                        Text("Adj R-Squared: %.4f".format(res.adjustedRSquared), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("F-Statistic (df %d, %d): %.4f (p = %.5f)".format(res.dfRegression, res.dfResidual, res.fStatistic, res.fPValue), fontSize = 12.sp)
                                    Text("Shapiro-Wilk residuals normality W: %.4f (p = %.5f)".format(res.shapiroWilkStat, res.shapiroWilkW), fontSize = 12.sp)
                                    Text("Levene's Homoscedasticity F: %.4f (p = %.5f)".format(res.leveneStat, res.levenePValue), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Coefficients Table
                                    Text("Model Coefficients", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(8.dp)
                                        ) {
                                            Text("Variable", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Estimate", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("Std.Error", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("t-val", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("p-val", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("VIF", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        res.coefficients.forEach { coef ->
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Text(coef.variable, modifier = Modifier.weight(2f), fontSize = 12.sp)
                                                Text("%.4f".format(coef.estimate), modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("%.4f".format(coef.stdError), modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("%.3f".format(coef.tValue), modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("%.4f".format(coef.pValue), modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text(coef.vif?.let { "%.2f".format(it) } ?: "-", modifier = Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                            ModelType.ANOVA -> {
                                val res = anovaResult!!
                                if (res.errorMessage != null) {
                                    Text(res.errorMessage, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Levene's Sphericity Assumption F: %.4f (p = %.5f)".format(res.leveneStat, res.levenePValue), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // ANOVA Effects Table
                                    Text("Analysis of Variance (Type III SS)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(8.dp)
                                        ) {
                                            Text("Source", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("SS", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("df", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("MS", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("F-stat", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("p-val", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("pes", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        res.effects.forEach { effect ->
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Text(effect.source, modifier = Modifier.weight(2.5f), fontSize = 12.sp)
                                                Text("%.3f".format(effect.sumOfSquares), modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("%d".format(effect.df), modifier = Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("%.3f".format(effect.meanSquare), modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text(if (effect.fStatistic > 0) "%.3f".format(effect.fStatistic) else "-", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text(if (effect.fStatistic > 0) "%.4f".format(effect.pValue) else "-", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text(if (effect.fStatistic > 0) "%.3f".format(effect.partialEtaSquared) else "-", modifier = Modifier.weight(1f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                            ModelType.CHI_SQUARE -> {
                                val res = chiSquareResult!!
                                if (res.errorMessage != null) {
                                    Text(res.errorMessage, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Chi-Square Stat: %.4f".format(res.chiSquareStat), fontWeight = FontWeight.Bold)
                                        Text("df: %d".format(res.df), fontWeight = FontWeight.Bold)
                                        Text("p-value: %.5f".format(res.pValue), fontWeight = FontWeight.Bold)
                                    }
                                    Text("Cramer's V (Effect size): %.4f".format(res.cramersV), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Contingency Table observed vs expected
                                    Text("Contingency Table (Observed [Expected])", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(8.dp)
                                        ) {
                                            Text("Row \\ Col", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            res.colCategories.forEach { col ->
                                                Text(col, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                        res.rowCategories.forEach { rowCat ->
                                            Row(modifier = Modifier.padding(8.dp)) {
                                                Text(rowCat, modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                res.colCategories.forEach { colCat ->
                                                    val cell = res.cells.find { it.rowValue == rowCat && it.colValue == colCat }
                                                    val text = if (cell != null) "%d [%.1f]".format(cell.observed, cell.expected) else "-"
                                                    Text(text, modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: GEMINI'S EXPERT APA REPORT WRITE-UP
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Senior Academic APA 7th Edition Write-Up",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isGeneratingReport) {
                            // Custom Progress skeleton loader
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Gemini AI Synthesizing Write-up with High-Thinking Reasoning...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Evaluating statistical assumptions, degrees of freedom, and effect sizes.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else if (apaReport != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = apaReport!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.Serif,
                                    modifier = Modifier.testTag("apa_report_text")
                                )
                            }
                        } else {
                            Text("No report synthesized. Click 'Run Diagnostics' to compute metrics and trigger synthesis.")
                        }
                    }
                }
            }

            // SECTION 4: UNIFIED EXPORT CENTER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Unified Lab Export Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Download publication-ready vectors, statistical tables, and reports with a single click:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 1. Export High-Fidelity Vector PDF
                            Button(
                                onClick = {
                                    val title = selectedDataset?.name ?: "StatEngine_Pro"
                                    val reportBody = apaReport ?: "Statistical calculations complete."
                                    generateHighFidelityPdf(
                                        context = context,
                                        reportTitle = title,
                                        reportBody = reportBody,
                                        model = selectedModel,
                                        dataset = selectedDataset,
                                        reg = regressionResult,
                                        anova = anovaResult,
                                        chi = chiSquareResult,
                                        depVar = viewModel.dependentVariable.value ?: "",
                                        indVar = viewModel.independentVariables.value.firstOrNull() ?: "",
                                        withinCols = viewModel.withinSubjectsFactors.value,
                                        betweenCol = viewModel.betweenSubjectsFactor.value ?: ""
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download High-Fidelity PDF Report", fontWeight = FontWeight.Bold)
                                }
                            }

                            // 2. Export APA Markdown document
                            Button(
                                onClick = {
                                    val reportBody = apaReport ?: "# Statistical Output"
                                    shareMarkdownDocument(context, "StatEngine_APA_Report", reportBody)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_markdown_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Academic APA Markdown (.md)", fontWeight = FontWeight.Bold)
                                }
                            }

                            // 3. Export Raw Diagnostic CSV Tables
                            Button(
                                onClick = {
                                    val csvContent = buildCsvReport(selectedModel, regressionResult, anovaResult, chiSquareResult)
                                    shareCsvTable(context, "StatEngine_Raw_Diagnostics", csvContent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_csv_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GridOn, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Raw Diagnostic Excel/CSV Tables", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Custom Canvas Vector Drawers ---

fun DrawScope.drawRegressionScatter(
    dataset: Dataset,
    depVar: String,
    indVar: String,
    res: MultipleLinearRegressionResult,
    paletteColors: List<Color>,
    fontScale: Float,
    xLabel: String,
    yLabel: String
) {
    val xVals = dataset.getNumericValues(indVar)
    val yVals = dataset.getNumericValues(depVar)
    if (xVals.isEmpty() || yVals.isEmpty()) return

    val xMin = xVals.minOrNull() ?: 0.0
    val xMax = xVals.maxOrNull() ?: 1.0
    val yMin = yVals.minOrNull() ?: 0.0
    val yMax = yVals.maxOrNull() ?: 1.0

    val xRange = if (xMax - xMin > 0) xMax - xMin else 1.0
    val yRange = if (yMax - yMin > 0) yMax - yMin else 1.0

    val graphWidth = size.width - 100
    val graphHeight = size.height - 100
    val paddingLeft = 70f
    val paddingTop = 30f

    // Draw Axes
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop), Offset(paddingLeft, paddingTop + graphHeight), strokeWidth = 2f)
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop + graphHeight), Offset(paddingLeft + graphWidth, paddingTop + graphHeight), strokeWidth = 2f)

    // Draw grid lines
    for (i in 1..4) {
        val y = paddingTop + graphHeight - (graphHeight / 5) * i
        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(paddingLeft, y), Offset(paddingLeft + graphWidth, y), strokeWidth = 1f)
    }

    // Plot Data Points
    for (i in xVals.indices) {
        val px = paddingLeft + ((xVals[i] - xMin) / xRange * graphWidth).toFloat()
        val py = paddingTop + graphHeight - ((yVals[i] - yMin) / yRange * graphHeight).toFloat()
        drawCircle(color = paletteColors[1], radius = 6f, center = Offset(px, py))
    }

    // Draw Regression Line
    // Outcome Y = Beta0 (Intercept) + Beta1 * Predictor X
    val beta0 = res.coefficients.find { it.variable == "Intercept" }?.estimate ?: 0.0
    val beta1 = res.coefficients.find { it.variable == indVar }?.estimate ?: 0.0

    val lx1 = paddingLeft
    val ly1 = paddingTop + graphHeight - (((beta0 + beta1 * xMin) - yMin) / yRange * graphHeight).toFloat()

    val lx2 = paddingLeft + graphWidth
    val ly2 = paddingTop + graphHeight - (((beta0 + beta1 * xMax) - yMin) / yRange * graphHeight).toFloat()

    drawLine(color = paletteColors[0], start = Offset(lx1, ly1), end = Offset(lx2, ly2), strokeWidth = 4f)

    // Axis Labels text
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 11.sp.toPx() * fontScale
            isAntiAlias = true
        }
        drawText(xLabel, paddingLeft + graphWidth / 2 - 40f, paddingTop + graphHeight + 40f, paint)
        
        save()
        rotate(-90f, paddingLeft - 50f, paddingTop + graphHeight / 2)
        drawText(yLabel, paddingLeft - 100f, paddingTop + graphHeight / 2 + 10f, paint)
        restore()
    }
}

fun DrawScope.drawAnovaProfile(
    dataset: Dataset,
    withinCols: List<String>,
    betweenCol: String,
    paletteColors: List<Color>,
    fontScale: Float,
    xLabel: String,
    yLabel: String
) {
    if (withinCols.isEmpty()) return
    val groups = dataset.rows.map { it[betweenCol] ?: "Group" }.distinct().sorted()

    val graphWidth = size.width - 100
    val graphHeight = size.height - 100
    val paddingLeft = 70f
    val paddingTop = 30f

    // Draw Axes
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop), Offset(paddingLeft, paddingTop + graphHeight), strokeWidth = 2f)
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop + graphHeight), Offset(paddingLeft + graphWidth, paddingTop + graphHeight), strokeWidth = 2f)

    // Identify Overall Min & Max values across repeated measures to scale graph correctly
    var overallMin = Double.MAX_VALUE
    var overallMax = Double.MIN_VALUE
    for (col in withinCols) {
        val vals = dataset.getNumericValues(col)
        overallMin = min(overallMin, vals.minOrNull() ?: 0.0)
        overallMax = max(overallMax, vals.maxOrNull() ?: 100.0)
    }
    val yRange = if (overallMax - overallMin > 0) overallMax - overallMin else 100.0

    val colSpacing = graphWidth / withinCols.size
    val grpIndexMap = groups.withIndex().associate { it.value to it.index }

    // Plot repeated measure profile line for each between group
    for (gIdx in groups.indices) {
        val grp = groups[gIdx]
        val groupRows = dataset.rows.filter { (it[betweenCol] ?: "Group") == grp }
        val grpColor = paletteColors[gIdx % paletteColors.size]

        var lastPx = 0f
        var lastPy = 0f

        for (wIdx in withinCols.indices) {
            val col = withinCols[wIdx]
            val cellScores = groupRows.mapNotNull { it[col]?.toDoubleOrNull() }
            val mean = if (cellScores.isNotEmpty()) cellScores.average() else 0.0
            
            val px = paddingLeft + colSpacing * wIdx + colSpacing / 2
            val py = paddingTop + graphHeight - ((mean - overallMin) / yRange * graphHeight).toFloat()

            // Draw point dot
            drawCircle(color = grpColor, radius = 7f, center = Offset(px, py))

            // Standard Error bars
            if (cellScores.size > 1) {
                val stdDev = sqrt(cellScores.map { (it - mean).pow2() }.sum() / (cellScores.size - 1))
                val sem = stdDev / sqrt(cellScores.size.toDouble())
                val errorHeight = (sem / yRange * graphHeight).toFloat()

                drawLine(color = Color.Gray, start = Offset(px, py - errorHeight), end = Offset(px, py + errorHeight), strokeWidth = 1.5f)
                drawLine(color = Color.Gray, start = Offset(px - 10f, py - errorHeight), end = Offset(px + 10f, py - errorHeight), strokeWidth = 1.5f)
                drawLine(color = Color.Gray, start = Offset(px - 10f, py + errorHeight), end = Offset(px + 10f, py + errorHeight), strokeWidth = 1.5f)
            }

            // Draw profile line segment connects timepoints
            if (wIdx > 0) {
                drawLine(color = grpColor, start = Offset(lastPx, lastPy), end = Offset(px, py), strokeWidth = 3f)
            }

            lastPx = px
            lastPy = py

            // Timepoint label under the axis
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = Color.Gray.toArgb()
                    textSize = 10.sp.toPx() * fontScale
                    isAntiAlias = true
                }
                drawText(col, px - 20f, paddingTop + graphHeight + 25f, paint)
            }
        }
    }

    // Legend
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            textSize = 11.sp.toPx() * fontScale
            isAntiAlias = true
        }
        for (gIdx in groups.indices) {
            paint.color = paletteColors[gIdx % paletteColors.size].toArgb()
            drawText("■ ${groups[gIdx]}", paddingLeft + graphWidth - 120f, paddingTop + 20f + gIdx * 18f, paint)
        }
    }
}

fun DrawScope.drawChiSquareBar(
    res: ChiSquareResult,
    paletteColors: List<Color>,
    fontScale: Float,
    xLabel: String,
    yLabel: String
) {
    val graphWidth = size.width - 100
    val graphHeight = size.height - 100
    val paddingLeft = 70f
    val paddingTop = 30f

    // Draw Axes
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop), Offset(paddingLeft, paddingTop + graphHeight), strokeWidth = 2f)
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop + graphHeight), Offset(paddingLeft + graphWidth, paddingTop + graphHeight), strokeWidth = 2f)

    val maxObserved = res.cells.maxOfOrNull { it.observed } ?: 10
    val yMax = (maxObserved * 1.2).toFloat()

    val numRows = res.rowCategories.size
    val numCols = res.colCategories.size
    val blockSpacing = graphWidth / numRows

    for (rIdx in res.rowCategories.indices) {
        val rowVal = res.rowCategories[rIdx]
        val blockLeft = paddingLeft + blockSpacing * rIdx

        val barWidth = (blockSpacing * 0.6f) / numCols

        for (cIdx in res.colCategories.indices) {
            val colVal = res.colCategories[cIdx]
            val cell = res.cells.find { it.rowValue == rowVal && it.colValue == colVal }
            val obs = cell?.observed ?: 0

            val barHeight = (obs.toFloat() / yMax) * graphHeight
            val barLeft = blockLeft + blockSpacing * 0.1f + barWidth * cIdx
            val barTop = paddingTop + graphHeight - barHeight

            drawRect(
                color = paletteColors[cIdx % paletteColors.size],
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth * 0.9f, barHeight)
            )
        }

        // Row categories label
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().apply {
                color = Color.Gray.toArgb()
                textSize = 10.sp.toPx() * fontScale
                isAntiAlias = true
            }
            drawText(rowVal, blockLeft + blockSpacing / 3 - 10f, paddingTop + graphHeight + 25f, paint)
        }
    }

    // Legend
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            textSize = 11.sp.toPx() * fontScale
            isAntiAlias = true
        }
        for (cIdx in res.colCategories.indices) {
            paint.color = paletteColors[cIdx % paletteColors.size].toArgb()
            drawText("■ ${res.colCategories[cIdx]}", paddingLeft + graphWidth - 120f, paddingTop + 20f + cIdx * 18f, paint)
        }
    }
}

fun DrawScope.drawHistogram(
    dataset: Dataset,
    colName: String,
    numBins: Int,
    paletteColors: List<Color>,
    fontScale: Float
) {
    val values = dataset.getNumericValues(colName)
    if (values.isEmpty()) return

    val minVal = values.minOrNull() ?: 0.0
    val maxVal = values.maxOrNull() ?: 1.0
    val range = maxVal - minVal

    val binCounts = IntArray(numBins)
    if (range > 0.0) {
        val binWidth = range / numBins
        for (v in values) {
            var binIdx = ((v - minVal) / binWidth).toInt()
            if (binIdx >= numBins) binIdx = numBins - 1
            if (binIdx < 0) binIdx = 0
            binCounts[binIdx]++
        }
    } else {
        binCounts[0] = values.size
    }

    val maxCount = binCounts.maxOrNull() ?: 1
    val yMax = (maxCount * 1.15).toFloat()

    val graphWidth = size.width - 120
    val graphHeight = size.height - 100
    val paddingLeft = 80f
    val paddingTop = 30f

    // Draw Axes
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop), Offset(paddingLeft, paddingTop + graphHeight), strokeWidth = 2f)
    drawLine(Color.Gray, Offset(paddingLeft, paddingTop + graphHeight), Offset(paddingLeft + graphWidth, paddingTop + graphHeight), strokeWidth = 2f)

    // Draw grid lines & Y labels
    drawContext.canvas.nativeCanvas.apply {
        val labelPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 9.sp.toPx() * fontScale
            isAntiAlias = true
        }
        for (i in 0..4) {
            val ratio = i / 4f
            val countVal = kotlin.math.round(ratio * maxCount).toInt()
            val y = paddingTop + graphHeight - ratio * graphHeight
            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(paddingLeft, y), Offset(paddingLeft + graphWidth, y), strokeWidth = 1f)
            drawText(countVal.toString(), paddingLeft - 30f, y + 4f, labelPaint)
        }
    }

    // Draw Bars
    val barWidth = graphWidth / numBins
    for (i in 0 until numBins) {
        val count = binCounts[i]
        if (count == 0) continue

        val barHeight = (count.toFloat() / yMax) * graphHeight
        val barLeft = paddingLeft + barWidth * i
        val barTop = paddingTop + graphHeight - barHeight

        // Fill bar with primary color
        drawRect(
            color = paletteColors[0].copy(alpha = 0.85f),
            topLeft = Offset(barLeft + 2f, barTop),
            size = Size(barWidth - 4f, barHeight)
        )
        // Outline bar with darker primary or solid
        drawRect(
            color = paletteColors[0],
            topLeft = Offset(barLeft + 2f, barTop),
            size = Size(barWidth - 4f, barHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    }

    // Draw X-axis label ticks (bin boundaries)
    drawContext.canvas.nativeCanvas.apply {
        val labelPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 9.sp.toPx() * fontScale
            isAntiAlias = true
        }
        if (range > 0.0) {
            val binWidth = range / numBins
            val labelIndices = listOf(0, numBins / 2, numBins)
            labelIndices.forEach { idx ->
                val v = minVal + idx * binWidth
                val px = paddingLeft + (idx.toFloat() / numBins) * graphWidth
                val label = String.format(java.util.Locale.US, "%.2f", v)
                drawText(label, px - 20f, paddingTop + graphHeight + 25f, labelPaint)
                drawLine(Color.Gray, Offset(px, paddingTop + graphHeight), Offset(px, paddingTop + graphHeight + 6f), strokeWidth = 1.5f)
            }
        } else {
            val label = String.format(java.util.Locale.US, "%.2f", minVal)
            drawText(label, paddingLeft + graphWidth / 2 - 20f, paddingTop + graphHeight + 25f, labelPaint)
        }
    }
}

fun DrawScope.drawBoxPlot(
    dataset: Dataset,
    colName: String,
    paletteColors: List<Color>,
    fontScale: Float
) {
    val rawValues = dataset.getNumericValues(colName)
    if (rawValues.isEmpty()) return
    val values = rawValues.sorted()

    val count = values.size
    val median = if (count % 2 == 1) values[count / 2] else (values[count / 2 - 1] + values[count / 2]) / 2.0
    val q1 = getPercentile(values, 25.0)
    val q3 = getPercentile(values, 75.0)
    val iqr = q3 - q1
    val lowerFence = q1 - 1.5 * iqr
    val upperFence = q3 + 1.5 * iqr

    val minNonOutlier = values.firstOrNull { it >= lowerFence } ?: values.first()
    val maxNonOutlier = values.lastOrNull { it <= upperFence } ?: values.last()

    val scaleMin = values.first()
    val scaleMax = values.last()
    val scaleRange = if (scaleMax - scaleMin > 0) scaleMax - scaleMin else 1.0

    val graphWidth = size.width - 120
    val graphHeight = size.height - 100
    val paddingLeft = 80f
    val paddingTop = 30f

    val centerY = paddingTop + graphHeight / 2

    // Draw Scale Grid Lines & Labels along bottom axis
    drawContext.canvas.nativeCanvas.apply {
        val labelPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 9.sp.toPx() * fontScale
            isAntiAlias = true
        }
        val gridLineCount = 5
        for (i in 0 until gridLineCount) {
            val ratio = i.toFloat() / (gridLineCount - 1)
            val valAtTick = scaleMin + ratio * scaleRange
            val px = paddingLeft + ratio * graphWidth
            
            // Draw thin vertical gridline
            drawLine(Color.LightGray.copy(alpha = 0.2f), Offset(px, paddingTop), Offset(px, paddingTop + graphHeight), strokeWidth = 1f)
            
            // Draw label tick
            drawLine(Color.Gray, Offset(px, paddingTop + graphHeight), Offset(px, paddingTop + graphHeight + 6f), strokeWidth = 1.5f)
            
            val label = String.format(java.util.Locale.US, "%.2f", valAtTick)
            drawText(label, px - 20f, paddingTop + graphHeight + 25f, labelPaint)
        }
    }

    // Compute pixel coordinates
    val pxQ1 = paddingLeft + ((q1 - scaleMin) / scaleRange * graphWidth).toFloat()
    val pxQ3 = paddingLeft + ((q3 - scaleMin) / scaleRange * graphWidth).toFloat()
    val pxMedian = paddingLeft + ((median - scaleMin) / scaleRange * graphWidth).toFloat()
    val pxMinNonOutlier = paddingLeft + ((minNonOutlier - scaleMin) / scaleRange * graphWidth).toFloat()
    val pxMaxNonOutlier = paddingLeft + ((maxNonOutlier - scaleMin) / scaleRange * graphWidth).toFloat()

    val boxHeight = 60f

    // Draw Box
    // Fill
    drawRect(
        color = paletteColors[1].copy(alpha = 0.4f),
        topLeft = Offset(pxQ1, centerY - boxHeight / 2),
        size = Size(pxQ3 - pxQ1, boxHeight)
    )
    // Outline
    drawRect(
        color = paletteColors[0],
        topLeft = Offset(pxQ1, centerY - boxHeight / 2),
        size = Size(pxQ3 - pxQ1, boxHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    // Draw Median line
    drawLine(
        color = paletteColors[0],
        start = Offset(pxMedian, centerY - boxHeight / 2),
        end = Offset(pxMedian, centerY + boxHeight / 2),
        strokeWidth = 4f
    )

    // Draw Whiskers (lines from Q1/Q3 to min/max non-outliers)
    drawLine(
        color = Color.Gray,
        start = Offset(pxMinNonOutlier, centerY),
        end = Offset(pxQ1, centerY),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray,
        start = Offset(pxQ3, centerY),
        end = Offset(pxMaxNonOutlier, centerY),
        strokeWidth = 2f
    )

    // Draw Whisker Caps
    drawLine(
        color = Color.Gray,
        start = Offset(pxMinNonOutlier, centerY - 15f),
        end = Offset(pxMinNonOutlier, centerY + 15f),
        strokeWidth = 2f
    )
    drawLine(
        color = Color.Gray,
        start = Offset(pxMaxNonOutlier, centerY - 15f),
        end = Offset(pxMaxNonOutlier, centerY + 15f),
        strokeWidth = 2f
    )

    // Draw Outliers
    for (v in values) {
        if (v < lowerFence || v > upperFence) {
            val pxOutlier = paddingLeft + ((v - scaleMin) / scaleRange * graphWidth).toFloat()
            drawCircle(
                color = Color.Red,
                radius = 5f,
                center = Offset(pxOutlier, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }
    }

    // Label critical values above the box
    drawContext.canvas.nativeCanvas.apply {
        val valPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = 9.sp.toPx() * fontScale
            isAntiAlias = true
        }
        
        // Label median
        val medianLabel = String.format(java.util.Locale.US, "Med: %.2f", median)
        drawText(medianLabel, pxMedian - 25f, centerY - boxHeight / 2 - 10f, valPaint)
        
        // Label Q1
        val q1Label = String.format(java.util.Locale.US, "Q1: %.2f", q1)
        drawText(q1Label, pxQ1 - 25f, centerY + boxHeight / 2 + 20f, valPaint)
        
        // Label Q3
        val q3Label = String.format(java.util.Locale.US, "Q3: %.2f", q3)
        drawText(q3Label, pxQ3 - 25f, centerY + boxHeight / 2 + 20f, valPaint)
    }
}

private fun getPercentile(sortedValues: List<Double>, percentile: Double): Double {
    if (sortedValues.isEmpty()) return 0.0
    val index = (percentile / 100.0) * (sortedValues.size - 1)
    val lower = kotlin.math.floor(index).toInt()
    val upper = kotlin.math.ceil(index).toInt()
    if (lower == upper) return sortedValues[lower]
    val weight = index - lower
    return sortedValues[lower] * (1.0 - weight) + sortedValues[upper] * weight
}

private fun Double.pow2() = this * this

// --- UNIFIED EXPORT UTILITIES ---

fun shareMarkdownDocument(context: Context, filename: String, content: String) {
    try {
        val file = File(context.cacheDir, "$filename.md")
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "StatEngine Pro Academic APA Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share APA Markdown Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareCsvTable(context: Context, filename: String, content: String) {
    try {
        val file = File(context.cacheDir, "$filename.csv")
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "StatEngine Pro Raw Diagnostic Tables")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Raw Diagnostics CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun buildCsvReport(
    model: ModelType,
    reg: MultipleLinearRegressionResult?,
    anova: MixedAnovaResult?,
    chi: ChiSquareResult?
): String {
    val builder = java.lang.StringBuilder()
    when (model) {
        ModelType.REGRESSION -> {
            if (reg != null) {
                builder.append("Multiple Linear Regression Model Diagnostics\n\n")
                builder.append("Metric,Value\n")
                builder.append("R-Squared,${reg.rSquared}\n")
                builder.append("Adjusted R-Squared,${reg.adjustedRSquared}\n")
                builder.append("F-Statistic,${reg.fStatistic}\n")
                builder.append("f-PValue,${reg.fPValue}\n\n")
                
                builder.append("Variable,Estimate,StdError,tValue,pValue,VIF\n")
                reg.coefficients.forEach { coef ->
                    builder.append("${coef.variable},${coef.estimate},${coef.stdError},${coef.tValue},${coef.pValue},${coef.vif ?: ""}\n")
                }
            }
        }
        ModelType.ANOVA -> {
            if (anova != null) {
                builder.append("Mixed-Design ANOVA Sum of Squares Table\n\n")
                builder.append("Source,SS,df,MS,F-Statistic,pValue,PartialEtaSquared\n")
                anova.effects.forEach { effect ->
                    builder.append("${effect.source},${effect.sumOfSquares},${effect.df},${effect.meanSquare},${effect.fStatistic},${effect.pValue},${effect.partialEtaSquared}\n")
                }
            }
        }
        ModelType.CHI_SQUARE -> {
            if (chi != null) {
                builder.append("Chi-Square Test of Independence Contingency Table\n\n")
                builder.append("Chi-Square Stat,${chi.chiSquareStat}\n")
                builder.append("df,${chi.df}\n")
                builder.append("pValue,${chi.pValue}\n")
                builder.append("Cramers V,${chi.cramersV}\n\n")

                builder.append("Row Category,Col Category,Observed,Expected\n")
                chi.cells.forEach { cell ->
                    builder.append("${cell.rowValue},${cell.colValue},${cell.observed},${cell.expected}\n")
                }
            }
        }
    }
    return builder.toString()
}

/**
 * Generates a high-fidelity PDF Document using Android's native PdfDocument.
 * It draws the academic text neatly line by line on standard letter-sized vector PDF page layout.
 */
/**
 * Generates a high-fidelity PDF Document using Android's native PdfDocument.
 * It draws the academic text neatly line by line on standard letter-sized vector PDF page layout.
 */
fun generateHighFidelityPdf(
    context: Context,
    reportTitle: String,
    reportBody: String,
    model: ModelType,
    dataset: Dataset?,
    reg: MultipleLinearRegressionResult?,
    anova: MixedAnovaResult?,
    chi: ChiSquareResult?,
    depVar: String,
    indVar: String,
    withinCols: List<String>,
    betweenCol: String
) {
    try {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create() // Standard US Letter size: 8.5 x 11 inches
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            strokeWidth = 1f
        }

        // Title Header
        canvas.drawText("StatEngine Pro - Academic Report", 54f, 54f, titlePaint)
        canvas.drawText("Dataset Session: $reportTitle", 54f, 76f, bodyPaint)

        // Divider Line
        canvas.drawLine(54f, 90f, 558f, 90f, linePaint)

        var yPos = 120f
        val margin = 54f
        val printableWidth = 612f - 2 * margin

        // Draw the Chart if dataset is not null and results exist
        if (dataset != null) {
            val chartHeight = 220f
            val chartWidth = 504f
            val chartLeft = 54f
            val chartTop = 110f

            // Drawing beautiful frame for the chart
            val borderPaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(chartLeft, chartTop, chartLeft + chartWidth, chartTop + chartHeight, borderPaint)

            // Draw specific chart
            when (model) {
                ModelType.REGRESSION -> {
                    if (reg != null) {
                        drawRegressionScatterNative(
                            canvas, dataset, depVar, indVar, reg,
                            chartLeft, chartTop, chartWidth, chartHeight
                        )
                    }
                }
                ModelType.ANOVA -> {
                    if (anova != null) {
                        drawAnovaProfileNative(
                            canvas, dataset, withinCols, betweenCol,
                            chartLeft, chartTop, chartWidth, chartHeight
                        )
                    }
                }
                ModelType.CHI_SQUARE -> {
                    if (chi != null) {
                        drawChiSquareBarNative(
                            canvas, chi,
                            chartLeft, chartTop, chartWidth, chartHeight
                        )
                    }
                }
            }

            // Figure Caption
            val captionPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("Figure 1: Generated publication-ready visual diagnostics for model $model.", chartLeft + 10f, chartTop + chartHeight + 20f, captionPaint)
            
            yPos = chartTop + chartHeight + 45f
        }

        // Split text body by paragraphs or lines
        val lines = reportBody.split("\n")
        for (rawLine in lines) {
            val words = rawLine.trim().split("\\s+".toRegex())
            val lineBuilder = StringBuilder()
            
            for (word in words) {
                if (word.isEmpty()) continue
                val potentialLine = if (lineBuilder.isEmpty()) word else "${lineBuilder.toString()} $word"
                val textWidth = bodyPaint.measureText(potentialLine)
                if (textWidth > printableWidth) {
                    canvas.drawText(lineBuilder.toString(), margin, yPos, bodyPaint)
                    yPos += 18f
                    
                    // Check if page needs to be advanced
                    if (yPos > 730f) {
                        // Draw footer before finishing
                        canvas.drawText("Page $pageNumber", 558f - headerPaint.measureText("Page $pageNumber"), 760f, headerPaint)
                        pdfDocument.finishPage(page)
                        
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        
                        // Draw header on new page
                        canvas.drawText("StatEngine Pro - Academic Report (Cont.)", 54f, 40f, headerPaint)
                        canvas.drawLine(54f, 48f, 558f, 48f, linePaint)
                        yPos = 70f
                    }
                    
                    lineBuilder.clear()
                    lineBuilder.append(word)
                } else {
                    lineBuilder.append(if (lineBuilder.isEmpty()) word else " $word")
                }
            }
            
            if (lineBuilder.isNotEmpty()) {
                canvas.drawText(lineBuilder.toString(), margin, yPos, bodyPaint)
                yPos += 18f
            }
            
            // Paragraph break space
            yPos += 8f
            
            // Check if page needs to be advanced after paragraph break
            if (yPos > 730f) {
                // Draw footer before finishing
                canvas.drawText("Page $pageNumber", 558f - headerPaint.measureText("Page $pageNumber"), 760f, headerPaint)
                pdfDocument.finishPage(page)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                // Draw header on new page
                canvas.drawText("StatEngine Pro - Academic Report (Cont.)", 54f, 40f, headerPaint)
                canvas.drawLine(54f, 48f, 558f, 48f, linePaint)
                yPos = 70f
            }
        }

        // Draw last page footer before finishing
        canvas.drawText("Page $pageNumber", 558f - headerPaint.measureText("Page $pageNumber"), 760f, headerPaint)
        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "StatEngine_APA_Report.pdf")
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "StatEngine Pro Academic APA Report PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share APA PDF Report"))

    } catch (e: Exception) {
        Toast.makeText(context, "PDF Generation Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Renders a scatter plot with regression line on an Android Canvas.
 */
fun drawRegressionScatterNative(
    canvas: android.graphics.Canvas,
    dataset: Dataset,
    depVar: String,
    indVar: String,
    res: MultipleLinearRegressionResult,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val xVals = dataset.getNumericValues(indVar)
    val yVals = dataset.getNumericValues(depVar)
    if (xVals.isEmpty() || yVals.isEmpty()) return

    val xMin = xVals.minOrNull() ?: 0.0
    val xMax = xVals.maxOrNull() ?: 1.0
    val yMin = yVals.minOrNull() ?: 0.0
    val yMax = yVals.maxOrNull() ?: 1.0

    val xRange = if (xMax - xMin > 0) xMax - xMin else 1.0
    val yRange = if (yMax - yMin > 0) yMax - yMin else 1.0

    val graphWidth = width - 80f
    val graphHeight = height - 80f
    val paddingLeft = left + 60f
    val paddingTop = top + 20f

    val axisPaint = Paint().apply {
        color = android.graphics.Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    val gridPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    val pointPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#3F51B5") // Primary Blue
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val linePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#FF5722") // Accent Red
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9f
        isAntiAlias = true
    }

    // Draw axes
    canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, axisPaint)
    canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, axisPaint)

    // Draw grids
    for (i in 1..4) {
        val y = paddingTop + graphHeight - (graphHeight / 5f) * i
        canvas.drawLine(paddingLeft, y, paddingLeft + graphWidth, y, gridPaint)
    }

    // Plot data points
    for (i in xVals.indices) {
        val px = paddingLeft + ((xVals[i] - xMin) / xRange * graphWidth).toFloat()
        val py = paddingTop + graphHeight - ((yVals[i] - yMin) / yRange * graphHeight).toFloat()
        canvas.drawCircle(px, py, 4f, pointPaint)
    }

    // Draw Regression Line
    val beta0 = res.coefficients.find { it.variable == "Intercept" }?.estimate ?: 0.0
    val beta1 = res.coefficients.find { it.variable == indVar }?.estimate ?: 0.0

    val lx1 = paddingLeft
    val ly1 = paddingTop + graphHeight - (((beta0 + beta1 * xMin) - yMin) / yRange * graphHeight).toFloat()
    val lx2 = paddingLeft + graphWidth
    val ly2 = paddingTop + graphHeight - (((beta0 + beta1 * xMax) - yMin) / yRange * graphHeight).toFloat()
    canvas.drawLine(lx1, ly1, lx2, ly2, linePaint)

    // Axis Labels
    canvas.drawText(indVar, paddingLeft + graphWidth / 2f - 20f, paddingTop + graphHeight + 35f, textPaint)
    canvas.save()
    canvas.rotate(-90f, paddingLeft - 35f, paddingTop + graphHeight / 2f)
    canvas.drawText(depVar, paddingLeft - 80f, paddingTop + graphHeight / 2f + 5f, textPaint)
    canvas.restore()
}

/**
 * Renders an ANOVA profile plot on an Android Canvas.
 */
fun drawAnovaProfileNative(
    canvas: android.graphics.Canvas,
    dataset: Dataset,
    withinCols: List<String>,
    betweenCol: String,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    if (withinCols.isEmpty()) return
    val groups = dataset.rows.map { it[betweenCol] ?: "Group" }.distinct().sorted()

    val graphWidth = width - 80f
    val graphHeight = height - 80f
    val paddingLeft = left + 60f
    val paddingTop = top + 20f

    val axisPaint = Paint().apply {
        color = android.graphics.Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    val gridPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9f
        isAntiAlias = true
    }

    // Draw axes
    canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, axisPaint)
    canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, axisPaint)

    // Identify Overall Min & Max values
    var overallMin = Double.MAX_VALUE
    var overallMax = Double.MIN_VALUE
    for (col in withinCols) {
        val vals = dataset.getNumericValues(col)
        if (vals.isNotEmpty()) {
            overallMin = kotlin.math.min(overallMin, vals.minOrNull() ?: 0.0)
            overallMax = kotlin.math.max(overallMax, vals.maxOrNull() ?: 100.0)
        }
    }
    if (overallMin == Double.MAX_VALUE) { overallMin = 0.0; overallMax = 100.0 }
    val yRange = if (overallMax - overallMin > 0) overallMax - overallMin else 100.0

    // Draw grid lines
    for (i in 1..4) {
        val y = paddingTop + graphHeight - (graphHeight / 5f) * i
        canvas.drawLine(paddingLeft, y, paddingLeft + graphWidth, y, gridPaint)
    }

    val colSpacing = graphWidth / withinCols.size
    val colors = listOf("#3F51B5", "#FF5722", "#4CAF50", "#9C27B0", "#FFEB3B")

    for (gIdx in groups.indices) {
        val grp = groups[gIdx]
        val groupRows = dataset.rows.filter { (it[betweenCol] ?: "Group") == grp }
        val grpColor = android.graphics.Color.parseColor(colors[gIdx % colors.size])

        val linePaint = Paint().apply {
            color = grpColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val dotPaint = Paint().apply {
            color = grpColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var lastPx = 0f
        var lastPy = 0f

        for (wIdx in withinCols.indices) {
            val col = withinCols[wIdx]
            val cellScores = groupRows.mapNotNull { it[col]?.toDoubleOrNull() }
            val mean = if (cellScores.isNotEmpty()) cellScores.average() else 0.0

            val px = paddingLeft + colSpacing * wIdx + colSpacing / 2f
            val py = paddingTop + graphHeight - ((mean - overallMin) / yRange * graphHeight).toFloat()

            canvas.drawCircle(px, py, 5f, dotPaint)

            if (wIdx > 0) {
                canvas.drawLine(lastPx, lastPy, px, py, linePaint)
            }
            lastPx = px
            lastPy = py

            // Column label
            canvas.drawText(col, px - 15f, paddingTop + graphHeight + 20f, textPaint)
        }
    }
}

/**
 * Renders a Chi-Square side-by-side bar chart on an Android Canvas.
 */
fun drawChiSquareBarNative(
    canvas: android.graphics.Canvas,
    res: ChiSquareResult,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val graphWidth = width - 80f
    val graphHeight = height - 80f
    val paddingLeft = left + 60f
    val paddingTop = top + 20f

    val axisPaint = Paint().apply {
        color = android.graphics.Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    val gridPaint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    val textPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9f
        isAntiAlias = true
    }

    // Draw axes
    canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, axisPaint)
    canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, axisPaint)

    val maxObserved = res.cells.maxOfOrNull { it.observed } ?: 10
    val yMax = (maxObserved * 1.2).toFloat()

    // Draw grids
    for (i in 1..4) {
        val y = paddingTop + graphHeight - (graphHeight / 5f) * i
        canvas.drawLine(paddingLeft, y, paddingLeft + graphWidth, y, gridPaint)
    }

    val numRows = res.rowCategories.size
    val numCols = res.colCategories.size
    val blockSpacing = graphWidth / numRows
    val colors = listOf("#3F51B5", "#FF5722", "#4CAF50", "#9C27B0", "#FFEB3B")

    for (rIdx in res.rowCategories.indices) {
        val rowVal = res.rowCategories[rIdx]
        val blockLeft = paddingLeft + blockSpacing * rIdx

        val barWidth = (blockSpacing * 0.6f) / numCols

        for (cIdx in res.colCategories.indices) {
            val colVal = res.colCategories[cIdx]
            val cell = res.cells.find { it.rowValue == rowVal && it.colValue == colVal }
            val obs = cell?.observed ?: 0

            val barHeight = (obs.toFloat() / yMax) * graphHeight
            val barLeft = blockLeft + blockSpacing * 0.1f + barWidth * cIdx
            val barTop = paddingTop + graphHeight - barHeight

            val rectPaint = Paint().apply {
                color = android.graphics.Color.parseColor(colors[cIdx % colors.size])
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawRect(barLeft, barTop, barLeft + barWidth * 0.9f, paddingTop + graphHeight, rectPaint)
        }

        // Label row
        canvas.drawText(rowVal, blockLeft + blockSpacing / 3f, paddingTop + graphHeight + 20f, textPaint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnDistributionSection(
    dataset: Dataset,
    numericCols: List<String>,
    selectedDistColumn: String,
    histogramBins: Int,
    onColumnSelected: (String) -> Unit,
    onBinsChanged: (Int) -> Unit,
    paletteColors: List<Color>,
    fontScale: Float
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive Column Distribution Plots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Examine individual continuous variables using automatically generated dynamic histograms and box-and-whisker plots.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Select Column & Bins Configuration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column Dropdown
                Box(modifier = Modifier.weight(1.2f)) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedDistColumn,
                            onValueChange = {},
                            label = { Text("Select Column to Plot") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            numericCols.forEach { col ->
                                DropdownMenuItem(
                                    text = { Text(col) },
                                    onClick = {
                                        onColumnSelected(col)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Number of Bins Slider
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Histogram Bins: $histogramBins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value = histogramBins.toFloat(),
                        onValueChange = { onBinsChanged(it.toInt()) },
                        valueRange = 5f..25f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display plots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Histogram Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(260.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Frequency Histogram: $selectedDistColumn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawHistogram(
                                dataset = dataset,
                                colName = selectedDistColumn,
                                numBins = histogramBins,
                                paletteColors = paletteColors,
                                fontScale = fontScale
                            )
                        }
                    }
                }

                // 2. Box Plot Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(260.dp)
                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Box-and-Whisker Plot: $selectedDistColumn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawBoxPlot(
                                dataset = dataset,
                                colName = selectedDistColumn,
                                paletteColors = paletteColors,
                                fontScale = fontScale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CorrelationHeatmapSection(
    numericCols: List<String>,
    correlationItems: List<CorrelationItem>,
    paletteColors: List<Color>,
    fontScale: Float
) {
    if (numericCols.isEmpty()) return

    var selectedCell by remember { mutableStateOf<CorrelationItem?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Numerical Correlation Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Observe linear relationships between numerical features. Cell colors range from deep orange/red (negative correlation) to bright blue (positive correlation). Tap a cell to view exact coefficients.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Heatmap Grid
            val cellSizePx = 150f * fontScale
            val labelWidthPx = 160f * fontScale
            val gridSizePx = numericCols.size * cellSizePx
            val totalWidthPx = labelWidthPx + gridSizePx
            val totalHeightPx = labelWidthPx + gridSizePx

            val density = androidx.compose.ui.platform.LocalDensity.current
            val totalWidthDp = with(density) { totalWidthPx.toDp() }
            val totalHeightDp = with(density) { totalHeightPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Canvas(
                    modifier = Modifier
                        .size(width = totalWidthDp + 20.dp, height = totalHeightDp + 20.dp)
                        .pointerInput(numericCols, correlationItems, fontScale) {
                            detectTapGestures { offset ->
                                val x = offset.x
                                val y = offset.y

                                if (x >= labelWidthPx && y >= labelWidthPx) {
                                    val colIdx = ((x - labelWidthPx) / cellSizePx).toInt().coerceIn(0, numericCols.size - 1)
                                    val rowIdx = ((y - labelWidthPx) / cellSizePx).toInt().coerceIn(0, numericCols.size - 1)

                                    val colA = numericCols[rowIdx] // Row
                                    val colB = numericCols[colIdx] // Column

                                    selectedCell = correlationItems.find {
                                        (it.columnA == colA && it.columnB == colB) ||
                                                (it.columnA == colB && it.columnB == colA)
                                    }
                                }
                            }
                        }
                ) {
                    val textPaint = Paint().apply {
                        color = Color.Gray.toArgb()
                        textSize = 9.sp.toPx() * fontScale
                        isAntiAlias = true
                    }
                    val valuePaint = Paint().apply {
                        color = Color.White.toArgb()
                        textSize = 10.sp.toPx() * fontScale
                        isAntiAlias = true
                    }
                    val headerPaint = Paint().apply {
                        color = Color.Gray.toArgb()
                        textSize = 10.sp.toPx() * fontScale
                        isAntiAlias = true
                    }

                    // 1. Draw Col Headers (top labels rotated or offset)
                    numericCols.forEachIndexed { colIdx, colName ->
                        val textX = labelWidthPx + colIdx * cellSizePx + cellSizePx / 2
                        val textY = labelWidthPx - 10f
                        
                        val abbreviated = if (colName.length > 10) colName.take(8) + ".." else colName
                        
                        drawContext.canvas.nativeCanvas.apply {
                            save()
                            rotate(-30f, textX, textY)
                            drawText(abbreviated, textX - 15f, textY, headerPaint)
                            restore()
                        }
                    }

                    // 2. Draw Row Headers (left labels)
                    numericCols.forEachIndexed { rowIdx, colName ->
                        val textX = 10f
                        val textY = labelWidthPx + rowIdx * cellSizePx + cellSizePx / 2 + 4f
                        
                        val abbreviated = if (colName.length > 15) colName.take(12) + ".." else colName
                        
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(abbreviated, textX, textY, headerPaint)
                        }
                    }

                    // 3. Draw Grid Cells
                    for (rowIdx in numericCols.indices) {
                        val colA = numericCols[rowIdx]
                        for (colIdx in numericCols.indices) {
                            val colB = numericCols[colIdx]

                            val item = correlationItems.find {
                                (it.columnA == colA && it.columnB == colB) ||
                                        (it.columnA == colB && it.columnB == colA)
                            }
                            val r = item?.coefficient

                            val cellLeft = labelWidthPx + colIdx * cellSizePx
                            val cellTop = labelWidthPx + rowIdx * cellSizePx

                            // Determine color based on r value
                            val color = when {
                                r == null -> Color.Gray.copy(alpha = 0.2f)
                                r >= 0f -> {
                                    val alpha = r.toFloat().coerceIn(0f, 1f)
                                    paletteColors[0].copy(alpha = alpha.coerceIn(0.15f, 1f))
                                }
                                else -> {
                                    val alpha = abs(r.toFloat()).coerceIn(0f, 1f)
                                    Color(0xFFEF4444).copy(alpha = alpha.coerceIn(0.15f, 1f))
                                }
                            }

                            // Fill cell
                            drawRect(
                                color = color,
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f)
                            )

                            // Outline cell
                            drawRect(
                                color = Color.Gray.copy(alpha = 0.3f),
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellSizePx - 4f, cellSizePx - 4f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
                            )

                            // Write r value in cell
                            if (r != null) {
                                val label = String.format(java.util.Locale.US, "%+.3f", r)
                                val textX = cellLeft + cellSizePx / 2 - 24f
                                val textY = cellTop + cellSizePx / 2 + 4f
                                
                                val textCol = if (abs(r) > 0.4) Color.White else Color.Black
                                valuePaint.color = textCol.toArgb()

                                drawContext.canvas.nativeCanvas.apply {
                                    drawText(label, textX, textY, valuePaint)
                                }
                            }
                        }
                    }
                }
            }

            selectedCell?.let { cell ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Relationship Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Correlation between ${cell.columnA} and ${cell.columnB}:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            val rVal = cell.coefficient
                            val descText = when {
                                rVal == null -> "Insufficient aligned numeric data points to calculate correlation."
                                rVal == 1.0 -> "Perfect positive self-correlation (1.0000)."
                                rVal > 0.7 -> "Strong positive correlation (${String.format(java.util.Locale.US, "%.4f", rVal)}). As ${cell.columnA} increases, ${cell.columnB} tends to increase significantly."
                                rVal > 0.3 -> "Moderate positive correlation (${String.format(java.util.Locale.US, "%.4f", rVal)})."
                                rVal > 0.0 -> "Weak positive correlation (${String.format(java.util.Locale.US, "%.4f", rVal)})."
                                rVal < -0.7 -> "Strong negative correlation (${String.format(java.util.Locale.US, "%.4f", rVal)}). As ${cell.columnA} increases, ${cell.columnB} tends to decrease significantly."
                                rVal < -0.3 -> "Moderate negative correlation (${String.format(java.util.Locale.US, "%.4f", rVal)})."
                                rVal < 0.0 -> "Weak negative correlation (${String.format(java.util.Locale.US, "%.4f", rVal)})."
                                else -> "No linear correlation (0.0000)."
                            }
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatRegressionResult(res: MultipleLinearRegressionResult): String {
    val sb = java.lang.StringBuilder()
    sb.append("=== DETAILED REGRESSION ANALYSIS REPORT ===\n")
    sb.append("R-Squared: %.4f\n".format(res.rSquared))
    sb.append("Adjusted R-Squared: %.4f\n".format(res.adjustedRSquared))
    sb.append("F-Statistic: %.4f (df=%d, %d), p-value: %.5f\n".format(res.fStatistic, res.dfRegression, res.dfResidual, res.fPValue))
    sb.append("Shapiro-Wilk residual normality W: %.4f (p = %.5f)\n".format(res.shapiroWilkStat, res.shapiroWilkW))
    sb.append("Levene's Homoscedasticity F: %.4f (p = %.5f)\n".format(res.leveneStat, res.levenePValue))
    sb.append("\nModel Coefficients:\n")
    sb.append("%-20s | %-12s | %-12s | %-10s | %-10s | %-8s\n".format("Variable", "Estimate", "Std.Error", "t-val", "p-val", "VIF"))
    sb.append("-".repeat(80) + "\n")
    res.coefficients.forEach { coef ->
        sb.append("%-20s | %-12.4f | %-12.4f | %-10.3f | %-10.4f | %-8s\n".format(
            coef.variable,
            coef.estimate,
            coef.stdError,
            coef.tValue,
            coef.pValue,
            coef.vif?.let { "%.2f".format(it) } ?: "-"
        ))
    }
    return sb.toString()
}

fun formatAnovaResult(res: MixedAnovaResult): String {
    val sb = java.lang.StringBuilder()
    sb.append("=== DETAILED MIXED ANOVA ANALYSIS REPORT ===\n")
    sb.append("Levene's Sphericity Assumption F: %.4f (p = %.5f)\n".format(res.leveneStat, res.levenePValue))
    sb.append("\nAnalysis of Variance (Type III SS):\n")
    sb.append("%-25s | %-12s | %-5s | %-12s | %-10s | %-10s | %-8s\n".format("Source", "SS", "df", "MS", "F-stat", "p-val", "pes"))
    sb.append("-".repeat(95) + "\n")
    res.effects.forEach { effect ->
        sb.append("%-25s | %-12.3f | %-5d | %-12.3f | %-10s | %-10s | %-8s\n".format(
            effect.source,
            effect.sumOfSquares,
            effect.df,
            effect.meanSquare,
            if (effect.fStatistic > 0) "%.3f".format(effect.fStatistic) else "-",
            if (effect.fStatistic > 0) "%.4f".format(effect.pValue) else "-",
            if (effect.fStatistic > 0) "%.3f".format(effect.partialEtaSquared) else "-"
        ))
    }
    return sb.toString()
}

fun formatChiSquareResult(res: ChiSquareResult): String {
    val sb = java.lang.StringBuilder()
    sb.append("=== DETAILED CHI-SQUARE INDEPENDENCE REPORT ===\n")
    sb.append("Chi-Square Stat: %.4f | df: %d | p-value: %.5f\n".format(res.chiSquareStat, res.df, res.pValue))
    sb.append("Cramer's V (Effect size): %.4f\n".format(res.cramersV))
    sb.append("\nContingency Table (Observed [Expected]):\n")
    sb.append("%-20s".format("Row \\ Col"))
    res.colCategories.forEach { col ->
        sb.append(" | %-15s".format(col))
    }
    sb.append("\n" + "-".repeat(20 + res.colCategories.size * 18) + "\n")
    res.rowCategories.forEach { rowCat ->
        sb.append("%-20s".format(rowCat))
        res.colCategories.forEach { colCat ->
            val cell = res.cells.find { it.rowValue == rowCat && it.colValue == colCat }
            val cellText = if (cell != null) "%d [%.1f]".format(cell.observed, cell.expected) else "-"
            sb.append(" | %-15s".format(cellText))
        }
        sb.append("\n")
    }
    return sb.toString()
}

