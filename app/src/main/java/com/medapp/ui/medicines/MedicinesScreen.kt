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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medapp.data.model.EqualDistanceRule
import com.medapp.domain.util.TimeParser

private val anchorOptions = listOf(
    "AFTER_WAKE",
    "BEFORE_BREAKFAST",
    "BREAKFAST_TIME",
    "BEFORE_LUNCH",
    "LUNCH_TIME",
    "BEFORE_DINNER",
    "DINNER_TIME",
    "BEFORE_SLEEP"
)

private fun anchorLabel(code: String): String = when (code) {
    "AFTER_WAKE" -> "After wake"
    "BEFORE_BREAKFAST" -> "30 min before breakfast"
    "BREAKFAST_TIME" -> "Breakfast time"
    "BEFORE_LUNCH" -> "30 min before lunch"
    "LUNCH_TIME" -> "Lunch time"
    "BEFORE_DINNER" -> "30 min before dinner"
    "DINNER_TIME" -> "Dinner time"
    "BEFORE_SLEEP" -> "Before sleep"
    else -> code
}

@Composable
fun MedicinesScreen(modifier: Modifier = Modifier, viewModel: MedicinesViewModel, onOpenLowStock: () -> Unit = {}) {
    val medicines by viewModel.medicines.collectAsState()
    var showWizard by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showWizard = true }) { Text("+") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(onClick = onOpenLowStock) { Text("Low stock") }

        if (medicines.isEmpty()) {
                Text("No medicines yet.")
                Text("Tap + to create your first medicine.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    var intakesPerDay by remember { mutableStateOf(1) }
    var anchors by remember { mutableStateOf(listOf("BREAKFAST_TIME")) }
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

    val tieRuleText = if (tieRule == EqualDistanceRule.PREFER_HIGHER) "Prefer higher dose on tie" else "Prefer lower dose on tie"

    val stepError = when (step) {
        1 -> if (name.isBlank()) "Medicine name cannot be blank" else null
        2 -> if ((targetDose.toIntOrNull() ?: 0) <= 0) "Target dose must be greater than 0" else null
        3 -> when {
            (pillDose.toIntOrNull() ?: 0) <= 0 -> "Pill dose must be greater than 0"
            (pillsInPack.toDoubleOrNull() ?: 0.0) <= 0.0 -> "Pills in pack must be greater than 0"
            purchaseLink.isNotBlank() && !(purchaseLink.startsWith("http://") || purchaseLink.startsWith("https://")) -> "Purchase link must start with http:// or https://"
            else -> null
        }
        4 -> when {
            scheduleType == "EVERY_N_HOURS" && (intervalHours.toIntOrNull() ?: 0) <= 0 -> "Interval hours must be greater than 0"
            scheduleType == "EVERY_N_HOURS" && !TimeParser.isValidTime(firstDoseTime) -> "First dose time must be valid HH:mm"
            scheduleType == "FOOD_SLEEP" && anchors.size != intakesPerDay -> "Pick anchors for all intakes"
            scheduleType == "FOOD_SLEEP" && anchors.toSet().size != anchors.size -> "Duplicate anchors are not allowed"
            else -> null
        }
        5 -> when (durationType) {
            "DAYS" -> if ((durationDays.toIntOrNull() ?: 0) <= 0) "Duration days must be greater than 0" else null
            "PILLS_COUNT" -> if ((totalPillsToTake.toDoubleOrNull() ?: 0.0) <= 0.0) "Total pills must be greater than 0" else null
            "COURSES" -> when {
                (courseDays.toIntOrNull() ?: 0) <= 0 -> "Course days must be greater than 0"
                (restDays.toIntOrNull() ?: -1) < 0 -> "Rest days cannot be negative"
                cyclesCount.isNotBlank() && (cyclesCount.toIntOrNull() ?: 0) <= 0 -> "Cycles count must be greater than 0"
                else -> null
            }
            else -> null
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create medicine (step $step/6)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (step) {
                    1 -> OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, isError = stepError != null)
                    2 -> OutlinedTextField(value = targetDose, onValueChange = { targetDose = it.filter(Char::isDigit) }, label = { Text("Target dose mg") }, isError = stepError != null)
                    3 -> {
                        OutlinedTextField(value = pillDose, onValueChange = { pillDose = it.filter(Char::isDigit) }, label = { Text("Pill dose mg") }, isError = stepError?.contains("Pill dose") == true)
                        Row { Checkbox(checked = divisibleHalf, onCheckedChange = { divisibleHalf = it }); Text("Divisible by half") }
                        OutlinedTextField(value = pillsInPack, onValueChange = { pillsInPack = it }, label = { Text("Pills in pack") }, isError = stepError?.contains("pack") == true)
                        OutlinedTextField(value = purchaseLink, onValueChange = { purchaseLink = it }, label = { Text("Purchase link") }, isError = stepError?.contains("http") == true)
                        Row { Switch(checked = warnWhenEnding, onCheckedChange = { warnWhenEnding = it }); Text("Warn when ending") }
                        Text("Tie rule: $tieRuleText")
                        val result = onCalculate(targetDose.toIntOrNull() ?: 1, pillDose.toIntOrNull() ?: 1, divisibleHalf, tieRule)
                        Text("Planned count: ${result.pillCount}, real dose: ${result.realDoseMg} mg")
                        if (result.showDeviationAlert) Text("Recommended dose differs by more than 10% from target.")
                    }
                    4 -> {
                        Row {
                            Switch(checked = scheduleType == "EVERY_N_HOURS", onCheckedChange = { scheduleType = if (it) "EVERY_N_HOURS" else "FOOD_SLEEP" })
                            Text(if (scheduleType == "EVERY_N_HOURS") "Every N hours" else "Food/sleep anchors")
                        }
                        if (scheduleType == "EVERY_N_HOURS") {
                            OutlinedTextField(value = intervalHours, onValueChange = { intervalHours = it.filter(Char::isDigit) }, label = { Text("Interval hours") }, isError = stepError?.contains("Interval") == true)
                            OutlinedTextField(value = firstDoseTime, onValueChange = { firstDoseTime = TimeParser.normalizeTimeInput(it) }, label = { Text("First dose time HH:mm") }, isError = stepError?.contains("First dose") == true)
                        } else {
                            OutlinedTextField(value = intakesPerDay.toString(), onValueChange = {
                                val parsed = it.filter(Char::isDigit).toIntOrNull() ?: 1
                                intakesPerDay = parsed.coerceIn(1, 6)
                                anchors = anchors.take(intakesPerDay).ifEmpty { listOf("BREAKFAST_TIME") }
                                if (anchors.size < intakesPerDay) anchors = anchors + List(intakesPerDay - anchors.size) { "BREAKFAST_TIME" }
                            }, label = { Text("How many times per day? (1..6)") })

                            repeat(intakesPerDay) { index ->
                                AnchorDropdown(
                                    label = "Intake ${index + 1} anchor",
                                    selected = anchors.getOrElse(index) { "BREAKFAST_TIME" },
                                    disallowed = anchors.filterIndexed { i, _ -> i != index }.toSet(),
                                    onSelected = { selected ->
                                        val updated = anchors.toMutableList()
                                        while (updated.size < intakesPerDay) updated += "BREAKFAST_TIME"
                                        updated[index] = selected
                                        anchors = updated
                                    }
                                )
                            }
                        }
                    }
                    5 -> {
                        Row { Switch(checked = durationType != "DAYS", onCheckedChange = { durationType = if (it) "PILLS_COUNT" else "DAYS" }); Text(durationType) }
                        when (durationType) {
                            "DAYS" -> OutlinedTextField(value = durationDays, onValueChange = { durationDays = it.filter(Char::isDigit) }, label = { Text("Duration days") }, isError = stepError != null)
                            "PILLS_COUNT" -> {
                                OutlinedTextField(value = totalPillsToTake, onValueChange = { totalPillsToTake = it }, label = { Text("Total pills") }, isError = stepError != null)
                                TextButton(onClick = { durationType = "COURSES" }) { Text("Switch to COURSES") }
                            }
                            "COURSES" -> {
                                OutlinedTextField(value = courseDays, onValueChange = { courseDays = it.filter(Char::isDigit) }, label = { Text("Course days") }, isError = stepError?.contains("Course") == true)
                                OutlinedTextField(value = restDays, onValueChange = { restDays = it.filter(Char::isDigit) }, label = { Text("Rest days") }, isError = stepError?.contains("Rest") == true)
                                OutlinedTextField(value = cyclesCount, onValueChange = { cyclesCount = it.filter(Char::isDigit) }, label = { Text("Cycles count (optional)") }, isError = stepError?.contains("Cycles") == true)
                            }
                        }
                    }
                    6 -> Text("Confirm creating medicine $name?")
                }
                stepError?.let { Text(it) }
            }
        },
        confirmButton = {
            if (step < 6) {
                Button(enabled = stepError == null, onClick = { step++ }) { Text("Next") }
            } else {
                Button(onClick = {
                    onComplete(
                        MedicinesViewModel.WizardInput(
                            name = name.trim(),
                            targetDoseMg = targetDose.toIntOrNull() ?: 0,
                            pillDoseMg = pillDose.toIntOrNull() ?: 0,
                            divisibleHalf = divisibleHalf,
                            pillsInPack = pillsInPack.toDoubleOrNull() ?: 0.0,
                            purchaseLink = purchaseLink,
                            warnWhenEnding = warnWhenEnding,
                            scheduleType = scheduleType,
                            intakesPerDay = if (scheduleType == "FOOD_SLEEP") intakesPerDay else 1,
                            anchors = if (scheduleType == "FOOD_SLEEP") anchors.take(intakesPerDay) else emptyList(),
                            intervalHours = if (scheduleType == "EVERY_N_HOURS") intervalHours.toIntOrNull() else null,
                            firstDoseTime = if (scheduleType == "EVERY_N_HOURS") TimeParser.normalizeTimeInput(firstDoseTime) else null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnchorDropdown(
    label: String,
    selected: String,
    disallowed: Set<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = anchorLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            anchorOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(anchorLabel(option)) },
                    enabled = option == selected || !disallowed.contains(option),
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
