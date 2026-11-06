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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var runningId by remember { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }

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
            onClearNotice = { vm.clearNotices() }
        )
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
                    runningId = runningId,
                    onRun = { p ->
                        runningId = p.id
                        vm.markRun(p.id)
                        ctx.startActivity(BrowserActivity.intent(ctx, p.id))
                    },
                    onStop = { runningId = null },
                    onEdit = { editing = it },
                    onDuplicate = { vm.duplicate(it) },
                    onTrash = { pendingDelete = it },
                    onRestore = { vm.restore(it) },
                    onDeleteForever = { pendingDelete = it },
                    onShowAbout = { showAbout = true }
                )
                HomeTab.PROXIES -> ProxiesTab(
                    profiles = profiles,
                    proxyCheckResult = proxyCheck,
                    onCheckProxy = { p -> vm.checkProxy(p, "https://api.ipify.org") },
                    onClearNotice = { vm.clearNotices() }
                )
                HomeTab.SETTINGS -> SettingsTab(
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

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About NovaProfile") },
            text = {
                Text(
                    "NovaProfile 1.1.0 — original multi-profile browser.\n\n" +
                        "• Unlimited profiles, everything free, no billing.\n" +
                        "• Per-profile cookies, proxy, user-agent, language, " +
                        "screen, WebGL and Canvas settings.\n" +
                        "• Multi-tab browser with history, bookmarks, downloads, " +
                        "desktop mode, find in page and translate.\n" +
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
            .padding(horizontal = 14.dp, vertical = 4.dp),
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
            fontSize = 11.sp,
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
    runningId: Long?,
    onRun: (Profile) -> Unit,
    onStop: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDuplicate: (Profile) -> Unit,
    onTrash: (Profile) -> Unit,
    onRestore: (Profile) -> Unit,
    onDeleteForever: (Profile) -> Unit,
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
                        running = runningId == p.id,
                        onRun = { onRun(p) },
                        onStop = onStop,
                        onEdit = { onEdit(p) },
                        onDuplicate = { onDuplicate(p) },
                        onTrash = { onTrash(p) },
                        onRestore = { onRestore(p) },
                        onDeleteForever = { onDeleteForever(p) }
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
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
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

@Composable
private fun ProxiesTab(
    profiles: List<Profile>,
    proxyCheckResult: String?,
    onCheckProxy: (Profile) -> Unit,
    onClearNotice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Proxies", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Each profile can use its own HTTP or SOCKS5 proxy. " +
                "Add one in the profile editor — no proxy is needed to browse.",
            color = GoGreyText
        )
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onClearNotice(); onCheckProxy(p) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoGreen)
                    ) { Text("Check proxy") }
                }
            }
        }
        proxyCheckResult?.let { Text(it) }
    }
}

@Composable
private fun SettingsTab(
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
    onClearNotice: () -> Unit
) {
    var draft by remember { mutableStateOf(initial ?: Profile.new("Profile 1")) }
    var error by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "New profile" else "Edit profile") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(scroll)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Overview", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = draft.name, onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = draft.folder, onValueChange = { draft = draft.copy(folder = it) },
                label = { Text("Folder") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = draft.homeUrl, onValueChange = { draft = draft.copy(homeUrl = it) },
                label = { Text("Home URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = draft.notes, onValueChange = { draft = draft.copy(notes = it) },
                label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth()
            )

            Text("Browser", fontWeight = FontWeight.Bold)
            DropDownField(
                "User-agent preset", Profile.USER_AGENT_PRESETS,
                draft.userAgentPreset
            ) { draft = draft.copy(userAgentPreset = it) }
            if (draft.userAgentPreset == "Custom") {
                OutlinedTextField(
                    value = draft.userAgent,
                    onValueChange = { draft = draft.copy(userAgent = it) },
                    label = { Text("Custom user-agent") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = draft.language, onValueChange = { draft = draft.copy(language = it) },
                label = { Text("Language (e.g. en-US)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = draft.timezone, onValueChange = { draft = draft.copy(timezone = it) },
                label = { Text("Timezone (Auto or e.g. America/New_York)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.screenWidth.toString(),
                    onValueChange = { draft = draft.copy(screenWidth = it.toIntOrNull() ?: draft.screenWidth) },
                    label = { Text("Screen width") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = draft.screenHeight.toString(),
                    onValueChange = { draft = draft.copy(screenHeight = it.toIntOrNull() ?: draft.screenHeight) },
                    label = { Text("Screen height") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.deviceMemory.toString(),
                    onValueChange = { draft = draft.copy(deviceMemory = it.toIntOrNull() ?: draft.deviceMemory) },
                    label = { Text("Device memory (GB)") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = draft.hardwareConcurrency.toString(),
                    onValueChange = { draft = draft.copy(hardwareConcurrency = it.toIntOrNull() ?: draft.hardwareConcurrency) },
                    label = { Text("CPU cores") },
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }

            Text("Proxy", fontWeight = FontWeight.Bold)
            DropDownField(
                "Proxy mode", listOf("NONE", "HTTP", "SOCKS5"), draft.proxyMode
            ) { draft = draft.copy(proxyMode = it) }
            if (draft.proxyMode != "NONE") {
                OutlinedTextField(
                    value = draft.proxyHost, onValueChange = { draft = draft.copy(proxyHost = it) },
                    label = { Text("Proxy host") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = draft.proxyPort.toString(),
                    onValueChange = { draft = draft.copy(proxyPort = it.toIntOrNull() ?: draft.proxyPort) },
                    label = { Text("Proxy port") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = draft.proxyUser, onValueChange = { draft = draft.copy(proxyUser = it) },
                    label = { Text("Proxy username (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = draft.proxyPass, onValueChange = { draft = draft.copy(proxyPass = it) },
                    label = { Text("Proxy password (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Button(onClick = { onClearNotice(); onCheckProxy(draft) }) {
                    Text("Check proxy")
                }
                proxyCheckResult?.let { Text(it) }
            }

            Text("Fingerprint (JS-level)", fontWeight = FontWeight.Bold)
            DropDownField(
                "Canvas", listOf("noise", "off"), draft.canvasMode
            ) { draft = draft.copy(canvasMode = it) }
            OutlinedTextField(
                value = draft.webglVendor, onValueChange = { draft = draft.copy(webglVendor = it) },
                label = { Text("WebGL vendor") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = draft.webglRenderer, onValueChange = { draft = draft.copy(webglRenderer = it) },
                label = { Text("WebGL renderer") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            DropDownField(
                "WebRTC", listOf("disable", "public"), draft.webrtcMode
            ) { draft = draft.copy(webrtcMode = it) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Do Not Track")
                Switch(
                    checked = draft.doNotTrack,
                    onCheckedChange = { draft = draft.copy(doNotTrack = it) }
                )
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
                    modifier = Modifier.weight(1f)
                ) { Text("Save profile") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
