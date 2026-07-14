package com.abdessalem.worktracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.domain.model.WorkSettings
import com.abdessalem.worktracker.ui.navigation.WorkTrackerAppRoot
import com.abdessalem.worktracker.ui.theme.WorkTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = WorkSettings())
            WorkTrackerTheme(
                darkTheme = settings.darkMode,
                dynamicColor = settings.dynamicColorEnabled
            ) {
                WorkTrackerAppRoot()
            }
        }
    }
}
