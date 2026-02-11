package com.medapp.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(modifier: Modifier = Modifier, viewModel: TodayViewModel) {
    val intakes by viewModel.intakes.collectAsState()
    if (intakes.isEmpty()) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("No intakes planned for today.")
        }
        return
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(intakes) { intake ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(intake.medicineName)
                    val time = Instant.ofEpochMilli(intake.plannedAt).atZone(ZoneId.systemDefault()).toLocalTime()
                    Text("Planned at ${formatter.format(time)} • ${intake.realDoseMgPlanned} mg")
                    AssistChip(onClick = {}, label = { Text(intake.status) })
                    if (intake.status == "PLANNED") {
                        Button(onClick = { viewModel.markTaken(intake.id) }) { Text("Taken") }
                    }
                }
            }
        }
    }
}
