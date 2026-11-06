package com.novaprofiles.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novaprofiles.app.NovaProfileApp
import com.novaprofiles.app.data.Profile
import com.novaprofiles.app.data.ProfileRepository
import com.novaprofiles.app.util.ProfileExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: ProfileRepository =
        (application as NovaProfileApp).repository

    val query = MutableStateFlow("")
    val selectedFolder = MutableStateFlow("All")
    val showTrash = MutableStateFlow(false)

    val profiles: StateFlow<List<Profile>> = combine(
        repo.allProfiles, query, selectedFolder
    ) { list, q, folder ->
        list.filter { p ->
            (folder == "All" || p.folder == folder) &&
                (q.isBlank() || p.name.contains(q, true) || p.folder.contains(q, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashProfiles: StateFlow<List<Profile>> = repo.trash
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = repo.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proxyCheckResult = MutableStateFlow<String?>(null)
    val importResult = MutableStateFlow<String?>(null)
    val exportResult = MutableStateFlow<String?>(null)

    fun create(profile: Profile) = viewModelScope.launch {
        repo.create(profile)
    }

    fun update(profile: Profile) = viewModelScope.launch {
        repo.update(profile)
    }

    // v2.6: suspending update — the Run-time location re-sync must land
    // in the DB before BrowserActivity reads the profile by id, or the
    // browser opens with the stale timezone.
    suspend fun updateNow(profile: Profile) {
        repo.update(profile)
    }

    fun duplicate(profile: Profile) = viewModelScope.launch {
        repo.duplicate(profile)
    }

    fun moveToTrash(profile: Profile) = viewModelScope.launch {
        repo.moveToTrash(profile)
    }

    fun restore(profile: Profile) = viewModelScope.launch {
        repo.restore(profile)
    }

    fun deleteForever(profile: Profile) = viewModelScope.launch {
        repo.deleteForever(profile)
    }

    fun markRun(id: Long) = viewModelScope.launch {
        repo.markRun(id)
    }

    fun exportAll(out: OutputStream, list: List<Profile>) = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) { ProfileExport.export(list, out) }
            exportResult.value = "Exported ${list.size} profile(s)."
        } catch (e: Exception) {
            exportResult.value = "Export failed: ${e.message}"
        }
    }

    fun importFrom(input: InputStream) = viewModelScope.launch {
        try {
            val imported = withContext(Dispatchers.IO) { ProfileExport.import(input) }
            var n = 0
            for (p in imported) {
                repo.create(p)
                n++
            }
            importResult.value = "Imported $n profile(s)."
        } catch (e: Exception) {
            importResult.value = "Import failed: ${e.message}"
        }
    }

    /** Real proxy check: fetch the probe URL through the profile's proxy. */
    fun checkProxy(profile: Profile, probeUrl: String) = viewModelScope.launch {
        proxyCheckResult.value = "Checking…"
        val ok = withContext(Dispatchers.IO) {
            try {
                val proxy = when (profile.proxyMode) {
                    "HTTP" -> Proxy(
                        Proxy.Type.HTTP,
                        InetSocketAddress(profile.proxyHost, profile.proxyPort)
                    )
                    "SOCKS5" -> Proxy(
                        Proxy.Type.SOCKS,
                        InetSocketAddress(profile.proxyHost, profile.proxyPort)
                    )
                    else -> Proxy.NO_PROXY
                }
                val conn = URL(probeUrl).openConnection(proxy) as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty(
                    "User-Agent",
                    if (profile.userAgent.isNotBlank()) profile.userAgent
                    else Profile.defaultUserAgent(profile.userAgentPreset)
                )
                if (profile.proxyUser.isNotBlank()) {
                    val creds = android.util.Base64.encodeToString(
                        "${profile.proxyUser}:${profile.proxyPass}".toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    conn.setRequestProperty("Proxy-Authorization", "Basic $creds")
                }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            } catch (e: Exception) {
                false
            }
        }
        proxyCheckResult.value = if (ok) "Proxy works ✓" else "Proxy failed ✗"
    }

    fun clearNotices() {
        proxyCheckResult.value = null
        importResult.value = null
        exportResult.value = null
    }
}
