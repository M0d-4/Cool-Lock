package com.mod4.cool_lock.logic

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mod4.cool_lock.data.CacheManager
import com.mod4.cool_lock.data.ModuleRepository
import com.mod4.cool_lock.data.ModuleState

/** Silently refreshes the module cache in the background so the app opens with fresh data. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("UpdateWorker", "Starting background update check (silent cache refresh)...")
        return try {
            val repository = ModuleRepository(applicationContext, CacheManager(applicationContext))
            if (repository.loadData(forceRefresh = true) is ModuleState.Success) {
                Log.d("UpdateWorker", "Background update check completed successfully.")
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("UpdateWorker", "Background update check failed", e)
            Result.failure()
        }
    }
}
