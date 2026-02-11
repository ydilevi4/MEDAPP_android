package com.medapp.domain.model

data class LowStockMedicineItem(
    val medicineId: String,
    val medicineName: String,
    val packageId: String,
    val pillsRemaining: Double,
    val estimatedDaysRemaining: Double
)
