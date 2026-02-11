package com.medapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.medapp.reminder.ReminderConstants
import com.medapp.ui.main.AppContainer
import com.medapp.ui.main.MainTab
import com.medapp.ui.main.MedAppRoot

class MainActivity : ComponentActivity() {
    private var selectedTab by mutableStateOf(MainTab.TODAY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = resolveInitialTab(intent)

        val container = AppContainer(applicationContext)
        setContent {
            MaterialTheme {
                MedAppRoot(container, initialTab = selectedTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedTab = resolveInitialTab(intent)
    }

    private fun resolveInitialTab(intent: Intent?): MainTab {
        return if (intent?.getStringExtra(ReminderConstants.EXTRA_OPEN_TAB) == ReminderConstants.TAB_TODAY) {
            MainTab.TODAY
        } else {
            MainTab.TODAY
        }
    }
}
