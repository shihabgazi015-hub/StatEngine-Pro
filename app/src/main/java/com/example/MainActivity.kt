package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.db.AppDatabase
import com.example.ui.StatEngineApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StatViewModel
import com.example.viewmodel.StatViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    private val viewModel: StatViewModel by viewModels {
        StatViewModelFactory(database.historyDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                StatEngineApp(viewModel = viewModel)
            }
        }
    }
}
