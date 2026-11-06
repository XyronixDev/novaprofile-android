package com.novaprofiles.app.ui

// File-level opt-in (Run #5 fix): TopAppBar / ExposedDropdownMenuBox /
// FilterChip are @ExperimentalMaterial3Api in the BOM 2026.08.00
// material3 version — the per-function @OptIn missed EditorScreen's
// TopAppBar (line 421), failing :app:compileReleaseKotlin.
@file:OptIn(ExperimentalMaterial3Api::class)

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaprofiles.app.data.Profile
import com.novaprofiles.app.util.ProxyHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NovaProfile", fontWeight = FontWeight.Bold)
                        Text(
                            "PRO • All features free",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.showTrash.value = !showTrash }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Trash")
                    }
                    IconButton(onClick = { exportLauncher.launch("novaprofile-profiles.zip") }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Export")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Import")
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "About")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search profiles…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFolder == "All",
                    onClick = { vm.selectedFolder.value = "All" },
                    label = { Text("All") }
                )
                for (f in folders) {
                    FilterChip(
                        selected = selectedFolder == f,
                        onClick = { vm.selectedFolder.value = f },
                        label = { Text(f) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

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
                        else "Tap + to create your first isolated profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { p ->
                        ProfileCard(
                            profile = p,
                            inTrash = showTrash,
                            onRun = {
                                vm.markRun(p.id)
                                ctx.startActivity(BrowserActivity.intent(ctx, p.id))
                            },
                            onEdit = { editing = p },
                            onDuplicate = { vm.duplicate(p) },
                            onTrash = { pendingDelete = p },
                            onRestore = { vm.restore(p) },
                            onDeleteForever = { pendingDelete = p }
                        )
                    }
                }
            }

            (importResult ?: exportResult)?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About NovaProfile") },
            text = {
                Text(
                    "NovaProfile 1.0.0 — original multi-profile browser.\n\n" +
                        "• Unlimited profiles, everything free, no billing.\n" +
                        "• Per-profile cookies, proxy, user-agent, language, " +
                        "screen, WebGL and Canvas settings.\n" +
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
fun ProfileCard(
    profile: Profile,
    inTrash: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${profile.folder} • ${ProxyHelper.summary(profile)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.homeUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (profile.lastRunAt == 0L) "Never run"
                        else "Last run: " + SimpleDateFormat(
                            "dd MMM, HH:mm", Locale.getDefault()
                        ).format(Date(profile.lastRunAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!inTrash) {
                    IconButton(onClick = onRun) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Run",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (inTrash) {
                    TextButton(onClick = onRestore) {
                        Icon(Icons.Filled.RestoreFromTrash, contentDescription = null)
                        Text("Restore")
                    }
                    TextButton(onClick = onDeleteForever) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Delete")
                    }
                } else {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("Edit")
                    }
                    TextButton(onClick = onDuplicate) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Text("Copy")
                    }
                    TextButton(onClick = onTrash) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Trash")
                    }
                }
            }
        }
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
                .padding(padding)
                .verticalScroll(scroll)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Basic", fontWeight = FontWeight.Bold)
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

            Text("Identity", fontWeight = FontWeight.Bold)
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
