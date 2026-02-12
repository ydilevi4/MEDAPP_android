package com.medapp.data.model

data class MedicineListItem(
    val id: String,
    val name: String,
    val targetDoseMg: Int,
    val notes: String?,
    val isActive: Boolean
)
