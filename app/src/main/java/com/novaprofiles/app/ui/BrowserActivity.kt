// File-level opt-in (Run #5 fix, same cause as MainActivity.kt):
// TopAppBar and friends are @ExperimentalMaterial3Api in BOM 2026.08.00.
// NOTE (Run #7): a @file: annotation MUST sit before the package line.
@file:OptIn(ExperimentalMaterial3Api::class)
package com.novaprofiles.app.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.novaprofiles.app.NovaProfileApp
import com.novaprofiles.app.data.Profile
import com.novaprofiles.app.util.CookieStore
import com.novaprofiles.app.util.FingerprintJs
import com.novaprofiles.app.util.ProxyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.net.URL
import java.net.URLEncoder

/**
 * One profile's multi-tab browser (v1.1).
 *
 * Real tabs: each tab owns its own WebView (created on demand, kept in a
 * map, swapped into one container). New tab page, tab switcher, history,
 * bookmarks, downloads, find in page, desktop mode, translate and share
 * are all implemented on top of the system WebView.
 *
 * Isolation order (unchanged from v1.0):
 *  1. WebView.setDataDirectorySuffix("p<id>") BEFORE any WebView exists.
 *  2. Restore this profile's saved cookies, apply the profile's proxy.
 *  3. Fingerprint JS injected on every page start/finish.
 * Stop: save cookies for visited hosts, flush, clear the proxy override.
 *
 * Honest limit: this is the system WebView, not a Chromium fork — it
 * cannot spoof fingerprints at engine level the way GoLogin's Orbita does.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_START_URL = "start_url"

        fun intent(context: Context, profileId: Long): Intent =
            Intent(context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }

        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }

    private var activeProfile: Profile? = null
    private var startUrl: String? = null
    private val visitedHosts = mutableSetOf<String>()
    private var suffixApplied = false
    private var setupStatus by mutableStateOf<String?>(null)
    private var setupDone = false
    // v2.7.1 fail-closed proxy gate — set before setContent.
    private var proxyFailed by mutableStateOf(false)
    private var allowDirect by mutableStateOf(false)

    /** One tab: own WebView, own URL/title, own desktop + incognito flags. */
    inner class BrowserTab(val id: Int) {
        var webView: WebView? = null
        var title by mutableStateOf("New tab")
        var url by mutableStateOf("")
        var isNtp by mutableStateOf(true)
        var incognito by mutableStateOf(false)
        var desktop by mutableStateOf(false)
        var loading by mutableStateOf(false)
        var canGoBack by mutableStateOf(false)
        var canGoForward by mutableStateOf(false)
    }

    private val tabs = mutableStateListOf<BrowserTab>()
    private var activeTabId by mutableStateOf(0)
    private var nextTabId = 1

    /** Sessions (v2.0 Pro): the profile list can stop this browser. */
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra("profile_id", -1) ?: -1
            val mine = activeProfile?.id ?: -1
            if (id == -1L || id == mine) finish()
        }
    }

    private fun activeTab(): BrowserTab? = tabs.firstOrNull { it.id == activeTabId }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter("com.novaprofiles.app.STOP_PROFILE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1)
        if (profileId == -1L) {
            finish()
            return
        }
        startUrl = intent.getStringExtra(EXTRA_START_URL)?.takeIf { it.isNotBlank() }

        lifecycleScope.launch {
            val repo = (application as NovaProfileApp).repository
            val profile = withContext(Dispatchers.IO) { repo.getById(profileId) }
            if (profile == null) {
                finish()
                return@launch
            }
            activeProfile = profile
            try {
                WebView.setDataDirectorySuffix("p${profile.id}")
                suffixApplied = true
            } catch (e: Exception) {
                suffixApplied = false
            }
            // v2.7.1 FIX (user's whoer screenshots proved it): the proxy
            // override MUST be in place BEFORE the first WebView exists.
            // The old code called setContent immediately while the
            // override was still pending, so the first WebView was
            // created with a direct connection — the browser showed the
            // REAL IP even though the proxy test said "Proxy is ok!".
            // Now we WAIT for cookies + proxy, and if a configured proxy
            // fails to apply we do NOT open the browser direct
            // (fail-closed): an error screen offers Retry, or opening
            // without the proxy only on the user's explicit tap. No
            // silent IP leak.
            val proxyApplied = suspendCancellableCoroutine { cont ->
                val saved = CookieStore.deserialize(profile.cookiesData)
                CookieStore.restore(saved) {
                    ProxyHelper.apply(
                        profile,
                        ContextCompat.getMainExecutor(this@BrowserActivity)
                    ) { applied ->
                        if (cont.isActive) cont.resume(applied)
                    }
                }
            }
            val needsProxy = profile.proxyMode != "NONE" && profile.proxyHost.isNotBlank()
            proxyFailed = needsProxy && !proxyApplied
            setupStatus = buildString {
                append(if (suffixApplied) "Isolated storage: on" else "Isolated storage: shared (process already used)")
                append(" • Proxy: ")
                append(if (proxyApplied) ProxyHelper.summary(profile) else "unavailable")
            }
            setupDone = true
            setContent {
                NovaProfileTheme {
                    if (proxyFailed && !allowDirect) {
                        ProxyFailedScreen(
                            profile = profile,
                            onRetry = { recreate() },
                            onOpenDirect = { allowDirect = true },
                            onClose = { finish() }
                        )
                    } else {
                        BrowserScreen(profile = profile)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveCookiesNow()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
        }
        saveCookiesNow()
        tabs.forEach { it.webView?.destroy() }
        tabs.clear()
        ProxyHelper.clear(ContextCompat.getMainExecutor(this)) {}
        super.onDestroy()
    }

    private fun saveCookiesNow() {
        val profile = activeProfile ?: return
        if (visitedHosts.isEmpty()) return
        val cookies = CookieStore.readAll(visitedHosts.toSet())
        CookieManager.getInstance().flush()
        lifecycleScope.launch(Dispatchers.IO) {
            val repo = (application as NovaProfileApp).repository
            val current = repo.getById(profile.id) ?: return@launch
            val merged = CookieStore.deserialize(current.cookiesData).toMutableMap()
            merged.putAll(cookies)
            repo.saveCookies(profile.id, CookieStore.serialize(merged))
        }
    }

    private fun uaFor(tab: BrowserTab, profile: Profile): String =
        if (tab.desktop) DESKTOP_UA
        else if (profile.userAgent.isNotBlank()) profile.userAgent
        else Profile.defaultUserAgent(profile.userAgentPreset)

    /** Create (or return) the WebView for a tab, fully configured. */
    private fun tabWebView(tab: BrowserTab, profile: Profile): WebView {
        tab.webView?.let { return it }
        val wv = WebView(this)
        wv.settings.apply {
            javaScriptEnabled = true
            // v2.6: Storage section — local storage + IndexedDB per profile.
            domStorageEnabled = profile.domStorage
            databaseEnabled = profile.domStorage
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            // v2.6: geolocation API only when the profile spoofs it from
            // the IP; OFF blocks it natively too.
            setGeolocationEnabled(profile.geoMode == "IP")
            userAgentString = uaFor(tab, profile)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tab.loading = true
                url?.let {
                    tab.url = it
                    // v2.6: Save history per profile (incognito never saves).
                    if (!tab.incognito && profile.saveHistory) {
                        try {
                            visitedHosts.add(URL(it).host)
                        } catch (e: Exception) {
                        }
                    }
                }
                view?.evaluateJavascript(FingerprintJs.build(profile), null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                tab.loading = false
                tab.title = view?.title?.takeIf { it.isNotBlank() } ?: hostOf(tab.url)
                tab.canGoBack = view?.canGoBack() == true
                tab.canGoForward = view?.canGoForward() == true
                CookieManager.getInstance().flush()
                view?.evaluateJavascript(FingerprintJs.build(profile), null)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false // keep navigation inside this WebView
            }

            override fun onReceivedHttpAuthRequest(
                view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
            ) {
                if (profile.proxyUser.isNotBlank()) {
                    handler?.proceed(profile.proxyUser, profile.proxyPass)
                } else {
                    handler?.cancel()
                }
            }
        }
        wv.setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimetype)
                    .setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimetype)
                    )
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
            }
        }
        tab.webView = wv
        return wv
    }

    private fun hostOf(url: String): String = try {
        URL(url).host
    } catch (e: Exception) {
        url
    }

    private fun newTab(profile: Profile, incognito: Boolean = false): BrowserTab {
        val tab = BrowserTab(nextTabId++)
        tab.incognito = incognito
        tabWebView(tab, profile)
        tabs.add(tab)
        activeTabId = tab.id
        return tab
    }

    private fun closeTab(tab: BrowserTab, profile: Profile) {
        tab.webView?.destroy()
        tabs.remove(tab)
        if (tabs.isEmpty()) {
            newTab(profile)
        } else if (activeTabId == tab.id) {
            activeTabId = tabs.last().id
        }
    }

    private fun navigate(tab: BrowserTab, rawUrl: String) {
        val target = normalizeUrl(rawUrl)
        tab.isNtp = false
        tab.url = target
        tab.webView?.loadUrl(target)
    }

    private fun normalizeUrl(input: String): String {
        val t = input.trim()
        return when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(".") -> "https://$t"
            else -> "https://www.google.com/search?q=" +
                URLEncoder.encode(t, "UTF-8")
        }
    }

    /** v2.7.1 fail-closed screen: a configured proxy that could not be
     * applied must never silently fall back to a direct connection. */
    @Composable
    fun ProxyFailedScreen(
        profile: Profile,
        onRetry: () -> Unit,
        onOpenDirect: () -> Unit,
        onClose: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFB3261E),
                modifier = Modifier.size(32.dp)
            )
            Text("Proxy apply nahi hua", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Is profile ka proxy (${ProxyHelper.summary(profile)}) browser " +
                    "par lag nahi saka. Browser direct khulne se aapka asli IP " +
                    "dikh jayega — is liye browser nahi khola gaya.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retry") }
            TextButton(onClick = onOpenDirect, modifier = Modifier.fillMaxWidth()) {
                Text("Open without proxy (apna asli IP dikhega)")
            }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BrowserScreen(profile: Profile) {
        val context = LocalContext.current
        if (tabs.isEmpty()) {
            val first = newTab(profile)
            // Deep link (v2.1): e.g. Fingerprint Lab opening whoer.net.
            startUrl?.let { navigate(first, it); startUrl = null }
        }
        val tab = activeTab() ?: return
        var urlText by remember(tab.id) { mutableStateOf(tab.url) }
        var menuOpen by remember { mutableStateOf(false) }
        var showTabs by remember { mutableStateOf(false) }
        var showHistory by remember { mutableStateOf(false) }
        var showBookmarks by remember { mutableStateOf(false) }
        var showFind by remember { mutableStateOf(false) }
        var findQuery by remember { mutableStateOf("") }
        var status by remember { mutableStateOf<String?>(null) }

        // Keep the address text in sync with the active tab.
        if (urlText != tab.url && !tab.isNtp) urlText = tab.url

        val prefs = remember {
            context.getSharedPreferences("novaprofile_bookmarks", Context.MODE_PRIVATE)
        }
        var bookmarks by remember {
            mutableStateOf(prefs.getStringSet("urls", emptySet())?.toSet() ?: emptySet())
        }
        fun saveBookmarks(set: Set<String>) {
            bookmarks = set
            prefs.edit().putStringSet("urls", set).apply()
        }
        val isBookmarked = tab.url.isNotBlank() && bookmarks.contains(tab.url)

        BackHandler {
            val wv = tab.webView
            if (wv != null && wv.canGoBack()) wv.goBack() else finish()
        }

        DisposableEffect(Unit) {
            onDispose { saveCookiesNow() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GoBrowserBar)
        ) {
            // ---- Chrome-like top bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green "N" mark (own mark, green family of the reference).
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(8.dp))
                // Address pill (editable).
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GoBrowserPill)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Color(0xFFBDC1C6), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    TextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search or type URL", color = Color(0xFFBDC1C6)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { if (urlText.isNotBlank()) navigate(tab, urlText) }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        )
                    )
                    // v2.6: the star only shows when this profile saves bookmarks.
                    if (profile.saveBookmarks) {
                    IconButton(
                        onClick = {
                            if (tab.url.isBlank()) return@IconButton
                            if (isBookmarked) saveBookmarks(bookmarks - tab.url)
                            else saveBookmarks(bookmarks + tab.url)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFFBBC04) else Color(0xFFBDC1C6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    }
                }
                IconButton(onClick = { newTab(profile) }) {
                    Icon(Icons.Filled.Add, null, tint = Color.White)
                }
                // Tab count badge -> tab switcher.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent)
                        .clickable { showTabs = true }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${tabs.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoBrowserPill)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("New tab") },
                            onClick = { menuOpen = false; newTab(profile) }
                        )
                        DropdownMenuItem(
                            text = { Text("New Incognito tab") },
                            onClick = { menuOpen = false; newTab(profile, incognito = true) }
                        )
                        if (profile.saveHistory) {
                            DropdownMenuItem(
                                text = { Text("History") },
                                onClick = { menuOpen = false; showHistory = true }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete browsing data") },
                            onClick = {
                                menuOpen = false
                                CookieManager.getInstance().removeAllCookies(null)
                                tab.webView?.clearCache(true)
                                tab.webView?.clearHistory()
                                visitedHosts.clear()
                                status = "Browsing data deleted"
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            onClick = {
                                menuOpen = false
                                try {
                                    context.startActivity(
                                        Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                    )
                                } catch (e: Exception) {
                                    status = "Downloads folder unavailable"
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            onClick = { menuOpen = false; showBookmarks = true }
                        )
                        if (profile.saveHistory) {
                            DropdownMenuItem(
                                text = { Text("Recent tabs") },
                                onClick = { menuOpen = false; showHistory = true }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Share…") },
                            onClick = {
                                menuOpen = false
                                if (tab.url.isNotBlank()) {
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, tab.url)
                                            },
                                            "Share link"
                                        )
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find in page") },
                            onClick = { menuOpen = false; showFind = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Translate…") },
                            onClick = {
                                menuOpen = false
                                if (tab.url.isNotBlank()) {
                                    navigate(
                                        tab,
                                        "https://translate.google.com/translate?sl=auto&tl=en&u=" +
                                            URLEncoder.encode(tab.url, "UTF-8")
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (tab.desktop) "✓ Desktop site" else "Desktop site") },
                            onClick = {
                                menuOpen = false
                                tab.desktop = !tab.desktop
                                tab.webView?.settings?.userAgentString = uaFor(tab, profile)
                                tab.webView?.reload()
                            }
                        )
                    }
                }
            }
            if (tab.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GoGreen,
                    trackColor = Color.Transparent
                )
            }
            // Find in page bar.
            if (showFind) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2521))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            tab.webView?.findAllAsync(it)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Find in page", color = Color(0xFFBDC1C6)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        )
                    )
                    IconButton(onClick = { tab.webView?.findNext(false) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White)
                    }
                    IconButton(onClick = { tab.webView?.findNext(true) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White)
                    }
                    IconButton(onClick = {
                        showFind = false
                        findQuery = ""
                        tab.webView?.clearMatches()
                    }) {
                        Icon(Icons.Filled.Close, null, tint = Color.White)
                    }
                }
            }

            // ---- Content: new-tab page or the active tab's WebView ----
            Box(modifier = Modifier.weight(1f)) {
                if (tab.isNtp) {
                    NewTabPage(
                        onSearch = { q -> navigate(tab, q) },
                        onOpen = { url -> navigate(tab, url) },
                        onIncognito = { newTab(profile, incognito = true) }
                    )
                } else {
                    AndroidView(
                        factory = { ctx -> FrameLayout(ctx) },
                        update = { frame ->
                            val wv = tabWebView(tab, profile)
                            val parent = wv.parent as? ViewGroup
                            if (parent != frame) {
                                parent?.removeView(wv)
                                frame.removeAllViews()
                                frame.addView(
                                    wv,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Status line (isolation / proxy / notices).
            (status ?: setupStatus)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBDC1C6),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            // ---- Bottom bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoBrowserBar)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { tab.webView?.goBack() },
                    enabled = tab.canGoBack && !tab.isNtp
                ) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
                IconButton(
                    onClick = { tab.webView?.goForward() },
                    enabled = tab.canGoForward && !tab.isNtp
                ) { Icon(Icons.Filled.ArrowForward, null, tint = Color.White) }
                IconButton(onClick = { tab.webView?.reload() }) {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (tab.isNtp) profile.name else tab.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBDC1C6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // ---- Tab switcher ----
        if (showTabs) {
            AlertDialog(
                onDismissRequest = { showTabs = false },
                title = { Text("Tabs (${tabs.size})") },
                text = {
                    LazyColumn {
                        items(tabs, key = { it.id }) { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTabId = t.id
                                        showTabs = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (t.isNtp) "New tab" else t.title,
                                        fontWeight = if (t.id == activeTabId) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!t.isNtp) {
                                        Text(
                                            hostOf(t.url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GoGreyText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(onClick = { closeTab(t, profile) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close tab")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        newTab(profile)
                        showTabs = false
                    }) { Text("New tab") }
                },
                dismissButton = {
                    TextButton(onClick = { showTabs = false }) { Text("Close") }
                }
            )
        }

        // ---- History / Recent tabs ----
        if (showHistory) {
            AlertDialog(
                onDismissRequest = { showHistory = false },
                title = { Text("History") },
                text = {
                    if (visitedHosts.isEmpty()) {
                        Text("No history yet in this run.")
                    } else {
                        LazyColumn {
                            items(visitedHosts.toList()) { host ->
                                Text(
                                    host,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showHistory = false
                                            navigate(tab, "https://$host")
                                        }
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistory = false }) { Text("Close") }
                }
            )
        }

        // ---- Bookmarks ----
        if (showBookmarks) {
            AlertDialog(
                onDismissRequest = { showBookmarks = false },
                title = { Text("Bookmarks") },
                text = {
                    if (bookmarks.isEmpty()) {
                        Text("No bookmarks yet. Tap the star in the address bar to save a page.")
                    } else {
                        LazyColumn {
                            items(bookmarks.toList()) { url ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        hostOf(url),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                showBookmarks = false
                                                navigate(tab, url)
                                            }
                                            .padding(vertical = 10.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { saveBookmarks(bookmarks - url) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarks = false }) { Text("Close") }
                }
            )
        }
    }

    /** Chrome-style new-tab page (own layout, dark like the reference). */
    @Composable
    private fun NewTabPage(
        onSearch: (String) -> Unit,
        onOpen: (String) -> Unit,
        onIncognito: () -> Unit
    ) {
        var query by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GoBrowserBar)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Text(
                "Google",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(28.dp))
            // Search pill.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(GoBrowserPill)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Color(0xFFBDC1C6))
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search Google or type URL", color = Color(0xFFBDC1C6)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (query.isNotBlank()) onSearch(query) }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )
                Icon(Icons.Filled.Mic, null, tint = Color(0xFFBDC1C6))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(GoBrowserPill)
                        .clickable { if (query.isNotBlank()) onSearch(query) else onSearch("https://www.google.com") }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI Mode", color = Color.White)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(GoBrowserPill)
                        .clickable { onIncognito() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Incognito", color = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            // Shortcuts card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GoBrowserCard)
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Shortcut("Facebook", Color(0xFF1877F2), "f") { onOpen("https://www.facebook.com") }
                    Shortcut("YouTube", Color(0xFFFF0000), null) { onOpen("https://www.youtube.com") }
                    Shortcut("X", Color.Black, "X") { onOpen("https://www.x.com") }
                    Shortcut("Wikipedia", Color.White, "W") { onOpen("https://www.wikipedia.org") }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Stories card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GoBrowserCard)
                    .padding(20.dp)
            ) {
                Text("No stories available", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Check back later for new stories", color = Color(0xFFBDC1C6))
            }
        }
    }

    @Composable
    private fun Shortcut(label: String, bg: Color, letter: String?, onClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A332E)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    if (label == "YouTube") {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    } else {
                        Text(
                            letter ?: "",
                            color = if (label == "Wikipedia") Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp)
        }
    }
}
