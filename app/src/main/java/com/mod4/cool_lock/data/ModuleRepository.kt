package com.mod4.cool_lock.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mod4.cool_lock.logic.LaunchHelper
import com.mod4.cool_lock.logic.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.random.Random

private const val INSTALLED_CHECK_THRESHOLD_MS = 3 * 24 * 60 * 60 * 1000L // re-check installed modules every 3 days

class ModuleRepository(private val context: Context, private val cacheManager: CacheManager) {

    fun getCachedState(): ModuleState.Success? = cacheManager.load(context)

    suspend fun loadData(forceRefresh: Boolean = false): ModuleState {
        val packageManager = context.packageManager
        val semaphore = Semaphore(5)
        val oldModulesMap = cacheManager.load(context)?.modules?.values?.flatten()?.associateBy { it.packageName } ?: emptyMap()
        val currentTime = System.currentTimeMillis()

        // One PackageManager call instead of one per module
        val installedPackages: Set<String> = try {
            packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        return withContext(Dispatchers.IO) {
            try {
                val allModules = coroutineScope {
                    GoodLockModules.modules.map { moduleInfo ->
                        async {
                            val isInstalled = installedPackages.contains(moduleInfo.packageName) ||
                                    (installedPackages.isEmpty() && try {
                                        packageManager.getPackageInfo(moduleInfo.packageName, 0); true
                                    } catch (e: Exception) { false })

                            val oldModule = oldModulesMap[moduleInfo.packageName]

                            // Only hit the network when needed
                            val needsUpdateCheck = isInstalled && (forceRefresh || oldModule == null ||
                                    oldModule.latestVersion == null ||
                                    (currentTime - oldModule.lastChecked) > INSTALLED_CHECK_THRESHOLD_MS)

                            var versionResult = VersionFetchResult(
                                version = oldModule?.latestVersion,
                                url = oldModule?.latestVersionUrl,
                                variantUrl = oldModule?.latestVariantUrl,
                                minAndroidVersion = oldModule?.minAndroidVersion
                            )
                            var installedVersion: String? = null
                            var launchIntent: Intent? = null
                            var lastCheckedTime = oldModule?.lastChecked ?: 0L

                            if (isInstalled) {
                                try {
                                    val pkgInfo = packageManager.getPackageInfo(moduleInfo.packageName, 0)
                                    installedVersion = pkgInfo.versionName
                                    launchIntent = LaunchHelper.getBestLaunchIntent(context, moduleInfo.packageName, moduleInfo.name)
                                } catch (e: Exception) {
                                    Log.e("BadlockLoad", "Error getting package info for ${moduleInfo.packageName}", e)
                                }

                                if (needsUpdateCheck) {
                                    semaphore.withPermit {
                                        Log.d("BadlockLoad", "Checking updates for ${moduleInfo.name} (force: $forceRefresh)")
                                        delay(Random.nextLong(50, 150))

                                        var fetched = UpdateChecker.fetchLatestVersionFromHtmlFallback(moduleInfo.apkMirrorMainPage)
                                        if (fetched.version == null) {
                                            try {
                                                fetched = UpdateChecker.fetchLatestVersionFromRssFeed(moduleInfo.apkMirrorMainPage)
                                            } catch (e: Exception) {
                                                Log.w("BadlockFetch", "RSS also failed for ${moduleInfo.name}", e)
                                            }
                                        }
                                        // A failed check must not wipe the last known good version
                                        if (fetched.version != null) {
                                            versionResult = fetched
                                            lastCheckedTime = System.currentTimeMillis()
                                        }
                                    }
                                }
                            }

                            val resourceName = moduleInfo.name.lowercase().replace(" ", "_").replace("+", "")
                            val iconResId = context.resources.getIdentifier(resourceName, "drawable", context.packageName).let { if (it == 0) null else it }
                            val updateAvailable = UpdateChecker.isUpdateAvailable(installedVersion, versionResult.version)

                            InstalledModule(
                                name = moduleInfo.name,
                                packageName = moduleInfo.packageName,
                                versionName = installedVersion,
                                latestVersion = versionResult.version,
                                latestVersionUrl = versionResult.url,
                                latestVariantUrl = versionResult.variantUrl,
                                minAndroidVersion = versionResult.minAndroidVersion,
                                launchIntent = launchIntent,
                                isInstalled = isInstalled,
                                isUpdateAvailable = updateAvailable,
                                category = moduleInfo.category,
                                apkMirrorMainPage = moduleInfo.apkMirrorMainPage,
                                iconResId = iconResId,
                                lastChecked = lastCheckedTime
                            )
                        }
                    }.awaitAll()
                }

                val groupedAndSorted = allModules.groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.sortedWith(
                            compareByDescending<InstalledModule> { it.isUpdateAvailable }
                                .thenByDescending { it.isInstalled }
                                .thenBy { it.name }
                        )
                    }
                val successState = ModuleState.Success(groupedAndSorted)
                cacheManager.save(successState)
                successState
            } catch (e: Exception) {
                when (e) {
                    is UnknownHostException, is SocketTimeoutException -> ModuleState.Error("Could not connect to server. Please check your internet connection.")
                    else -> {
                        Log.e("BadlockLoad", "An unexpected error occurred during data load", e)
                        ModuleState.Error("An unexpected error occurred.")
                    }
                }
            }
        }
    }
}
