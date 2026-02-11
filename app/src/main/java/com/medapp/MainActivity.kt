package com.medapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.medapp.reminder.ReminderConstants
import com.medapp.ui.main.AppContainer
import com.medapp.ui.main.MainTab
import com.medapp.ui.main.MedAppRoot

class MainActivity : ComponentActivity() {
    private var selectedTab by mutableStateOf(MainTab.TODAY)
    private var openLowStockScreen by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = resolveInitialTab(intent)
        openLowStockScreen = shouldOpenLowStock(intent)
        maybeRequestNotificationPermission()

        val container = AppContainer(applicationContext)
        setContent {
            MaterialTheme {
                MedAppRoot(
                    container = container,
                    initialTab = selectedTab,
                    openLowStockOnStart = openLowStockScreen
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedTab = resolveInitialTab(intent)
        openLowStockScreen = shouldOpenLowStock(intent)
    }

    private fun resolveInitialTab(intent: Intent?): MainTab {
        return when (intent?.getStringExtra(ReminderConstants.EXTRA_OPEN_TAB)) {
            ReminderConstants.TAB_LOW_STOCK -> MainTab.MEDICINES
            ReminderConstants.TAB_TODAY -> MainTab.TODAY
            else -> MainTab.TODAY
        }
    }

    private fun shouldOpenLowStock(intent: Intent?): Boolean {
        return intent?.getStringExtra(ReminderConstants.EXTRA_OPEN_TAB) == ReminderConstants.TAB_LOW_STOCK
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences("medapp_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("notif_permission_requested", false)) return

        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        prefs.edit().putBoolean("notif_permission_requested", true).apply()
    }
}
