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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.novaprofiles.app.util.ProxyEntry
import com.novaprofiles.app.util.ProxyStore
import com.novaprofiles.app.util.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaprofiles.app.data.Profile
import com.novaprofiles.app.util.ProxyHelper

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

/** Profile templates (v2.0 Pro): one tap fills home URL + identity. */
fun applyTemplate(draft: Profile, template: String): Profile = when (template) {
    "Facebook" -> draft.copy(
        homeUrl = "https://www.facebook.com",
        userAgentPreset = "Chrome Android",
        language = "en-US",
        timezone = "America/New_York",
        screenWidth = 1080, screenHeight = 2400
    )
    "Google" -> draft.copy(
        homeUrl = "https://www.google.com",
        userAgentPreset = "Chrome Android",
        language = "en-US",
        timezone = "America/Los_Angeles",
        screenWidth = 1080, screenHeight = 2400
    )
    "Amazon" -> draft.copy(
        homeUrl = "https://www.amazon.com",
        userAgentPreset = "Chrome Desktop",
        language = "en-US",
        timezone = "America/New_York",
        screenWidth = 1920, screenHeight = 1080
    )
    "TikTok" -> draft.copy(
        homeUrl = "https://www.tiktok.com",
        userAgentPreset = "Chrome Android",
        language = "en-US",
        timezone = "America/Los_Angeles",
        screenWidth = 1080, screenHeight = 2400
    )
    else -> draft
}

/** Country flag + code derived from the profile language (display only). */
fun countryOf(profile: Profile): Pair<String, String> = when {
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

private enum class HomeTab { PROFILES, PROXIES, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: ProfileViewModel = viewModel()) {
    val profiles by vm.profiles.collectAsState()
    val trash by vm.trashProfiles.collectAsState()
    val folders by vm.folders.collectAsState()
    val query by vm.query.collectAsState()
    val selectedFolder by vm.selectedFolder.collectAsState()
    val showTrash by vm.showTrash.collectAsState()
    val proxyCheck by vm.proxyCheckResult.collectAsState()
    val importResult by vm.importResult.collectAsState()
    val exportResult by vm.exportResult.collectAsState()

    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }
    var tab by remember { mutableStateOf(HomeTab.PROFILES) }
    var runningIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    // v2.6: profiles whose proxy location is being re-synced before launch.
    var syncingIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var searchOpen by remember { mutableStateOf(false) }
    var labProfile by remember { mutableStateOf<Profile?>(null) }
    val proxyStore = remember { ProxyStore(ctx) }
    val settingsStore = remember { SettingsStore(ctx) }
    val scope = rememberCoroutineScope()

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

    // Fingerprint Lab (v2.0 Pro) — full-screen check for one profile.
    labProfile?.let { lp ->
        FingerprintLabScreen(profile = lp, onBack = { labProfile = null })
        return
    }

    Scaffold(
        bottomBar = {
            // GoLogin-style bottom: pill nav (Profiles / Proxies / Settings)
            // + green "Add profile" button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFFF1F3F4))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = { Icon(Icons.Outlined.Person, null, Modifier.size(22.dp)) },
                        label = "Profiles",
                        selected = tab == HomeTab.PROFILES,
                        onClick = { tab = HomeTab.PROFILES }
                    )
                    BottomNavItem(
                        icon = { Text("🌐", fontSize = 20.sp) },
                        label = "Proxies",
                        selected = tab == HomeTab.PROXIES,
                        onClick = { tab = HomeTab.PROXIES }
                    )
                    BottomNavItem(
                        icon = { Icon(Icons.Filled.Settings, null, Modifier.size(22.dp)) },
                        label = "Settings",
                        selected = tab == HomeTab.SETTINGS,
                        onClick = { tab = HomeTab.SETTINGS }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { creating = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                        Text("Add profile", fontSize = 11.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            when (tab) {
                HomeTab.PROFILES -> ProfilesTab(
                    vm = vm,
                    profiles = profiles,
                    trash = trash,
                    folders = folders,
                    query = query,
                    selectedFolder = selectedFolder,
                    showTrash = showTrash,
                    searchOpen = searchOpen,
                    onSearchOpen = { searchOpen = it },
                    runningIds = runningIds,
                    syncingIds = syncingIds,
                    onRun = { p ->
                        // v2.6 smart 90% fix: before opening the browser,
                        // re-resolve the proxy IP's CURRENT timezone +
                        // coordinates (one ip-api call through the proxy)
                        // and save them, so the run never uses a stale
                        // timezone. Free US proxies sit in Mountain View
                        // (America/Los_Angeles) while the country default
                        // is New York — a 3-hour mismatch whoer deducts
                        // for. On lookup failure, launch with the stored
                        // values instead of blocking.
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
                    },
                    onStop = { p -> stopProfile(p.id) },
                    onStopAll = { stopProfile(null) },
                    onEdit = { editing = it },
                    onDuplicate = { vm.duplicate(it) },
                    onTrash = { pendingDelete = it },
                    onRestore = { vm.restore(it) },
                    onDeleteForever = { pendingDelete = it },
                    onCheckFingerprint = { labProfile = it },
                    onShowAbout = { showAbout = true }
                )
                HomeTab.PROXIES -> ProxiesTab(
                    proxyStore = proxyStore,
                    profiles = profiles,
                    proxyCheckResult = proxyCheck,
                    onCheckProxy = { p -> vm.checkProxy(p, "https://api.ipify.org") },
                    onClearNotice = { vm.clearNotices() },
                    onAssignProxy = { entry, p ->
                        // v2.5/v2.6: timezone + language + coordinates
                        // follow the proxy's IP — but only when the
                        // profile's modes say IP-based; Custom keeps
                        // the user's own values.
                        val code = entry.country.substringBefore(" •").trim()
                        val cs = if (code.length == 2) Profile.countrySettings(code) else null
                        vm.update(
                            p.copy(
                                proxyMode = entry.mode,
                                proxyHost = entry.host,
                                proxyPort = entry.port,
                                proxyUser = entry.user,
                                proxyPass = entry.pass,
                                timezone = if (p.timezoneMode == "IP")
                                    entry.lastTimezone.ifBlank { p.timezone } else p.timezone,
                                language = if (p.languagesMode == "IP" && cs != null)
                                    cs.language else p.language,
                                latitude = if (p.geoMode == "IP" && entry.lastLat != 0.0)
                                    entry.lastLat else p.latitude,
                                longitude = if (p.geoMode == "IP" && entry.lastLon != 0.0)
                                    entry.lastLon else p.longitude
                            )
                        )
                    }
                )
                HomeTab.SETTINGS -> SettingsTab(
                    profiles = profiles,
                    proxyStore = proxyStore,
                    settingsStore = settingsStore,
                    onOpenLab = { labProfile = it },
                    trashCount = trash.size,
                    onExport = { exportLauncher.launch("novaprofile-profiles.zip") },
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onShowTrash = { vm.showTrash.value = true; tab = HomeTab.PROFILES },
                    onHideTrash = { vm.showTrash.value = false },
                    showTrash = showTrash,
                    onAbout = { showAbout = true },
                    notice = importResult ?: exportResult
                )
            }
        }
    }

    // Add/Edit profile sheet (v2.5) — overlays the home, like GoLogin.
    if (editing != null || creating) {
        EditorScreen(
            initial = editing,
            onSave = { p ->
                if (editing != null) vm.update(p) else vm.create(p)
                editing = null
                creating = false
            },
            onCancel = { editing = null; creating = false },
            onCheckProxy = { p -> vm.checkProxy(p, "https://api.ipify.org") },
            proxyCheckResult = proxyCheck,
            onClearNotice = { vm.clearNotices() },
            proxyStore = proxyStore
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About NovaProfile") },
            text = {
                Text(
                    "NovaProfile 2.7.1 Pro — original multi-profile browser.\n\n" +
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

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (showTrash) "Delete forever?" else "Move to trash?") },
            text = { Text("\"${p.name}\" will be ${if (showTrash) "deleted permanently" else "moved to trash"}.") },
            confirmButton = {
                TextButton(onClick = {
                    if (showTrash) vm.deleteForever(p) else vm.moveToTrash(p)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
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

@Composable
private fun ProfilesTab(
    vm: ProfileViewModel,
    profiles: List<Profile>,
    trash: List<Profile>,
    folders: List<String>,
    query: String,
    selectedFolder: String,
    showTrash: Boolean,
    searchOpen: Boolean,
    onSearchOpen: (Boolean) -> Unit,
    runningIds: Set<Long>,
    syncingIds: Set<Long> = emptySet(),
    onRun: (Profile) -> Unit,
    onStop: (Profile) -> Unit,
    onStopAll: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDuplicate: (Profile) -> Unit,
    onTrash: (Profile) -> Unit,
    onRestore: (Profile) -> Unit,
    onDeleteForever: (Profile) -> Unit,
    onCheckFingerprint: (Profile) -> Unit,
    onShowAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Account row: green avatar + name + search.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "NovaProfile",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            // v2.3: GoLogin shows "Connected" up here when a profile runs.
            if (runningIds.isNotEmpty()) {
                Text("Connected", color = GoGreyText, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = { onSearchOpen(!searchOpen) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Black)
            }
            IconButton(onClick = onShowAbout) {
                Icon(Icons.Filled.Info, contentDescription = "About", tint = Color.Black)
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search profiles") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
        }
        // Folder row: folder icon + All + folder pills.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* folders are created in the editor */ }) {
                Icon(Icons.Filled.Folder, contentDescription = "Folders", tint = Color.Black)
            }
            FilterChip(
                selected = selectedFolder == "All",
                onClick = { vm.selectedFolder.value = "All" },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE8EAED),
                    containerColor = Color.White
                )
            )
            for (f in folders) {
                FilterChip(
                    selected = selectedFolder == f,
                    onClick = { vm.selectedFolder.value = f },
                    label = { Text(f) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE8EAED),
                        containerColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Sessions banner (v2.0 Pro): running profiles + Stop all.
        if (runningIds.isNotEmpty() && !showTrash) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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

        val shown = if (showTrash) trash else profiles
        if (shown.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (showTrash) "Trash is empty" else "No profiles yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (showTrash) "Deleted profiles land here first."
                    else "Tap Add profile to create your first profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoGreyText
                )
            }
        } else {
            LazyColumn {
                items(shown, key = { it.id }) { p ->
                    GoProfileRow(
                        profile = p,
                        inTrash = showTrash,
                        running = runningIds.contains(p.id),
                        syncing = syncingIds.contains(p.id),
                        onRun = { onRun(p) },
                        onStop = { onStop(p) },
                        onEdit = { onEdit(p) },
                        onDuplicate = { onDuplicate(p) },
                        onTrash = { onTrash(p) },
                        onRestore = { onRestore(p) },
                        onDeleteForever = { onDeleteForever(p) },
                        onCheckFingerprint = { onCheckFingerprint(p) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoProfileRow(
    profile: Profile,
    inTrash: Boolean,
    running: Boolean,
    syncing: Boolean = false,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onCheckFingerprint: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val (flag, code) = countryOf(profile)
    val connection = when (profile.proxyMode) {
        "NONE" -> "Direct"
        "HTTP" -> "HTTP proxy"
        "SOCKS5" -> "SOCKS5 proxy"
        else -> profile.proxyMode
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (running) GoMintBg else Color.White)
            .clickable { if (!inTrash) onEdit() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(GoGreen)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (running) GoGreenDark else Color.Black
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$flag  $code • $connection",
                    fontSize = 14.sp,
                    color = if (running) GoGreenDark else GoGreyText
                )
            }
            Icon(
                Icons.Filled.Android,
                contentDescription = null,
                tint = Color(0xFF9AA0A6),
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(10.dp))
            if (!inTrash) {
                if (running) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GoStopRed)
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else if (syncing) {
                    // v2.6: location re-sync in progress — spinner, no double tap.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GoGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GoGreen,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, GoGreen, CircleShape)
                            .clickable { onRun() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = GoGreen, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = GoGreyText)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (inTrash) {
                        DropdownMenuItem(
                            text = { Text("Restore") },
                            onClick = { menuOpen = false; onRestore() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete forever") },
                            onClick = { menuOpen = false; onDeleteForever() }
                        )
                    } else {
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
                            text = { Text("Move to trash") },
                            onClick = { menuOpen = false; onTrash() }
                        )
                    }
                }
            }
        }
        // Tag pills: folder + "new" when never run.
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F3F4))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(profile.folder, fontSize = 12.sp, color = GoGreyText) }
            if (profile.lastRunAt == 0L && !inTrash) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE6F4EA))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("new", fontSize = 12.sp, color = GoGreenDark) }
            }
            if (profile.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(profile.notes, fontSize = 12.sp, color = Color(0xFF1967D2)) }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GoDivider)
    )
}

/**
 * Proxy Manager (v2.0 Pro): saved proxies with live test (IP + country +
 * speed) and one-tap assign to a profile, plus the per-profile check.
 */
@Composable
private fun ProxiesTab(
    proxyStore: ProxyStore,
    profiles: List<Profile>,
    proxyCheckResult: String?,
    onCheckProxy: (Profile) -> Unit,
    onClearNotice: () -> Unit,
    onAssignProxy: (ProxyEntry, Profile) -> Unit
) {
    val savedProxies by proxyStore.proxies.collectAsState()
    val scope = rememberCoroutineScope()
    var checkingId by remember { mutableStateOf<String?>(null) }
    var assignTarget by remember { mutableStateOf<ProxyEntry?>(null) }
    var newName by remember { mutableStateOf("") }
    var newHost by remember { mutableStateOf("") }
    var newPort by remember { mutableStateOf("8080") }
    var newMode by remember { mutableStateOf("HTTP") }
    var newUser by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    // v2.6: the country picker moved into Add profile (user order) —
    // this tab keeps only saved proxies + manual add.
    var showManual by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Proxies", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Your proxies, saved on this phone. Test one, then assign it to a profile.",
            color = GoGreyText
        )

        // Manual add stays, but hidden behind one small link (v2.5).
        TextButton(onClick = { showManual = !showManual }) {
            Text(if (showManual) "Hide manual add" else "Add proxy manually")
        }
        // Add-proxy form.
        if (showManual) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Add proxy", fontWeight = FontWeight.Bold)
            Text(
                "Tip: copy a fast IP:port from hide.me and paste it here, then Test it.",
                color = GoGreyText
            )
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                label = { Text("Name (e.g. US residential)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropDownField(
                    "Mode", listOf("HTTP", "SOCKS5"), newMode
                ) { newMode = it }
            }
            OutlinedTextField(
                value = newHost, onValueChange = { newHost = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = newPort, onValueChange = { newPort = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = newUser, onValueChange = { newUser = it },
                label = { Text("Username (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = newPass, onValueChange = { newPass = it },
                label = { Text("Password (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val port = newPort.toIntOrNull()
                    when {
                        newName.isBlank() -> formError = "Name is required."
                        newHost.isBlank() -> formError = "Host is required."
                        port == null || port !in 1..65535 -> formError = "Port must be 1–65535."
                        else -> {
                            proxyStore.add(
                                ProxyEntry(
                                    name = newName.trim(),
                                    mode = newMode,
                                    host = newHost.trim(),
                                    port = port,
                                    user = newUser.trim(),
                                    pass = newPass
                                )
                            )
                            newName = ""; newHost = ""; newPort = "8080"
                            newUser = ""; newPass = ""; formError = null
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoGreen)
            ) { Text("Save proxy") }
        }
        }

        // Saved proxies.
        if (savedProxies.isEmpty()) {
            Text("No saved proxies yet.", color = GoGreyText)
        }
        for (entry in savedProxies) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    val okChip = entry.lastIp.isNotBlank()
                    if (okChip) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (entry.ok) Color(0xFFE6F4EA) else Color(0xFFFCE8E6))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (entry.ok) "Working" else "Failed",
                                fontSize = 12.sp,
                                color = if (entry.ok) GoGreenDark else Color(0xFFD93025),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text("${entry.mode} ${entry.host}:${entry.port}", color = GoGreyText)
                if (entry.lastIp.isNotBlank()) {
                    Text(
                        "IP ${entry.lastIp}" +
                            (if (entry.country.isNotBlank()) " • ${entry.country}" else "") +
                            (if (entry.lastTimezone.isNotBlank()) " • ${entry.lastTimezone}" else "") +
                            " • ${entry.lastMs} ms",
                        color = GoGreyText
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            checkingId = entry.id
                            scope.launch {
                                val checked = proxyStore.check(entry)
                                proxyStore.update(checked)
                                checkingId = null
                            }
                        },
                        enabled = checkingId != entry.id,
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen)
                    ) { Text(if (checkingId == entry.id) "Testing…" else "Test") }
                    Button(
                        onClick = { assignTarget = entry },
                        enabled = profiles.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen)
                    ) { Text("Assign") }
                    TextButton(onClick = { proxyStore.remove(entry.id) }) {
                        Text("Delete", color = Color(0xFFD93025))
                    }
                }
            }
        }

        // Per-profile proxies (quick check).
        Text("Profile proxies", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (profiles.isEmpty()) {
            Text("No profiles yet.", color = GoGreyText)
        }
        for (p in profiles) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GoDivider, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(p.name, fontWeight = FontWeight.Bold)
                Text(ProxyHelper.summary(p), color = GoGreyText)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onClearNotice(); onCheckProxy(p) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoGreen)
                ) { Text("Check proxy") }
            }
        }
        proxyCheckResult?.let { Text(it) }
    }

    // Assign-to-profile picker.
    assignTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { assignTarget = null },
            title = { Text("Assign \"${target.name}\" to…") },
            text = {
                LazyColumn {
                    items(profiles, key = { it.id }) { p ->
                        Text(
                            p.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAssignProxy(target, p)
                                    assignTarget = null
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { assignTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsTab(
    profiles: List<Profile>,
    proxyStore: ProxyStore,
    settingsStore: SettingsStore,
    onOpenLab: (Profile) -> Unit,
    trashCount: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onShowTrash: () -> Unit,
    onHideTrash: () -> Unit,
    showTrash: Boolean,
    onAbout: () -> Unit,
    notice: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Button(
            onClick = { profiles.firstOrNull()?.let(onOpenLab) },
            enabled = profiles.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fingerprint Lab — check a profile")
        }
        Button(
            onClick = onExport,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("Export all profiles (.zip)")
        }
        Button(
            onClick = onImport,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Import profiles (.zip)")
        }
        Button(
            onClick = { if (showTrash) onHideTrash() else onShowTrash() },
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text(if (showTrash) "Hide trash" else "Trash ($trashCount)")
        }
        Button(
            onClick = onAbout,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Info, null)
            Spacer(Modifier.width(8.dp))
            Text("About NovaProfile")
        }
        // v2.7: Webshare (optional) — free plan = 10 stable proxies +
        // 1GB/month, renewing monthly. The user creates an API key in
        // the Webshare dashboard (Integrations -> API Keys) and pastes
        // it here; the key stays on this phone (DataStore) and is sent
        // only to Webshare's own API. Paid ($2.99/mo) is optional and
        // never required: a free-plan key works exactly the same way.
        // Free Webshare proxies are shared datacenter IPs — stable, but
        // the whoer ~90% ceiling still applies.
        val wsScope = rememberCoroutineScope()
        val savedWsKey by settingsStore.webshareApiKey.collectAsState(initial = "")
        var wsKey by remember { mutableStateOf("") }
        var wsStatus by remember { mutableStateOf<String?>(null) }
        var wsFetching by remember { mutableStateOf(false) }
        Spacer(Modifier.height(4.dp))
        Text("Webshare (optional)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Stable proxies from Webshare: free plan gives 10 proxies + " +
                "1GB/month (renews monthly). Create an API key at " +
                "dashboard.webshare.io → Integrations → API Keys and " +
                "paste it here. The key stays on this phone. Paid " +
                "($2.99/mo) is optional — a free key works the same.",
            color = GoGreyText
        )
        if (savedWsKey.isNotBlank()) {
            Text("Key saved ✓", color = GoGreen)
        }
        OutlinedTextField(
            value = wsKey,
            onValueChange = { wsKey = it },
            label = { Text("Webshare API key (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    wsScope.launch {
                        settingsStore.setWebshareApiKey(wsKey.trim())
                        wsStatus = "Key saved on this phone."
                    }
                },
                enabled = wsKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                modifier = Modifier.weight(1f)
            ) { Text("Save key") }
            Button(
                onClick = {
                    wsScope.launch {
                        wsFetching = true
                        wsStatus = "Fetching from Webshare…"
                        val key = wsKey.ifBlank { savedWsKey }
                        val list = proxyStore.fetchWebshareProxies(key)
                        wsFetching = false
                        if (list.isEmpty()) {
                            wsStatus = "Nothing fetched — check the API key and try again."
                        } else {
                            val existing = proxyStore.proxies.value
                                .map { "${it.host}:${it.port}" }.toSet()
                            var added = 0
                            for (e in list) {
                                if ("${e.host}:${e.port}" !in existing) {
                                    proxyStore.add(e)
                                    added++
                                }
                            }
                            val us = list.count { it.country.startsWith("US") }
                            wsStatus = "Webshare: ${list.size} proxies found " +
                                "($us US), $added new saved." +
                                if (added == 0) " All were already saved." else ""
                        }
                    }
                },
                enabled = !wsFetching && (wsKey.isNotBlank() || savedWsKey.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                modifier = Modifier.weight(1f)
            ) { Text(if (wsFetching) "Fetching…" else "Fetch my proxies") }
        }
        if (savedWsKey.isNotBlank()) {
            TextButton(onClick = {
                wsScope.launch {
                    settingsStore.setWebshareApiKey("")
                    wsStatus = "Key cleared."
                }
            }) { Text("Clear saved key") }
        }
        wsStatus?.let { Text(it, color = GoGreyText) }
        notice?.let { Text(it, color = GoGreyText) }
        Spacer(Modifier.height(8.dp))
        Text(
            "Fingerprint protection is JS-level inside the system WebView. " +
                "It is NOT a Chromium fork — advanced fingerprint tests can " +
                "still detect the real device.",
            color = GoGreyText
        )
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

@Composable
fun EditorScreen(
    initial: Profile?,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit,
    onCheckProxy: (Profile) -> Unit,
    proxyCheckResult: String?,
    onClearNotice: () -> Unit,
    proxyStore: ProxyStore
) {
    var draft by remember { mutableStateOf(initial ?: Profile.new("Profile 1")) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    // v2.6: sheet views — main form, country proxy picker (moved here
    // from the Proxies tab, user order), extended-settings section.
    var proxyPickerOpen by remember { mutableStateOf(false) }
    var extSection by remember { mutableStateOf<String?>(null) }
    var connectingCountry by remember { mutableStateOf<String?>(null) }
    var countryStatus by remember { mutableStateOf<String?>(null) }

    // v2.5: GoLogin-style bottom sheet instead of a full-screen form.
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        draft = draft.copy(
                            proxyMode = working.mode,
                            proxyHost = working.host,
                            proxyPort = working.port,
                            proxyUser = "",
                            proxyPass = "",
                            // v2.5/v2.6: timezone + language + coordinates
                            // follow the proxy's IP — but only when the
                            // profile's modes say IP-based.
                            timezone = if (draft.timezoneMode == "IP") working.lastTimezone.ifBlank { cs.timezone } else draft.timezone,
                            language = if (draft.languagesMode == "IP") cs.language else draft.language,
                            latitude = if (draft.geoMode == "IP" && working.lastLat != 0.0) working.lastLat else draft.latitude,
                            longitude = if (draft.geoMode == "IP" && working.lastLon != 0.0) working.lastLon else draft.longitude
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
                    onDraft = { draft = it },
                    onBack = { extSection = null },
                    onOpenProxyPicker = { extSection = null; proxyPickerOpen = true },
                    onCheckProxy = onCheckProxy,
                    onClearNotice = onClearNotice,
                    proxyCheckResult = proxyCheckResult
                )
                else -> {
                    Text(
                        if (initial == null) "Add profile" else "Edit profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    // Templates (v2.0 Pro): one tap fills the identity below.
                    Text("Template", style = MaterialTheme.typography.labelLarge, color = GoGreyText)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (t in listOf("Facebook", "Google", "Amazon", "TikTok")) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val filled = applyTemplate(draft, t)
                                    draft = if (initial == null &&
                                        (draft.name == "Profile 1" || draft.name.isBlank())
                                    ) filled.copy(name = t) else filled
                                },
                                label = { Text(t) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8EAED),
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draft.name, onValueChange = { draft = draft.copy(name = it) },
                        label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.folder, onValueChange = { draft = draft.copy(folder = it) },
                        label = { Text("Folder") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    // Proxy row (v2.6): tap opens the country picker.
                    Text("Proxy", fontWeight = FontWeight.Bold)
                    SectionRow(
                        label = if (draft.proxyMode == "NONE") "Connect proxy"
                        else "${draft.proxyMode} ${draft.proxyHost}:${draft.proxyPort}",
                        sub = if (draft.proxyMode == "NONE") "Tap to pick a country"
                        else "Timezone ${draft.timezone} • ${draft.language}",
                        onClick = { proxyPickerOpen = true }
                    )
                    countryStatus?.let { Text(it, color = GoGreyText) }
                    // Extended settings (v2.6): GoLogin's 5 sections.
                    Text("Extended settings", fontWeight = FontWeight.Bold)
                    for ((key, label) in listOf(
                        "overview" to "Profile Overview",
                        "proxy" to "Proxy & Location",
                        "browser" to "Browser",
                        "storage" to "Storage",
                        "hardware" to "Hardware"
                    )) {
                        SectionRow(label) { extSection = key }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                when {
                                    draft.name.isBlank() -> error = "Name is required."
                                    draft.proxyMode != "NONE" && draft.proxyHost.isBlank() ->
                                        error = "Proxy host is required for ${draft.proxyMode}."
                                    draft.proxyPort !in 1..65535 ->
                                        error = "Proxy port must be 1–65535."
                                    else -> onSave(draft)
                                }
                            },
                            // v2.3: Save stays locked while a one-tap connect is
                            // running — saving early used to cancel the job, which
                            // is exactly how v2.2 lost the proxy on the phone.
                            enabled = connectingCountry == null,
                            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (connectingCountry != null) "Connecting ${connectingCountry}…"
                                else if (initial == null) "Add profile" else "Save profile"
                            )
                        }
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
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
