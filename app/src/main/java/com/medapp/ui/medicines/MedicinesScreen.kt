package com.medapp.ui.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.medapp.data.model.EqualDistanceRule

@Composable
fun MedicinesScreen(modifier: Modifier = Modifier, viewModel: MedicinesViewModel) {
    val medicines by viewModel.medicines.collectAsState()
    var showWizard by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showWizard = true }) { Text("+") }
        }
    ) { padding ->
        if (medicines.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("No medicines yet.")
                Text("Tap + to create your first medicine.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(medicines) { med ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(med.name)
                            Text("Target dose: ${med.targetDoseMg} mg")
                            Text(if (med.isActive) "Active" else "Inactive")
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        CreateMedicineWizard(
            onDismiss = { showWizard = false },
            onComplete = { input -> viewModel.createMedicine(input) { showWizard = false } },
            onCalculate = { target, pillDose, divisible, tieRule ->
                viewModel.calculateDose(target, pillDose, divisible, tieRule)
            },
            loadTieRule = { viewModel.loadTieRule() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMedicineWizard(
    onDismiss: () -> Unit,
    onComplete: (MedicinesViewModel.WizardInput) -> Unit,
    onCalculate: (Int, Int, Boolean, EqualDistanceRule) -> com.medapp.domain.usecase.DoseCalculationUseCase.Result,
    loadTieRule: suspend () -> EqualDistanceRule
) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var targetDose by remember { mutableStateOf("500") }
    var pillDose by remember { mutableStateOf("500") }
    var divisibleHalf by remember { mutableStateOf(false) }
    var pillsInPack by remember { mutableStateOf("30") }
    var purchaseLink by remember { mutableStateOf("") }
    var warnWhenEnding by remember { mutableStateOf(true) }
    var scheduleType by remember { mutableStateOf("FOOD_SLEEP") }
    var anchor by remember { mutableStateOf("BREAKFAST_TIME") }
    var intervalHours by remember { mutableStateOf("8") }
    var firstDoseTime by remember { mutableStateOf("08:00") }
    var durationType by remember { mutableStateOf("DAYS") }
    var durationDays by remember { mutableStateOf("30") }
    var totalPillsToTake by remember { mutableStateOf("60") }
    var courseDays by remember { mutableStateOf("10") }
    var restDays by remember { mutableStateOf("5") }
    var cyclesCount by remember { mutableStateOf("") }
    var tieRule by remember { mutableStateOf(EqualDistanceRule.PREFER_HIGHER) }

    LaunchedEffect(Unit) {
        tieRule = loadTieRule()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create medicine (step $step/6)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (step) {
                    1 -> OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    2 -> OutlinedTextField(value = targetDose, onValueChange = { targetDose = it.filter(Char::isDigit) }, label = { Text("Target dose mg") })
                    3 -> {
                        OutlinedTextField(value = pillDose, onValueChange = { pillDose = it.filter(Char::isDigit) }, label = { Text("Pill dose mg") })
                        Row { Checkbox(checked = divisibleHalf, onCheckedChange = { divisibleHalf = it }); Text("Divisible by half") }
                        OutlinedTextField(value = pillsInPack, onValueChange = { pillsInPack = it }, label = { Text("Pills in pack") })
                        OutlinedTextField(value = purchaseLink, onValueChange = { purchaseLink = it }, label = { Text("Purchase link") })
                        Row { Switch(checked = warnWhenEnding, onCheckedChange = { warnWhenEnding = it }); Text("Warn when ending") }
                        val result = onCalculate(targetDose.toIntOrNull() ?: 1, pillDose.toIntOrNull() ?: 1, divisibleHalf, tieRule)
                        Text("Planned count: ${result.pillCount}, real dose: ${result.realDoseMg} mg")
                        if (result.showDeviationAlert) Text("⚠️ Dose deviation above 10%")
                    }
                    4 -> {
                        Row { Switch(checked = scheduleType == "EVERY_N_HOURS", onCheckedChange = { scheduleType = if (it) "EVERY_N_HOURS" else "FOOD_SLEEP" }); Text(scheduleType) }
                        if (scheduleType == "EVERY_N_HOURS") {
                            OutlinedTextField(value = intervalHours, onValueChange = { intervalHours = it.filter(Char::isDigit) }, label = { Text("Interval hours") })
                            OutlinedTextField(value = firstDoseTime, onValueChange = { firstDoseTime = it }, label = { Text("First dose time HH:mm") })
                        } else {
                            OutlinedTextField(value = anchor, onValueChange = { anchor = it }, label = { Text("Anchor") })
                        }
                    }
                    5 -> {
                        Row { Switch(checked = durationType != "DAYS", onCheckedChange = { durationType = if (it) "PILLS_COUNT" else "DAYS" }); Text(durationType) }
                        when (durationType) {
                            "DAYS" -> OutlinedTextField(value = durationDays, onValueChange = { durationDays = it.filter(Char::isDigit) }, label = { Text("Duration days") })
                            "PILLS_COUNT" -> {
                                OutlinedTextField(value = totalPillsToTake, onValueChange = { totalPillsToTake = it }, label = { Text("Total pills") })
                                TextButton(onClick = { durationType = "COURSES" }) { Text("Switch to COURSES") }
                            }
                            "COURSES" -> {
                                OutlinedTextField(value = courseDays, onValueChange = { courseDays = it.filter(Char::isDigit) }, label = { Text("Course days") })
                                OutlinedTextField(value = restDays, onValueChange = { restDays = it.filter(Char::isDigit) }, label = { Text("Rest days") })
                                OutlinedTextField(value = cyclesCount, onValueChange = { cyclesCount = it.filter(Char::isDigit) }, label = { Text("Cycles count (optional)") })
                            }
                        }
                    }
                    6 -> Text("Confirm creating medicine $name?")
                }
            }
        },
        confirmButton = {
            if (step < 6) {
                Button(onClick = { step++ }) { Text("Next") }
            } else {
                Button(onClick = {
                    onComplete(
                        MedicinesViewModel.WizardInput(
                            name = name,
                            targetDoseMg = targetDose.toIntOrNull() ?: 0,
                            pillDoseMg = pillDose.toIntOrNull() ?: 0,
                            divisibleHalf = divisibleHalf,
                            pillsInPack = pillsInPack.toDoubleOrNull() ?: 0.0,
                            purchaseLink = purchaseLink,
                            warnWhenEnding = warnWhenEnding,
                            scheduleType = scheduleType,
                            anchors = if (scheduleType == "FOOD_SLEEP") listOf(anchor) else emptyList(),
                            intervalHours = if (scheduleType == "EVERY_N_HOURS") intervalHours.toIntOrNull() else null,
                            firstDoseTime = if (scheduleType == "EVERY_N_HOURS") firstDoseTime else null,
                            durationType = durationType,
                            durationDays = if (durationType == "DAYS") durationDays.toIntOrNull() else null,
                            totalPillsToTake = if (durationType == "PILLS_COUNT") totalPillsToTake.toDoubleOrNull() else null,
                            courseDays = if (durationType == "COURSES") courseDays.toIntOrNull() else null,
                            restDays = if (durationType == "COURSES") restDays.toIntOrNull() else null,
                            cyclesCount = if (durationType == "COURSES") cyclesCount.toIntOrNull() else null
                        )
                    )
                }) { Text("Create") }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step > 1) {
                    TextButton(onClick = { step-- }) { Text("Back") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
