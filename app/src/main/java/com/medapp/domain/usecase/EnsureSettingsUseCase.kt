package com.medapp.domain.usecase

import com.medapp.data.dao.SettingsDao
import com.medapp.data.entity.SettingsEntity
import com.medapp.data.model.EqualDistanceRule
import com.medapp.data.model.Language

class EnsureSettingsUseCase(
    private val settingsDao: SettingsDao
) {
    suspend operator fun invoke(): SettingsEntity {
        val existing = settingsDao.getById()
        if (existing != null) return existing

        val defaults = SettingsEntity(
            wakeTime = "07:00",
            breakfastTime = "08:00",
            lunchTime = "13:00",
            dinnerTime = "19:00",
            sleepTime = "23:00",
            equalDistanceRule = EqualDistanceRule.PREFER_HIGHER.name,
            lowStockWarningEnabled = true,
            lowStockWarningDays = 30,
            language = Language.EN.name
        )
        settingsDao.upsert(defaults)
        return defaults
    }
}
