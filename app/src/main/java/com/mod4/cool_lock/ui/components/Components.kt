package com.mod4.cool_lock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mod4.cool_lock.R
import com.mod4.cool_lock.data.InstalledModule
import com.mod4.cool_lock.logic.LaunchHelper
import com.mod4.cool_lock.logic.UpdateChecker

// ───────────────────────────── Bottom dock ─────────────────────────────

/**
 * Header + navigation island as ONE piece, docked to the bottom edge of the screen.
 * (The search bar is a separate piece that floats above it.)
 */
@Composable
fun BottomDock(
    versionName: String,
    tabs: List<String>,
    currentPage: Int,
    updatableCount: Int,
    searchActive: Boolean,
    refreshEnabled: Boolean,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Cool-Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "v$versionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh, enabled = refreshEnabled) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            BottomIsland(
                tabs = tabs,
                currentPage = currentPage,
                updatableCount = updatableCount,
                onTabClick = onTabClick
            )
        }
    }
}

/**
 * Real Material 3 Expressive navigation: ShortNavigationBar + ShortNavigationBarItem, the
 * pill-indicator nav bar introduced alongside the expressive component set.
 */
@Composable
fun BottomIsland(
    tabs: List<String>,
    currentPage: Int,
    updatableCount: Int,
    onTabClick: (Int) -> Unit
) {
    ShortNavigationBar(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .widthIn(max = 340.dp)
            .clip(RoundedCornerShape(32.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = currentPage == index
            val icon = when (title) {
                "Updates" -> Icons.Default.SystemUpdate
                "Make up" -> Icons.Default.Palette
                else -> Icons.Default.Style
            }
            ShortNavigationBarItem(
                selected = isSelected,
                onClick = { onTabClick(index) },
                icon = {
                    if (title == "Updates" && updatableCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) { Text("$updatableCount", fontSize = 10.sp) }
                            }
                        ) {
                            Icon(icon, contentDescription = title, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Icon(icon, contentDescription = title, modifier = Modifier.size(22.dp))
                    }
                },
                label = { Text(title, fontSize = 11.sp) }
            )
        }
    }
}

/** Separate floating piece that sits above the dock. */
@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Search modules...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            IconButton(onClick = { if (query.isNotEmpty()) onQueryChange("") else onClose() }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ───────────────────────────── Module list ─────────────────────────────

@Composable
fun ModuleList(
    modules: List<InstalledModule>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    showEmptyMessage: Boolean = false,
    emptyTitle: String = "All Clear!",
    emptySubtitle: String = "All your modules are up-to-date.",
    emptyIcon: ImageVector = Icons.Default.DoneAll,
    listState: LazyListState = rememberLazyListState(),
    onModuleClick: (InstalledModule) -> Unit,
    onModuleLongClick: (InstalledModule) -> Unit,
    onWebsiteClick: (String) -> Unit,
    onUpdateClick: (InstalledModule) -> Unit,
    onAppInfoClick: (String) -> Unit,
    onOpenClick: (InstalledModule) -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (modules.isEmpty() && showEmptyMessage) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(104.dp),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(emptyTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(emptySubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 16.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = modules, key = { it.packageName }) { module ->
                ModuleCard(
                    module = module,
                    onAppInfoClick = { onAppInfoClick(module.packageName) },
                    onLongClick = { onModuleLongClick(module) },
                    onWebsiteClick = { onWebsiteClick(module.apkMirrorMainPage) },
                    onUpdateClick = { onUpdateClick(module) },
                    onOpenClick = { onOpenClick(module) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModuleCard(
    module: InstalledModule,
    onAppInfoClick: () -> Unit,
    onLongClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (module.isUpdateAvailable) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .combinedClickable(onClick = onAppInfoClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = module.iconResId?.let { painterResource(id = it) }
                        ?: painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "${module.name} icon",
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    module.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                VersionInfo(module)
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (module.isInstalled) {
                    if (module.isUpdateAvailable) {
                        Button(
                            onClick = onUpdateClick,
                            modifier = Modifier.height(36.dp).widthIn(min = 84.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) { Text("Update", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                    FilledTonalButton(
                        onClick = onOpenClick,
                        modifier = Modifier.height(36.dp).widthIn(min = 84.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { Text("Open", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = onWebsiteClick,
                        modifier = Modifier.height(36.dp).widthIn(min = 84.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { Text("Install", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }

            IconButton(onClick = onWebsiteClick) {
                Icon(Icons.Default.Public, contentDescription = "Go to Website", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun VersionInfo(module: InstalledModule) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    if (module.isInstalled) {
        val currentColor = if (module.isUpdateAvailable) MaterialTheme.colorScheme.primary else secondary
        Text("Current: v${module.versionName ?: "N/A"}", color = currentColor, fontSize = 12.sp, maxLines = 1)
    } else {
        Text("Not Installed", color = secondary, fontSize = 12.sp, maxLines = 1)
    }

    if (module.latestVersion != null) {
        val color = if (module.isUpdateAvailable) MaterialTheme.colorScheme.tertiary else secondary
        Text("Latest: v${module.latestVersion}", color = color, fontSize = 12.sp, maxLines = 1)
        Text("Requires: ${module.minAndroidVersion?.takeIf { it.isNotBlank() } ?: "N/A"}", color = secondary, fontSize = 12.sp, maxLines = 1)
    }

    if (module.latestVersion == null && module.isInstalled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, contentDescription = "Error fetching version", tint = secondary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Update check failed", color = secondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun ErrorScreen(errorMessage: String, bottomPadding: androidx.compose.ui.unit.Dp, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SignalWifiOff,
                    contentDescription = "Connection Error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Connection Issue", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Text(errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.height(52.dp).fillMaxWidth(0.7f)
        ) { Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    }
}

// ───────────────────────────── Skeleton loading ─────────────────────────────

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing)),
        label = "shimmerOffset"
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.05f)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}

@Composable
fun ShimmerModuleList(bottomPadding: androidx.compose.ui.unit.Dp) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(8) { ShimmerModuleItem() }
    }
}

@Composable
fun ShimmerModuleItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).shimmerEffect())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Box(modifier = Modifier.width(140.dp).height(18.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.width(90.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).shimmerEffect())
            }
        }
    }
}

// ───────────────────────────── Details sheet ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailsSheet(
    module: InstalledModule,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onWebsite: () -> Unit,
    onAppInfo: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = module.iconResId?.let { painterResource(id = it) }
                            ?: painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(module.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(module.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoItem("Installed", module.versionName ?: "Not installed", Icons.Default.Smartphone, Modifier.weight(1f))
                InfoItem("Latest", module.latestVersion ?: "N/A", Icons.Default.Cloud, Modifier.weight(1f), highlight = module.isUpdateAvailable)
            }
            Spacer(Modifier.height(12.dp))
            InfoItem("Device Architecture", UpdateChecker.getDeviceArchitecture(), Icons.Default.Memory, Modifier.fillMaxWidth())

            if (!module.minAndroidVersion.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                InfoItem("Requires", module.minAndroidVersion, Icons.Default.Android, Modifier.fillMaxWidth())
            }

            if (module.isUpdateAvailable) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Tip: pick the '${UpdateChecker.getDeviceArchitecture()}' variant on APKMirror for the best compatibility.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (module.isInstalled) {
                    Button(
                        onClick = onLaunch,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open", fontWeight = FontWeight.Bold)
                    }
                } else {
                    val installLabel = if (module.latestVariantUrl != null) "Fast Install" else "Install"
                    Button(
                        onClick = { module.latestVariantUrl?.let { LaunchHelper.openUrl(context, it) } ?: onWebsite() },
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(installLabel, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onAppInfo,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "App info", modifier = Modifier.size(20.dp))
                }

                val fastDownload = module.isUpdateAvailable && module.latestVariantUrl != null
                OutlinedButton(
                    onClick = {
                        val url = if (module.isUpdateAvailable) module.latestVariantUrl ?: module.latestVersionUrl ?: module.apkMirrorMainPage else module.apkMirrorMainPage
                        LaunchHelper.openUrl(context, url)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = if (fastDownload) Icons.Default.FlashOn else Icons.Default.Public,
                        contentDescription = if (fastDownload) "Fast download" else "Website",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val accent = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (highlight) Modifier.border(BorderStroke(1.dp, accent.copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
                else Modifier
            )
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
