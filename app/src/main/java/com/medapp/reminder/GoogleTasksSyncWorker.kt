package com.medapp.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.medapp.data.db.AppDatabaseProvider
import com.medapp.domain.usecase.GoogleTasksSyncUseCase
import com.medapp.domain.usecase.MarkIntakeCompletedUseCase
import com.medapp.integration.google.GoogleSignInManager
import com.medapp.integration.google.GoogleTasksHttpService
import okhttp3.OkHttpClient

class GoogleTasksSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabaseProvider.get(applicationContext)
        val useCase = GoogleTasksSyncUseCase(
            database = database,
            googleTasksService = GoogleTasksHttpService(OkHttpClient(), Gson()),
            googleSignInManager = GoogleSignInManager(applicationContext),
            markIntakeCompletedUseCase = MarkIntakeCompletedUseCase(database)
        )
        return when (val result = useCase()) {
            is GoogleTasksSyncUseCase.Result.Success,
            is GoogleTasksSyncUseCase.Result.Skipped -> Result.success()
            is GoogleTasksSyncUseCase.Result.Error -> {
                Log.w("GoogleTasksSyncWorker", "Sync failed: ${result.reason}")
                Result.retry()
            }
        }
    }
}
