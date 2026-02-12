package com.medapp.domain.usecase

import android.util.Log
import androidx.room.withTransaction
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.medapp.data.db.AppDatabase
import com.medapp.data.entity.IntakeEntity
import com.medapp.data.model.IntakeStatus
import com.medapp.integration.google.GoogleAuthException
import com.medapp.integration.google.GoogleRemoteTask
import com.medapp.integration.google.GoogleSignInManager
import com.medapp.integration.google.GoogleTaskMarker
import com.medapp.integration.google.GoogleTaskUpsertPayload
import com.medapp.integration.google.GoogleTasksService
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GoogleTasksSyncUseCase(
    private val database: AppDatabase,
    private val googleTasksService: GoogleTasksService,
    private val googleSignInManager: GoogleSignInManager,
    private val markIntakeCompletedUseCase: MarkIntakeCompletedUseCase
) {

    suspend operator fun invoke(): Result {
        return runCatching {
            val settings = database.settingsDao().getById() ?: return Result.Skipped("Settings unavailable")
            val listId = settings.googleTasksListId ?: return Result.Skipped("Google Tasks list is not configured")
            if (settings.googleAccountEmail.isNullOrBlank()) return Result.Skipped("Google account is not connected")

            val account = GoogleSignIn.getLastSignedInAccount(googleSignInManager.appContext)
                ?: return Result.Skipped("Google account session is missing")
            val accessToken = try {
                googleSignInManager.fetchAccessToken(account)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to get Google token", error)
                return Result.Skipped("Google token unavailable")
            }

            val now = Instant.now()
            val fromMillis = now.minusSeconds(7L * 24L * 60L * 60L).toEpochMilli()
            val toMillis = now.plusSeconds(90L * 24L * 60L * 60L).toEpochMilli()
            val remoteTasks = googleTasksService.listTasks(
                accessToken = accessToken,
                taskListId = listId,
                dueMinUtc = toRfc3339(fromMillis),
                dueMaxUtc = toRfc3339(toMillis)
            )

            val markerGroups = remoteTasks
                .mapNotNull { task -> GoogleTaskMarker.extractIntakeId(task.notes)?.let { it to task } }
                .groupBy({ it.first }, { it.second })

            val canonicalByMarker = markerGroups.mapValues { (_, tasks) ->
                val sorted = tasks.sortedByDescending { it.updated.orEmpty() }
                if (sorted.size > 1) {
                    Log.w(TAG, "Duplicate Google Tasks marker detected for intake; keeping newest")
                }
                sorted.first()
            }

            val localIntakes = database.intakeDao().getInWindow(fromMillis, toMillis)
            localIntakes.forEach { intake ->
                syncIntake(accessToken, listId, intake, canonicalByMarker[intake.id])
            }

            val finalSettings = database.settingsDao().getById() ?: settings
            database.settingsDao().upsert(finalSettings.copy(googleLastSyncAt = System.currentTimeMillis()))
            Result.Success
        }.getOrElse { throwable ->
            when (throwable) {
                is GoogleAuthException.Unauthorized -> Result.Error("Google authorization expired")
                is IOException -> Result.Error("No internet connection")
                else -> Result.Error(throwable.message ?: "Sync failed")
            }
        }
    }

    private suspend fun syncIntake(
        accessToken: String,
        listId: String,
        intake: IntakeEntity,
        matchedByMarker: GoogleRemoteTask?
    ) {
        val medicine = database.medicineDao().getById(intake.medicineId) ?: return
        val titleTime = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneOffset.systemDefault())
            .format(Instant.ofEpochMilli(intake.plannedAt))
        val payload = GoogleTaskUpsertPayload(
            title = "${medicine.name} – $titleTime",
            due = toRfc3339(intake.plannedAt),
            notes = buildNotes(intake),
            status = if (intake.status == IntakeStatus.COMPLETED.name) "completed" else "needsAction",
            completed = if (intake.status == IntakeStatus.COMPLETED.name) toRfc3339(intake.updatedAt) else null
        )

        var currentIntake = intake
        var remoteTask = matchedByMarker

        if (currentIntake.googleTaskId == null) {
            if (remoteTask != null) {
                updateIntakeGoogleTaskId(currentIntake, remoteTask.id)
                currentIntake = currentIntake.copy(googleTaskId = remoteTask.id)
            } else {
                val created = googleTasksService.insertTask(accessToken, listId, payload)
                updateIntakeGoogleTaskId(currentIntake, created.id)
                currentIntake = currentIntake.copy(googleTaskId = created.id)
                remoteTask = created
            }
        }

        val taskId = currentIntake.googleTaskId ?: remoteTask?.id ?: return
        if (remoteTask == null || remoteTask.id != taskId) {
            remoteTask = googleTasksService.getTask(accessToken, listId, taskId)
        }

        if (currentIntake.status == IntakeStatus.COMPLETED.name && remoteTask?.status != "completed") {
            googleTasksService.patchTask(accessToken, listId, taskId, payload)
        }

        if (currentIntake.status == IntakeStatus.PLANNED.name && remoteTask?.status == "completed") {
            markIntakeCompletedUseCase(currentIntake.id)
        }
    }

    private suspend fun updateIntakeGoogleTaskId(intake: IntakeEntity, taskId: String) {
        database.withTransaction {
            val latest = database.intakeDao().getById(intake.id) ?: return@withTransaction
            database.intakeDao().update(
                latest.copy(googleTaskId = taskId, updatedAt = System.currentTimeMillis())
            )
        }
    }

    private fun buildNotes(intake: IntakeEntity): String {
        return buildString {
            append(GoogleTaskMarker.markerForIntakeId(intake.id))
            append("\n")
            append("Dose: ${intake.realDoseMgPlanned} mg")
            append("\n")
            append("Pills: ${intake.pillCountPlanned}")
        }
    }

    private fun toRfc3339(epochMillis: Long): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))
    }

    sealed class Result {
        data object Success : Result()
        data class Skipped(val reason: String) : Result()
        data class Error(val reason: String) : Result()
    }

    companion object {
        private const val TAG = "GoogleTasksSyncUseCase"
    }
}
