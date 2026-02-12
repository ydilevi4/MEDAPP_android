package com.medapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medapp.domain.usecase.LowStockEstimator
import com.medapp.ui.lowstock.LowStockScreen
import com.medapp.ui.lowstock.LowStockViewModel
import com.medapp.ui.medicines.MedicinesScreen
import com.medapp.ui.medicines.MedicinesViewModel
import com.medapp.ui.settings.SettingsScreen
import com.medapp.ui.settings.SettingsViewModel
import com.medapp.ui.today.TodayScreen
import com.medapp.ui.today.TodayViewModel

enum class MainTab(val title: String, val icon: String) {
    TODAY("Today", "🗓️"),
    MEDICINES("Medicines", "💊"),
    SETTINGS("Settings", "⚙️")
}

@Composable
fun MedAppRoot(container: AppContainer, initialTab: MainTab = MainTab.TODAY, openLowStockEventToken: Long = 0L) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var pendingTab by rememberSaveable { mutableStateOf<MainTab?>(null) }
    var showLowStock by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    // Token is a consumable event trigger; booleans are sticky and won't retrigger after first true.
    LaunchedEffect(openLowStockEventToken) {
        if (openLowStockEventToken != 0L) showLowStock = true
    }

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
            container.generateIntakesUseCase,
            container.googleSignInManager,
            container.tasksListBootstrapUseCase,
            container.googleTasksSyncUseCase
        )
    )
    val lowStockViewModel: LowStockViewModel = viewModel(
        factory = LowStockViewModel.Factory(
            container.ensureSettingsUseCase,
            LowStockEstimator(container.database.medicineDao(), container.database.pillPackageDao(), container.database.intakeDao()),
            container.confirmPackagePurchaseUseCase
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
            MainTab.MEDICINES -> MedicinesScreen(
                modifier = Modifier.padding(padding),
                viewModel = medicinesViewModel,
                onOpenLowStock = { showLowStock = true }
            )
            MainTab.SETTINGS -> SettingsScreen(modifier = Modifier.padding(padding), viewModel = settingsViewModel)
        }
    }

    if (showLowStock) {
        LowStockScreen(viewModel = lowStockViewModel, onDismiss = { showLowStock = false })
    }
}
