package com.medapp.domain.usecase

import com.medapp.data.dao.IntakeDao
import com.medapp.data.dao.MedicineDao
import com.medapp.data.dao.PackageTransitionDao
import com.medapp.data.dao.PillPackageDao
import com.medapp.data.dao.SettingsDao
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.entity.MedicineEntity
import com.medapp.data.entity.PackageTransitionEntity
import com.medapp.data.entity.PillPackageEntity
import com.medapp.data.entity.SettingsEntity
import com.medapp.data.model.MedicineListItem
import com.medapp.reminder.OverdueIntakeNotificationItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageTransitionAccountingTest {
    @Test
    fun `completion accounting updates only old-package planned completions`() {
        val newAccounting = MarkIntakeCompletedUseCase.completionAccounting(
            targetPackageId = "new-package",
            oldPackageId = "old-package",
            plannedPillCount = 1.0
        )
        assertFalse(newAccounting.shouldUpdateOldConsumed)
        assertEquals(1.0, newAccounting.pillDelta, 0.0)

        val oldAccounting = MarkIntakeCompletedUseCase.completionAccounting(
            targetPackageId = "old-package",
            oldPackageId = "old-package",
            plannedPillCount = 1.0
        )
        assertTrue(oldAccounting.shouldUpdateOldConsumed)
        assertEquals(1.0, oldAccounting.pillDelta, 0.0)
    }

    @Test
    fun `planner requires enough old leftovers before assigning old package`() = runBlocking {
        val medicine = medicine()
        val oldPackage = pillPackage(id = "old", isCurrent = false, pillsRemaining = 0.25)
        val newPackage = pillPackage(id = "new", isCurrent = true, pillsRemaining = 20.0)

        val fakeIntakeDao = FakeIntakeDao()
        val useCase = GenerateIntakesUseCase(
            medicineDao = FakeMedicineDao(listOf(medicine)),
            intakeDao = fakeIntakeDao,
            pillPackageDao = FakePillPackageDao(mapOf(oldPackage.id to oldPackage, newPackage.id to newPackage), newPackage.id),
            packageTransitionDao = FakeTransitionDao(
                PackageTransitionEntity(
                    id = "t1",
                    medicineId = medicine.id,
                    oldPackageId = oldPackage.id,
                    newPackageId = newPackage.id,
                    oldPillsLeft = 0.25,
                    oldPillsConsumed = 0.0,
                    createdAt = System.currentTimeMillis()
                )
            ),
            doseCalculationUseCase = DoseCalculationUseCase(),
            ensureSettingsUseCase = EnsureSettingsUseCase(FakeSettingsDao(defaultSettings()))
        )

        useCase()

        val created = fakeIntakeDao.inserted
        assertTrue(created.isNotEmpty())
        assertTrue(created.all { it.pillCountPlanned >= 0.5 })
        assertTrue(created.all { it.packageIdPlanned == newPackage.id })
    }

    private fun medicine(): MedicineEntity {
        val now = System.currentTimeMillis()
        return MedicineEntity(
            id = "m1",
            name = "Med",
            targetDoseMg = 100,
            scheduleType = "FOOD_SLEEP",
            intakesPerDay = 1,
            anchorsJson = "[\"BREAKFAST_TIME\"]",
            intervalHours = null,
            firstDoseTime = null,
            durationType = "DAYS",
            durationDays = 1,
            totalPillsToTake = null,
            courseDays = null,
            restDays = null,
            cyclesCount = null,
            notes = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun pillPackage(id: String, isCurrent: Boolean, pillsRemaining: Double): PillPackageEntity {
        val now = System.currentTimeMillis()
        return PillPackageEntity(
            id = id,
            medicineId = "m1",
            pillDoseMg = 200,
            divisibleHalf = true,
            pillsInPack = 30.0,
            pillsRemaining = pillsRemaining,
            purchaseLink = null,
            warnWhenEnding = true,
            isCurrent = isCurrent,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun defaultSettings() = SettingsEntity(
        wakeTime = "07:00",
        breakfastTime = "08:00",
        lunchTime = "13:00",
        dinnerTime = "19:00",
        sleepTime = "23:00",
        equalDistanceRule = "PREFER_HIGHER",
        lowStockWarningEnabled = true,
        lowStockWarningDays = 30,
        language = "EN"
    )
}

private class FakeMedicineDao(private val meds: List<MedicineEntity>) : MedicineDao {
    override suspend fun insert(medicine: MedicineEntity) = Unit
    override fun observeMedicines(): Flow<List<MedicineListItem>> = emptyFlow()
    override suspend fun getActiveMedicines(): List<MedicineEntity> = meds
    override suspend fun getById(id: String): MedicineEntity? = meds.firstOrNull { it.id == id }
}

private class FakeIntakeDao : IntakeDao {
    val inserted = mutableListOf<IntakeEntity>()

    override suspend fun insertAll(intakes: List<IntakeEntity>) { inserted += intakes }
    override suspend fun update(intakeEntity: IntakeEntity) = Unit
    override suspend fun getById(id: String): IntakeEntity? = inserted.firstOrNull { it.id == id }
    override fun observeTodayIntakes(dayStart: Long, dayEnd: Long): Flow<List<com.medapp.data.model.TodayIntakeItem>> = emptyFlow()
    override suspend fun countForMedicineInRange(medicineId: String, dayStart: Long, dayEnd: Long): Int = 0
    override suspend fun getPlannedAtInRange(medicineId: String, fromMillis: Long, toMillis: Long): List<Long> = emptyList()
    override suspend fun getPlannedInRangeForMedicine(medicineId: String, fromMillis: Long, toMillis: Long): List<IntakeEntity> = emptyList()
    override suspend fun getPlannedFromForMedicine(medicineId: String, fromMillis: Long): List<IntakeEntity> = emptyList()
    override suspend fun getPlannedPillsSum(medicineId: String): Double = 0.0
    override suspend fun deletePlannedFrom(fromMillis: Long) = Unit
    override suspend fun getOverduePlannedIntakes(nowMillis: Long, horizonStart: Long, horizonEnd: Long): List<OverdueIntakeNotificationItem> = emptyList()
    override suspend fun getNextIntakeForMedicine(medicineId: String, plannedAt: Long): IntakeEntity? = null
    override suspend fun updateStatusById(intakeId: String, status: String, updatedAt: Long) = Unit
}

private class FakePillPackageDao(
    private val packages: Map<String, PillPackageEntity>,
    private val currentPackageId: String
) : PillPackageDao {
    override suspend fun insert(pillPackageEntity: PillPackageEntity) = Unit
    override suspend fun update(pillPackageEntity: PillPackageEntity) = Unit
    override suspend fun getCurrentForMedicine(medicineId: String): PillPackageEntity? = packages[currentPackageId]
    override suspend fun getForMedicine(medicineId: String): List<PillPackageEntity> = packages.values.toList()
    override suspend fun getAllCurrent(): List<PillPackageEntity> = packages.values.filter { it.isCurrent }
    override suspend fun getById(id: String): PillPackageEntity? = packages[id]
}

private class FakeTransitionDao(private val transition: PackageTransitionEntity?) : PackageTransitionDao {
    override suspend fun insert(transition: PackageTransitionEntity) = Unit
    override suspend fun update(transition: PackageTransitionEntity) = Unit
    override suspend fun getForMedicine(medicineId: String): PackageTransitionEntity? = transition
    override suspend fun deleteForMedicine(medicineId: String) = Unit
}

private class FakeSettingsDao(private var settings: SettingsEntity?) : SettingsDao {
    override suspend fun upsert(settingsEntity: SettingsEntity) { settings = settingsEntity }
    override suspend fun getById(id: String): SettingsEntity? = settings
    override fun observeById(id: String): Flow<SettingsEntity?> = emptyFlow()
}
