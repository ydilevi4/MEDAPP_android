package com.medapp.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medapp.data.dao.IntakeDao
import com.medapp.data.model.TodayIntakeItem
import com.medapp.domain.usecase.MarkIntakeCompletedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TodayViewModel(
    intakeDao: IntakeDao,
    private val markIntakeCompletedUseCase: MarkIntakeCompletedUseCase
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()
    private val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    val intakes: StateFlow<List<TodayIntakeItem>> = intakeDao.observeTodayIntakes(dayStart, dayEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markTaken(intakeId: String) {
        viewModelScope.launch { markIntakeCompletedUseCase(intakeId) }
    }

    class Factory(
        private val intakeDao: IntakeDao,
        private val markIntakeCompletedUseCase: MarkIntakeCompletedUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(intakeDao, markIntakeCompletedUseCase) as T
        }
    }
}
