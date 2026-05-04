package com.dark.badlock

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// --- UI THEME & COLORS ---
data class AppColors(
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val iconBox: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val tabActive: Color,
    val tabBarBackground: Color,
    val tabBarScrimEnd: Color,
    val pillBarBg: Color,
    val openButtonBg: Color,
    val installButtonBg: Color,
    val updateButtonBg: Color,
    val buttonTextColor: Color,
    val updateLatestText: Color,
    val currentVersionText: Color,
    val websiteIconTint: Color,
    val titleBarBackground: Color,
    val accentPrimary: Color,
    val badgeBg: Color
)

val DarkAppColors = AppColors(
    background        = Color(0xFF000000),
    surface           = Color(0xFF1E1E1E),
    cardBackground    = Color(0xFF141414),
    iconBox           = Color(0xFF1C1C1C),
    textPrimary       = Color.White.copy(alpha = 0.9f),
    textSecondary     = Color.White.copy(alpha = 0.6f),
    tabActive         = Color(0xFF1565C0),
    tabBarBackground  = Color(0xFF0D2A6E),
    tabBarScrimEnd    = Color.Black,
    pillBarBg         = Color(0xFF0D47A1),
    openButtonBg      = Color(0xFF1A1A3A),
    installButtonBg   = Color(0xFF0A1F4E),
    updateButtonBg    = Color(0xFF2A3010),
    buttonTextColor   = Color.White,
    updateLatestText  = Color(0xFFB8860B),
    currentVersionText= Color(0xFF64B5F6),
    websiteIconTint   = Color.White.copy(alpha = 0.35f),
    titleBarBackground= Color(0xFF000000),
    accentPrimary     = Color(0xFF8A2BE2),
    badgeBg           = Color(0xFF2A3010)
)

val LightAppColors = AppColors(
    background        = Color(0xFFF2F2F7),
    surface           = Color(0xFFFFFFFF),
    cardBackground    = Color(0xFFFFFFFF),
    iconBox           = Color(0xFFEEEEF4),
    textPrimary       = Color(0xFF1C1C1E),
    textSecondary     = Color(0xFF6C6C70),
    tabActive         = Color(0xFF1976D2),
    tabBarBackground  = Color(0xFF1565C0),
    tabBarScrimEnd    = Color(0xFFF2F2F7),
    pillBarBg         = Color(0xFF1565C0),
    openButtonBg      = Color(0xFFDDDDF0),
    installButtonBg   = Color(0xFFD0E0FF),
    updateButtonBg    = Color(0xFFE8F0C8),
    buttonTextColor   = Color.Black,
    updateLatestText  = Color(0xFF8B6914),
    currentVersionText= Color(0xFF1565C0),
    websiteIconTint   = Color(0xFF6C6C70),
    titleBarBackground= Color(0xFFF2F2F7),
    accentPrimary     = Color(0xFF6B3FA0),
    badgeBg           = Color(0xFF7A8C2A)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
val LocalMaterialYou = staticCompositionLocalOf { false }

private const val PREFS_NAME = "cool_lock_prefs"
private const val KEY_MATERIAL_YOU = "material_you_enabled"

fun getMaterialYouEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_MATERIAL_YOU, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

fun setMaterialYouEnabled(context: Context, enabled: Boolean) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_MATERIAL_YOU, enabled).apply()

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var materialYouEnabled by remember {
        mutableStateOf(getMaterialYouEnabled(context))
    }

    // Expose setter so child composables can toggle it
    val setMY: (Boolean) -> Unit = { enabled ->
        materialYouEnabled = enabled
        setMaterialYouEnabled(context, enabled)
    }

    val supportsMyou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val dynamicScheme: ColorScheme? =
        if (supportsMyou && materialYouEnabled) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else null

    val colors = if (dynamicScheme != null) {
        // Map Material You tokens — use primary/secondary/tertiary containers for vivid wallpaper colours
        val base = if (isDark) DarkAppColors else LightAppColors
        base.copy(
            background         = dynamicScheme.background,
            surface            = dynamicScheme.surface,
            cardBackground     = dynamicScheme.surfaceContainer,
            iconBox            = dynamicScheme.secondaryContainer,
            textPrimary        = dynamicScheme.onBackground,
            textSecondary      = dynamicScheme.onSurfaceVariant,
            tabActive          = dynamicScheme.primaryContainer,
            tabBarBackground   = dynamicScheme.primary,
            tabBarScrimEnd     = dynamicScheme.background,
            pillBarBg          = dynamicScheme.background,
            openButtonBg       = dynamicScheme.secondaryContainer,
            installButtonBg    = dynamicScheme.tertiaryContainer,
            updateButtonBg     = dynamicScheme.primaryContainer,
            buttonTextColor    = if (isDark) dynamicScheme.onPrimaryContainer else dynamicScheme.onPrimary,
            titleBarBackground = dynamicScheme.background,
            accentPrimary      = dynamicScheme.primary,
            badgeBg            = dynamicScheme.error,
            currentVersionText = dynamicScheme.primary,
            updateLatestText   = dynamicScheme.tertiary,
            websiteIconTint    = dynamicScheme.onSurfaceVariant
        )
    } else {
        if (isDark) DarkAppColors else LightAppColors
    }

    val materialScheme = dynamicScheme ?: if (isDark) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalMaterialYou provides (dynamicScheme != null),
        LocalMaterialYouSetter provides setMY,
        LocalMaterialYouEnabled provides materialYouEnabled,
        LocalSupportsMyou provides supportsMyou
    ) {
        MaterialTheme(colorScheme = materialScheme, typography = Typography(), content = content)
    }
}

val LocalMaterialYouSetter = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
val LocalMaterialYouEnabled = staticCompositionLocalOf { false }
val LocalSupportsMyou = staticCompositionLocalOf { false }

// Legacy aliases so existing references keep compiling
val DarkBackground   @Composable get() = LocalAppColors.current.background
val PrimaryAccent    @Composable get() = LocalAppColors.current.accentPrimary
val TextPrimary      @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary    @Composable get() = LocalAppColors.current.textSecondary
val UpdateOlive      @Composable get() = LocalAppColors.current.updateButtonBg
val UpdateLatestAmber @Composable get() = LocalAppColors.current.updateLatestText
val InstallBlue      @Composable get() = LocalAppColors.current.installButtonBg

// --- DATA & STATE CLASSES ---
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
    val minAndroidVersion: String?,
    @Transient var launchIntent: Intent?, // Ignored by cache
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val category: String,
    val apkMirrorMainPage: String,
    val iconResId: Int?
)

data class VersionFetchResult(
    val version: String? = null,
    val url: String? = null,
    val minAndroidVersion: String? = null
)

sealed interface ModuleState {
    object Loading : ModuleState
    data class Success(val modules: Map<String, List<InstalledModule>>) : ModuleState
    data class Error(val message: String) : ModuleState
}

// --- ON-DISK CACHE MANAGER ---
class CacheManager(context: Context) {
    private val prefs = context.getSharedPreferences("BadlockCache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var lastLoadedState: ModuleState.Success? = null

    fun save(state: ModuleState.Success) {
        val json = gson.toJson(state.modules)
        prefs.edit()
            .putString("cached_modules", json)
            .putLong("last_refresh_time", System.currentTimeMillis())
            .apply()
        lastLoadedState = state // Keep in-memory copy
    }

    fun load(context: Context): ModuleState.Success? {
        if (lastLoadedState != null) return lastLoadedState

        val json = prefs.getString("cached_modules", null) ?: return null
        val type = object : TypeToken<Map<String, List<InstalledModule>>>() {}.type
        val modules: Map<String, List<InstalledModule>> = gson.fromJson(json, type)

        // Rebuild launch intents as they are not cached
        val modulesWithIntents = modules.mapValues { entry ->
            entry.value.map { module ->
                if (module.isInstalled) {
                    module.apply {
                        launchIntent = getBestLaunchIntent(context, module.packageName, module.name)
                    }
                } else {
                    module
                }
            }
        }
        val state = ModuleState.Success(modulesWithIntents)
        lastLoadedState = state
        return state
    }

    fun getLastRefreshTime(): Long {
        return prefs.getLong("last_refresh_time", 0L)
    }
}


// --- MODULE DEFINITIONS ---
object GoodLockModules {
    val modules = listOf(
        ModuleInfo("Home Up", "com.samsung.android.app.homestar", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/home-up/"),
        ModuleInfo("LockStar", "com.samsung.systemui.lockstar", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/lockstar/"),
        ModuleInfo("MultiStar", "com.samsung.android.multistar", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/samsung-multistar/"),
        ModuleInfo("QuickStar", "com.samsung.android.qstuner", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/quickstar/"),
        ModuleInfo("NavStar", "com.samsung.systemui.navillera", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/samsung-navstar/"),
        ModuleInfo("SoundAssistant", "com.samsung.android.soundassistant", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/soundassistant/"),
        ModuleInfo("Keys Cafe", "com.samsung.android.keyscafe", "Make up", "https://www.apkmirror.com/apk/good-lock-labs/keys-cafe/"),
        ModuleInfo("Theme Park", "com.samsung.android.themedesigner", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/samsung-theme-park/"),
        ModuleInfo("Nice Shot", "com.samsung.android.app.captureplugin", "Make up", "https://www.apkmirror.com/apk/samsung-electronics/nice-shot/"),
        ModuleInfo("Wonderland", "com.samsung.android.wonderland.wallpaper", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/wonderland/"),
        ModuleInfo("Pentastic", "com.samsung.android.pentastic", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/pentastic/"),
        ModuleInfo("Clockface", "com.samsung.android.app.clockface", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/samsung-clockface/"),
        ModuleInfo("Edge lighting+", "com.samsung.android.edgelightingplus", "Make up", "https://www.apkmirror.com/apk/good-lock-labs/edge-lighting/"),
        ModuleInfo("Edge touch", "com.samsung.android.app.edgetouch", "Make up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/edge-touch/"),
        ModuleInfo("Display Assistant", "com.samsung.android.displayassistant", "Make up", "https://www.apkmirror.com/apk/galaxy-labs/display-assistant-beta/"),
        ModuleInfo("Routines+", "com.samsung.android.app.routineplus", "Life up", "https://www.apkmirror.com/apk/good-lock-labs/samsung-routine/"),
        ModuleInfo("NotiStar", "com.samsung.systemui.notilus", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/notistar/"),
        ModuleInfo("RegiStar", "com.samsung.android.app.galaxyregistry", "Life up", "https://www.apkmirror.com/apk/good-lock-labs/registar/"),
        ModuleInfo("Camera Assistant", "com.samsung.android.app.cameraassistant", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/camera-assistant/"),
        ModuleInfo("Nice Catch", "com.samsung.android.app.goodcatch", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/nice-catch/"),
        ModuleInfo("Good Lock", "com.samsung.android.goodlock", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd/good-lock-2018/"),
        ModuleInfo("Battery Guardian", "com.samsung.android.statsd", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/battery-guardian/"),
        ModuleInfo("File Guardian", "com.android.samsung.icebox", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/file-guardian/"),
        ModuleInfo("Memory Guardian", "com.samsung.android.memoryguardian", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/memory-guardian/"),
        ModuleInfo("App Booster", "com.samsung.android.appbooster", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/app-booster/"),
        ModuleInfo("Thermal Guardian", "com.samsung.android.thermalguardian", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/thermal-guardian/"),
        ModuleInfo("Media File Guardian", "com.samsung.android.mediaguardian", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/media-file-guardian/"),
        ModuleInfo("One Hand Operation+", "com.samsung.android.sidegesturepad", "Life up", "https://www.apkmirror.com/apk/samsung-electronics-co-ltd-co-ltd/one-hand-operation/"),
    )
}

// --- Helper functions for scraping ---
private val browserUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"

private fun cleanVersionText(rawText: String): String {
    var cleaned = rawText.trim()
    cleaned = cleaned.replace("""(?i)(version|api|level|sdk)""".toRegex(), "").trim()
    if (cleaned.matches("""\d+\+?""".toRegex())) {
        val number = cleaned.replace("+", "")
        return if (cleaned.contains("+")) "Android $number+" else "Android $number"
    }
    if (cleaned.matches("""(?i)android\s*\d+\+?""".toRegex())) {
        return cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    if (cleaned.matches("""\d+(?:\.\d+)*""".toRegex())) {
        return "Android $cleaned"
    }
    if (cleaned.matches("""\d{2,}""".toRegex())) {
        return "API $cleaned"
    }
    return cleaned.ifEmpty { null } ?: "Unknown"
}

private fun scrapeMinVersion(doc: Document): String? {
    // Strategy 1: Enhanced table parsing with flexible selectors
    try {
        val possibleTables = doc.select("div[class*=table], table, div.downloadBox")
        for (table in possibleTables) {
            val rows = table.select("div[class*=row], tr, div[class*=variant]")
            var minVersionIndex = -1
            var headerRow: Element? = null

            for (row in rows) {
                val cells = row.select("div[class*=cell], td, th, div[class*=col]")
                for (index in cells.indices) {
                    val cell = cells[index]
                    val cellText = cell.text().lowercase().trim()
                    if (cellText.contains("minimum") || cellText.contains("min") ||
                        cellText.contains("requires") || cellText.contains("android")) {
                        minVersionIndex = index
                        headerRow = row
                        break
                    }
                }
                if (minVersionIndex != -1) break
            }

            if (minVersionIndex != -1 && headerRow != null) {
                val headerIndex = rows.indexOf(headerRow)
                for (i in (headerIndex + 1) until rows.size) {
                    val dataRow = rows[i]
                    val dataCells = dataRow.select("div[class*=cell], td, div[class*=col]")
                    if (dataCells.size > minVersionIndex) {
                        val versionText = dataCells[minVersionIndex].text().trim()
                        if (versionText.isNotEmpty() &&
                            !versionText.lowercase().contains("minimum") &&
                            (versionText.contains("android", ignoreCase = true) ||
                                    versionText.matches(""".*\d+.*""".toRegex()))) {
                            return cleanVersionText(versionText)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.w("BadlockScrape", "Table parsing failed: ${e.message}")
    }

    // Strategy 2: Enhanced appspec-row with flexible matching
    try {
        val rows = doc.select("div[class*=appspec], div[class*=spec], div[class*=info-row]")
        for (row in rows) {
            val titleElements = row.select("div[class*=title], div[class*=label], span[class*=label]")
            val valueElements = row.select("div[class*=value], div[class*=content]")

            if (titleElements.isNotEmpty() && valueElements.isNotEmpty()) {
                val title = titleElements.first()!!.text().lowercase().trim()
                if (title.contains("minimum") || title.contains("requires") || title.contains("android")) {
                    val value = valueElements.first()!!.text().trim()
                    if (value.isNotEmpty() && (value.contains("android", ignoreCase = true) ||
                                value.matches(""".*\d+.*""".toRegex()))) {
                        return cleanVersionText(value)
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.w("BadlockScrape", "Appspec parsing failed: ${e.message}")
    }

    return null
}

// --- VERSION FETCHING FUNCTIONS ---
suspend fun fetchLatestVersionFromRssFeed(url: String): VersionFetchResult {
    val feedUrl = if (url.endsWith("/")) "${url}feed/" else "$url/feed/"
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(feedUrl).userAgent(browserUserAgent).timeout(15000).get()
            val firstItem = doc.selectFirst("item") ?: return@withContext VersionFetchResult()

            val title = firstItem.selectFirst("title")?.text()
            val link = firstItem.selectFirst("link")?.text()

            val regex = """\s(\d+(\.\d+)+(\.\d+)*)""".toRegex()
            val matchResult = regex.find(title ?: "")
            val version = matchResult?.groups?.get(1)?.value?.trim()

            var minAndroidVersion: String? = null
            if (link != null) {
                try {
                    val versionDoc = Jsoup.connect(link).userAgent(browserUserAgent).timeout(15000).get()
                    minAndroidVersion = scrapeMinVersion(versionDoc)
                } catch (e: Exception) {
                    Log.w("BadlockFetch", "Could not fetch min version from $link", e)
                }
            }

            VersionFetchResult(version = version, url = link, minAndroidVersion = minAndroidVersion)
        } catch (e: Exception) {
            throw e
        }
    }
}

suspend fun fetchLatestVersionFromHtmlFallback(url: String): VersionFetchResult {
    return withContext(Dispatchers.IO) {
        try {
            val mainDoc = Jsoup.connect(url).userAgent(browserUserAgent).timeout(20000).get()
            val latestVersionLinkElement = mainDoc.selectFirst("div.list-row a.fontBlack") ?: return@withContext VersionFetchResult()

            val latestVersionPageUrl = "https://www.apkmirror.com" + latestVersionLinkElement.attr("href")
            val versionDoc = Jsoup.connect(latestVersionPageUrl).userAgent(browserUserAgent).timeout(20000).get()
            val version = versionDoc.selectFirst(".appspec-value")?.text()?.trim()?.split(" ")?.first()
            val minAndroidVersion = scrapeMinVersion(versionDoc)

            VersionFetchResult(version = version, url = latestVersionPageUrl, minAndroidVersion = minAndroidVersion)
        } catch (e: Exception) {
            Log.e("BadlockFetch", "FAIL: HTML Fallback. An error occurred for URL: $url", e)
            VersionFetchResult()
        }
    }
}


fun isUpdateAvailable(moduleName: String, installedVersion: String?, latestVersion: String?): Boolean {
    if (installedVersion.isNullOrEmpty() || latestVersion.isNullOrEmpty()) return false
    try {
        val installedParts = installedVersion.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val latestParts = latestVersion.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val maxParts = maxOf(installedParts.size, latestParts.size)
        for (i in 0 until maxParts) {
            val installed = installedParts.getOrElse(i) { 0 }
            val latest = latestParts.getOrElse(i) { 0 }
            if (latest > installed) return true
            if (latest < installed) return false
        }
    } catch (e: Exception) {
        return false
    }
    return false
}

// --- ENHANCED LAUNCH INTENT FUNCTIONS ---
fun getSpecialLaunchIntent(context: Context, packageName: String, moduleName: String): Intent? {
    return when (packageName) {
        "com.samsung.android.app.clockface" -> {
            // ClockFaceSelectSetting requires WRITE_SECURE_SETTINGS, which cannot be granted
            // to third-party apps on One UI. Open Good Lock instead so the user can navigate there.
            context.packageManager.getLaunchIntentForPackage("com.samsung.android.goodlock")
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
        "com.samsung.systemui.lockstar" -> {
            val settingsLockIntent = Intent().apply {
                action = "android.intent.action.MAIN"
                component = ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.lockscreen.LockScreenSettings"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(settingsLockIntent, 0) != null) {
                return settingsLockIntent
            }

            val possibleActivities = listOf(
                "com.samsung.systemui.lockstar.presentation.ui.LockStarActivity",
                "com.samsung.systemui.lockstar.presentation.main.LockStarActivity",
                "com.samsung.systemui.lockstar.LockStarActivity",
                "com.samsung.systemui.lockstar.MainActivity"
            )
            return findWorkingActivity(context, packageName, possibleActivities)
        }
        "com.samsung.android.app.routineplus" -> {
            val modesRoutinesIntent = Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.routine.RoutineSettings"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(modesRoutinesIntent, 0) != null) {
                return modesRoutinesIntent
            }

            val bixbyRoutinesIntent = Intent().apply {
                component = ComponentName(
                    "com.samsung.android.bixby.service",
                    "com.samsung.android.bixby.routines.ui.RoutinesMainActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return if (context.packageManager.resolveActivity(bixbyRoutinesIntent, 0) != null) {
                bixbyRoutinesIntent
            } else null
        }
        "com.samsung.android.soundassistant" -> {
            val soundSettingsIntent = Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.samsung.android.settings.soundquality.SoundQualitySettings"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(soundSettingsIntent, 0) != null) {
                return soundSettingsIntent
            }

            return findBestActivityDeepSearch(context, packageName, moduleName)
        }
        else -> null
    }
}

fun isProblematicLauncherIntent(packageName: String, intent: Intent): Boolean {
    val component = intent.component?.className ?: return false
    return when (packageName) {
        "com.samsung.systemui.lockstar" -> {
            component.contains("shortcut", ignoreCase = true) || component.contains("widget", ignoreCase = true)
        }
        "com.samsung.android.app.routineplus" -> {
            component.contains("credit", ignoreCase = true) || component.contains("about", ignoreCase = true)
        }
        else -> false
    }
}
fun isProblematicActivity(packageName: String, activityName: String): Boolean {
    val problematicKeywords = listOf("shortcut", "widget", "credit", "about", "help")
    return problematicKeywords.any { keyword ->
        activityName.contains(keyword, ignoreCase = true)
    }
}

fun findWorkingActivity(context: Context, packageName: String, activityNames: List<String>): Intent? {
    val packageManager = context.packageManager

    for (activityName in activityNames) {
        try {
            val intent = Intent().apply {
                component = ComponentName(packageName, activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val activityInfo = packageManager.getActivityInfo(intent.component!!, 0)
            if (activityInfo.enabled) {
                Log.d("BadlockLaunch", "Found working activity: $activityName")
                return intent
            }
        } catch (e: Exception) {
            continue
        }
    }
    return null
}

fun findBestActivityDeepSearch(context: Context, packageName: String, moduleName: String): Intent? {
    val packageManager = context.packageManager

    try {
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        val activities = packageInfo.activities ?: return null

        val scoredActivities = activities.filter { it.exported }.map { activityInfo ->
            val score = calculateActivityScore(activityInfo.name, moduleName)
            Pair(activityInfo, score)
        }.sortedByDescending { it.second }

        val bestActivity = scoredActivities.firstOrNull()?.first
        if (bestActivity != null) {
            val intent = Intent().apply {
                component = ComponentName(bestActivity.packageName, bestActivity.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Log.d("BadlockLaunch", "Found best activity via deep search for $packageName: ${intent.component}")
            return intent
        }

    } catch (e: Exception) {
        Log.e("BadlockLaunch", "Deep search failed for $packageName", e)
    }

    return null
}

fun calculateActivityScore(activityName: String, moduleName: String): Int {
    var score = 0
    val goodKeywords = listOf("main" to 50, "home" to 40, "launcher" to 35, "settings" to 30, "ui" to 25, moduleName.lowercase().replace(" ", "") to 45)
    val badKeywords = listOf("shortcut" to -100, "widget" to -80, "credit" to -90, "about" to -70, "help" to -60, "splash" to -40, "intro" to -40)
    val lowerActivityName = activityName.lowercase()
    goodKeywords.forEach { (keyword, points) -> if (lowerActivityName.contains(keyword)) score += points }
    badKeywords.forEach { (keyword, points) -> if (lowerActivityName.contains(keyword)) score += points }
    if (lowerActivityName.endsWith("activity")) score += 10
    return score
}

fun getBestLaunchIntent(context: Context, packageName: String, moduleName: String): Intent? {
    val packageManager = context.packageManager
    try {
        val specialIntent = getSpecialLaunchIntent(context, packageName, moduleName)
        if (specialIntent != null) {
            Log.d("BadlockLaunch", "Using special intent for $moduleName: ${specialIntent.component}")
            return specialIntent
        }
        val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launcherIntent != null && !isProblematicLauncherIntent(packageName, launcherIntent)) {
            Log.d("BadlockLaunch", "Found launcher intent for $packageName: ${launcherIntent.component}")
            return launcherIntent
        }
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val activities = packageManager.queryIntentActivities(mainIntent, 0)
        if (activities.isNotEmpty()) {
            val goodActivity = activities.find { resolveInfo -> !isProblematicActivity(packageName, resolveInfo.activityInfo.name) } ?: activities[0]
            val activity = goodActivity.activityInfo
            val intent = Intent().apply {
                component = ComponentName(activity.packageName, activity.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Log.d("BadlockLaunch", "Found MAIN/LAUNCHER activity for $packageName: ${intent.component}")
            return intent
        }
        return findBestActivityDeepSearch(context, packageName, moduleName)
    } catch (e: Exception) {
        Log.e("BadlockLaunch", "Error finding launch intent for $packageName", e)
        return null
    }
}

// --- DATA LOADING FUNCTION ---
suspend fun loadData(context: Context, cacheManager: CacheManager): ModuleState {
    val packageManager = context.packageManager
    return withContext(Dispatchers.IO) {
        try {
            val allModules = coroutineScope {
                GoodLockModules.modules.map { moduleInfo ->
                    async {
                        val isInstalled = try {
                            packageManager.getPackageInfo(moduleInfo.packageName, 0); true
                        } catch (e: Exception) { false }

                        var versionResult = VersionFetchResult()
                        var installedVersion: String? = null
                        var launchIntent: Intent? = null

                        if (isInstalled) {
                            try {
                                val pkgInfo = packageManager.getPackageInfo(moduleInfo.packageName, 0)
                                installedVersion = pkgInfo.versionName
                                launchIntent = getBestLaunchIntent(context, moduleInfo.packageName, moduleInfo.name)
                            } catch (e: Exception) {
                                Log.e("BadlockLoad", "Error getting package info for ${moduleInfo.packageName}", e)
                            }

                            versionResult = try {
                                fetchLatestVersionFromRssFeed(moduleInfo.apkMirrorMainPage)
                            } catch (e: Exception) {
                                Log.w("BadlockFetch", "RSS fetch failed for ${moduleInfo.name}, trying HTML fallback.", e)
                                fetchLatestVersionFromHtmlFallback(moduleInfo.apkMirrorMainPage)
                            }
                        }

                        val resourceName = moduleInfo.name.lowercase().replace(" ", "_").replace("+", "")
                        val iconResId = context.resources.getIdentifier(resourceName, "drawable", context.packageName).let { if (it == 0) null else it }
                        val updateAvailable = isUpdateAvailable(moduleInfo.name, installedVersion, versionResult.version)

                        InstalledModule(
                            name = moduleInfo.name,
                            packageName = moduleInfo.packageName,
                            versionName = installedVersion,
                            latestVersion = versionResult.version,
                            latestVersionUrl = versionResult.url,
                            minAndroidVersion = versionResult.minAndroidVersion,
                            launchIntent = launchIntent,
                            isInstalled = isInstalled,
                            isUpdateAvailable = updateAvailable,
                            category = moduleInfo.category,
                            apkMirrorMainPage = moduleInfo.apkMirrorMainPage,
                            iconResId = iconResId
                        )
                    }
                }.map { it.await() }
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
            when(e) {
                is UnknownHostException, is SocketTimeoutException -> ModuleState.Error("Could not connect to server. Please check your internet connection.")
                else -> {
                    Log.e("BadlockLoad", "An unexpected error occurred during data load", e)
                    ModuleState.Error("An unexpected error occurred.")
                }
            }
        }
    }
}

// --- UTILITY FUNCTIONS ---
fun openRelevantSettings(context: Context, packageName: String) {
    val settingsIntent = when (packageName) {
        "com.samsung.android.app.clockface" -> Intent("android.settings.DISPLAY_SETTINGS")
        "com.samsung.systemui.lockstar" -> Intent("android.settings.SECURITY_SETTINGS")
        "com.samsung.android.app.routineplus" -> Intent("android.settings.SETTINGS")
        else -> Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
            data = Uri.fromParts("package", packageName, null)
        }
    }
    try {
        context.startActivity(settingsIntent)
    } catch (e: Exception) {
        context.startActivity(Intent("android.settings.SETTINGS"))
    }
}

fun launchModule(context: Context, module: InstalledModule) {
    try {
        val intent = getBestLaunchIntent(context, module.packageName, module.name)
            ?: module.launchIntent
        intent?.let {
            if (module.packageName == "com.samsung.android.app.clockface") {
                Toast.makeText(context, "Opening Good Lock — tap Clockface from there", Toast.LENGTH_LONG).show()
            }
            Log.d("BadlockLaunch", "Launching ${module.name} with intent: ${it.action} / ${it.component}")
            context.startActivity(it)
        } ?: run {
            Log.w("BadlockLaunch", "No launch intent available for ${module.name}")
            val message = "${module.name} needs to be configured from Samsung Settings"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            openRelevantSettings(context, module.packageName)
        }
    } catch (e: Exception) {
        Log.e("BadlockLaunch", "Failed to launch ${module.name}", e)
        Toast.makeText(context, "Could not launch ${module.name}.", Toast.LENGTH_SHORT).show()
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No browser found.", Toast.LENGTH_SHORT).show()
    }
}

fun openAppInfo(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Could not open app settings.", Toast.LENGTH_SHORT).show()
    }
}

// --- MAIN ACTIVITY & UI ---
class MainActivity : ComponentActivity() {
    private lateinit var cacheManager: CacheManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cacheManager = CacheManager(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()
        setContent {
            AppTheme {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = Color.Transparent.toArgb()
                        window.navigationBarColor = Color.Transparent.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = appColors.background) {
                    MainScreen(cacheManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(cacheManager: CacheManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var moduleState by remember { mutableStateOf<ModuleState>(cacheManager.load(context) ?: ModuleState.Loading) }

    fun refreshData(force: Boolean = false) {
        if (cacheManager.load(context) == null || force) {
            moduleState = ModuleState.Loading
        }
        coroutineScope.launch {
            val newState = loadData(context, cacheManager)
            if (newState is ModuleState.Success) {
                moduleState = newState
            } else if (newState is ModuleState.Error && cacheManager.load(context) == null) {
                moduleState = newState
            } else {
                Toast.makeText(context, "Update check failed, showing last known data.", Toast.LENGTH_SHORT).show()
                moduleState = cacheManager.load(context)!!
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val lastRefreshTime = cacheManager.getLastRefreshTime()
                val currentTime = System.currentTimeMillis()
                if (lastRefreshTime == 0L || (currentTime - lastRefreshTime) > 3 * 24 * 60 * 60 * 1000L) {
                    refreshData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onModuleClick = remember<(InstalledModule) -> Unit> {
        { module -> if (module.isInstalled) launchModule(context, module) else openUrl(context, module.apkMirrorMainPage) }
    }
    val onWebsiteClick = remember<(String) -> Unit> {
        { url -> openUrl(context, url) }
    }
    val onUpdateClick = remember<(InstalledModule) -> Unit> {
        { module -> module.latestVersionUrl?.let { openUrl(context, it) } }
    }
    val onAppInfoClick = remember<(String) -> Unit> {
        { packageName -> openAppInfo(context, packageName) }
    }
    val onOpenClick = remember<(InstalledModule) -> Unit> {
        { module -> launchModule(context, module) }
    }

    Scaffold(
        containerColor = appColors.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = moduleState) {
                is ModuleState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = appColors.accentPrimary)
                    }
                }
                is ModuleState.Error -> {
                    ErrorScreen(errorMessage = state.message, onRetry = { refreshData(force = true) })
                }
                is ModuleState.Success -> {
                    val updatableModules = remember(state.modules) {
                        state.modules.values.flatten().filter { it.isUpdateAvailable }
                    }
                    val tabs = listOf("Make up", "Life up", "Updates")
                    val pagerState = rememberPagerState(pageCount = { tabs.size })

                    val materialYouEnabled = LocalMaterialYouEnabled.current
                    val setMaterialYou = LocalMaterialYouSetter.current
                    val supportsMyou = LocalSupportsMyou.current

                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val pageTitle = tabs[page]
                            val modulesToShow = when (pageTitle) {
                                "Updates" -> updatableModules
                                else -> state.modules[pageTitle] ?: emptyList()
                            }
                            ModuleList(
                                modules = modulesToShow,
                                showEmptyMessage = (pageTitle == "Updates"),
                                onModuleClick = onModuleClick,
                                onWebsiteClick = onWebsiteClick,
                                onUpdateClick = onUpdateClick,
                                onAppInfoClick = onAppInfoClick,
                                onOpenClick = onOpenClick
                            )
                        }

                        // Top title + Material You toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            appColors.background,
                                            appColors.background.copy(alpha = 0.9f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .statusBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Cool-Lock",
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary,
                                fontSize = 20.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (supportsMyou) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Material You",
                                            color = appColors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                        Switch(
                                            checked = materialYouEnabled,
                                            onCheckedChange = { setMaterialYou(it) },
                                            modifier = Modifier.height(24.dp),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = appColors.accentPrimary,
                                                checkedTrackColor = appColors.accentPrimary.copy(alpha = 0.4f),
                                                uncheckedThumbColor = appColors.textSecondary,
                                                uncheckedTrackColor = appColors.textSecondary.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { refreshData(force = true) },
                                    enabled = moduleState != ModuleState.Loading
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = appColors.textSecondary
                                    )
                                }
                            }
                        }
                            // Gradient scrim + floating pill tab bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                // Gradient scrim
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    appColors.tabBarScrimEnd.copy(alpha = 0.5f),
                                                    appColors.tabBarScrimEnd.copy(alpha = 0.9f),
                                                    appColors.tabBarScrimEnd
                                                )
                                            )
                                        )
                                )

                                val isDark = isSystemInDarkTheme()
                                val barColor = appColors.background

                                // Blur bloom layer (behind content, purely visual)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 6.dp, vertical = 10.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .blur(30.dp)
                                        .background(barColor.copy(alpha = 0.9f))
                                        .height(52.dp)
                                        .fillMaxWidth(0.92f)
                                )
                                // Content layer on top
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 6.dp, vertical = 10.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(barColor.copy(alpha = 0.55f))
                                        .padding(horizontal = 4.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.92f),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tabs.forEachIndexed { index, title ->
                                            val isSelected = pagerState.currentPage == index
                                            val iconTint by animateColorAsState(
                                                targetValue = if (isSelected) appColors.textPrimary else appColors.textSecondary,
                                                animationSpec = tween(durationMillis = 200)
                                            )
                                            val icon = when (title) {
                                                "Updates" -> Icons.Default.SystemUpdate
                                                "Make up" -> Icons.Default.Palette
                                                else -> Icons.Default.Style
                                            }

                                            // Selector: blur bloom behind, semi-transparent fill in front
                                            Box(contentAlignment = Alignment.Center) {
                                                // Blur layer for selected tab
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .clip(RoundedCornerShape(50.dp))
                                                            .blur(20.dp)
                                                            .background(appColors.tabActive.copy(alpha = 0.8f))
                                                    )
                                                }
                                                // Actual clickable content box
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50.dp))
                                                        .background(
                                                            if (isSelected) appColors.tabActive.copy(alpha = 0.45f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                                                        .padding(horizontal = 20.dp, vertical = 7.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        if (title == "Updates" && updatableModules.isNotEmpty()) {
                                                            BadgedBox(
                                                                badge = { Badge(containerColor = appColors.badgeBg, contentColor = Color.White) { Text("${updatableModules.size}", fontSize = 9.sp) } }
                                                            ) {
                                                                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
                                                            }
                                                        } else {
                                                            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
                                                        }
                                                        Text(
                                                            title,
                                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                            color = iconTint,
                                                            fontSize = 10.sp,
                                                            letterSpacing = 0.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } // end Box(fillMaxSize)
                } // end is ModuleState.Success
            }
        }
    }
}

@Composable
fun ModuleList(
    modules: List<InstalledModule>,
    showEmptyMessage: Boolean = false,
    onModuleClick: (InstalledModule) -> Unit,
    onWebsiteClick: (String) -> Unit,
    onUpdateClick: (InstalledModule) -> Unit,
    onAppInfoClick: (String) -> Unit,
    onOpenClick: (InstalledModule) -> Unit
) {
    if (modules.isEmpty() && showEmptyMessage) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = "All up to date", tint = appColors.textSecondary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("All Clear!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("All your modules are up-to-date.", color = appColors.textSecondary, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = modules, key = { it.packageName }) { module ->
                ModuleCard(
                    module = module,
                    onModuleClick = { onModuleClick(module) },
                    onWebsiteClick = { onWebsiteClick(module.apkMirrorMainPage) },
                    onUpdateClick = { onUpdateClick(module) },
                    onAppInfoClick = { onAppInfoClick(module.packageName) },
                    onOpenClick = { onOpenClick(module) }
                )
            }
        }
    }
}

@Composable
fun ModuleCard(
    module: InstalledModule,
    onModuleClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(50.dp)
    Card(
        shape = pillShape,
        colors = CardDefaults.cardColors(containerColor = appColors.cardBackground),
        modifier = Modifier.fillMaxWidth().clip(pillShape).clickable(onClick = onAppInfoClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(appColors.iconBox),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = module.iconResId?.let { painterResource(id = it) } ?: painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "${module.name} icon",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(module.name, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                VersionInfo(module)
            }
            Spacer(Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (module.isInstalled) {
                    if (module.isUpdateAvailable) {
                        Button(
                            onClick = onUpdateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = appColors.updateButtonBg, contentColor = appColors.buttonTextColor),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp).widthIn(min = 80.dp)
                        ) { Text("Update", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                    Button(
                        onClick = onOpenClick,
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.openButtonBg, contentColor = appColors.buttonTextColor),
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp).widthIn(min = 80.dp)
                    ) { Text("Open", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                } else {
                    Button(
                        onClick = onWebsiteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.installButtonBg, contentColor = appColors.buttonTextColor),
                        shape = RoundedCornerShape(50.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp).widthIn(min = 80.dp)
                    ) { Text("Install", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
            IconButton(onClick = onWebsiteClick) {
                Icon(Icons.Default.Public, contentDescription = "Go to Website", tint = appColors.websiteIconTint)
            }
        }
    }
}

@Composable
fun VersionInfo(module: InstalledModule) {
    if (module.isInstalled) {
        val currentColor = if (module.isUpdateAvailable) appColors.currentVersionText else appColors.textSecondary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Current: ", color = currentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("v${module.versionName ?: "N/A"}", color = currentColor.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
        }
    } else {
        Text("Not Installed", color = appColors.textSecondary, fontSize = 12.sp, maxLines = 1)
    }

    if (module.latestVersion != null) {
        val color = if (module.isUpdateAvailable) appColors.updateLatestText else appColors.textSecondary
        Text("Latest: v${module.latestVersion}", color = color, fontSize = 12.sp, maxLines = 1)

        val minVersionText = if (!module.minAndroidVersion.isNullOrBlank()) {
            module.minAndroidVersion
        } else {
            "N/A"
        }
        Text("Requires: $minVersionText", color = appColors.textSecondary, fontSize = 12.sp, maxLines = 1)
    }

    if (module.latestVersion == null && module.isInstalled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, contentDescription = "Error fetching version", tint = appColors.textSecondary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Update check failed", color = appColors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.SignalWifiOff, contentDescription = "Connection Error", tint = appColors.textSecondary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connection Error", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage, color = appColors.textSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = appColors.accentPrimary)) {
            Text("Retry", color = Color.White)
        }
    }
}
