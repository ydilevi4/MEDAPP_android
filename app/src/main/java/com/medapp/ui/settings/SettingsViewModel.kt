package com.medapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medapp.data.dao.SettingsDao
import com.medapp.data.entity.SettingsEntity
import com.medapp.domain.usecase.EnsureSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val ensureSettingsUseCase: EnsureSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<SettingsEntity?> = settingsDao.observeById()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { ensureSettingsUseCase() }
    }

    fun save(updated: SettingsEntity) {
        viewModelScope.launch { settingsDao.upsert(updated) }
    }

    class Factory(
        private val settingsDao: SettingsDao,
        private val ensureSettingsUseCase: EnsureSettingsUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDao, ensureSettingsUseCase) as T
        }
    }
}
