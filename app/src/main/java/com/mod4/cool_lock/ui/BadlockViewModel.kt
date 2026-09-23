package com.mod4.cool_lock.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mod4.cool_lock.data.AppUpdateInfo
import com.mod4.cool_lock.data.AppUpdateStatus
import com.mod4.cool_lock.data.InstalledModule
import com.mod4.cool_lock.data.ModuleRepository
import com.mod4.cool_lock.data.ModuleState
import com.mod4.cool_lock.logic.ApkDownloader
import com.mod4.cool_lock.logic.UpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BadlockViewModel(private val repository: ModuleRepository, private val context: Context) : ViewModel() {

    private var lastSuccess: ModuleState.Success? = repository.getCachedState()
    private var refreshJob: Job? = null

    private val _moduleState = MutableStateFlow<ModuleState>(lastSuccess ?: ModuleState.Loading)
    val moduleState: StateFlow<ModuleState> = _moduleState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<InstalledModule>> = combine(_moduleState, _searchQuery) { state, query ->
        val q = query.trim()
        if (q.isEmpty() || state !is ModuleState.Success) return@combine emptyList()
        state.modules.values.flatten().filter {
            it.name.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    val updateStatus: StateFlow<AppUpdateStatus> = _updateStatus.asStateFlow()

    init {
        checkForAppUpdate()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
        if (_updateStatus.value !is AppUpdateStatus.ReadyToInstall) {
            _updateStatus.value = AppUpdateStatus.Idle
        }
    }

    private fun checkForAppUpdate() {
        viewModelScope.launch {
            val update = UpdateChecker.checkAppUpdate() ?: return@launch
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
            } catch (e: Exception) {
                "0.0"
            }
            if (UpdateChecker.isUpdateAvailable(currentVersion, update.latestVersion)) {
                _appUpdateInfo.value = update
                _showUpdateDialog.value = true
            }
        }
    }

    /** Downloads the release's .apk asset, then hands off to the system installer. */
    fun downloadAndInstallUpdate() {
        val apkUrl = _appUpdateInfo.value?.apkAssetUrl
        if (apkUrl == null) {
            _updateStatus.value = AppUpdateStatus.Failed("No downloadable APK on this release")
            return
        }
        if (ApkDownloader.needsInstallPermission(context)) {
            _updateStatus.value = AppUpdateStatus.Failed("Allow \"Install unknown apps\" for Cool-Lock, then try again")
            return
        }

        _updateStatus.value = AppUpdateStatus.Downloading(-1)
        viewModelScope.launch {
            ApkDownloader.download(context, apkUrl)
                .catch { e -> _updateStatus.value = AppUpdateStatus.Failed(e.message ?: "Download failed") }
                .collect { progress ->
                    _updateStatus.value = if (progress >= 100) {
                        AppUpdateStatus.ReadyToInstall
                    } else {
                        AppUpdateStatus.Downloading(progress)
                    }
                }
        }
    }

    fun installDownloadedUpdate() {
        ApkDownloader.installApk(context)
    }

    fun refreshData(force: Boolean = false) {
        // A quiet refresh never interrupts one that is already running; a forced one replaces it
        if (!force && refreshJob?.isActive == true) return
        refreshJob?.cancel()

        if (_moduleState.value !is ModuleState.Success || force) {
            _moduleState.value = ModuleState.Loading
        }

        refreshJob = viewModelScope.launch {
            val newState = repository.loadData(force)
            val fallback = lastSuccess
            when {
                newState is ModuleState.Success -> {
                    lastSuccess = newState
                    _moduleState.value = newState
                }
                fallback != null -> {
                    _moduleState.value = fallback
                    _messages.tryEmit("Update check failed, showing last known data.")
                }
                else -> _moduleState.value = newState
            }
        }
    }

    class Factory(private val repository: ModuleRepository, private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BadlockViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BadlockViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
