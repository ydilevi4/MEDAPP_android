package com.medapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medapp.ui.medicines.MedicinesScreen
import com.medapp.ui.medicines.MedicinesViewModel
import com.medapp.ui.settings.SettingsScreen
import com.medapp.ui.settings.SettingsViewModel
import com.medapp.ui.today.TodayScreen
import com.medapp.ui.today.TodayViewModel
import androidx.compose.runtime.collectAsState

enum class MainTab(val title: String, val icon: String) {
    TODAY("Today", "🗓️"),
    MEDICINES("Medicines", "💊"),
    SETTINGS("Settings", "⚙️")
}

@Composable
fun MedAppRoot(container: AppContainer) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
    var pendingTab by rememberSaveable { mutableStateOf<MainTab?>(null) }

    val medicinesViewModel: MedicinesViewModel = viewModel(
        factory = MedicinesViewModel.Factory(
            container.createMedicineUseCase,
            container.generateIntakesUseCase,
            container.ensureSettingsUseCase,
            container.doseUseCase,
            container.database.medicineDao()
        )
    )
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.Factory(
            container.database.intakeDao(),
            container.markIntakeCompletedUseCase
        )
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            container.database.settingsDao(),
            container.ensureSettingsUseCase,
            container.generateIntakesUseCase
        )
    )
    val settingsUi by settingsViewModel.uiState.collectAsState()

    if (pendingTab != null) {
        AlertDialog(
            onDismissRequest = { pendingTab = null },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved changes in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.discardChanges()
                    selectedTab = pendingTab ?: selectedTab
                    pendingTab = null
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { pendingTab = null }) { Text("Stay") } }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = {
                            if (tab != selectedTab && selectedTab == MainTab.SETTINGS && settingsUi.isDirty) {
                                pendingTab = tab
                            } else if (tab != selectedTab) {
                                selectedTab = tab
                            }
                        },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            MainTab.TODAY -> TodayScreen(modifier = Modifier.padding(padding), viewModel = todayViewModel)
            MainTab.MEDICINES -> MedicinesScreen(modifier = Modifier.padding(padding), viewModel = medicinesViewModel)
            MainTab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(padding), viewModel = settingsViewModel)
        }
    }
}
