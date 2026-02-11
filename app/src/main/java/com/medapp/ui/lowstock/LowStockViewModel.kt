package com.medapp.ui.lowstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medapp.domain.model.LowStockMedicineItem
import com.medapp.domain.usecase.ConfirmPackagePurchaseUseCase
import com.medapp.domain.usecase.EnsureSettingsUseCase
import com.medapp.domain.usecase.LowStockEstimator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LowStockViewModel(
    private val ensureSettingsUseCase: EnsureSettingsUseCase,
    private val estimator: LowStockEstimator,
    private val confirmPackagePurchaseUseCase: ConfirmPackagePurchaseUseCase
) : ViewModel() {
    private val _items = MutableStateFlow<List<LowStockMedicineItem>>(emptyList())
    val items: StateFlow<List<LowStockMedicineItem>> = _items.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            val settings = ensureSettingsUseCase()
            _items.value = estimator.estimate(settings)
        }
    }

    fun confirmPurchase(params: ConfirmPackagePurchaseUseCase.Params) {
        viewModelScope.launch {
            confirmPackagePurchaseUseCase(params)
            _events.emit("Package updated")
            refresh()
        }
    }

    class Factory(
        private val ensureSettingsUseCase: EnsureSettingsUseCase,
        private val estimator: LowStockEstimator,
        private val confirmPackagePurchaseUseCase: ConfirmPackagePurchaseUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LowStockViewModel(ensureSettingsUseCase, estimator, confirmPackagePurchaseUseCase) as T
        }
    }
}
