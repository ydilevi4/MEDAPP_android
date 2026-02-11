package com.medapp.ui.lowstock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medapp.domain.model.LowStockMedicineItem
import com.medapp.domain.usecase.ConfirmPackagePurchaseUseCase
import kotlin.math.ceil

@Composable
fun LowStockScreen(
    modifier: Modifier = Modifier,
    viewModel: LowStockViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    var updateFor by remember { mutableStateOf<LowStockMedicineItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Low stock")
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            if (items.isEmpty()) {
                Text("No medicines currently running low.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.medicineName)
                                Text("Remaining pills: ${item.pillsRemaining}")
                                Text("Estimated days: ${ceil(item.estimatedDaysRemaining)}")
                                Button(onClick = { updateFor = item }) {
                                    Text("Confirm purchase")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    updateFor?.let { item ->
        UpdatePackageDialog(
            medicineName = item.medicineName,
            medicineId = item.medicineId,
            onDismiss = { updateFor = null },
            onSave = { params ->
                viewModel.confirmPurchase(params)
                updateFor = null
            }
        )
    }
}

@Composable
private fun UpdatePackageDialog(
    medicineName: String,
    medicineId: String,
    onDismiss: () -> Unit,
    onSave: (ConfirmPackagePurchaseUseCase.Params) -> Unit
) {
    var hasOldLeft by remember { mutableStateOf(false) }
    var oldPillsLeft by remember { mutableStateOf("0") }
    var pillDoseMg by remember { mutableStateOf("500") }
    var divisibleHalf by remember { mutableStateOf(false) }
    var pillsInPack by remember { mutableStateOf("30") }
    var purchaseLink by remember { mutableStateOf("") }
    var warnWhenEnding by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Package - $medicineName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Do you have old pills left?")
                Row {
                    Checkbox(checked = hasOldLeft, onCheckedChange = { hasOldLeft = it })
                    Text(if (hasOldLeft) "Yes" else "No")
                }
                if (hasOldLeft) {
                    OutlinedTextField(value = oldPillsLeft, onValueChange = { oldPillsLeft = it }, label = { Text("Old pills left") })
                }
                OutlinedTextField(value = pillDoseMg, onValueChange = { pillDoseMg = it }, label = { Text("pillDoseMg") })
                Row {
                    Checkbox(checked = divisibleHalf, onCheckedChange = { divisibleHalf = it })
                    Text("divisibleHalf")
                }
                OutlinedTextField(value = pillsInPack, onValueChange = { pillsInPack = it }, label = { Text("pillsInPack") })
                OutlinedTextField(value = purchaseLink, onValueChange = { purchaseLink = it }, label = { Text("purchaseLink") })
                Row {
                    Checkbox(checked = warnWhenEnding, onCheckedChange = { warnWhenEnding = it })
                    Text("warnWhenEnding")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val params = ConfirmPackagePurchaseUseCase.Params(
                    medicineId = medicineId,
                    oldPillsLeft = if (hasOldLeft) (oldPillsLeft.toDoubleOrNull() ?: 0.0) else 0.0,
                    pillDoseMg = (pillDoseMg.toIntOrNull() ?: 500).coerceAtLeast(1),
                    divisibleHalf = divisibleHalf,
                    pillsInPack = (pillsInPack.toDoubleOrNull() ?: 30.0).coerceAtLeast(1.0),
                    purchaseLink = purchaseLink.takeIf { it.isNotBlank() },
                    warnWhenEnding = warnWhenEnding
                )
                onSave(params)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
