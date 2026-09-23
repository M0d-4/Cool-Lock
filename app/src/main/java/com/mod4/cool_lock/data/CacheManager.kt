package com.mod4.cool_lock.data

import android.content.Context
import com.mod4.cool_lock.logic.LaunchHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CacheManager(context: Context) {
    private val prefs = context.getSharedPreferences("CoolLockCache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var lastLoadedState: ModuleState.Success? = null
    private var lastLoadedTime = 0L

    fun save(state: ModuleState.Success) {
        val json = gson.toJson(state.modules)
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString("cached_modules", json)
            .putLong("last_refresh_time", now)
            .apply()
        lastLoadedState = state // Keep in-memory copy
        lastLoadedTime = now
    }

    fun load(context: Context): ModuleState.Success? {
        // The in-memory copy is only valid if nobody else (e.g. UpdateWorker) saved since
        if (lastLoadedState != null && lastLoadedTime == prefs.getLong("last_refresh_time", 0L)) return lastLoadedState

        val json = prefs.getString("cached_modules", null) ?: return null
        val type = object : TypeToken<Map<String, List<InstalledModule>>>() {}.type
        val modules: Map<String, List<InstalledModule>> = gson.fromJson(json, type)

        // Rebuild launch intents as they are not cached
        val modulesWithIntents = modules.mapValues { entry ->
            entry.value.map { module ->
                if (module.isInstalled) {
                    module.apply {
                        launchIntent = LaunchHelper.getBestLaunchIntent(context, module.packageName, module.name)
                    }
                } else {
                    module
                }
            }
        }
        val state = ModuleState.Success(modulesWithIntents)
        lastLoadedState = state
        lastLoadedTime = prefs.getLong("last_refresh_time", 0L)
        return state
    }

    fun getLastRefreshTime(): Long {
        return prefs.getLong("last_refresh_time", 0L)
    }
}
