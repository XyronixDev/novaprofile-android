// File-level opt-in (Run #5 fix): TopAppBar / ExposedDropdownMenuBox /
// FilterChip are @ExperimentalMaterial3Api in the BOM 2026.08.00
// material3 version — the per-function @OptIn missed EditorScreen's
// TopAppBar (line 421), failing :app:compileReleaseKotlin.
// NOTE (Run #7): a @file: annotation MUST sit before the package line —
// after it, the parser rejects the imports ("imports are only allowed in
// the beginning of file"). That was my placement mistake.
@file:OptIn(ExperimentalMaterial3Api::class)
package com.novaprofiles.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.novaprofiles.app.BuildConfig
import com.novaprofiles.app.util.ProxyEntry
import com.novaprofiles.app.util.ProxyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaprofiles.app.data.Profile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaProfileTheme {
                MainScreen()
            }
        }
    }
}

/**
 * v2.7.2: the card flag comes from the profile's stored proxy country
 * first — that is the real proxy. Language is only a fallback (old
 * profiles created before proxyCountry existed).
 */
fun countryOf(profile: Profile): Pair<String, String> {
    val pc = profile.proxyCountry.trim().uppercase()
    if (pc.length == 2 && pc in Profile.pickerCountries) {
        return Profile.flagEmoji(pc) to pc
    }
    return when {
        profile.language.startsWith("en-GB") -> "🇬🇧" to "GB"
        profile.language.startsWith("en-CA") -> "🇨🇦" to "CA"
        profile.language.startsWith("fr") -> "🇫🇷" to "FR"
        profile.language.startsWith("de") -> "🇩🇪" to "DE"
        profile.language.startsWith("es") -> "🇪🇸" to "ES"
        profile.language.startsWith("pt") -> "🇧🇷" to "BR"
        profile.language.startsWith("it") -> "🇮🇹" to "IT"
        profile.language.startsWith("nl") -> "🇳🇱" to "NL"
        profile.language.startsWith("ja") -> "🇯🇵" to "JP"
        profile.language.startsWith("ko") -> "🇰🇷" to "KR"
        profile.language.startsWith("zh") -> "🇨🇳" to "CN"
        profile.language.startsWith("ru") -> "🇷🇺" to "RU"
        profile.language.startsWith("pl") -> "🇵🇱" to "PL"
        profile.language.startsWith("tr") -> "🇹🇷" to "TR"
        profile.language.startsWith("ar") -> "🇸🇦" to "SA"
        else -> "🇺🇸" to "US"
    }
}

/**
 * v2.7.2: resolve a saved proxy's country to a 2-letter code. Sources
 * disagree on the format — "US • United States" (ProxyScrape/GitHub),
 * a bare full name "United States" (Geonode), or a bare code "US".
 * The old code only handled the first form, so Geonode proxies never
 * updated the language and the card kept showing the old country.
 */
fun proxyCountryCode(entryCountry: String): String? {
    val token = entryCountry.substringBefore(" •").trim()
    if (token.length == 2 && token.uppercase() in Profile.pickerCountries) {
        return token.uppercase()
    }
    return Profile.pickerCountries.firstOrNull {
        Profile.countrySettings(it).label.equals(token, ignoreCase = true)
    }
}

/**
 * v2.7.3: parse the manual Add-proxy IP field ("198.165.0.0:8080" or
 * a bare host — port defaults to 8080). Returns null when the field
 * cannot be a proxy address; the Add button stays disabled then.
 */
fun parseManualIp(raw: String): Pair<String, Int>? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val lastColon = t.lastIndexOf(':')
    val host: String
    val port: Int
    if (lastColon > 0) {
        host = t.substring(0, lastColon).trim()
        port = t.substring(lastColon + 1).trim().toIntOrNull() ?: return null
    } else {
        host = t
        port = 8080
    }
    if (host.isEmpty() || port !in 1..65535) return null
    return host to port
}

/**
 * v2.8.24: city for a profile card ("London, United Kingdom") from the
 * profile's timezone. Known zones map to their city; anything else uses
 * the zone's city part (after the "/") with underscores as spaces.
 */
fun cityFromTimezone(tz: String): String = when (tz) {
    "America/New_York" -> "New York"
    "America/Chicago" -> "Chicago"
    "America/Denver" -> "Denver"
    "America/Los_Angeles" -> "Los Angeles"
    "America/Toronto" -> "Toronto"
    "America/Sao_Paulo" -> "Sao Paulo"
    "Europe/London" -> "London"
    "Europe/Paris" -> "Paris"
    "Europe/Berlin" -> "Berlin"
    "Europe/Amsterdam" -> "Amsterdam"
    "Europe/Rome" -> "Rome"
    "Europe/Madrid" -> "Madrid"
    "Europe/Stockholm" -> "Stockholm"
    "Europe/Zurich" -> "Zurich"
    "Europe/Dublin" -> "Dublin"
    "Asia/Tokyo" -> "Tokyo"
    "Asia/Kolkata" -> "Kolkata"
    "Australia/Sydney" -> "Sydney"
    "Pacific/Auckland" -> "Auckland"
    "UTC" -> "UTC"
    "Auto" -> ""
    else -> tz.substringAfter("/", tz).replace("_", " ")
}

/** v2.8.24: (city, country label) for a profile card. */
fun profileCityCountry(profile: Profile): Pair<String, String> {
    val (_, code) = countryOf(profile)
    return cityFromTimezone(profile.timezone) to Profile.countrySettings(code).label
}

/** v2.8.24: the saved entry for a profile's proxy (matched by host:port). */
fun proxyEntryFor(profile: Profile, proxies: List<ProxyEntry>): ProxyEntry? =
    proxies.firstOrNull { it.host == profile.proxyHost && it.port == profile.proxyPort }

/**
 * v2.8.24: Active vs Offline on the profile card. Direct profiles are
 * Active (nothing to fail). A proxied profile is Active only when its
 * saved entry was tested working (ok + a real IP); an untested or
 * failed proxy reads Offline.
 */
fun isProfileActive(profile: Profile, proxies: List<ProxyEntry>): Boolean {
    if (profile.proxyMode == "NONE") return true
    val entry = proxyEntryFor(profile, proxies) ?: return false
    return entry.ok && entry.lastIp.isNotBlank()
}

/**
 * v2.8.24: parse one line of a proxy .txt file (the Webshare format
 * "IP:port:user:pass", a bare "host:port", or a "socks5://..." URL with
 * optional "user:pass@" userinfo). SOCKS5 only — the mode is fixed.
 * Returns null for blank, comment (#) or unparsable lines.
 */
fun parseProxyFileLine(raw: String): ProxyEntry? {
    var line = raw.trim()
    if (line.isEmpty() || line.startsWith("#")) return null
    var user = ""
    var pass = ""
    val schemeIdx = line.indexOf("://")
    if (schemeIdx >= 0) line = line.substring(schemeIdx + 3)
    val atIdx = line.lastIndexOf('@')
    if (atIdx >= 0) {
        val creds = line.substring(0, atIdx).split(":")
        if (creds.size >= 2) {
            user = creds[0]
            pass = creds.drop(1).joinToString(":")
        }
        line = line.substring(atIdx + 1)
    }
    val parts = line.split(":")
    if (parts.size < 2) return null
    val host = parts[0].trim()
    val port = parts[1].trim().toIntOrNull() ?: return null
    if (host.isEmpty() || port !in 1..65535) return null
    if (parts.size >= 4) {
        user = parts[2].trim()
        pass = parts.drop(3).joinToString(":").trim()
    }
    return ProxyEntry(name = "", mode = "SOCKS5", host = host, port = port, user = user, pass = pass)
}

/** v2.8.24: next auto name ("Myproxy #1", "#2"…) when the name is blank. */
fun nextAutoProxyName(proxies: List<ProxyEntry>): String {
    val max = proxies.mapNotNull {
        Regex("""^Myproxy #(\d+)$""").find(it.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return "Myproxy #${max + 1}"
}

/**
 * v2.8.24: apply a saved proxy entry to a profile (pure — the caller
 * decides whether the result is a draft or a vm.update). Timezone +
 * language + coordinates follow the proxy's IP, but only when the
 * profile's modes say IP-based; Custom keeps the user's own values.
 * The country code is stored on the profile so the card flag reads the
 * real proxy country (same logic as the old ProxiesTab onAssignProxy).
 */
fun profileWithProxy(p: Profile, entry: ProxyEntry): Profile {
    val code = proxyCountryCode(entry.country)
    val cs = code?.let { Profile.countrySettings(it) }
    return p.copy(
        proxyMode = entry.mode,
        proxyHost = entry.host,
        proxyPort = entry.port,
        proxyUser = entry.user,
        proxyPass = entry.pass,
        proxyCountry = code ?: p.proxyCountry,
        timezone = if (p.timezoneMode == "IP")
            entry.lastTimezone.ifBlank { p.timezone } else p.timezone,
        language = if (p.languagesMode == "IP" && cs != null)
            cs.language else p.language,
        latitude = if (p.geoMode == "IP" && entry.lastLat != 0.0)
            entry.lastLat else p.latitude,
        longitude = if (p.geoMode == "IP" && entry.lastLon != 0.0)
            entry.lastLon else p.longitude
    )
}

/** v2.8.24: premium home navigation — one route per screenshot screen. */
private enum class HomeRoute { PROFILES, PROXY, SETTINGS, FORM }

/** v2.8.24: the Profiles-list filter behind the funnel button. */
private enum class ProfileFilter { ALL, ACTIVE, OFFLINE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ProfileViewModel = viewModel()) {
    val profiles by vm.profiles.collectAsState()
    val query by vm.query.collectAsState()
    val proxyCheck by vm.proxyCheckResult.collectAsState()
    val importResult by vm.importResult.collectAsState()
    val exportResult by vm.exportResult.collectAsState()

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val proxyStore = remember { ProxyStore(ctx) }
    val savedProxies by proxyStore.proxies.collectAsState()

    var route by remember { mutableStateOf(HomeRoute.PROFILES) }
    // v2.8.24: the Add/Edit form draft lives here (not in the form) so
    // it survives the trip to the Add Proxy screen and back.
    var draft by remember { mutableStateOf<Profile?>(null) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var proxyReturnToForm by remember { mutableStateOf(false) }
    var runningIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var syncingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var labProfile by remember { mutableStateOf<Profile?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }
    var filter by remember { mutableStateOf(ProfileFilter.ALL) }

    /** Sessions (v2.0 Pro): tell a running browser to stop itself. */
    fun stopProfile(id: Long?) {
        val intent = Intent("com.novaprofiles.app.STOP_PROFILE").apply {
            setPackage(ctx.packageName)
            if (id != null) putExtra("profile_id", id)
        }
        ctx.sendBroadcast(intent)
        runningIds = if (id == null) emptySet() else runningIds - id
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.let { out ->
                vm.exportAll(out, profiles)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ctx.contentResolver.openInputStream(uri)?.let { input ->
                vm.importFrom(input)
            }
        }
    }

    fun openAddForm() {
        val max = profiles.mapNotNull {
            Regex("""^Profile (\d+)$""").find(it.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull() ?: 0
        // v2.8.25: Add starts with an EMPTY name — the reference shows
        // the "Enter profile name..." placeholder with 0/50, not a
        // pre-filled "Profile N". (Profile.new() would re-fill it.)
        draft = Profile.new("Profile ${max + 1}").copy(name = "")
        editingId = null
        route = HomeRoute.FORM
    }

    fun openEditForm(p: Profile) {
        draft = p.copy()
        editingId = p.id
        route = HomeRoute.FORM
    }

    fun cancelForm() {
        draft = null
        editingId = null
        route = HomeRoute.PROFILES
    }

    fun saveDraft() {
        val d = draft ?: return
        if (editingId != null) vm.update(d) else vm.create(d)
        draft = null
        editingId = null
        route = HomeRoute.PROFILES
    }

    fun launchProfile(p: Profile) {
        // v2.6 smart 90% fix: before opening the browser, re-resolve
        // the proxy IP's CURRENT timezone + coordinates (one ip-api
        // call through the proxy) and save them, so the run never uses
        // a stale timezone. Free US proxies sit in Mountain View
        // (America/Los_Angeles) while the country default is New York
        // — a 3-hour mismatch whoer deducts for. On lookup failure,
        // launch with the stored values instead of blocking.
        if (p.proxyMode != "NONE" &&
            (p.timezoneMode == "IP" || p.geoMode == "IP")
        ) {
            syncingIds = syncingIds + p.id
            scope.launch {
                val entry = ProxyEntry(
                    name = p.name,
                    mode = p.proxyMode, host = p.proxyHost,
                    port = p.proxyPort, user = p.proxyUser,
                    pass = p.proxyPass
                )
                val loc = proxyStore.resolveLocation(entry)
                if (loc != null) {
                    val updated = p.copy(
                        timezone = if (p.timezoneMode == "IP") loc.timezone else p.timezone,
                        latitude = if (p.geoMode == "IP" && loc.lat != 0.0) loc.lat else p.latitude,
                        longitude = if (p.geoMode == "IP" && loc.lon != 0.0) loc.lon else p.longitude
                    )
                    vm.updateNow(updated)
                    val saved = proxyStore.proxies.value.firstOrNull {
                        it.host == p.proxyHost && it.port == p.proxyPort
                    }
                    if (saved != null) {
                        proxyStore.update(
                            saved.copy(
                                lastIp = loc.ip, lastTimezone = loc.timezone,
                                lastLat = loc.lat, lastLon = loc.lon, ok = true
                            )
                        )
                    }
                }
                syncingIds = syncingIds - p.id
                runningIds = runningIds + p.id
                vm.markRun(p.id)
                ctx.startActivity(BrowserActivity.intent(ctx, p.id))
            }
        } else {
            runningIds = runningIds + p.id
            vm.markRun(p.id)
            ctx.startActivity(BrowserActivity.intent(ctx, p.id))
        }
    }

    // System back: form/proxy/settings return to the list; the list exits.
    BackHandler(enabled = route != HomeRoute.PROFILES) {
        when (route) {
            HomeRoute.FORM -> cancelForm()
            HomeRoute.PROXY -> {
                if (proxyReturnToForm) {
                    proxyReturnToForm = false
                    route = HomeRoute.FORM
                } else {
                    route = HomeRoute.PROFILES
                }
            }
            HomeRoute.SETTINGS -> route = HomeRoute.PROFILES
            HomeRoute.PROFILES -> { /* unreachable — handler disabled */ }
        }
    }

    // Fingerprint Lab (v2.0 Pro) — full-screen check for one profile.
    labProfile?.let { lp ->
        FingerprintLabScreen(profile = lp, onBack = { labProfile = null })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoContentBg)
    ) {
        BottomWave(modifier = Modifier.align(Alignment.BottomCenter))
        when (route) {
            HomeRoute.PROFILES -> ProfilesScreen(
                profiles = profiles,
                query = query,
                onQuery = { vm.query.value = it },
                filter = filter,
                onFilter = { filter = it },
                savedProxies = savedProxies,
                runningIds = runningIds,
                syncingIds = syncingIds,
                onLaunch = { launchProfile(it) },
                onStop = { stopProfile(it.id) },
                onStopAll = { stopProfile(null) },
                onEdit = { openEditForm(it) },
                onDelete = { pendingDelete = it },
                onDuplicate = { vm.duplicate(it) },
                onCheckFingerprint = { labProfile = it },
                onOpenSettings = { route = HomeRoute.SETTINGS },
                onOpenProxy = { route = HomeRoute.PROXY },
                onAddProfile = { openAddForm() },
                onShowAbout = { showAbout = true }
            )
            HomeRoute.PROXY -> AddProxyScreen(
                proxyStore = proxyStore,
                savedProxies = savedProxies,
                scope = scope,
                draft = draft,
                onApplyToDraft = { entry ->
                    draft = draft?.let { profileWithProxy(it, entry) }
                    proxyReturnToForm = false
                    route = HomeRoute.FORM
                },
                onDirectForDraft = {
                    draft = draft?.copy(
                        proxyMode = "NONE", proxyHost = "", proxyPort = 8080,
                        proxyUser = "", proxyPass = "", proxyCountry = ""
                    )
                    proxyReturnToForm = false
                    route = HomeRoute.FORM
                },
                onBack = {
                    if (proxyReturnToForm) {
                        proxyReturnToForm = false
                        route = HomeRoute.FORM
                    } else {
                        route = HomeRoute.PROFILES
                    }
                },
                onOpenProfiles = { route = HomeRoute.PROFILES },
                onOpenSettings = { route = HomeRoute.SETTINGS },
                onAddProfile = { openAddForm() }
            )
            HomeRoute.SETTINGS -> SettingsScreen(
                onExport = { exportLauncher.launch("novaprofile-profiles.zip") },
                onImport = { importLauncher.launch(arrayOf("*/*")) },
                onAbout = { showAbout = true },
                notice = importResult ?: exportResult,
                onOpenProfiles = { route = HomeRoute.PROFILES },
                onOpenSettings = { },
                onAddProfile = { openAddForm() }
            )
            HomeRoute.FORM -> {
                val d = draft
                if (d != null) {
                    AddEditFormScreen(
                        draft = d,
                        editing = editingId != null,
                        onDraft = { draft = it },
                        onSave = { saveDraft() },
                        onCancel = { cancelForm() },
                        onDeleteToTrash = {
                            vm.moveToTrash(d)
                            draft = null
                            editingId = null
                            route = HomeRoute.PROFILES
                        },
                        onOpenProxyPicker = {
                            proxyReturnToForm = true
                            route = HomeRoute.PROXY
                        },
                        onCheckProxy = { p -> vm.checkProxy(p, "https://api.ipify.org") },
                        proxyCheckResult = proxyCheck,
                        onClearNotice = { vm.clearNotices() },
                        proxyStore = proxyStore,
                        onCheckFingerprint = { labProfile = it }
                    )
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About NovaProfile") },
            text = {
                Text(
                    "NovaProfile ${BuildConfig.VERSION_NAME} " +
                        "(Build ${BuildConfig.VERSION_CODE}) — original " +
                        "multi-profile browser.\n\n" +
                        "• Unlimited profiles, everything free, no billing, no trial.\n" +
                        "• Per-profile cookies, proxy, user-agent, language, " +
                        "screen, WebGL and Canvas settings.\n" +
                        "• Multi-tab browser with history, bookmarks, downloads, " +
                        "desktop mode, find in page and translate.\n" +
                        "• Pro: Fingerprint Lab, Proxy Manager, templates, sessions.\n" +
                        "• Fingerprint protection is JS-level inside the system " +
                        "WebView — it is NOT a Chromium fork and advanced " +
                        "fingerprint tests can still detect the real device.\n" +
                        "• One profile runs at a time. Data never leaves your phone."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("OK") }
            }
        )
    }

    // v2.7.2: delete is permanent — one confirm, no trash step.
    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete profile?") },
            text = { Text("\"${p.name}\" will be deleted permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteForever(p)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * v2.8.24: the premium wave header — dark green, with the layered wave
 * edge from the reference screenshots, drawn on a Canvas (no assets).
 * [content] is the header row (back/logo/title/premium or bell/gear).
 */
@Composable
private fun WaveHeader(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GoHeaderDark)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopStart
        ) {
            content()
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
            val w = size.width
            val h = size.height
            // Mint wave band underneath.
            val mintPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.55f)
                cubicTo(w * 0.75f, h * 1.1f, w * 0.45f, h * 0.1f, w * 0.2f, h * 0.7f)
                cubicTo(w * 0.08f, h * 0.95f, w * 0.03f, h * 0.8f, 0f, h * 0.6f)
                close()
            }
            drawPath(mintPath, Color(0xFF9BE8C8))
            // Dark wave on top, slightly higher.
            val darkPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.35f)
                cubicTo(w * 0.7f, h * 0.9f, w * 0.4f, -h * 0.1f, w * 0.15f, h * 0.5f)
                cubicTo(w * 0.06f, h * 0.7f, w * 0.02f, h * 0.6f, 0f, h * 0.4f)
                close()
            }
            drawPath(darkPath, GoHeaderDark)
        }
    }
}

/**
 * v2.8.24: the green wave rising from the bottom of the screen (behind
 * the bottom nav / buttons), drawn on a Canvas (no assets).
 */
@Composable
private fun BottomWave(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(56.dp)) {
        val w = size.width
        val h = size.height
        val lightPath = Path().apply {
            moveTo(0f, h)
            lineTo(w, h)
            lineTo(w, h * 0.45f)
            cubicTo(w * 0.7f, h * 0.1f, w * 0.4f, h * 0.9f, w * 0.15f, h * 0.4f)
            cubicTo(w * 0.06f, h * 0.2f, w * 0.02f, h * 0.5f, 0f, h * 0.35f)
            close()
        }
        drawPath(lightPath, Color(0xFF4ADE9E))
        val darkPath = Path().apply {
            moveTo(0f, h)
            lineTo(w, h)
            lineTo(w, h * 0.7f)
            cubicTo(w * 0.75f, h * 0.3f, w * 0.45f, h * 1.0f, w * 0.2f, h * 0.65f)
            cubicTo(w * 0.08f, h * 0.45f, w * 0.03f, h * 0.7f, 0f, h * 0.6f)
            close()
        }
        drawPath(darkPath, GoHeaderDark)
    }
}

/**
 * v2.8.24: the NovaProfile infinity mark. The app ships no infinity
 * asset (ic_launcher_foreground is a white "N"), so the mark from the
 * reference screenshots is drawn with the standard infinity glyph —
 * no new logo is invented.
 */
@Composable
private fun InfinityLogo(color: Color, fontSize: Int) {
    Text(
        "∞",
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

/** v2.8.25: the gold Premium pill (decorative text only — no paywall). One line, never wrapped. */
@Composable
private fun PremiumPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, GoGold, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("♛", color = GoGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Spacer(Modifier.width(4.dp))
        Text("Premium", color = GoGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** v2.8.25: the "NovaProfile / Anti-Detect Browser" title block. One line each, never wrapped. */
@Composable
private fun BrandTitle(modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nova", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Text("Profile", color = GoGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Text("Anti-Detect Browser", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
    }
}

/**
 * v2.8.24: premium header row. Sub-screens show a back arrow (in a
 * translucent dark circle) and the green infinity; the Profiles list
 * shows the infinity in a green circle plus bell + gear; Settings
 * shows the green infinity with no back arrow.
 */
@Composable
private fun PremiumHeader(
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    logoInCircle: Boolean = false,
    onBell: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    showPremium: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (logoInCircle) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GoGreen),
                contentAlignment = Alignment.Center
            ) {
                InfinityLogo(color = Color.White, fontSize = 34)
            }
        } else {
            InfinityLogo(color = GoGreen, fontSize = 34)
        }
        Spacer(Modifier.width(8.dp))
        // v2.8.25: the title yields to the Premium pill — the pill keeps
        // its full width, so "Premium" can never wrap to "Premiu m".
        BrandTitle(Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        if (onBell != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onBell),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        if (onSettings != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        } else if (showPremium) {
            PremiumPill()
        }
    }
}

/**
 * v2.8.24: premium bottom nav — white pill (Profiles / Settings) plus
 * the green round "+ Add Profile" button. [selected] is null on the
 * Add Proxy screen (no item highlighted there).
 */
@Composable
private fun PremiumBottomNav(
    selected: HomeRoute?,
    onProfiles: () -> Unit,
    onSettings: () -> Unit,
    onAddProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = { Icon(Icons.Outlined.Person, null, Modifier.size(22.dp)) },
                label = "Profiles",
                selected = selected == HomeRoute.PROFILES,
                onClick = onProfiles
            )
            BottomNavItem(
                icon = { Icon(Icons.Filled.Settings, null, Modifier.size(22.dp)) },
                label = "Settings",
                selected = selected == HomeRoute.SETTINGS,
                onClick = onSettings
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(GoGreen)
                .clickable(onClick = onAddProfile),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                Text("Add Profile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Color(0xFFDFF3EC) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (selected) GoGreen else GoGreyText
            ) { icon() }
        }
        Text(
            label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (selected) GoGreen else GoGreyText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * v2.8.25: the dark pill tabs on the Profiles list — "Profiles (N)"
 * selected green, "Settings" and "Proxy" plain white text. Every label
 * is one line, never wrapped ("Pro/xy" is impossible by construction):
 * small type, tight padding, SpaceEvenly across the full width.
 */
@Composable
private fun PillTabs(
    profileCount: Int,
    onSettings: () -> Unit,
    onProxy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(GoHeaderDark)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(GoGreen)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Layers, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Profiles ($profileCount)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
        }
        Row(
            modifier = Modifier.clickable(onClick = onSettings).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Settings", color = Color.White, fontSize = 14.sp, maxLines = 1, softWrap = false)
        }
        Row(
            modifier = Modifier.clickable(onClick = onProxy).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Proxy", color = Color.White, fontSize = 14.sp, maxLines = 1, softWrap = false)
        }
    }
}

/** v2.8.24: a green-icon section header ("Profile Name", "Proxy", …). */
@Composable
private fun SectionHeader(icon: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoTitle)
    }
}

/** v2.8.24: a white rounded row with a green icon, title, subtitle, chevron. */
@Composable
private fun MenuRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GoTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = GoGreyText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GoGreyText)
    }
}

/** v2.8.24: the Profiles list — the home screen of the reference design. */
@Composable
private fun ProfilesScreen(
    profiles: List<Profile>,
    query: String,
    onQuery: (String) -> Unit,
    filter: ProfileFilter,
    onFilter: (ProfileFilter) -> Unit,
    savedProxies: List<ProxyEntry>,
    runningIds: Set<Long>,
    syncingIds: Set<Long>,
    onLaunch: (Profile) -> Unit,
    onStop: (Profile) -> Unit,
    onStopAll: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onDuplicate: (Profile) -> Unit,
    onCheckFingerprint: (Profile) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProxy: () -> Unit,
    onAddProfile: () -> Unit,
    onShowAbout: () -> Unit
) {
    val ctx = LocalContext.current
    var filterOpen by remember { mutableStateOf(false) }
    val shown = profiles.filter {
        when (filter) {
            ProfileFilter.ALL -> true
            ProfileFilter.ACTIVE -> isProfileActive(it, savedProxies)
            ProfileFilter.OFFLINE -> !isProfileActive(it, savedProxies)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WaveHeader {
            PremiumHeader(
                logoInCircle = true,
                onBell = {
                    Toast.makeText(ctx, "No new notifications", Toast.LENGTH_SHORT).show()
                },
                onSettings = onOpenSettings
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            PillTabs(
                profileCount = profiles.size,
                onSettings = onOpenSettings,
                onProxy = onOpenProxy
            )
            // Search field + filter button.
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    placeholder = { Text("Search profiles...", color = GoGreyText) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null, tint = GoGreen)
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GoGreen,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(10.dp))
                Box {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { filterOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.FilterList, null, tint = GoGreen, modifier = Modifier.size(24.dp))
                    }
                    DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("All profiles") },
                            onClick = { filterOpen = false; onFilter(ProfileFilter.ALL) }
                        )
                        DropdownMenuItem(
                            text = { Text("Active only") },
                            onClick = { filterOpen = false; onFilter(ProfileFilter.ACTIVE) }
                        )
                        DropdownMenuItem(
                            text = { Text("Offline only") },
                            onClick = { filterOpen = false; onFilter(ProfileFilter.OFFLINE) }
                        )
                    }
                }
            }
            if (filter != ProfileFilter.ALL) {
                Text(
                    when (filter) {
                        ProfileFilter.ACTIVE -> "Showing active profiles"
                        ProfileFilter.OFFLINE -> "Showing offline profiles"
                        else -> ""
                    },
                    color = GoGreyText,
                    fontSize = 13.sp
                )
            }
            if (shown.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No profiles yet", style = MaterialTheme.typography.titleMedium, color = GoTitle)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap Add Profile to create your first profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoGreyText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (runningIds.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(GoGreen)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${runningIds.size} profile${if (runningIds.size > 1) "s" else ""} running",
                                    color = GoGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onStopAll) { Text("Stop all", color = GoStopRed) }
                            }
                        }
                    }
                    items(shown, key = { it.id }) { p ->
                        ProfileCard(
                            profile = p,
                            entry = proxyEntryFor(p, savedProxies),
                            active = isProfileActive(p, savedProxies),
                            running = runningIds.contains(p.id),
                            syncing = syncingIds.contains(p.id),
                            onLaunch = { onLaunch(p) },
                            onStop = { onStop(p) },
                            onEdit = { onEdit(p) },
                            onDelete = { onDelete(p) },
                            onDuplicate = { onDuplicate(p) },
                            onCheckFingerprint = { onCheckFingerprint(p) }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GoMintBg)
                                .clickable(onClick = onShowAbout)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GoGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Your Profiles are Secure",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GoTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Anti-detect protection is active",
                                    color = GoGreyText,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GoGreyText)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
        PremiumBottomNav(
            selected = HomeRoute.PROFILES,
            onProfiles = { },
            onSettings = onOpenSettings,
            onAddProfile = onAddProfile
        )
    }
}

/** v2.8.24: one profile card, exactly as in the reference screenshot. */
@Composable
private fun ProfileCard(
    profile: Profile,
    entry: ProxyEntry?,
    active: Boolean,
    running: Boolean,
    syncing: Boolean,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCheckFingerprint: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val (flag, _) = countryOf(profile)
    val (city, country) = profileCityCountry(profile)
    val location = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
    val ipLine = when {
        profile.proxyMode == "NONE" -> "Direct connection"
        entry == null -> "IP: ${profile.proxyHost}"
        entry.lastIp.isBlank() -> "IP: ${profile.proxyHost} • not tested"
        else -> "IP: ${entry.lastIp} • ${entry.lastMs} ms"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        // Green left accent.
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(GoGreen)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(flag, fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = GoTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Color(0xFFD9F2E5) else Color(0xFFF1F3F4))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (active) GoGreenDark else GoGreyText)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (active) "Active" else "Offline",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) GoGreenDark else GoGreyText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, null, tint = GoGreyText, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "Proxy • ${profile.proxyMode}",
                            fontSize = 14.sp,
                            color = GoGreyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = GoGreyText, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(location, fontSize = 14.sp, color = GoGreyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, tint = GoGreyText, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(ipLine, fontSize = 14.sp, color = GoGreyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = GoTitle)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Check fingerprint") },
                            onClick = { menuOpen = false; onCheckFingerprint() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { menuOpen = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFD93025)) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Bottom action bar: Launch | Edit | More.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF1F3F4))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (running) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = GoStopRed),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                } else if (syncing) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GoGreen)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Button(
                        onClick = onLaunch,
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Launch", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEdit)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, null, tint = GoTitle, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", fontWeight = FontWeight.Bold, color = GoTitle, maxLines = 1, softWrap = false)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(GoDivider)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { menuOpen = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("•••", fontWeight = FontWeight.Bold, color = GoTitle, fontSize = 16.sp, maxLines = 1, softWrap = false)
                    Spacer(Modifier.width(6.dp))
                    Text("More", fontWeight = FontWeight.Bold, color = GoTitle, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** v2.8.24: the Add / Edit Profile form — the reference screenshot form. */
@Composable
private fun AddEditFormScreen(
    draft: Profile,
    editing: Boolean,
    onDraft: (Profile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onOpenProxyPicker: () -> Unit,
    onCheckProxy: (Profile) -> Unit,
    proxyCheckResult: String?,
    onClearNotice: () -> Unit,
    proxyStore: ProxyStore,
    onCheckFingerprint: (Profile) -> Unit
) {
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var proxyPickerOpen by remember { mutableStateOf(false) }
    var extSection by remember { mutableStateOf<String?>(null) }
    var connectingCountry by remember { mutableStateOf<String?>(null) }
    var countryStatus by remember { mutableStateOf<String?>(null) }

    // System back inside a sub-screen returns to the main form.
    BackHandler(enabled = proxyPickerOpen || extSection != null) {
        proxyPickerOpen = false
        extSection = null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WaveHeader {
            PremiumHeader(showBack = true, onBack = onCancel)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            when {
                proxyPickerOpen -> ProxyPickerView(
                    proxyStore = proxyStore,
                    scope = scope,
                    connectingCountry = connectingCountry,
                    onConnectingCountry = { connectingCountry = it },
                    countryStatus = countryStatus,
                    onCountryStatus = { countryStatus = it },
                    onPicked = { working, cs ->
                        proxyStore.add(working)
                        onDraft(
                            draft.copy(
                                proxyMode = working.mode,
                                proxyHost = working.host,
                                proxyPort = working.port,
                                proxyUser = "",
                                proxyPass = "",
                                proxyCountry = cs.code,
                                timezone = if (draft.timezoneMode == "IP") working.lastTimezone.ifBlank { cs.timezone } else draft.timezone,
                                language = if (draft.languagesMode == "IP") cs.language else draft.language,
                                latitude = if (draft.geoMode == "IP" && working.lastLat != 0.0) working.lastLat else draft.latitude,
                                longitude = if (draft.geoMode == "IP" && working.lastLon != 0.0) working.lastLon else draft.longitude
                            )
                        )
                        countryStatus =
                            "${cs.label} connected • ${working.lastIp} • " +
                                "${working.lastMs} ms — " +
                                "timezone + language auto-matched. " +
                                "Save the profile to apply."
                        proxyPickerOpen = false
                    },
                    onBack = { connectingCountry = null; proxyPickerOpen = false }
                )
                extSection != null -> ExtendedSectionView(
                    section = extSection!!,
                    draft = draft,
                    onDraft = onDraft,
                    onBack = { extSection = null },
                    onOpenProxyPicker = { extSection = null; proxyPickerOpen = true },
                    onCheckProxy = onCheckProxy,
                    onClearNotice = onClearNotice,
                    proxyCheckResult = proxyCheckResult
                )
                else -> {
                    // Avatar + title.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(110.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(GoMintBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    null,
                                    tint = GoGreenDark,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoGreen)
                                    .border(3.dp, Color.White, CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (editing) Icons.Filled.Edit else Icons.Filled.Add,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (editing) "Edit Profile" else "Add Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = GoTitle
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (editing) "Update your profile settings and customize your browser environment."
                                else "Create a new profile with unique fingerprint and proxy settings.",
                                color = GoGreyText,
                                fontSize = 15.sp
                            )
                        }
                    }
                    // Profile Name card.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, null, tint = GoGreen, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Profile Name", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GoTitle)
                        }
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { onDraft(draft.copy(name = it.take(50))) },
                            placeholder = { Text("Enter profile name...", color = GoGreyText) },
                            trailingIcon = {
                                Text("${draft.name.length}/50", color = GoGreyText, fontSize = 13.sp)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = GoGreen,
                                unfocusedBorderColor = GoDivider
                            )
                        )
                    }
                    // Proxy.
                    SectionHeader(
                        icon = { Icon(Icons.Filled.Layers, null, tint = GoGreen, modifier = Modifier.size(22.dp)) },
                        label = "Proxy"
                    )
                    MenuRow(
                        icon = {
                            Icon(
                                if (draft.proxyMode == "NONE") Icons.Filled.Link else Icons.Filled.Storage,
                                null,
                                tint = GoGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        title = if (draft.proxyMode == "NONE") "Connect Proxy"
                        else "${draft.proxyMode} ${draft.proxyHost}:${draft.proxyPort}",
                        subtitle = if (draft.proxyMode == "NONE") "Tap to select a proxy"
                        else "Timezone: ${draft.timezone} • ${draft.language}",
                        onClick = onOpenProxyPicker
                    )
                    countryStatus?.let { Text(it, color = GoGreyText, fontSize = 13.sp) }
                    // Extended settings.
                    SectionHeader(
                        icon = { Icon(Icons.Filled.Settings, null, tint = GoGreen, modifier = Modifier.size(22.dp)) },
                        label = "Extended Settings"
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.DesktopWindows, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Profile Overview",
                        // v2.8.25: the reference text differs per screen —
                        // Add says "Set profile details…", Edit says
                        // "View and manage profile details".
                        subtitle = if (editing) "View and manage profile details"
                        else "Set profile details and advanced options",
                        onClick = { extSection = "overview" }
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.LocationOn, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Proxy & Location",
                        subtitle = "Choose country and proxy settings",
                        onClick = { extSection = "proxy" }
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.Language, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Browser",
                        subtitle = "Configure browser settings",
                        onClick = { extSection = "browser" }
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.Storage, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Storage",
                        subtitle = "Manage profile storage",
                        onClick = { extSection = "storage" }
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.Memory, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Hardware",
                        subtitle = "Customize hardware fingerprint",
                        onClick = { extSection = "hardware" }
                    )
                    MenuRow(
                        icon = { Icon(Icons.Filled.Fingerprint, null, tint = GoGreen, modifier = Modifier.size(28.dp)) },
                        title = "Check fingerprint",
                        subtitle = "Verify your profile fingerprint",
                        onClick = { onCheckFingerprint(draft) }
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        // Bottom buttons (main form view only).
        if (!proxyPickerOpen && extSection == null) {
            // v2.8.25: the reference bottom rows differ per screen —
            // Add: Cancel (mint, left) + Add Profile (green, right);
            // Edit: Save Profile (mint, left) + Cancel (white, right).
            val trySave: () -> Unit = {
                when {
                    draft.name.isBlank() -> error = "Name is required."
                    draft.proxyMode != "NONE" && draft.proxyHost.isBlank() ->
                        error = "Proxy host is required for ${draft.proxyMode}."
                    draft.proxyPort !in 1..65535 ->
                        error = "Proxy port must be 1–65535."
                    else -> onSave()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (editing) {
                    Button(
                        onClick = trySave,
                        enabled = connectingCountry == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDFF3EC),
                            contentColor = GoGreenDark
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (connectingCountry != null) "Connecting ${connectingCountry}…"
                            else "Save Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = GoTitle
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDFF3EC),
                            contentColor = GoGreenDark
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = trySave,
                        enabled = connectingCountry == null,
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(3f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (connectingCountry != null) "Connecting ${connectingCountry}…"
                            else "Add Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            if (editing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDeleteToTrash)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, null, tint = Color(0xFFD93025), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Profile", color = Color(0xFFD93025), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

/** v2.8.24: the Add Proxy screen — import, manual add, saved list. */
@Composable
private fun AddProxyScreen(
    proxyStore: ProxyStore,
    savedProxies: List<ProxyEntry>,
    scope: CoroutineScope,
    draft: Profile?,
    onApplyToDraft: (ProxyEntry) -> Unit,
    onDirectForDraft: () -> Unit,
    onBack: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddProfile: () -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()
    var showManual by remember { mutableStateOf(false) }
    var newIp by remember { mutableStateOf("") }
    var newUser by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var testingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var testAllRunning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            var added = 0
            var skipped = 0
            try {
                val text = ctx.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: ""
                var counter = proxyStore.proxies.value.mapNotNull {
                    Regex("""^Myproxy #(\d+)$""").find(it.name)
                        ?.groupValues?.getOrNull(1)?.toIntOrNull()
                }.maxOrNull() ?: 0
                for (raw in text.lines()) {
                    if (raw.isBlank()) continue
                    val parsed = parseProxyFileLine(raw)
                    if (parsed == null) {
                        skipped++
                        continue
                    }
                    val existing = proxyStore.proxies.value.firstOrNull {
                        it.host == parsed.host && it.port == parsed.port
                    }
                    val name = existing?.name ?: run {
                        counter++
                        "Myproxy #${counter}"
                    }
                    proxyStore.add(parsed.copy(name = name))
                    added++
                }
                withContext(Dispatchers.Main) {
                    status = "Imported $added prox${if (added == 1) "y" else "ies"}" +
                        if (skipped > 0) " ($skipped lines skipped)" else ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = "Import failed: ${e.message}"
                }
            }
        }
    }

    fun testEntry(entry: ProxyEntry, applyAfter: Boolean) {
        testingIds = testingIds + entry.id
        scope.launch {
            val checked = proxyStore.check(entry)
            proxyStore.update(checked)
            testingIds = testingIds - entry.id
            if (applyAfter) {
                if (checked.ok) onApplyToDraft(checked)
                else status = "Proxy failed — not applied"
            } else {
                status = if (checked.ok) "${entry.host} works • ${checked.lastMs} ms"
                else "${entry.host} failed"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WaveHeader {
            PremiumHeader(showBack = true, onBack = onBack)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            // Shield-gear avatar + title.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = GoGreen, modifier = Modifier.size(96.dp))
                    Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Add Proxy", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = GoTitle)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildAnnotatedString {
                            append("Import a .txt file, or tap a saved proxy below – it is tested and applied at once. ")
                            append("Or add one manually. Protocol is ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GoTitle)) {
                                append("SOCKS5")
                            }
                            append(" – permanently.")
                        },
                        color = GoGreyText,
                        fontSize = 15.sp
                    )
                }
            }
            // Import from file.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoMintBg)
                    .clickable { txtLauncher.launch(arrayOf("text/plain", "*/*")) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(".txt", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Import Proxies from File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = GoTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Select a .txt file with your proxies", color = GoGreyText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GoGreyText)
            }
            status?.let { Text(it, color = GoGreyText, fontSize = 13.sp) }
            // Manual add (expandable, SOCKS5 only).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showManual = !showManual },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Tune, null, tint = GoGreen, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add Proxy Manually",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GoTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    if (showManual) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = GoTitle
                )
            }
            if (showManual) {
                val suggestedName = nextAutoProxyName(savedProxies)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GoDivider, RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add proxy", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GoTitle)
                    Text(
                        "SOCKS5 only — the proxy is tested before it is saved.",
                        color = GoGreyText,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = newIp,
                        onValueChange = { newIp = it; formError = null },
                        label = { Text("IP") },
                        placeholder = { Text("198.165.0.0:8080") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newUser,
                        onValueChange = { newUser = it },
                        label = { Text("Username") },
                        placeholder = { Text("Proxy login") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Password") },
                        placeholder = { Text("Proxy password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPass) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Proxy name") },
                        placeholder = { Text(suggestedName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            val parsed = parseManualIp(newIp) ?: return@Button
                            val (host, port) = parsed
                            val name = newName.trim().ifBlank { suggestedName }
                            adding = true
                            formError = null
                            scope.launch {
                                val checked = proxyStore.check(
                                    ProxyEntry(
                                        name = name, mode = "SOCKS5", host = host,
                                        port = port, user = newUser.trim(), pass = newPass
                                    )
                                )
                                adding = false
                                if (checked.ok) {
                                    proxyStore.add(checked)
                                    newIp = ""
                                    newUser = ""
                                    newPass = ""
                                    newName = ""
                                    showManual = false
                                    if (draft != null) onApplyToDraft(checked)
                                    else status = "Proxy saved ✓"
                                } else {
                                    formError = "Proxy did not answer — check the IP:port and login, then try again."
                                }
                            }
                        },
                        enabled = !adding && parseManualIp(newIp) != null,
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (adding) "Testing…" else "Add proxy") }
                }
            }
            // Direct. v2.8.25: Direct is the default (no proxy) — it
            // shows selected both from a draft and from the tab, exactly
            // as in the reference (green text + green dot).
            val directSelected = draft?.proxyMode != "SOCKS5"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable {
                        if (draft != null) onDirectForDraft()
                        else status = "Open Add or Edit Profile to use Direct"
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = GoGreen, modifier = Modifier.size(36.dp))
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Use no proxy (Direct)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (directSelected) GoGreen else GoTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (directSelected) GoGreen else GoDivider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (directSelected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(GoGreen)
                        )
                    }
                }
            }
            // Saved proxies header.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, null, tint = GoGreen, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Saved Proxies (${savedProxies.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GoTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Tap one to test & apply it at once", color = GoGreyText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = {
                        testAllRunning = true
                        status = "Testing ${savedProxies.size} proxies…"
                        scope.launch {
                            val results = savedProxies.map { entry -> async { proxyStore.check(entry) } }.awaitAll()
                            results.forEach { proxyStore.update(it) }
                            testAllRunning = false
                            status = "Tested ${results.size} — ${results.count { it.ok }} working"
                        }
                    },
                    enabled = !testAllRunning && savedProxies.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoMintBg,
                        contentColor = GoGreenDark
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test All", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
            // Saved proxy rows.
            if (savedProxies.isEmpty()) {
                Text("No saved proxies yet.", color = GoGreyText)
            }
            for (entry in savedProxies) {
                ProxyRow(
                    entry = entry,
                    selected = draft?.proxyHost == entry.host && draft?.proxyPort == entry.port && draft?.proxyMode != "NONE",
                    testing = testingIds.contains(entry.id),
                    onTap = { testEntry(entry, applyAfter = draft != null) },
                    onTest = { testEntry(entry, applyAfter = false) },
                    onDelete = { proxyStore.remove(entry.id) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        PremiumBottomNav(
            selected = null,
            onProfiles = onOpenProfiles,
            onSettings = onOpenSettings,
            onAddProfile = onAddProfile
        )
    }
}

/** v2.8.24: one saved proxy row on the Add Proxy screen. */
@Composable
private fun ProxyRow(
    entry: ProxyEntry,
    selected: Boolean,
    testing: Boolean,
    onTap: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val code = proxyCountryCode(entry.country)
    val flag = if (code != null) Profile.flagEmoji(code) else "🌐"
    val countryLabel = if (entry.country.isBlank()) "Unknown"
    else entry.country.substringAfter(" • ", entry.country)
    val ipLine = if (entry.lastIp.isBlank()) "Not tested yet — tap play"
    else "IP ${entry.lastIp} • ${entry.lastMs} ms"
    val ready = entry.ok && entry.lastIp.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                if (selected) 2.dp else 0.dp,
                if (selected) GoGreen else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(flag, fontSize = 36.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "SOCKS5 ${entry.host}:${entry.port}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GoTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = GoGreyText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(countryLabel, fontSize = 14.sp, color = GoGreyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = GoGreyText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(ipLine, fontSize = 14.sp, color = GoGreyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ready) Color(0xFFD9F2E5) else Color(0xFFF1F3F4))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = if (ready) GoGreenDark else GoGreyText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Ready",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ready) GoGreenDark else GoGreyText,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, GoGreen, CircleShape)
                        .clickable(onClick = onTest),
                    contentAlignment = Alignment.Center
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, tint = GoGreen, modifier = Modifier.size(20.dp))
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = GoTitle)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFD93025)) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

/** v2.8.24: the Settings screen — export / import / about + the JS-level notice. */
@Composable
private fun SettingsScreen(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAbout: () -> Unit,
    notice: String?,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddProfile: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        WaveHeader {
            PremiumHeader()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(GoMintBg)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, null, tint = GoGreenDark, modifier = Modifier.size(52.dp))
            }
            Text(
                "Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = GoTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Customize your NovaProfile experience",
                color = GoGreyText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            SettingsRow(
                icon = { Icon(Icons.Filled.FileUpload, null, tint = GoGreenDark, modifier = Modifier.size(32.dp)) },
                title = "Export All Profiles",
                subtitle = "Save your profiles to a .zip file",
                onClick = onExport
            )
            SettingsRow(
                icon = { Icon(Icons.Filled.FileDownload, null, tint = GoGreenDark, modifier = Modifier.size(32.dp)) },
                title = "Import Profiles",
                subtitle = "Load profiles from a .zip file",
                onClick = onImport
            )
            SettingsRow(
                icon = { Icon(Icons.Filled.Info, null, tint = GoGreenDark, modifier = Modifier.size(32.dp)) },
                title = "About NovaProfile",
                // v2.8.25: the reference label, exactly as screenshotted.
                subtitle = "Version 1.0.0 • Build 1000",
                onClick = onAbout
            )
            notice?.let { Text(it, color = GoGreyText, fontSize = 13.sp) }
            // Fingerprint notice card.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoMintBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = GoGreen, modifier = Modifier.size(56.dp))
                    Icon(Icons.Filled.Fingerprint, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(64.dp)
                        .background(GoGreyText.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Fingerprint protection is JS-level inside the system WebView.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GoTitle
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "It is NOT a Chromium fork — advanced fingerprint tests can still detect the real device.",
                        fontSize = 14.sp,
                        color = GoGreyText
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        PremiumBottomNav(
            selected = HomeRoute.SETTINGS,
            onProfiles = onOpenProfiles,
            onSettings = onOpenSettings,
            onAddProfile = onAddProfile
        )
    }
}

/** v2.8.24: a Settings row — mint icon tile, title, subtitle, chevron. */
@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GoMintBg),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = GoTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = GoGreyText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GoGreyText)
    }
}

/** A bordered row with a label, optional sub-text and a chevron. */
@Composable
private fun SectionRow(label: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold)
            sub?.let { Text(it, color = GoGreyText, fontSize = 13.sp) }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GoGreyText)
    }
}

/** A row of single-choice chips. */
@Composable
private fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((value, label) in options) {
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

/** A label + switch row. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for (o in options) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(o) },
                    onClick = { onSelect(o); expanded = false }
                )
            }
        }
    }
}

/**
 * Country proxy picker (v2.6): the GoLogin-style Add proxy screen, now
 * inside Add profile. Tap a country → fetch + test free proxies for it
 * (fastest working wins) → "Proxy is ok!" → Add proxy applies it to
 * the draft.
 */
@Composable
private fun ProxyPickerView(
    proxyStore: ProxyStore,
    scope: CoroutineScope,
    connectingCountry: String?,
    onConnectingCountry: (String?) -> Unit,
    countryStatus: String?,
    onCountryStatus: (String?) -> Unit,
    onPicked: (ProxyEntry, Profile.CountrySettings) -> Unit,
    onBack: () -> Unit
) {
    var pickedProxy by remember { mutableStateOf<ProxyEntry?>(null) }
    var pickedCode by remember { mutableStateOf<String?>(null) }
    var pickingError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Add proxy", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Text(
            "Pick a country — the app fetches and tests free proxies for it.",
            color = GoGreyText
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
        ) {
            for (code in Profile.pickerCountries) {
                val cs = Profile.countrySettings(code)
                val expanded = connectingCountry == code || pickedCode == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = connectingCountry == null) {
                            pickedProxy = null
                            pickedCode = code
                            pickingError = null
                            onConnectingCountry(code)
                            onCountryStatus("Fetching $code proxies…")
                            scope.launch {
                                val candidates = proxyStore.fetchFreeProxies(code, limit = 10)
                                if (candidates.isEmpty()) {
                                    pickingError =
                                        "Could not fetch any free ${cs.label} proxies — " +
                                            "check connection and try again."
                                } else {
                                    onCountryStatus("Testing ${candidates.size} ${cs.label} proxies…")
                                    val working = proxyStore.checkFirstWorking(candidates)
                                    if (working != null) {
                                        pickedProxy = working
                                        onCountryStatus(
                                            "${cs.label} connected • ${working.lastIp} • " +
                                                "${working.lastMs} ms"
                                        )
                                    } else {
                                        pickingError =
                                            "No working free ${cs.label} proxy right now — " +
                                                "free proxies die fast. Try again."
                                    }
                                }
                                onConnectingCountry(null)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Profile.flagEmoji(code), fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(cs.label, modifier = Modifier.weight(1f))
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = GoGreyText
                    )
                }
                // v2.7.1: search + result expand UNDER the tapped country
                // (user order) — no more result card pinned at the bottom.
                if (expanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GoDivider)
                    )
                    if (connectingCountry == code) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(countryStatus ?: "Checking…", color = GoGreyText)
                        }
                    } else if (pickedCode == code && pickedProxy != null) {
                        val wp = pickedProxy!!
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Proxy is ok! • ${wp.lastIp} • ${wp.lastMs} ms" +
                                    (if (wp.lastTimezone.isNotBlank()) " • ${wp.lastTimezone}" else ""),
                                color = GoGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text("HTTP • Free", color = GoGreyText)
                            Button(
                                onClick = { onPicked(wp, cs) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add proxy") }
                        }
                    } else if (pickedCode == code) {
                        pickingError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Extended settings (v2.6): GoLogin's 5 sub-screens. */
@Composable
private fun ExtendedSectionView(
    section: String,
    draft: Profile,
    onDraft: (Profile) -> Unit,
    onBack: () -> Unit,
    onOpenProxyPicker: () -> Unit,
    onCheckProxy: (Profile) -> Unit,
    onClearNotice: () -> Unit,
    proxyCheckResult: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                when (section) {
                    "overview" -> "Profile Overview"
                    "proxy" -> "Proxy & Location"
                    "browser" -> "Browser"
                    "storage" -> "Storage"
                    else -> "Hardware"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        when (section) {
            "overview" -> OverviewSection(draft, onDraft)
            "proxy" -> ProxyLocationSection(
                draft, onDraft, onOpenProxyPicker,
                onCheckProxy, onClearNotice, proxyCheckResult
            )
            "browser" -> BrowserSection(draft, onDraft)
            "storage" -> StorageSection(draft, onDraft)
            else -> HardwareSection(draft, onDraft)
        }
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OverviewSection(draft: Profile, onDraft: (Profile) -> Unit) {
    OutlinedTextField(
        value = draft.name, onValueChange = { onDraft(draft.copy(name = it)) },
        label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    OutlinedTextField(
        value = draft.notes, onValueChange = { onDraft(draft.copy(notes = it)) },
        label = { Text("Note") }, modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = draft.folder, onValueChange = { onDraft(draft.copy(folder = it)) },
        label = { Text("Folder") }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    DropDownField("Operating System", Profile.OS_OPTIONS, draft.os) { os ->
        val (vendor, renderer) = Profile.osWebgl(os)
        onDraft(
            draft.copy(
                os = os,
                userAgent = if (draft.userAgentPreset == "Custom") draft.userAgent
                else Profile.osUserAgent(os),
                webglVendor = vendor,
                webglRenderer = renderer
            )
        )
    }
    Button(
        onClick = { onDraft(Profile.randomFingerprint(draft)) },
        colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
        modifier = Modifier.fillMaxWidth()
    ) { Text("NEW FINGERPRINT") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Fingerprint data", fontWeight = FontWeight.Bold)
        Text("Resolution  ${draft.screenWidth}x${draft.screenHeight}", color = GoGreyText)
        Text("OS  ${draft.os}", color = GoGreyText)
        Text("Languages  ${draft.language}", color = GoGreyText)
        Text("Fonts  ${draft.fontsList.split(",").size} fonts", color = GoGreyText)
        Text(
            "User-agent  ${if (draft.userAgent.isNotBlank()) draft.userAgent else Profile.defaultUserAgent(draft.userAgentPreset)}",
            color = GoGreyText
        )
        val canvasSpoofed = draft.canvasMode == "noise" || draft.canvasMode == "block"
        val spoofed = canvasSpoofed || draft.webglMode == "MASKED" || draft.audioMode == "MASKED"
        Text(
            if (spoofed) "Fingerprint: Unique ✓" else "Fingerprint: Real (not spoofed)",
            color = if (spoofed) GoGreenDark else GoGreyText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProxyLocationSection(
    draft: Profile,
    onDraft: (Profile) -> Unit,
    onOpenProxyPicker: () -> Unit,
    onCheckProxy: (Profile) -> Unit,
    onClearNotice: () -> Unit,
    proxyCheckResult: String?
) {
    Text("Proxy", fontWeight = FontWeight.Bold)
    SectionRow(
        label = if (draft.proxyMode == "NONE") "Connect proxy"
        else "${draft.proxyMode} ${draft.proxyHost}:${draft.proxyPort}",
        sub = if (draft.proxyMode == "NONE") "Tap to pick a country"
        else "Timezone ${draft.timezone}",
        onClick = onOpenProxyPicker
    )
    Button(onClick = { onClearNotice(); onCheckProxy(draft) }) {
        Text("Check proxy")
    }
    proxyCheckResult?.let { Text(it) }
    Text("Timezone", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("IP" to "Fill based on IP", "CUSTOM" to "Custom"),
        selected = draft.timezoneMode,
        onSelect = { onDraft(draft.copy(timezoneMode = it)) }
    )
    if (draft.timezoneMode == "CUSTOM") {
        DropDownField("Timezone", Profile.TIMEZONE_OPTIONS, draft.timezone) {
            onDraft(draft.copy(timezone = it))
        }
    } else {
        Text("Current: ${draft.timezone} (from the proxy IP)", color = GoGreyText)
    }
    Text("Geolocation", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("IP" to "Based on IP", "OFF" to "Off"),
        selected = draft.geoMode,
        onSelect = { onDraft(draft.copy(geoMode = it)) }
    )
    if (draft.geoMode == "IP") {
        Text(
            if (draft.latitude != null && draft.longitude != null)
                "Current: ${draft.latitude}, ${draft.longitude} (from the proxy IP)"
            else "Set when a proxy connects or the profile runs.",
            color = GoGreyText
        )
    }
    Text("WebRTC", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("disable" to "Disabled", "real" to "Real"),
        selected = draft.webrtcMode,
        onSelect = { onDraft(draft.copy(webrtcMode = it)) }
    )
    Text("Disabled is safest — Real can leak your real IP through the proxy.", color = GoGreyText)
    Text("Custom DNS", fontWeight = FontWeight.Bold)
    Text("Based on ip — DNS resolves through the proxy.", color = GoGreyText)
}

@Composable
private fun BrowserSection(draft: Profile, onDraft: (Profile) -> Unit) {
    DropDownField(
        "User-agent preset", Profile.USER_AGENT_PRESETS,
        draft.userAgentPreset
    ) { onDraft(draft.copy(userAgentPreset = it)) }
    if (draft.userAgentPreset == "Custom") {
        OutlinedTextField(
            value = draft.userAgent,
            onValueChange = { onDraft(draft.copy(userAgent = it)) },
            label = { Text("Custom user-agent") },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text(
            "User-agent: ${if (draft.userAgent.isNotBlank()) draft.userAgent else Profile.defaultUserAgent(draft.userAgentPreset)}",
            color = GoGreyText
        )
    }
    OutlinedTextField(
        value = draft.homeUrl, onValueChange = { onDraft(draft.copy(homeUrl = it)) },
        label = { Text("Start URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
    Text("Languages", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("IP" to "IP based", "CUSTOM" to "Custom"),
        selected = draft.languagesMode,
        onSelect = { onDraft(draft.copy(languagesMode = it)) }
    )
    if (draft.languagesMode == "CUSTOM") {
        OutlinedTextField(
            value = draft.language, onValueChange = { onDraft(draft.copy(language = it)) },
            label = { Text("Language (e.g. en-US)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    } else {
        Text("Current: ${draft.language} (from the proxy country)", color = GoGreyText)
    }
    Text("Fonts", fontWeight = FontWeight.Bold)
    SwitchRow(
        if (draft.fontsMasking) "Masked" else "Real",
        draft.fontsMasking
    ) { onDraft(draft.copy(fontsMasking = it)) }
    Text("Masking hides unusual fonts that could fingerprint the profile.", color = GoGreyText)
    if (draft.fontsMasking) {
        val selected = draft.fontsList.split(",").map { it.trim() }
            .filter { it.isNotEmpty() }.toSet()
        for (font in Profile.FONT_OPTIONS) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newList = if (selected.contains(font)) selected - font
                        else selected + font
                        onDraft(draft.copy(fontsList = newList.joinToString(",")))
                    }
            ) {
                Checkbox(checked = selected.contains(font), onCheckedChange = null)
                Text(font)
            }
        }
    } else {
        Text("Real fonts installed on this device are reported — no selection needed.", color = GoGreyText)
    }
}

@Composable
private fun StorageSection(draft: Profile, onDraft: (Profile) -> Unit) {
    SwitchRow("Save history", draft.saveHistory) {
        onDraft(draft.copy(saveHistory = it))
    }
    SwitchRow("Save bookmarks", draft.saveBookmarks) {
        onDraft(draft.copy(saveBookmarks = it))
    }
    SwitchRow("Local storage and indexedDB", draft.domStorage) {
        onDraft(draft.copy(domStorage = it))
    }
    Text(
        "Lock session, Google services, passwords, plugins and extensions " +
            "are not available in the system WebView — they are not shown here.",
        color = GoGreyText
    )
}

@Composable
private fun HardwareSection(draft: Profile, onDraft: (Profile) -> Unit) {
    val resOptions = Profile.RESOLUTIONS.map { "${it.first}x${it.second}" }
    val resCurrent = "${draft.screenWidth}x${draft.screenHeight}"
    DropDownField(
        "Resolution",
        if (resOptions.contains(resCurrent)) resOptions else resOptions + resCurrent,
        resCurrent
    ) { v ->
        val parts = v.split("x")
        onDraft(
            draft.copy(
                screenWidth = parts[0].toIntOrNull() ?: draft.screenWidth,
                screenHeight = parts.getOrNull(1)?.toIntOrNull() ?: draft.screenHeight
            )
        )
    }
    DropDownField(
        "Device memory",
        listOf("1 GB", "2 GB", "4 GB", "8 GB"),
        "${draft.deviceMemory} GB"
    ) { v ->
        onDraft(draft.copy(deviceMemory = v.removeSuffix(" GB").toIntOrNull() ?: draft.deviceMemory))
    }
    DropDownField(
        "Number of threads",
        Profile.THREAD_COUNTS.map { it.toString() },
        draft.hardwareConcurrency.toString()
    ) { v ->
        onDraft(draft.copy(hardwareConcurrency = v.toIntOrNull() ?: draft.hardwareConcurrency))
    }
    Text("Media devices", fontWeight = FontWeight.Bold)
    SwitchRow(
        if (draft.mediaMasked) "Masked • 0/0/0" else "Real",
        draft.mediaMasked
    ) { onDraft(draft.copy(mediaMasked = it)) }
    Text("Canvas", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("real" to "Real", "noise" to "Noise", "block" to "Block"),
        selected = draft.canvasMode,
        onSelect = { onDraft(draft.copy(canvasMode = it)) }
    )
    Text("WebGL image", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("REAL" to "Real", "MASKED" to "Masked"),
        selected = draft.webglMode,
        onSelect = { onDraft(draft.copy(webglMode = it)) }
    )
    if (draft.webglMode == "MASKED") {
        Text("WebGL metadata", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = draft.webglVendor, onValueChange = { onDraft(draft.copy(webglVendor = it)) },
            label = { Text("WebGL vendor") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = draft.webglRenderer, onValueChange = { onDraft(draft.copy(webglRenderer = it)) },
            label = { Text("WebGL renderer") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
    }
    Text("Audio context", fontWeight = FontWeight.Bold)
    ChoiceChips(
        options = listOf("REAL" to "Real", "MASKED" to "Masked"),
        selected = draft.audioMode,
        onSelect = { onDraft(draft.copy(audioMode = it)) }
    )
}
