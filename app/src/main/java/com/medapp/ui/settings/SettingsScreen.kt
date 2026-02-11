package com.medapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.medapp.data.entity.SettingsEntity

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val state = settings ?: run {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) { Text("Loading settings...") }
        return
    }

    var wake by remember(state) { mutableStateOf(state.wakeTime) }
    var breakfast by remember(state) { mutableStateOf(state.breakfastTime) }
    var lunch by remember(state) { mutableStateOf(state.lunchTime) }
    var dinner by remember(state) { mutableStateOf(state.dinnerTime) }
    var sleep by remember(state) { mutableStateOf(state.sleepTime) }
    var rule by remember(state) { mutableStateOf(state.equalDistanceRule) }
    var lowStockEnabled by remember(state) { mutableStateOf(state.lowStockWarningEnabled) }
    var lowStockDays by remember(state) { mutableFloatStateOf(state.lowStockWarningDays.toFloat()) }
    var language by remember(state) { mutableStateOf(state.language) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Base times")
        OutlinedTextField(value = wake, onValueChange = { wake = it }, label = { Text("Wake") })
        OutlinedTextField(value = breakfast, onValueChange = { breakfast = it }, label = { Text("Breakfast") })
        OutlinedTextField(value = lunch, onValueChange = { lunch = it }, label = { Text("Lunch") })
        OutlinedTextField(value = dinner, onValueChange = { dinner = it }, label = { Text("Dinner") })
        OutlinedTextField(value = sleep, onValueChange = { sleep = it }, label = { Text("Sleep") })

        Text("Equal distance rule: $rule")
        Switch(checked = rule == "PREFER_HIGHER", onCheckedChange = { rule = if (it) "PREFER_HIGHER" else "PREFER_LOWER" })

        Text("Low stock warning")
        Switch(checked = lowStockEnabled, onCheckedChange = { lowStockEnabled = it })
        Text("Days: ${lowStockDays.toInt()}")
        Slider(value = lowStockDays, onValueChange = { lowStockDays = it }, valueRange = 7f..90f)

        Text("Language: $language")
        Switch(checked = language == "EN", onCheckedChange = { language = if (it) "EN" else "RU" })

        Button(onClick = {
            viewModel.save(
                SettingsEntity(
                    wakeTime = wake,
                    breakfastTime = breakfast,
                    lunchTime = lunch,
                    dinnerTime = dinner,
                    sleepTime = sleep,
                    equalDistanceRule = rule,
                    lowStockWarningEnabled = lowStockEnabled,
                    lowStockWarningDays = lowStockDays.toInt(),
                    language = language
                )
            )
        }) { Text("Save") }
    }
}
