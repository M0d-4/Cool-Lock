package com.mod4.cool_lock

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mod4.cool_lock.logic.UpdateWorker
import java.util.concurrent.TimeUnit

class CoolLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupBackgroundUpdateCheck()
    }

    private fun setupBackgroundUpdateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(3, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )
    }
}
