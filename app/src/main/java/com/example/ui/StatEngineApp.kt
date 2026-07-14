package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.model.ModelType
import com.example.viewmodel.StatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatEngineApp(viewModel: StatViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val selectedDataset by viewModel.selectedDataset.collectAsStateWithLifecycle()
    val currentUser by AuthManager.currentUser.collectAsStateWithLifecycle()

    var showAuthDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isFilterSidebarOpen by remember { mutableStateOf(false) }
    var helpTerm by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Persistent Sidebar / Navigation Rail
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(0.dp))
                    .testTag("sidebar_navigation"),
                header = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "StatEngine Pro Logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                content = {
                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        selected = activeTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = "Data Ingestion") },
                        label = { Text("Ingest", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_ingest")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationRailItem(
                        selected = activeTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Analysis Settings") },
                        label = { Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_analyze")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationRailItem(
                        selected = activeTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Report & Plots") },
                        label = { Text("Report", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_report")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // User profile button in Sidebar
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { showAuthDialog = true }
                            .testTag("profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentUser?.isGuest == true) Icons.Default.AccountCircle else Icons.Default.VerifiedUser,
                            contentDescription = "User Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            )

            // Collapsible Filter Sidebar next to Navigation Rail
            AnimatedVisibility(
                visible = isFilterSidebarOpen,
                enter = androidx.compose.animation.expandHorizontally() + fadeIn(),
                exit = androidx.compose.animation.shrinkHorizontally() + fadeOut()
            ) {
                FilterSidebarView(viewModel)
            }

            // 2. Main content container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Top Global Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "StatEngine Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = selectedDataset?.name ?: "No Dataset Loaded",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    actions = {
                        // Filter Sidebar Toggle Button
                        IconButton(
                            onClick = { isFilterSidebarOpen = !isFilterSidebarOpen },
                            modifier = Modifier.testTag("filter_sidebar_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Toggle Filter Sidebar",
                                tint = if (isFilterSidebarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick help button
                        IconButton(onClick = {
                            helpTerm = "General Overview"
                            showHelpDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = "Quick Help"
                            )
                        }

                        // Theme switch toggle
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // Sub-view screens with fade animation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeTab == 0,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        IngestionView(viewModel)
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeTab == 1,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        AnalysisView(viewModel)
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeTab == 2,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        ReportView(viewModel, onTriggerHelp = { term ->
                            helpTerm = term
                            showHelpDialog = true
                        })
                    }
                }
            }
        }

        // --- Account Management / Authentication Dialog ---
        if (showAuthDialog) {
            Dialog(onDismissRequest = { showAuthDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentUser?.isGuest == true) "Secure Cloud Sync" else "Authenticated Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentUser?.isGuest == true) 
                                "Sign in with your Google account to securely persist and sync your statistical laboratory sessions to Firestore."
                                else "Logged in as ${currentUser?.displayName} (${currentUser?.email}). Your sessions are synchronized automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (currentUser?.isGuest == true) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        AuthManager.signInWithGoogle(context)
                                        showAuthDialog = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_signin_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google")
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        AuthManager.signOut(context)
                                        showAuthDialog = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signout_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign Out of Session")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showAuthDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // --- Quick Help Term Tooltip Dialog ---
        if (showHelpDialog) {
            Dialog(onDismissRequest = { showHelpDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = helpTerm,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val description = when (helpTerm) {
                            "VIF" -> "Variance Inflation Factor (VIF) measures the severity of multicollinearity in multiple linear regression. A VIF value > 5 or 10 suggests that the independent variable is highly collinear with other variables, inflating coefficient standard errors."
                            "p-value" -> "The probability value used in hypothesis testing. A p-value <= 0.05 indicates statistical significance, prompting the rejection of the null hypothesis in favor of the alternative hypothesis."
                            "Levene's test" -> "Levene's Test of Homogeneity evaluates whether variances across different groups are equal. If p <= 0.05, the assumption of homoscedasticity is violated, suggesting that standard ANOVA results should be treated with caution, or robust standard errors applied."
                            "Homoscedasticity" -> "The assumption that the residuals/errors of a regression model have constant variance across all levels of independent variables. Heteroscedasticity violates standard OLS modeling assumptions, requiring robust standard errors (HC1) to correct."
                            else -> "Welcome to StatEngine Pro, your modern statistical laboratory workstation. Upload datasets in CSV format, perform cleaning filters (outliers, missing values), select advanced statistical diagnostics (Regression, ANOVA, Chi-Square), run calculations, and export high-fidelity academic-ready APA 7th Edition write-ups compiled dynamically via Gemini."
                        }

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showHelpDialog = false },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Got it")
                        }
                    }
                }
            }
        }
    }
}
