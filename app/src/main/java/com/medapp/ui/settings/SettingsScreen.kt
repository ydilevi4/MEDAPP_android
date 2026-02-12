package com.medapp.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val editable = uiState.editable
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connectGoogle(result.data)
        } else {
            viewModel.onGoogleSignInFailed()
        }
    }

    if (uiState.showSavedMessage) {
        LaunchedEffect(uiState.showSavedMessage) {
            snackbarHostState.showSnackbar("Settings saved")
            viewModel.dismissSavedMessage()
        }
    }

    uiState.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    BackHandler(enabled = uiState.isDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved changes in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardChanges()
                    showDiscardDialog = false
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            }
        )
    }

    if (uiState.isLoading || editable == null) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) { Text("Loading settings...") }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = uiState.canSave,
                onClick = { viewModel.save() }
            ) { Text("Save") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Google")
            if (editable.googleAccountEmail == null) {
                Button(
                    enabled = !uiState.isGoogleLoading,
                    onClick = { googleLauncher.launch(viewModel.googleSignInIntent()) }
                ) { Text("Connect Google") }
            } else {
                Text("Connected: ${editable.googleAccountEmail}")
                Button(enabled = !uiState.isGoogleLoading, onClick = { viewModel.disconnectGoogle() }) {
                    Text("Disconnect")
                }
            }
            Text(if (editable.googleTasksListId == null) "Tasks list not created" else "Tasks list connected")

            Text("Base times")
            OutlinedTextField(
                value = editable.wakeTime,
                onValueChange = { viewModel.updateTime("wake", it) },
                label = { Text("Wake") },
                isError = uiState.timeErrors.containsKey("wake"),
                supportingText = { uiState.timeErrors["wake"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = editable.breakfastTime,
                onValueChange = { viewModel.updateTime("breakfast", it) },
                label = { Text("Breakfast") },
                isError = uiState.timeErrors.containsKey("breakfast"),
                supportingText = { uiState.timeErrors["breakfast"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = editable.lunchTime,
                onValueChange = { viewModel.updateTime("lunch", it) },
                label = { Text("Lunch") },
                isError = uiState.timeErrors.containsKey("lunch"),
                supportingText = { uiState.timeErrors["lunch"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = editable.dinnerTime,
                onValueChange = { viewModel.updateTime("dinner", it) },
                label = { Text("Dinner") },
                isError = uiState.timeErrors.containsKey("dinner"),
                supportingText = { uiState.timeErrors["dinner"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = editable.sleepTime,
                onValueChange = { viewModel.updateTime("sleep", it) },
                label = { Text("Sleep") },
                isError = uiState.timeErrors.containsKey("sleep"),
                supportingText = { uiState.timeErrors["sleep"]?.let { Text(it) } }
            )

            val ruleText = if (editable.equalDistanceRule == "PREFER_HIGHER") {
                "Prefer higher dose on tie"
            } else {
                "Prefer lower dose on tie"
            }
            Text("Equal distance rule: $ruleText")
            Switch(
                checked = editable.equalDistanceRule == "PREFER_HIGHER",
                onCheckedChange = { viewModel.updateRule(if (it) "PREFER_HIGHER" else "PREFER_LOWER") }
            )

            Text("Low stock warning")
            Switch(
                checked = editable.lowStockWarningEnabled,
                onCheckedChange = { viewModel.updateLowStockEnabled(it) }
            )
            Text("Days: ${editable.lowStockWarningDays}")
            Slider(
                value = editable.lowStockWarningDays.toFloat(),
                onValueChange = { viewModel.updateLowStockDays(it.toInt()) },
                valueRange = 7f..90f
            )

            Text("Language: ${editable.language}")
            Switch(
                checked = editable.language == "EN",
                onCheckedChange = { viewModel.updateLanguage(if (it) "EN" else "RU") }
            )
        }
    }
}
