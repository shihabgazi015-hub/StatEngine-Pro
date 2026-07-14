package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Done

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ColumnType
import com.example.model.Dataset
import com.example.model.ColumnDescriptiveStats
import com.example.model.MissingValueHandling
import com.example.model.MissingValueItem
import com.example.viewmodel.StatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestionView(viewModel: StatViewModel) {
    val context = LocalContext.current
    val selectedDataset by viewModel.selectedDataset.collectAsStateWithLifecycle()
    val isCleaning by viewModel.isCleaning.collectAsStateWithLifecycle()
    val descriptiveStats by viewModel.descriptiveStats.collectAsStateWithLifecycle()
    val detectedOutliers by viewModel.detectedOutliers.collectAsStateWithLifecycle()

    
    val dropMissing by viewModel.dropMissing.collectAsStateWithLifecycle()
    val filterOutliers by viewModel.filterOutliers.collectAsStateWithLifecycle()
    val detectedMissingValues by viewModel.detectedMissingValues.collectAsStateWithLifecycle()
    val missingValueHandling by viewModel.missingValueHandling.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.parseAndLoadFile(context, uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("ingestion_view"),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section: File Ingest
        item {
            Text(
                text = "Data Ingestion Workstation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Load academic, research, or clinical dataset in CSV or Excel spreadsheet formatting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Drag & Drop Upload Zone Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { filePickerLauncher.launch("*/*") }
                    .testTag("upload_zone"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Dataset Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Import CSV or Excel Dataset",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accepts .csv, .xlsx spreadsheet tables. Tap to browse device storage.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Preloaded Laboratory Templates
        item {
            Column {
                Text(
                    text = "Standard Analytical Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    viewModel.templates.forEach { template ->
                        val isSelected = selectedDataset?.name == template.name
                        Card(
                            onClick = { viewModel.loadTemplate(template) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .width(220.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .testTag("template_${template.name.replace(" ", "_")}"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Dataset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = template.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${template.rows.size} observations | ${template.columns.size} variables",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Data Cleaning & Preprocessing Suite
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
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Data Cleaning Utility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Missing values diagnostics box
                    if (detectedMissingValues.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Identified ${detectedMissingValues.size} Missing Value(s)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Missing data points per column:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val groupedByCol = detectedMissingValues.groupBy { it.columnName }
                                groupedByCol.forEach { (colName, items) ->
                                    Text(
                                        text = "• $colName: ${items.size} missing value(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Pristine",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dataset is pristine! No missing values identified.",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Handling Strategy options
                    Text(
                        "Missing Values Imputation Strategy",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.missingValueHandling.value = MissingValueHandling.DROP_ROWS }
                        ) {
                            RadioButton(
                                selected = missingValueHandling == MissingValueHandling.DROP_ROWS,
                                onClick = { viewModel.missingValueHandling.value = MissingValueHandling.DROP_ROWS },
                                modifier = Modifier.testTag("missing_strategy_drop")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Drop rows with missing values", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Removes any observations that contain missing/empty fields.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.missingValueHandling.value = MissingValueHandling.IMPUTE_MEAN }
                        ) {
                            RadioButton(
                                selected = missingValueHandling == MissingValueHandling.IMPUTE_MEAN,
                                onClick = { viewModel.missingValueHandling.value = MissingValueHandling.IMPUTE_MEAN },
                                modifier = Modifier.testTag("missing_strategy_mean")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Impute missing with mean value", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Replaces missing numeric values with the column's mean.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.missingValueHandling.value = MissingValueHandling.IMPUTE_MEDIAN }
                        ) {
                            RadioButton(
                                selected = missingValueHandling == MissingValueHandling.IMPUTE_MEDIAN,
                                onClick = { viewModel.missingValueHandling.value = MissingValueHandling.IMPUTE_MEDIAN },
                                modifier = Modifier.testTag("missing_strategy_median")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Impute missing with median value", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Replaces missing numeric values with the column's median.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = filterOutliers,
                            onCheckedChange = { viewModel.filterOutliers.value = it },
                            modifier = Modifier.testTag("filter_outliers_checkbox")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Filter Statistical Outliers",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Removes extreme sample records where values fall beyond standard Z-scores > ±3.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.cleanActiveDataset() },
                        enabled = !isCleaning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_cleaning_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Apply Preprocessing & Clean Dataset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Descriptive Column Statistics Section
        selectedDataset?.let { dataset ->
            if (descriptiveStats.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dataset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Descriptive Column Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                        ) {
                            // Headers Row
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(vertical = 12.dp)
                            ) {
                                val headers = listOf("Column Name", "Type", "Mean", "Median", "Std Dev", "Min", "Max", "Count", "Missing")
                                val columnWidths = listOf(160.dp, 100.dp, 100.dp, 100.dp, 100.dp, 100.dp, 100.dp, 90.dp, 90.dp)
                                
                                headers.forEachIndexed { index, header ->
                                    Text(
                                        text = header,
                                        modifier = Modifier
                                            .width(columnWidths[index])
                                            .padding(horizontal = 16.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Data Rows
                            descriptiveStats.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val columnWidths = listOf(160.dp, 100.dp, 100.dp, 100.dp, 100.dp, 100.dp, 100.dp, 90.dp, 90.dp)
                                    
                                    // 1. Column Name
                                    Text(
                                        text = stat.columnName,
                                        modifier = Modifier
                                            .width(columnWidths[0])
                                            .padding(horizontal = 16.dp),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // 2. Type
                                    Text(
                                        text = stat.columnType.name,
                                        modifier = Modifier
                                            .width(columnWidths[1])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (stat.columnType == ColumnType.NUMERIC) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary
                                    )

                                    // 3. Mean
                                    Text(
                                        text = stat.mean?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "N/A",
                                        modifier = Modifier
                                            .width(columnWidths[2])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 4. Median
                                    Text(
                                        text = stat.median?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "N/A",
                                        modifier = Modifier
                                            .width(columnWidths[3])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 5. Std Dev
                                    Text(
                                        text = stat.stdDev?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "N/A",
                                        modifier = Modifier
                                            .width(columnWidths[4])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 6. Min
                                    Text(
                                        text = stat.min?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "N/A",
                                        modifier = Modifier
                                            .width(columnWidths[5])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 7. Max
                                    Text(
                                        text = stat.max?.let { String.format(java.util.Locale.US, "%.4f", it) } ?: "N/A",
                                        modifier = Modifier
                                            .width(columnWidths[6])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 8. Count
                                    Text(
                                        text = stat.count.toString(),
                                        modifier = Modifier
                                            .width(columnWidths[7])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // 9. Missing
                                    Text(
                                        text = stat.missingCount.toString(),
                                        modifier = Modifier
                                            .width(columnWidths[8])
                                            .padding(horizontal = 16.dp),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (stat.missingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Automated Outlier Detection Summary Section
        selectedDataset?.let { dataset ->
            if (detectedOutliers.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated IQR Outlier Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Detected ${detectedOutliers.size} anomalous data point(s) using the Interquartile Range (IQR) method with standard 1.5x threshold fences.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val groupedOutliers = detectedOutliers.groupBy { it.columnName }
                            groupedOutliers.forEach { (colName, items) ->
                                val sampleVals = items.take(5).map { String.format(java.util.Locale.US, "%.2f", it.value) }.joinToString(", ")
                                val remaining = items.size - 5
                                val displayVals = if (remaining > 0) "$sampleVals (and $remaining more)" else sampleVals
                                
                                val firstItem = items.first()
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Column {
                                        Text(
                                            text = "$colName: ${items.size} anomaly/anomalies found.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fences: [${String.format(java.util.Locale.US, "%.2f", firstItem.lowerFence)} to ${String.format(java.util.Locale.US, "%.2f", firstItem.upperFence)}]. Values: $displayVals",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (dataset.columns.any { dataset.columnTypes[it] == ColumnType.NUMERIC }) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Automated IQR Outlier Analysis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Checked continuous numeric columns using the Interquartile Range (IQR) method. 0 statistical outliers / anomalies detected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Data Preview Table Section

        selectedDataset?.let { dataset ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Data Preview (${dataset.rows.size} rows)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Table Grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                    ) {
                        // Headers Row
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 12.dp)
                        ) {
                            dataset.columns.forEach { col ->
                                val colType = dataset.columnTypes[col] ?: ColumnType.CATEGORICAL
                                Column(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = col,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = colType.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (colType == ColumnType.NUMERIC) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Data Rows
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(dataset.rows) { rowIndex, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    dataset.columns.forEach { col ->
                                        val isOutlier = detectedOutliers.any { it.columnName == col && it.rowIndex == rowIndex }
                                        
                                        Box(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .padding(horizontal = 4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isOutlier) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    if (isOutlier) 1.dp else 0.dp,
                                                    if (isOutlier) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isOutlier) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "Outlier",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = row[col] ?: "-",
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = if (isOutlier) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isOutlier) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
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
    }
}
