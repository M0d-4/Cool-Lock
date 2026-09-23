package com.mod4.cool_lock.data

import android.content.Intent

data class ModuleInfo(
    val name: String,
    val packageName: String,
    val category: String,
    val apkMirrorMainPage: String
)

data class InstalledModule(
    val name: String,
    val packageName: String,
    val versionName: String?,
    val latestVersion: String?,
    val latestVersionUrl: String?,
    val latestVariantUrl: String? = null,
    val minAndroidVersion: String?,
    @Transient var launchIntent: Intent?, // Ignored by cache
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val category: String,
    val apkMirrorMainPage: String,
    val iconResId: Int?,
    val lastChecked: Long = 0L
)

data class VersionFetchResult(
    val version: String? = null,
    val url: String? = null,
    val variantUrl: String? = null,
    val minAndroidVersion: String? = null
)

sealed interface ModuleState {
    object Loading : ModuleState
    data class Success(val modules: Map<String, List<InstalledModule>>) : ModuleState
    data class Error(val message: String) : ModuleState
}

data class AppUpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val apkAssetUrl: String?,
    val releaseNotes: String?
)

/** Download/install progress of the in-app self-update. */
sealed interface AppUpdateStatus {
    object Idle : AppUpdateStatus
    data class Downloading(val progress: Int) : AppUpdateStatus // 0..100, -1 = indeterminate
    object ReadyToInstall : AppUpdateStatus
    data class Failed(val message: String) : AppUpdateStatus
}
