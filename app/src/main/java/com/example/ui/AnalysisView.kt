package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ColumnType
import com.example.model.ModelType
import com.example.model.SubsetTestType
import com.example.viewmodel.StatViewModel
import androidx.compose.material.icons.filled.Calculate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisView(viewModel: StatViewModel) {
    val selectedDataset by viewModel.selectedDataset.collectAsStateWithLifecycle()
    val isComputing by viewModel.isComputing.collectAsStateWithLifecycle()

    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val dependentVariable by viewModel.dependentVariable.collectAsStateWithLifecycle()
    val independentVariables by viewModel.independentVariables.collectAsStateWithLifecycle()

    val subjectColumn by viewModel.subjectColumn.collectAsStateWithLifecycle()
    val betweenSubjectsFactor by viewModel.betweenSubjectsFactor.collectAsStateWithLifecycle()
    val withinSubjectsFactors by viewModel.withinSubjectsFactors.collectAsStateWithLifecycle()

    val chiRowVariable by viewModel.chiRowVariable.collectAsStateWithLifecycle()
    val chiColVariable by viewModel.chiColVariable.collectAsStateWithLifecycle()

    val applyRobustErrors by viewModel.applyRobustErrors.collectAsStateWithLifecycle()
    val runPostHoc by viewModel.runPostHoc.collectAsStateWithLifecycle()

    var modelExpanded by remember { mutableStateOf(false) }
    var depExpanded by remember { mutableStateOf(false) }
    var subExpanded by remember { mutableStateOf(false) }
    var betweenExpanded by remember { mutableStateOf(false) }
    var chiRowExpanded by remember { mutableStateOf(false) }
    var chiColExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("analysis_view"),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Advanced Analytical Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure parameters, variable maps, and robust standard error estimations for deterministic academic modeling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (selectedDataset == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please load or select a dataset in the 'Ingest' tab before running statistics.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            val columns = selectedDataset!!.columns
            val numericCols = columns.filter { selectedDataset!!.columnTypes[it] == ColumnType.NUMERIC }
            val categoricalCols = columns.filter { selectedDataset!!.columnTypes[it] == ColumnType.CATEGORICAL }

            // Card: Model Picker
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
                                imageVector = Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Analytical Mathematical Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = modelExpanded,
                                onExpandedChange = { modelExpanded = !modelExpanded },
                                modifier = Modifier.testTag("model_type_dropdown")
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = when (selectedModel) {
                                        ModelType.REGRESSION -> "Multiple Linear Regression (OLS / HC1)"
                                        ModelType.ANOVA -> "Mixed-Design ANOVA (Within & Between Subjects)"
                                        ModelType.CHI_SQUARE -> "Chi-Square Test of Independence"
                                    },
                                    onValueChange = {},
                                    label = { Text("Model Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = modelExpanded,
                                    onDismissRequest = { modelExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Multiple Linear Regression") },
                                        onClick = {
                                            viewModel.selectedModel.value = ModelType.REGRESSION
                                            modelExpanded = false
                                        },
                                        modifier = Modifier.testTag("model_opt_regression")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Mixed-Design ANOVA") },
                                        onClick = {
                                            viewModel.selectedModel.value = ModelType.ANOVA
                                            modelExpanded = false
                                        },
                                        modifier = Modifier.testTag("model_opt_anova")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Chi-Square Test of Independence") },
                                        onClick = {
                                            viewModel.selectedModel.value = ModelType.CHI_SQUARE
                                            modelExpanded = false
                                        },
                                        modifier = Modifier.testTag("model_opt_chi")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card: Variable Mappings
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
                            text = "Variable Maps & Column Alignment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedModel) {
                            ModelType.REGRESSION -> {
                                // 1. Dependent Variable
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = depExpanded,
                                        onExpandedChange = { depExpanded = !depExpanded },
                                        modifier = Modifier.testTag("dependent_var_dropdown")
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = dependentVariable ?: "Select Dependent Variable (Y)",
                                            onValueChange = {},
                                            label = { Text("Dependent Variable (Numeric Continuous)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = depExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = depExpanded,
                                            onDismissRequest = { depExpanded = false }
                                        ) {
                                            numericCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.dependentVariable.value = col
                                                        depExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // 2. Independent Variables (Multi-select checklist)
                                Text(
                                    "Independent Variables / Predictors (Numeric Continuous)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                numericCols.filter { it != dependentVariable }.forEach { col ->
                                    val isChecked = independentVariables.contains(col)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val currentList = independentVariables.toMutableList()
                                                if (isChecked) currentList.remove(col) else currentList.add(col)
                                                viewModel.independentVariables.value = currentList
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                val currentList = independentVariables.toMutableList()
                                                if (it) currentList.add(col) else currentList.remove(col)
                                                viewModel.independentVariables.value = currentList
                                            },
                                            modifier = Modifier.testTag("ind_checkbox_$col")
                                        )
                                        Text(col, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }

                            ModelType.ANOVA -> {
                                // Subject Identifier
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = subExpanded,
                                        onExpandedChange = { subExpanded = !subExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = subjectColumn ?: "Select Subject ID",
                                            onValueChange = {},
                                            label = { Text("Subject ID Column") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = subExpanded,
                                            onDismissRequest = { subExpanded = false }
                                        ) {
                                            columns.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.subjectColumn.value = col
                                                        subExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Between-Subjects factor (Categorical group)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = betweenExpanded,
                                        onExpandedChange = { betweenExpanded = !betweenExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = betweenSubjectsFactor ?: "Select Between-Subjects Factor",
                                            onValueChange = {},
                                            label = { Text("Between-Subjects Factor (e.g. Group/Condition)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = betweenExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = betweenExpanded,
                                            onDismissRequest = { betweenExpanded = false }
                                        ) {
                                            categoricalCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.betweenSubjectsFactor.value = col
                                                        betweenExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Within-Subjects columns
                                Text(
                                    "Within-Subjects Factors (e.g. Repeated Measures over Pre, Post)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                numericCols.forEach { col ->
                                    val isChecked = withinSubjectsFactors.contains(col)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val currentList = withinSubjectsFactors.toMutableList()
                                                if (isChecked) currentList.remove(col) else currentList.add(col)
                                                viewModel.withinSubjectsFactors.value = currentList
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                val currentList = withinSubjectsFactors.toMutableList()
                                                if (it) currentList.add(col) else currentList.remove(col)
                                                viewModel.withinSubjectsFactors.value = currentList
                                            }
                                        )
                                        Text(col, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }

                            ModelType.CHI_SQUARE -> {
                                // Row Categorical Variable
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = chiRowExpanded,
                                        onExpandedChange = { chiRowExpanded = !chiRowExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = chiRowVariable ?: "Select First Variable",
                                            onValueChange = {},
                                            label = { Text("Row Categorical Variable") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chiRowExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = chiRowExpanded,
                                            onDismissRequest = { chiRowExpanded = false }
                                        ) {
                                            categoricalCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.chiRowVariable.value = col
                                                        chiRowExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Col Categorical Variable
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = chiColExpanded,
                                        onExpandedChange = { chiColExpanded = !chiColExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = chiColVariable ?: "Select Second Variable",
                                            onValueChange = {},
                                            label = { Text("Column Categorical Variable") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chiColExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = chiColExpanded,
                                            onDismissRequest = { chiColExpanded = false }
                                        ) {
                                            categoricalCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.chiColVariable.value = col
                                                        chiColExpanded = false
                                                    }
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

            // Card: Advanced parameter overrides
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
                            text = "Model Specific Parameter Overrides",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedModel == ModelType.REGRESSION) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Apply Robust Standard Errors", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Calculates White's heteroscedasticity-consistent (HC1) standard errors to protect coefficients variance integrity.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(
                                    checked = applyRobustErrors,
                                    onCheckedChange = { viewModel.applyRobustErrors.value = it },
                                    modifier = Modifier.testTag("robust_se_switch")
                                )
                            }
                        } else if (selectedModel == ModelType.ANOVA) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Run Post-hoc Corrections", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Performs Tukey's HSD and pairwise Bonferroni significance corrections to mitigate false positives.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Switch(
                                    checked = runPostHoc,
                                    onCheckedChange = { viewModel.runPostHoc.value = it }
                                )
                            }
                        } else {
                            Text(
                                "No parameter overrides necessary for the Chi-Square Independence Test. Deterministic cell expected frequencies will be calculated directly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Card: Collapsible/Dedicated Subset Hypothesis Testing Tool
            item {
                val subsetTestType by viewModel.subsetTestType.collectAsStateWithLifecycle()
                val subsetGroupCol by viewModel.subsetGroupCol.collectAsStateWithLifecycle()
                val subsetGroupA by viewModel.subsetGroupA.collectAsStateWithLifecycle()
                val subsetGroupB by viewModel.subsetGroupB.collectAsStateWithLifecycle()
                val subsetDepVar by viewModel.subsetDepVar.collectAsStateWithLifecycle()
                val subsetPairedVar1 by viewModel.subsetPairedVar1.collectAsStateWithLifecycle()
                val subsetPairedVar2 by viewModel.subsetPairedVar2.collectAsStateWithLifecycle()
                val subsetHypothesisResult by viewModel.subsetHypothesisResult.collectAsStateWithLifecycle()

                var typeExpanded by remember { mutableStateOf(false) }
                var groupExpanded by remember { mutableStateOf(false) }
                var groupAExpanded by remember { mutableStateOf(false) }
                var groupBExpanded by remember { mutableStateOf(false) }
                var depVarExpanded by remember { mutableStateOf(false) }
                var paired1Expanded by remember { mutableStateOf(false) }
                var paired2Expanded by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .testTag("subset_hypothesis_tool_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Subset Hypothesis Testing Tool",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Perform real-time Welch's t-tests, paired t-tests, or One-Way ANOVAs directly on subsets of the filtered dataset.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Type Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = typeExpanded,
                                onExpandedChange = { typeExpanded = !typeExpanded },
                                modifier = Modifier.testTag("subset_test_type_dropdown")
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = when (subsetTestType) {
                                        SubsetTestType.INDEPENDENT_T_TEST -> "Independent Welch's T-Test"
                                        SubsetTestType.PAIRED_T_TEST -> "Paired Samples T-Test"
                                        SubsetTestType.ONE_WAY_ANOVA -> "One-Way ANOVA"
                                    },
                                    onValueChange = {},
                                    label = { Text("Select Hypothesis Test") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Independent Welch's T-Test") },
                                        onClick = {
                                            viewModel.subsetTestType.value = SubsetTestType.INDEPENDENT_T_TEST
                                            typeExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Paired Samples T-Test") },
                                        onClick = {
                                            viewModel.subsetTestType.value = SubsetTestType.PAIRED_T_TEST
                                            typeExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("One-Way ANOVA") },
                                        onClick = {
                                            viewModel.subsetTestType.value = SubsetTestType.ONE_WAY_ANOVA
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Parameter inputs based on Test Type
                        when (subsetTestType) {
                            SubsetTestType.INDEPENDENT_T_TEST -> {
                                // 1. Grouping Variable (Categorical)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = groupExpanded,
                                        onExpandedChange = { groupExpanded = !groupExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = subsetGroupCol ?: "None Selected",
                                            onValueChange = {},
                                            label = { Text("Grouping Variable (Categorical)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                                            colors = OutlinedTextFieldDefaults.colors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = groupExpanded,
                                            onDismissRequest = { groupExpanded = false }
                                        ) {
                                            categoricalCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.subsetGroupCol.value = col
                                                        groupExpanded = false
                                                        // Update categories list and select first two defaults
                                                        val cats = selectedDataset!!.rows.map { it[col] ?: "" }.filter { it.isNotBlank() }.distinct().sorted()
                                                        if (cats.isNotEmpty()) {
                                                            viewModel.subsetGroupA.value = cats.first()
                                                            if (cats.size > 1) {
                                                                viewModel.subsetGroupB.value = cats[1]
                                                            } else {
                                                                viewModel.subsetGroupB.value = cats.first()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Group A and Group B Selection
                                if (!subsetGroupCol.isNullOrEmpty()) {
                                    val groupCol = subsetGroupCol!!
                                    val uniqueCats = selectedDataset!!.rows.map { it[groupCol] ?: "" }.filter { it.isNotBlank() }.distinct().sorted()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Group A Selection
                                        Box(modifier = Modifier.weight(1f)) {
                                            ExposedDropdownMenuBox(
                                                expanded = groupAExpanded,
                                                onExpandedChange = { groupAExpanded = !groupAExpanded }
                                            ) {
                                                OutlinedTextField(
                                                    readOnly = true,
                                                    value = subsetGroupA,
                                                    onValueChange = {},
                                                    label = { Text("Group A Value") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupAExpanded) },
                                                    colors = OutlinedTextFieldDefaults.colors(),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = groupAExpanded,
                                                    onDismissRequest = { groupAExpanded = false }
                                                ) {
                                                    uniqueCats.forEach { valStr ->
                                                        DropdownMenuItem(
                                                            text = { Text(valStr) },
                                                            onClick = {
                                                                viewModel.subsetGroupA.value = valStr
                                                                groupAExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Group B Selection
                                        Box(modifier = Modifier.weight(1f)) {
                                            ExposedDropdownMenuBox(
                                                expanded = groupBExpanded,
                                                onExpandedChange = { groupBExpanded = !groupBExpanded }
                                            ) {
                                                OutlinedTextField(
                                                    readOnly = true,
                                                    value = subsetGroupB,
                                                    onValueChange = {},
                                                    label = { Text("Group B Value") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupBExpanded) },
                                                    colors = OutlinedTextFieldDefaults.colors(),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = groupBExpanded,
                                                    onDismissRequest = { groupBExpanded = false }
                                                ) {
                                                    uniqueCats.forEach { valStr ->
                                                        DropdownMenuItem(
                                                            text = { Text(valStr) },
                                                            onClick = {
                                                                viewModel.subsetGroupB.value = valStr
                                                                groupBExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // Dependent Variable (Numeric)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = depVarExpanded,
                                        onExpandedChange = { depVarExpanded = !depVarExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = subsetDepVar ?: "None Selected",
                                            onValueChange = {},
                                            label = { Text("Dependent Variable (Numeric)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = depVarExpanded) },
                                            colors = OutlinedTextFieldDefaults.colors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = depVarExpanded,
                                            onDismissRequest = { depVarExpanded = false }
                                        ) {
                                            numericCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.subsetDepVar.value = col
                                                        depVarExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SubsetTestType.PAIRED_T_TEST -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Variable 1 (Numeric)
                                    Box(modifier = Modifier.weight(1f)) {
                                        ExposedDropdownMenuBox(
                                            expanded = paired1Expanded,
                                            onExpandedChange = { paired1Expanded = !paired1Expanded }
                                        ) {
                                            OutlinedTextField(
                                                readOnly = true,
                                                value = subsetPairedVar1 ?: "None Selected",
                                                onValueChange = {},
                                                label = { Text("Variable 1 (Numeric)") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paired1Expanded) },
                                                colors = OutlinedTextFieldDefaults.colors(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = paired1Expanded,
                                                onDismissRequest = { paired1Expanded = false }
                                            ) {
                                                numericCols.forEach { col ->
                                                    DropdownMenuItem(
                                                        text = { Text(col) },
                                                        onClick = {
                                                            viewModel.subsetPairedVar1.value = col
                                                            paired1Expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Variable 2 (Numeric)
                                    Box(modifier = Modifier.weight(1f)) {
                                        ExposedDropdownMenuBox(
                                            expanded = paired2Expanded,
                                            onExpandedChange = { paired2Expanded = !paired2Expanded }
                                        ) {
                                            OutlinedTextField(
                                                readOnly = true,
                                                value = subsetPairedVar2 ?: "None Selected",
                                                onValueChange = {},
                                                label = { Text("Variable 2 (Numeric)") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paired2Expanded) },
                                                colors = OutlinedTextFieldDefaults.colors(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = paired2Expanded,
                                                onDismissRequest = { paired2Expanded = false }
                                            ) {
                                                numericCols.forEach { col ->
                                                    DropdownMenuItem(
                                                        text = { Text(col) },
                                                        onClick = {
                                                            viewModel.subsetPairedVar2.value = col
                                                            paired2Expanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SubsetTestType.ONE_WAY_ANOVA -> {
                                // Grouping Variable (Categorical)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = groupExpanded,
                                        onExpandedChange = { groupExpanded = !groupExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = subsetGroupCol ?: "None Selected",
                                            onValueChange = {},
                                            label = { Text("Grouping Variable (Categorical)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                                            colors = OutlinedTextFieldDefaults.colors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = groupExpanded,
                                            onDismissRequest = { groupExpanded = false }
                                        ) {
                                            categoricalCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.subsetGroupCol.value = col
                                                        groupExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dependent Variable (Numeric)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    ExposedDropdownMenuBox(
                                        expanded = depVarExpanded,
                                        onExpandedChange = { depVarExpanded = !depVarExpanded }
                                    ) {
                                        OutlinedTextField(
                                            readOnly = true,
                                            value = subsetDepVar ?: "None Selected",
                                            onValueChange = {},
                                            label = { Text("Dependent Variable (Numeric)") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = depVarExpanded) },
                                            colors = OutlinedTextFieldDefaults.colors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = depVarExpanded,
                                            onDismissRequest = { depVarExpanded = false }
                                        ) {
                                            numericCols.forEach { col ->
                                                DropdownMenuItem(
                                                    text = { Text(col) },
                                                    onClick = {
                                                        viewModel.subsetDepVar.value = col
                                                        depVarExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Display Results dynamically in real-time
                        if (subsetHypothesisResult != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            val res = subsetHypothesisResult!!
                            if (res.errorMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = res.errorMessage,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                // Prominent Result Summary Badge
                                val isSignificant = res.pValue < 0.05
                                val badgeColor = if (isSignificant) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                val badgeTextColor = if (isSignificant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = badgeColor,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isSignificant) "SIGNIFICANT EFFECT" else "NS (NOT SIGNIFICANT)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextColor,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = String.format(java.util.Locale.US, "p = %.5f", res.pValue),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeTextColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = when (res.testType) {
                                                        SubsetTestType.INDEPENDENT_T_TEST, SubsetTestType.PAIRED_T_TEST -> "t-statistic"
                                                        SubsetTestType.ONE_WAY_ANOVA -> "F-statistic"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = String.format(java.util.Locale.US, "%.4f", res.statistic),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = "Degrees of Freedom (df)",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = String.format(java.util.Locale.US, "%.1f", res.df),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (res.meanDifference != null) {
                                                Column {
                                                    Text(
                                                        text = "Mean Diff",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = String.format(java.util.Locale.US, "%.3f", res.meanDifference),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        if (res.confidenceIntervalLower != null && res.confidenceIntervalUpper != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = badgeTextColor.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "95% Confidence Interval of Difference",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = String.format(
                                                    java.util.Locale.US,
                                                    "[%.4f to %.4f]",
                                                    res.confidenceIntervalLower,
                                                    res.confidenceIntervalUpper
                                                ),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Descriptive Subset Stats Table
                                Text(
                                    text = "SUBSET DESCRIPTIVE BREAKDOWN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    res.groupsStats.forEach { g ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = g.groupName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Sample size (N) = ${g.count}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("Mean (M)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                        Text(String.format(java.util.Locale.US, "%.3f", g.mean), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("Std Dev (SD)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                        Text(String.format(java.util.Locale.US, "%.3f", g.stdDev), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("Std Error (SEM)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                                        Text(String.format(java.util.Locale.US, "%.3f", g.sem), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Academic Interpretation
                                Text(
                                    text = "ACADEMIC EXPLANATION & APA NARRATIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = res.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Big Calculation Trigger Button
            item {
                Button(
                    onClick = { viewModel.runStatisticalModeling() },
                    enabled = !isComputing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("run_analysis_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isComputing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Computing Advanced Math...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Diagnostics & Execute Calculations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
