package com.mod4.cool_lock.logic

import android.os.Build
import android.util.Log
import com.mod4.cool_lock.data.AppUpdateInfo
import com.mod4.cool_lock.data.VersionFetchResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object UpdateChecker {

    /** Cool-Lock's own releases (used by the in-app self-update dialog). */
    private const val APP_RELEASES_API = "https://api.github.com/repos/M0d-4/Cool-Lock/releases/latest"

    fun isUpdateAvailable(current: String?, latest: String?): Boolean {
        if (current.isNullOrEmpty() || latest.isNullOrEmpty()) return false
        try {
            val cParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
            val lParts = latest.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
            for (i in 0 until maxOf(cParts.size, lParts.size)) {
                val lVal = lParts.getOrElse(i) { 0 }
                val cVal = cParts.getOrElse(i) { 0 }
                if (lVal > cVal) return true
                if (lVal < cVal) return false
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /** Checks the latest GitHub release of Cool-Lock itself, including its .apk asset. Returns null on any failure. */
    suspend fun checkAppUpdate(): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val json = Jsoup.connect(APP_RELEASES_API)
                    .ignoreContentType(true)
                    .userAgent("Cool-Lock-Update-Checker")
                    .header("Accept", "application/vnd.github+json")
                    .timeout(10000)
                    .execute()
                    .body()

                @Suppress("UNCHECKED_CAST")
                val release = Gson().fromJson(json, Map::class.java) as Map<String, Any?>
                val tagName = release["tag_name"] as? String ?: return@withContext null
                val htmlUrl = release["html_url"] as? String ?: return@withContext null
                val body = release["body"] as? String

                @Suppress("UNCHECKED_CAST")
                val assets = release["assets"] as? List<Map<String, Any?>> ?: emptyList()
                val apkAssetUrl = assets
                    .firstOrNull { (it["name"] as? String)?.endsWith(".apk", ignoreCase = true) == true }
                    ?.get("browser_download_url") as? String

                AppUpdateInfo(
                    latestVersion = tagName.trim().removePrefix("v").removePrefix("V"),
                    downloadUrl = htmlUrl,
                    apkAssetUrl = apkAssetUrl,
                    releaseNotes = body
                )
            } catch (e: Exception) {
                Log.e("CoolLockUpdate", "Failed to check for app updates", e)
                null
            }
        }
    }

    private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

    private fun createJsoupConnection(url: String, timeoutMs: Int = 20000) = Jsoup.connect(url)
        .userAgent(BROWSER_USER_AGENT)
        .timeout(timeoutMs)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Cache-Control", "no-cache")
        .header("Connection", "keep-alive")
        .header("Sec-Ch-Ua", "\"Not(A:Brand\";v=\"99\", \"Google Chrome\";v=\"133\", \"Chromium\";v=\"133\"")
        .header("Sec-Ch-Ua-Mobile", "?1")
        .header("Sec-Ch-Ua-Platform", "\"Android\"")

    fun getDeviceArchitecture(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "arm64-v8a"
            abi.contains("v7") -> "armeabi-v7a"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> abi
        }
    }

    /** The device's own API level, used to filter out module updates the device can't actually run. */
    fun getDeviceApiLevel(): Int = Build.VERSION.SDK_INT

    // Best-effort Android-version -> API-level table for turning APKMirror's "Android 7.0+" text
    // into something comparable to Build.VERSION.SDK_INT. Covers released versions; entries for
    // versions newer than this app's own knowledge are approximate.
    private val VERSION_TO_API: Map<String, Int> = mapOf(
        "1.0" to 1, "1.1" to 2, "1.5" to 3, "1.6" to 4,
        "2.0" to 5, "2.0.1" to 6, "2.1" to 7, "2.2" to 8, "2.3" to 9, "2.3.3" to 10,
        "3.0" to 11, "3.1" to 12, "3.2" to 13,
        "4.0" to 14, "4.0.3" to 15, "4.1" to 16, "4.2" to 17, "4.3" to 18, "4.4" to 19,
        "5.0" to 21, "5.1" to 22, "6.0" to 23, "7.0" to 24, "7.1" to 25,
        "8.0" to 26, "8.1" to 27, "9" to 28, "9.0" to 28, "10" to 29, "11" to 30,
        "12" to 31, "12.1" to 32, "13" to 33, "14" to 34, "15" to 35, "16" to 36, "17" to 37
    )

    /** Parses cleaned min-version text ("Android 7.0+", "API 24", "Android 12") into an API level, or null if unrecognized. */
    fun parseApiLevel(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val t = text.trim()
        Regex("""(?i)API\s*(\d+)""").find(t)?.let { return it.groupValues[1].toIntOrNull() }
        Regex("""(?i)Android\s*(\d+(?:\.\d+)*)\+?""").find(t)?.let { m ->
            val ver = m.groupValues[1]
            VERSION_TO_API[ver]?.let { return it }
            // Fall back to matching on the major version only (e.g. "7.2" -> treat like "7.0")
            val major = ver.substringBefore('.')
            return VERSION_TO_API.entries.firstOrNull { it.key.substringBefore('.') == major }?.value
        }
        return null
    }

    private data class VariantRow(val url: String, val archText: String, val minVersionRaw: String?, val minApi: Int?)

    /**
     * Reads every APK variant listed for a version page (architecture + "Min Version" column when
     * present in the table itself), so different variants with different minimum Android version
     * requirements can be told apart without a page fetch per variant.
     */
    private fun scrapeVariantRows(doc: Document): List<VariantRow> {
        val rows = mutableListOf<VariantRow>()
        try {
            val tableRows = doc.select(".variants-table .table-row, table tr").drop(1) // drop header
            for (row in tableRows) {
                val cells = row.select(".table-cell, td")
                if (cells.size < 2) continue
                val linkEl = cells.first()?.selectFirst("a") ?: continue
                val url = "https://www.apkmirror.com" + linkEl.attr("href")
                val cellTexts = cells.map { it.text().trim() }
                val archText = cellTexts.joinToString(" ")
                // The min-OS cell reads like "Android 7.0+" and never contains "dpi"
                val minCell = cellTexts.firstOrNull {
                    !it.contains("dpi", ignoreCase = true) &&
                        Regex("""(?i)(android\s*\d|api\s*\d)""").containsMatchIn(it)
                }
                rows.add(VariantRow(url, archText, minCell, parseApiLevel(minCell)))
            }
        } catch (e: Exception) {
            Log.e("CoolLockFetch", "Error scraping variant rows", e)
        }
        return rows
    }

    /**
     * Among a version's variants, picks the one the device can actually run: its minimum Android
     * version must be <= the device's, and among those, the HIGHEST minimum version wins (i.e. the
     * newest/most-optimized build the device still qualifies for). Falls back to fetching a small
     * number of variant sub-pages when the table itself doesn't list a min version. Returns null if
     * every variant found needs a newer Android version than this device has.
     */
    private suspend fun pickCompatibleVariant(doc: Document, preferredArch: String, deviceApi: Int): VariantRow? {
        var rows = scrapeVariantRows(doc)
        if (rows.isEmpty()) return null

        // Prefer rows matching the device's architecture (or universal/no-arch builds) first,
        // since that's what we'd fall back on anyway; check at most a handful of sub-pages.
        if (rows.none { it.minApi != null }) {
            val archMatch = rows.filter {
                it.archText.contains(preferredArch, ignoreCase = true) ||
                    it.archText.contains("universal", ignoreCase = true) ||
                    it.archText.contains("no arch", ignoreCase = true) ||
                    it.archText.contains("noarch", ignoreCase = true)
            }
            val candidates = (archMatch.ifEmpty { rows }).take(6)
            rows = candidates.map { row ->
                if (row.minApi != null) return@map row
                try {
                    val minText = scrapeMinVersion(createJsoupConnection(row.url).get())
                    row.copy(minVersionRaw = minText ?: row.minVersionRaw, minApi = parseApiLevel(minText))
                } catch (e: Exception) {
                    row
                }
            }
        }

        val compatible = rows.filter { it.minApi == null || it.minApi <= deviceApi }
        if (compatible.isEmpty()) return null

        fun archScore(r: VariantRow) = when {
            r.archText.contains(preferredArch, ignoreCase = true) -> 2
            r.archText.contains("universal", ignoreCase = true) || r.archText.contains("no arch", ignoreCase = true) -> 1
            else -> 0
        }

        return compatible.sortedWith(
            compareByDescending<VariantRow> { it.minApi ?: -1 }.thenByDescending { archScore(it) }
        ).first()
    }

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
            Log.w("CoolLockScrape", "Table parsing failed: ${e.message}")
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
            Log.w("CoolLockScrape", "Appspec parsing failed: ${e.message}")
        }

        return null
    }

    suspend fun fetchLatestVersionFromRssFeed(url: String): VersionFetchResult {
        val feedUrl = if (url.endsWith("/")) "${url}feed/" else "$url/feed/"
        return withContext(Dispatchers.IO) {
            try {
                val doc = createJsoupConnection(feedUrl, 15000).get()
                val firstItem = doc.selectFirst("item") ?: return@withContext VersionFetchResult()

                val title = firstItem.selectFirst("title")?.text() ?: ""
                val link = firstItem.selectFirst("link")?.text()

                val version = """(\d+(\.\d+)+)""".toRegex().find(title)?.value?.trim()

                var minAndroidVersion: String? = null
                var variantUrl: String? = null
                if (link != null) {
                    try {
                        val versionDoc = createJsoupConnection(link).get()
                        val best = pickCompatibleVariant(versionDoc, getDeviceArchitecture(), getDeviceApiLevel())
                            ?: return@withContext VersionFetchResult() // no variant this device qualifies for
                        minAndroidVersion = best.minVersionRaw
                        variantUrl = best.url
                    } catch (e: Exception) {
                        Log.w("CoolLockFetch", "Could not fetch details from $link", e)
                    }
                }

                VersionFetchResult(version = version, url = link, variantUrl = variantUrl, minAndroidVersion = minAndroidVersion)
            } catch (e: Exception) {
                Log.e("CoolLockFetch", "RSS fetch failed for $url", e)
                throw e
            }
        }
    }

    suspend fun fetchLatestVersionFromHtmlFallback(url: String): VersionFetchResult {
        return withContext(Dispatchers.IO) {
            try {
                val mainDoc = createJsoupConnection(url).get()
                // Only plain APK rows (bundles are harder to parse/install)
                val versionElements = mainDoc.select("#primary div.list-row a.fontBlack")
                    .filter { it.text().contains("APK", ignoreCase = true) }

                if (versionElements.isEmpty()) {
                    Log.w("CoolLockFetch", "No valid APK version links found for $url")
                    return@withContext VersionFetchResult()
                }

                val regex = """(\d+(\.\d+)+)""".toRegex()
                val foundVersions = versionElements.take(10).mapNotNull { element ->
                    val ver = regex.find(element.text())?.value?.trim()
                    if (ver != null) Pair(ver, "https://www.apkmirror.com" + element.attr("href")) else null
                }
                if (foundVersions.isEmpty()) return@withContext VersionFetchResult()

                // Newest of the top rows, compared numerically
                val latestEntry = foundVersions.maxByOrNull { (ver, _) ->
                    ver.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }.let { parts ->
                        List(6) { i -> parts.getOrElse(i) { 0 } }.joinToString(",") { it.toString().padStart(5, '0') }
                    }
                } ?: foundVersions[0]

                val (version, latestVersionPageUrl) = latestEntry
                var minAndroidVersion: String? = null
                var variantUrl: String? = null
                try {
                    val versionDoc = createJsoupConnection(latestVersionPageUrl).get()
                    val best = pickCompatibleVariant(versionDoc, getDeviceArchitecture(), getDeviceApiLevel())
                        ?: return@withContext VersionFetchResult() // no variant this device qualifies for
                    minAndroidVersion = best.minVersionRaw
                    variantUrl = best.url
                } catch (e: Exception) {
                    Log.w("CoolLockFetch", "Could not fetch details from $latestVersionPageUrl", e)
                }

                VersionFetchResult(version = version, url = latestVersionPageUrl, variantUrl = variantUrl, minAndroidVersion = minAndroidVersion)
            } catch (e: Exception) {
                Log.e("CoolLockFetch", "FAIL: HTML Fallback. An error occurred for URL: $url", e)
                VersionFetchResult()
            }
        }
    }
}
