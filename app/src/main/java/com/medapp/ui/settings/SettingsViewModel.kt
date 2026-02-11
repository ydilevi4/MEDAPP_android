package com.medapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medapp.data.dao.SettingsDao
import com.medapp.data.entity.SettingsEntity
import com.medapp.domain.usecase.EnsureSettingsUseCase
import com.medapp.domain.usecase.GenerateIntakesUseCase
import com.medapp.domain.util.TimeParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val ensureSettingsUseCase: EnsureSettingsUseCase,
    private val generateIntakesUseCase: GenerateIntakesUseCase
) : ViewModel() {

    data class EditableSettings(
        val wakeTime: String,
        val breakfastTime: String,
        val lunchTime: String,
        val dinnerTime: String,
        val sleepTime: String,
        val equalDistanceRule: String,
        val lowStockWarningEnabled: Boolean,
        val lowStockWarningDays: Int,
        val language: String
    ) {
        companion object {
            fun from(entity: SettingsEntity): EditableSettings = EditableSettings(
                wakeTime = entity.wakeTime,
                breakfastTime = entity.breakfastTime,
                lunchTime = entity.lunchTime,
                dinnerTime = entity.dinnerTime,
                sleepTime = entity.sleepTime,
                equalDistanceRule = entity.equalDistanceRule,
                lowStockWarningEnabled = entity.lowStockWarningEnabled,
                lowStockWarningDays = entity.lowStockWarningDays,
                language = entity.language
            )
        }

        fun toEntity(): SettingsEntity = SettingsEntity(
            wakeTime = wakeTime,
            breakfastTime = breakfastTime,
            lunchTime = lunchTime,
            dinnerTime = dinnerTime,
            sleepTime = sleepTime,
            equalDistanceRule = equalDistanceRule,
            lowStockWarningEnabled = lowStockWarningEnabled,
            lowStockWarningDays = lowStockWarningDays,
            language = language
        )
    }

    data class UiState(
        val isLoading: Boolean = true,
        val persisted: SettingsEntity? = null,
        val editable: EditableSettings? = null,
        val timeErrors: Map<String, String> = emptyMap(),
        val showSavedMessage: Boolean = false
    ) {
        val isDirty: Boolean
            get() = persisted != null && editable != null && EditableSettings.from(persisted) != editable

        val canSave: Boolean
            get() = editable != null && timeErrors.isEmpty() && isDirty
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureSettingsUseCase()
            settingsDao.observeById().collect { entity ->
                _uiState.update { current ->
                    val currentEditable = current.editable
                    val nextEditable = if (currentEditable == null || !current.isDirty) {
                        entity?.let(EditableSettings.Companion::from)
                    } else {
                        currentEditable
                    }
                    current.copy(
                        isLoading = false,
                        persisted = entity,
                        editable = nextEditable,
                        timeErrors = nextEditable?.let(::validateTimeErrors) ?: emptyMap()
                    )
                }
            }
        }
    }

    fun updateTime(field: String, value: String) {
        _uiState.update { current ->
            val editable = current.editable ?: return@update current
            val normalized = TimeParser.normalizeTimeInput(value)
            val updated = when (field) {
                "wake" -> editable.copy(wakeTime = normalized)
                "breakfast" -> editable.copy(breakfastTime = normalized)
                "lunch" -> editable.copy(lunchTime = normalized)
                "dinner" -> editable.copy(dinnerTime = normalized)
                "sleep" -> editable.copy(sleepTime = normalized)
                else -> editable
            }
            current.copy(editable = updated, timeErrors = validateTimeErrors(updated), showSavedMessage = false)
        }
    }

    fun updateRule(rule: String) = updateEditable { it.copy(equalDistanceRule = rule) }
    fun updateLowStockEnabled(enabled: Boolean) = updateEditable { it.copy(lowStockWarningEnabled = enabled) }
    fun updateLowStockDays(days: Int) = updateEditable { it.copy(lowStockWarningDays = days.coerceIn(7, 90)) }
    fun updateLanguage(language: String) = updateEditable { it.copy(language = language) }

    fun dismissSavedMessage() {
        _uiState.update { it.copy(showSavedMessage = false) }
    }

    fun discardChanges() {
        _uiState.update { current ->
            val persisted = current.persisted ?: return@update current
            val editable = EditableSettings.from(persisted)
            current.copy(editable = editable, timeErrors = validateTimeErrors(editable))
        }
    }

    fun save() {
        viewModelScope.launch {
            val snapshot = uiState.first()
            val editable = snapshot.editable ?: return@launch
            if (!snapshot.canSave) return@launch

            val entity = editable.toEntity()
            settingsDao.upsert(entity)
            generateIntakesUseCase.refreshFuturePlanned()
            _uiState.update {
                it.copy(
                    persisted = entity,
                    editable = EditableSettings.from(entity),
                    timeErrors = validateTimeErrors(EditableSettings.from(entity)),
                    showSavedMessage = true
                )
            }
        }
    }

    private fun updateEditable(transform: (EditableSettings) -> EditableSettings) {
        _uiState.update { current ->
            val editable = current.editable ?: return@update current
            val updated = transform(editable)
            current.copy(editable = updated, timeErrors = validateTimeErrors(updated), showSavedMessage = false)
        }
    }

    private fun validateTimeErrors(editable: EditableSettings): Map<String, String> {
        return buildMap {
            validateAndPut("wake", editable.wakeTime)
            validateAndPut("breakfast", editable.breakfastTime)
            validateAndPut("lunch", editable.lunchTime)
            validateAndPut("dinner", editable.dinnerTime)
            validateAndPut("sleep", editable.sleepTime)
        }
    }

    private fun MutableMap<String, String>.validateAndPut(field: String, value: String) {
        if (!TimeParser.isValidTime(value)) {
            put(field, "Time must be in HH:mm format and within 00:00..23:59")
        }
    }

    class Factory(
        private val settingsDao: SettingsDao,
        private val ensureSettingsUseCase: EnsureSettingsUseCase,
        private val generateIntakesUseCase: GenerateIntakesUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDao, ensureSettingsUseCase, generateIntakesUseCase) as T
        }
    }
}
