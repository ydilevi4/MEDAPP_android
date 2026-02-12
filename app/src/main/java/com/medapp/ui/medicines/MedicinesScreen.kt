package com.medapp.ui.medicines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medapp.data.entity.SettingsEntity
import com.medapp.data.model.EqualDistanceRule
import com.medapp.domain.usecase.IntakeScheduleResolver
import com.medapp.domain.util.TimeParser
import java.time.LocalTime

enum class BasePeriod { AFTER_WAKE, BREAKFAST, LUNCH, DINNER, BEFORE_SLEEP }
enum class MealRelation { BEFORE, DURING, AFTER }
enum class IntakeScheduleMode { ANCHOR, EXACT_TIME }

data class IntakeRow(
    val base: BasePeriod = BasePeriod.BREAKFAST,
    val relation: MealRelation = MealRelation.DURING,
    val exactTime: String = "08:00"
)

@Composable
fun MedicinesScreen(modifier: Modifier = Modifier, viewModel: MedicinesViewModel, onOpenLowStock: () -> Unit = {}) {
    val medicines by viewModel.medicines.collectAsState()
    var showWizard by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = { FloatingActionButton(onClick = { showWizard = true }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Button(onClick = onOpenLowStock) { Text("Low stock") }
            if (medicines.isEmpty()) {
                Text("No medicines yet.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
                ) {
                    items(medicines) { med ->
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(med.name)
                                Text("Target dose: ${med.targetDoseMg} mg")
                                med.notes?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 2) }
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
            onCalculate = { target, pillDose, divisible, tieRule -> viewModel.calculateDose(target, pillDose, divisible, tieRule) },
            loadTieRule = { viewModel.loadTieRule() },
            loadSettings = { viewModel.loadSettings() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMedicineWizard(
    onDismiss: () -> Unit,
    onComplete: (MedicinesViewModel.WizardInput) -> Unit,
    onCalculate: (Int, Int, Boolean, EqualDistanceRule) -> com.medapp.domain.usecase.DoseCalculationUseCase.Result,
    loadTieRule: suspend () -> EqualDistanceRule,
    loadSettings: suspend () -> SettingsEntity
) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var targetDose by remember { mutableStateOf("500") }
    var pillDose by remember { mutableStateOf("500") }
    var divisibleHalf by remember { mutableStateOf(false) }
    var pillsInPack by remember { mutableStateOf("30") }
    var scheduleType by remember { mutableStateOf("FOOD_SLEEP") }
    var intakeScheduleMode by remember { mutableStateOf(IntakeScheduleMode.ANCHOR) }
    var intakesPerDayText by remember { mutableStateOf("1") }
    var intakeRows by remember { mutableStateOf(listOf(IntakeRow())) }
    var intervalHours by remember { mutableStateOf("8") }
    var firstDoseTime by remember { mutableStateOf("08:00") }
    var limitType by remember { mutableStateOf("DAYS") }
    var repeating by remember { mutableStateOf(false) }
    var durationDays by remember { mutableStateOf("30") }
    var totalPillsToTake by remember { mutableStateOf("60") }
    var courseDays by remember { mutableStateOf("10") }
    var restDays by remember { mutableStateOf("5") }
    var cyclesCount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tieRule by remember { mutableStateOf(EqualDistanceRule.PREFER_HIGHER) }
    var settings by remember { mutableStateOf<SettingsEntity?>(null) }

    LaunchedEffect(Unit) {
        tieRule = loadTieRule()
        settings = loadSettings()
    }

    val intakesPerDay = intakesPerDayText.toIntOrNull()
    val timesError = when {
        scheduleType != "FOOD_SLEEP" -> null
        intakesPerDay == null -> "Times per day is required"
        intakesPerDay !in 1..12 -> "Times per day must be 1..12"
        else -> null
    }

    val currentRows = if (intakesPerDay != null && intakesPerDay in 1..12) intakeRows.take(intakesPerDay) else emptyList()
    val exactTimeFormatError = if (scheduleType == "FOOD_SLEEP" && intakeScheduleMode == IntakeScheduleMode.EXACT_TIME && currentRows.any { !TimeParser.isValidTime(it.exactTime) }) {
        "Each exact time must be valid HH:mm"
    } else null

    val duplicateTimesError = when {
        scheduleType != "FOOD_SLEEP" -> null
        intakeScheduleMode == IntakeScheduleMode.EXACT_TIME -> {
            val normalized = currentRows.mapNotNull { if (TimeParser.isValidTime(it.exactTime)) TimeParser.normalizeTimeInput(it.exactTime) else null }
            if (normalized.size != normalized.toSet().size) "Duplicate exact times are not allowed" else null
        }
        settings == null -> null
        else -> {
            val anchors = currentRows.map { rowToAnchor(it, IntakeScheduleMode.ANCHOR) }
            if (anchors.size != anchors.toSet().size) "Duplicate anchors are not allowed" else null
        }
    }

    val stepError = when (step) {
        1 -> if (name.isBlank()) "Medicine name cannot be blank" else null
        2 -> if ((targetDose.toIntOrNull() ?: 0) <= 0) "Target dose must be greater than 0" else null
        3 -> if ((pillDose.toIntOrNull() ?: 0) <= 0 || (pillsInPack.toDoubleOrNull() ?: 0.0) <= 0.0) "Package values must be valid" else null
        4 -> when {
            scheduleType == "EVERY_N_HOURS" && (intervalHours.toIntOrNull() ?: 0) <= 0 -> "Interval hours must be greater than 0"
            scheduleType == "EVERY_N_HOURS" && !TimeParser.isValidTime(firstDoseTime) -> "First dose time must be valid HH:mm"
            timesError != null -> timesError
            exactTimeFormatError != null -> exactTimeFormatError
            duplicateTimesError != null -> duplicateTimesError
            else -> null
        }
        5 -> when {
            repeating && (courseDays.toIntOrNull() ?: 0) <= 0 -> "Intake phase length must be > 0"
            repeating && (restDays.toIntOrNull() ?: 0) < 0 -> "Break phase length must be >= 0"
            !repeating && limitType == "DAYS" && (durationDays.toIntOrNull() ?: 0) <= 0 -> "Total days must be > 0"
            !repeating && limitType == "PILLS_COUNT" && (totalPillsToTake.toDoubleOrNull() ?: 0.0) <= 0.0 -> "Total pills must be > 0"
            else -> null
        }
        else -> null
    }

    LaunchedEffect(intakesPerDayText, scheduleType, intakeScheduleMode, settings, intakeRows) {
        val n = intakesPerDayText.toIntOrNull() ?: return@LaunchedEffect
        if (scheduleType != "FOOD_SLEEP" || n !in 1..12) return@LaunchedEffect

        var normalized = intakeRows.take(n)
        if (normalized.size < n) normalized = normalized + List(n - normalized.size) { IntakeRow() }

        intakeRows = if (intakeScheduleMode == IntakeScheduleMode.ANCHOR && settings != null) {
            normalized.sortedBy { resolveRowTime(it, settings!!) ?: LocalTime.MAX }
        } else {
            normalized
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create medicine (step $step/6)") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (step) {
                    1 -> OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine name") })
                    2 -> OutlinedTextField(value = targetDose, onValueChange = { targetDose = it.filter(Char::isDigit) }, label = { Text("Target dose mg") })
                    3 -> {
                        OutlinedTextField(value = pillDose, onValueChange = { pillDose = it.filter(Char::isDigit) }, label = { Text("Pill dose mg") })
                        OutlinedTextField(value = pillsInPack, onValueChange = { pillsInPack = it }, label = { Text("Pills in pack") })
                    }

                    4 -> {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Switch(
                                checked = scheduleType == "EVERY_N_HOURS",
                                onCheckedChange = { scheduleType = if (it) "EVERY_N_HOURS" else "FOOD_SLEEP" }
                            )
                            Text(if (scheduleType == "EVERY_N_HOURS") "Every N hours" else "By daily intakes")
                        }

                        if (scheduleType == "EVERY_N_HOURS") {
                            OutlinedTextField(value = intervalHours, onValueChange = { intervalHours = it.filter(Char::isDigit) }, label = { Text("Interval hours") })
                            OutlinedTextField(value = firstDoseTime, onValueChange = { firstDoseTime = TimeParser.normalizeTimeInput(it) }, label = { Text("First dose HH:mm") })
                        } else {
                            Text("Schedule mode")
                            Row {
                                RadioButton(
                                    selected = intakeScheduleMode == IntakeScheduleMode.ANCHOR,
                                    onClick = { intakeScheduleMode = IntakeScheduleMode.ANCHOR }
                                )
                                Text("Anchor (food/sleep)")
                            }
                            Row {
                                RadioButton(
                                    selected = intakeScheduleMode == IntakeScheduleMode.EXACT_TIME,
                                    onClick = { intakeScheduleMode = IntakeScheduleMode.EXACT_TIME }
                                )
                                Text("Exact time")
                            }

                            OutlinedTextField(
                                value = intakesPerDayText,
                                onValueChange = { intakesPerDayText = it.filter(Char::isDigit) },
                                label = { Text("How many times per day? (1..12)") },
                                isError = timesError != null
                            )

                            repeat((intakesPerDay ?: 0).coerceIn(0, 12)) { idx ->
                                IntakeRowEditor(
                                    row = intakeRows.getOrElse(idx) { IntakeRow() },
                                    mode = intakeScheduleMode,
                                    showInlineExactTimeError = intakeScheduleMode == IntakeScheduleMode.EXACT_TIME
                                ) {
                                    val up = intakeRows.toMutableList()
                                    while (up.size <= idx) up += IntakeRow()
                                    up[idx] = it
                                    intakeRows = up
                                }
                            }
                        }
                    }

                    5 -> {
                        Text("Course plan")
                        Text("How is the course limited?")
                        Row { RadioButton(selected = limitType == "DAYS", onClick = { limitType = "DAYS" }); Text("By days") }
                        Row { RadioButton(selected = limitType == "PILLS_COUNT", onClick = { limitType = "PILLS_COUNT" }); Text("By total pills") }
                        Text("Is it repeating?")
                        Row { RadioButton(selected = !repeating, onClick = { repeating = false }); Text("One-time course") }
                        Row { RadioButton(selected = repeating, onClick = { repeating = true }); Text("Repeating course") }
                        if (repeating) {
                            OutlinedTextField(value = courseDays, onValueChange = { courseDays = it.filter(Char::isDigit) }, label = { Text("Intake phase length (days)") })
                            OutlinedTextField(value = restDays, onValueChange = { restDays = it.filter(Char::isDigit) }, label = { Text("Break phase length (days)") })
                            OutlinedTextField(value = cyclesCount, onValueChange = { cyclesCount = it.filter(Char::isDigit) }, label = { Text("Number of cycles (optional)") })
                            if (limitType == "DAYS") OutlinedTextField(value = durationDays, onValueChange = { durationDays = it.filter(Char::isDigit) }, label = { Text("Total days horizon (optional)") })
                            if (limitType == "PILLS_COUNT") OutlinedTextField(value = totalPillsToTake, onValueChange = { totalPillsToTake = it }, label = { Text("Total pills to take") })
                        } else {
                            if (limitType == "DAYS") OutlinedTextField(value = durationDays, onValueChange = { durationDays = it.filter(Char::isDigit) }, label = { Text("Total days") })
                            if (limitType == "PILLS_COUNT") OutlinedTextField(value = totalPillsToTake, onValueChange = { totalPillsToTake = it }, label = { Text("Total pills to take") })
                        }
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Comment / Notes") })
                    }

                    6 -> Text("Summary: $name, ${if (repeating) "repeating" else "one-time"}, limit ${if (limitType == "DAYS") "days" else "pills"}")
                }
                stepError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (step < 6) {
                Button(enabled = stepError == null, onClick = { step++ }) { Text("Next") }
            } else {
                Button(onClick = {
                    val n = intakesPerDayText.toIntOrNull() ?: 1
                    val currentSettings = settings ?: return@Button
                    val finalRows = intakeRows.take(n)
                    val sortedRows = when {
                        scheduleType != "FOOD_SLEEP" -> finalRows
                        intakeScheduleMode == IntakeScheduleMode.EXACT_TIME -> finalRows.sortedBy {
                            val parts = TimeParser.normalizeTimeInput(it.exactTime).split(":")
                            if (parts.size == 2 && TimeParser.isValidTime(it.exactTime)) LocalTime.of(parts[0].toInt(), parts[1].toInt()) else LocalTime.MAX
                        }
                        else -> finalRows.sortedBy { resolveRowTime(it, currentSettings) ?: LocalTime.MAX }
                    }

                    onComplete(
                        MedicinesViewModel.WizardInput(
                            name = name.trim(),
                            targetDoseMg = targetDose.toIntOrNull() ?: 0,
                            pillDoseMg = pillDose.toIntOrNull() ?: 0,
                            divisibleHalf = divisibleHalf,
                            pillsInPack = pillsInPack.toDoubleOrNull() ?: 0.0,
                            purchaseLink = "",
                            warnWhenEnding = true,
                            scheduleType = scheduleType,
                            intakesPerDay = if (scheduleType == "FOOD_SLEEP") n else 1,
                            anchors = if (scheduleType == "FOOD_SLEEP") sortedRows.map { rowToAnchor(it, intakeScheduleMode) } else emptyList(),
                            intervalHours = if (scheduleType == "EVERY_N_HOURS") intervalHours.toIntOrNull() else null,
                            firstDoseTime = if (scheduleType == "EVERY_N_HOURS") TimeParser.normalizeTimeInput(firstDoseTime) else null,
                            durationType = if (repeating) "COURSES" else limitType,
                            durationDays = durationDays.toIntOrNull(),
                            totalPillsToTake = totalPillsToTake.toDoubleOrNull(),
                            courseDays = if (repeating) courseDays.toIntOrNull() else null,
                            restDays = if (repeating) restDays.toIntOrNull() else null,
                            cyclesCount = if (repeating) cyclesCount.toIntOrNull() else null,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    )
                }) { Text("Create") }
            }
        },
        dismissButton = { Row { if (step > 1) TextButton(onClick = { step-- }) { Text("Back") }; TextButton(onClick = onDismiss) { Text("Cancel") } } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntakeRowEditor(
    row: IntakeRow,
    mode: IntakeScheduleMode,
    showInlineExactTimeError: Boolean,
    onChange: (IntakeRow) -> Unit
) {
    if (mode == IntakeScheduleMode.EXACT_TIME) {
        val isError = showInlineExactTimeError && !TimeParser.isValidTime(row.exactTime)
        OutlinedTextField(
            value = row.exactTime,
            onValueChange = { onChange(row.copy(exactTime = TimeParser.normalizeTimeInput(it))) },
            label = { Text("Exact time HH:mm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = { if (isError) Text("Time must be HH:mm") }
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = row.base.name.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Base period") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BasePeriod.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name.replace('_', ' ')) },
                    onClick = {
                        onChange(row.copy(base = item))
                        expanded = false
                    }
                )
            }
        }
    }

    if (row.base in listOf(BasePeriod.BREAKFAST, BasePeriod.LUNCH, BasePeriod.DINNER)) {
        Text("Meal relation")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MealRelation.entries.forEach { relation ->
                OutlinedButton(
                    onClick = { onChange(row.copy(relation = relation)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (row.relation == relation) "[${relation.name}]" else relation.name)
                }
            }
        }
    }
}

private fun rowToAnchor(row: IntakeRow, mode: IntakeScheduleMode): String =
    if (mode == IntakeScheduleMode.EXACT_TIME) {
        IntakeScheduleResolver.customAnchor(row.exactTime)
    } else {
        when (row.base) {
            BasePeriod.AFTER_WAKE -> "AFTER_WAKE"
            BasePeriod.BEFORE_SLEEP -> "BEFORE_SLEEP"
            BasePeriod.BREAKFAST -> when (row.relation) {
                MealRelation.BEFORE -> "BEFORE_BREAKFAST"
                MealRelation.DURING -> "BREAKFAST_TIME"
                MealRelation.AFTER -> "BREAKFAST_TIME"
            }
            BasePeriod.LUNCH -> when (row.relation) {
                MealRelation.BEFORE -> "BEFORE_LUNCH"
                MealRelation.DURING -> "LUNCH_TIME"
                MealRelation.AFTER -> "LUNCH_TIME"
            }
            BasePeriod.DINNER -> when (row.relation) {
                MealRelation.BEFORE -> "BEFORE_DINNER"
                MealRelation.DURING -> "DINNER_TIME"
                MealRelation.AFTER -> "DINNER_TIME"
            }
        }
    }

private fun resolveRowTime(row: IntakeRow, settings: SettingsEntity): LocalTime? =
    IntakeScheduleResolver.resolveAnchorTime(rowToAnchor(row, IntakeScheduleMode.ANCHOR), settings)
