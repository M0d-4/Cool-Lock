package com.mod4.cool_lock.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mod4.cool_lock.data.AppUpdateStatus
import com.mod4.cool_lock.data.InstalledModule
import com.mod4.cool_lock.data.ModuleState
import com.mod4.cool_lock.logic.LaunchHelper
import com.mod4.cool_lock.ui.BadlockViewModel
import com.mod4.cool_lock.ui.components.BottomDock
import com.mod4.cool_lock.ui.components.ErrorScreen
import com.mod4.cool_lock.ui.components.ModuleDetailsSheet
import com.mod4.cool_lock.ui.components.ModuleList
import com.mod4.cool_lock.ui.components.SearchPill
import com.mod4.cool_lock.ui.components.ShimmerModuleList
import com.mod4.cool_lock.ui.components.captureForBackdropBlur
import com.mod4.cool_lock.ui.components.drawBackdropBlur
import com.mod4.cool_lock.ui.components.rememberBackdropBlurLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TABS = listOf("Make up", "Life up", "Updates")

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BadlockViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val moduleState by viewModel.moduleState.collectAsState()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf<InstalledModule?>(null) }
    var overlayHeight by remember { mutableStateOf(0.dp) }

    // Backdrop blur: the content box records itself into blurLayer every frame; the strip
    // right above the header redraws that recording (blurred + translated into place).
    val blurLayer = rememberBackdropBlurLayer(radius = 24.dp)
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    var blurStripOrigin by remember { mutableStateOf(Offset.Zero) }

    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val listStates = remember { TABS.map { LazyListState() } }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // Quiet refresh on resume (cache thresholds decide what actually hits the network)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshData()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Full forced refresh whenever ANY app is installed / updated / removed on the device
    DisposableEffect(Unit) {
        var debounceJob: Job? = null
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("BadlockAutoRefresh", "Package change detected, refreshing data...")
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    delay(600) // REMOVED/ADDED/REPLACED arrive back-to-back on updates
                    viewModel.refreshData(force = true)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            debounceJob?.cancel()
            context.unregisterReceiver(packageReceiver)
        }
    }

    if (showUpdateDialog && appUpdateInfo != null) {
        val info = appUpdateInfo!!
        val status = updateStatus
        AlertDialog(
            onDismissRequest = { if (status !is AppUpdateStatus.Downloading) viewModel.dismissUpdateDialog() },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Cool-Lock Update", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("A new version of Cool-Lock is available!", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Version: v${info.latestVersion}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    if (!info.releaseNotes.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("What's new:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            info.releaseNotes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .height(96.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                    when (status) {
                        is AppUpdateStatus.Downloading -> {
                            Spacer(Modifier.height(16.dp))
                            if (status.progress >= 0) {
                                LinearProgressIndicator(
                                    progress = { status.progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${status.progress}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        is AppUpdateStatus.Failed -> {
                            Spacer(Modifier.height(12.dp))
                            Text(status.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        is AppUpdateStatus.ReadyToInstall -> {
                            Spacer(Modifier.height(12.dp))
                            Text("Downloaded — ready to install.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                        }
                        AppUpdateStatus.Idle -> {}
                    }
                }
            },
            confirmButton = {
                when (status) {
                    AppUpdateStatus.Idle, is AppUpdateStatus.Failed -> {
                        if (info.apkAssetUrl != null) {
                            Button(onClick = { viewModel.downloadAndInstallUpdate() }) { Text("Download & Install") }
                        } else {
                            Button(onClick = {
                                LaunchHelper.openUrl(context, info.downloadUrl)
                                viewModel.dismissUpdateDialog()
                            }) { Text("Go to GitHub") }
                        }
                    }
                    is AppUpdateStatus.Downloading -> {
                        Button(onClick = {}, enabled = false) { Text("Downloading…") }
                    }
                    AppUpdateStatus.ReadyToInstall -> {
                        Button(onClick = {
                            viewModel.installDownloadedUpdate()
                            viewModel.dismissUpdateDialog()
                        }) { Text("Install") }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissUpdateDialog() },
                    enabled = status !is AppUpdateStatus.Downloading
                ) { Text("Later") }
            }
        )
    }

    selectedModule?.let { module ->
        ModuleDetailsSheet(
            module = module,
            onDismiss = { selectedModule = null },
            onLaunch = {
                LaunchHelper.launchModule(context, module)
                selectedModule = null
            },
            onWebsite = {
                LaunchHelper.openUrl(context, module.apkMirrorMainPage)
                selectedModule = null
            },
            onAppInfo = {
                LaunchHelper.openAppInfo(context, module.packageName)
                selectedModule = null
            }
        )
    }

    val onModuleClick: (InstalledModule) -> Unit = { module ->
        if (module.isInstalled) LaunchHelper.launchModule(context, module)
        else LaunchHelper.openUrl(context, module.apkMirrorMainPage)
    }
    val onWebsiteClick: (String) -> Unit = { url -> LaunchHelper.openUrl(context, url) }
    val onUpdateClick: (InstalledModule) -> Unit = { module ->
        module.latestVersionUrl?.let { LaunchHelper.openUrl(context, it) }
    }
    val onAppInfoClick: (String) -> Unit = { pkg -> LaunchHelper.openAppInfo(context, pkg) }
    val onOpenClick: (InstalledModule) -> Unit = { module -> LaunchHelper.launchModule(context, module) }

    val listBottomPadding: Dp = overlayHeight + 16.dp
    val successState = moduleState as? ModuleState.Success
    val updatableModules = remember(successState) {
        successState?.modules?.values?.flatten()?.filter { it.isUpdateAvailable } ?: emptyList()
    }
    val searching = searchActive && searchQuery.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { contentOrigin = it.positionInRoot() }
                .captureForBackdropBlur(blurLayer)
        ) {
            when (val state = moduleState) {
                is ModuleState.Loading -> ShimmerModuleList(bottomPadding = listBottomPadding)
                is ModuleState.Error -> ErrorScreen(
                    errorMessage = state.message,
                    bottomPadding = listBottomPadding,
                    onRetry = { viewModel.refreshData(force = true) }
                )
                is ModuleState.Success -> {
                    if (searching) {
                        // Cross-module search: results come from every tab
                        ModuleList(
                            modules = searchResults,
                            bottomPadding = listBottomPadding,
                            showEmptyMessage = true,
                            emptyTitle = "No Results",
                            emptySubtitle = "No modules match your search.",
                            emptyIcon = Icons.Default.SearchOff,
                            onModuleClick = onModuleClick,
                            onModuleLongClick = { selectedModule = it },
                            onWebsiteClick = onWebsiteClick,
                            onUpdateClick = onUpdateClick,
                            onAppInfoClick = onAppInfoClick,
                            onOpenClick = onOpenClick
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val pageTitle = TABS[page]
                            val modulesToShow = when (pageTitle) {
                                "Updates" -> updatableModules
                                else -> state.modules[pageTitle] ?: emptyList()
                            }
                            ModuleList(
                                modules = modulesToShow,
                                bottomPadding = listBottomPadding,
                                showEmptyMessage = (pageTitle == "Updates"),
                                listState = listStates[page],
                                onModuleClick = onModuleClick,
                                onModuleLongClick = { selectedModule = it },
                                onWebsiteClick = onWebsiteClick,
                                onUpdateClick = onUpdateClick,
                                onAppInfoClick = onAppInfoClick,
                                onOpenClick = onOpenClick
                            )
                        }
                    }
                }
            }
        }

        // Bottom overlay: [ search pill (separate piece) ] above [ header + island (one docked piece) ].
        // NOTE: imePadding() lives on the search pill's AnimatedVisibility only, not on this whole
        // Column — otherwise the keyboard inset padding grows the Column itself, which (since it's
        // bottom-aligned) shoves the header/dock up above the keyboard along with the search field.
        // Keeping it scoped to just the search pill lets the dock stay pinned to the real bottom.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { overlayHeight = with(density) { it.size.height.toDp() } }
        ) {
            AnimatedVisibility(
                visible = searchActive,
                modifier = Modifier.imePadding(),
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                SearchPill(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClose = {
                        searchActive = false
                        viewModel.updateSearchQuery("")
                    }
                )
            }

            // Frosted strip: blurs whatever list content is scrolling by right above the header,
            // so the dock reads as "glass" instead of hard-cutting the list off.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .onGloballyPositioned { blurStripOrigin = it.positionInRoot() }
                    .drawBackdropBlur(
                        layer = blurLayer,
                        sourceTopLeft = { contentOrigin },
                        targetTopLeft = { blurStripOrigin }
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            BottomDock(
                versionName = versionName,
                tabs = TABS,
                currentPage = pagerState.currentPage,
                updatableCount = updatableModules.size,
                searchActive = searchActive,
                refreshEnabled = moduleState != ModuleState.Loading,
                onSearchClick = {
                    if (searchActive) viewModel.updateSearchQuery("")
                    searchActive = !searchActive
                },
                onRefresh = { viewModel.refreshData(force = true) },
                onTabClick = { index ->
                    // Leaving search when a tab is chosen
                    if (searchActive) {
                        searchActive = false
                        viewModel.updateSearchQuery("")
                    }
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    }
}
