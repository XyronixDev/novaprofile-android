package com.novaprofiles.app.ui

// File-level opt-in (Run #5 fix, same cause as MainActivity.kt):
// TopAppBar and friends are @ExperimentalMaterial3Api in BOM 2026.08.00.
@file:OptIn(ExperimentalMaterial3Api::class)

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Runs one profile's isolated WebView.
 *
 * Order matters:
 *  1. WebView.setDataDirectorySuffix("p<id>") BEFORE any WebView exists in
 *     this process (first launch per profile). If the process already
 *     created a WebView for another profile, the call throws — caught, and
 *     the run falls back to cookie-restore isolation only.
 *  2. Restore this profile's saved cookies (clearing all others first).
 *  3. Apply the profile's proxy override.
 *  4. Load the home URL with the profile's user-agent and JS fingerprint
 *     overrides injected on every page start.
 *
 * Stop: save cookies for visited hosts, flush, clear the proxy override.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"

        fun intent(context: Context, profileId: Long): Intent =
            Intent(context, BrowserActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
    }

    private var activeProfile: Profile? = null
    private val visitedHosts = mutableSetOf<String>()
    private var webViewRef: WebView? = null
    private var suffixApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1)
        if (profileId == -1L) {
            finish()
            return
        }

        lifecycleScope.launch {
            val repo = (application as NovaProfileApp).repository
            val profile = withContext(Dispatchers.IO) { repo.getById(profileId) }
            if (profile == null) {
                finish()
                return@launch
            }
            activeProfile = profile
            // Data-dir suffix: only valid before the first WebView exists.
            try {
                WebView.setDataDirectorySuffix("p${profile.id}")
                suffixApplied = true
            } catch (e: Exception) {
                suffixApplied = false
            }
            setContent {
                NovaProfileTheme {
                    BrowserScreen(profile = profile)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveCookiesNow()
    }

    override fun onDestroy() {
        saveCookiesNow()
        webViewRef?.destroy()
        webViewRef = null
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BrowserScreen(profile: Profile) {
        var urlText by remember { mutableStateOf(profile.homeUrl) }
        var currentUrl by remember { mutableStateOf(profile.homeUrl) }
        var loading by remember { mutableStateOf(false) }
        var canGoBack by remember { mutableStateOf(false) }
        var canGoForward by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf<String?>(null) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        val context = LocalContext.current

        BackHandler {
            val wv = webView
            if (wv != null && wv.canGoBack()) wv.goBack() else finish()
        }

        DisposableEffect(Unit) {
            onDispose {
                // Save once more when the screen leaves.
                saveCookiesNow()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(profile.name) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Stop profile")
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Enter URL") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val target = normalizeUrl(urlText)
                        urlText = target
                        webView?.loadUrl(target)
                    }) { Text("Go") }
                }
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            webView = this
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = false
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                setSupportMultipleWindows(false)
                                cacheMode = WebSettings.LOAD_DEFAULT
                                setGeolocationEnabled(false)
                                userAgentString =
                                    if (profile.userAgent.isNotBlank()) profile.userAgent
                                    else Profile.defaultUserAgent(profile.userAgentPreset)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    super.onPageStarted(view, url, favicon)
                                    loading = true
                                    url?.let {
                                        currentUrl = it
                                        urlText = it
                                        try {
                                            visitedHosts.add(URL(it).host)
                                        } catch (e: Exception) {
                                        }
                                    }
                                    view?.evaluateJavascript(FingerprintJs.build(profile), null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    loading = false
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                    CookieManager.getInstance().flush()
                                    // Re-inject after load so late-reading scripts see it too.
                                    view?.evaluateJavascript(FingerprintJs.build(profile), null)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false // keep navigation inside this WebView
                                }

                                override fun onReceivedHttpAuthRequest(
                                    view: WebView?,
                                    handler: HttpAuthHandler?,
                                    host: String?,
                                    realm: String?
                                ) {
                                    if (profile.proxyUser.isNotBlank()) {
                                        handler?.proceed(profile.proxyUser, profile.proxyPass)
                                    } else {
                                        handler?.cancel()
                                    }
                                }
                            }
                            // 1) restore cookies, 2) apply proxy, 3) load home URL.
                            val saved = CookieStore.deserialize(profile.cookiesData)
                            CookieStore.restore(saved) {
                                ProxyHelper.apply(
                                    profile,
                                    ContextCompat.getMainExecutor(context)
                                ) { applied ->
                                    status = buildString {
                                        append(if (suffixApplied) "Isolated storage: on" else "Isolated storage: shared (process already used)")
                                        append(" • Proxy: ")
                                        append(if (applied) ProxyHelper.summary(profile) else "unavailable")
                                    }
                                    loadUrl(profile.homeUrl)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack
                    ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = canGoForward
                    ) { Icon(Icons.Filled.ArrowForward, contentDescription = "Forward") }
                    Spacer(Modifier.weight(1f))
                    Text(
                        currentUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }

    private fun normalizeUrl(input: String): String {
        val t = input.trim()
        return when {
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(".") -> "https://$t"
            else -> "https://www.google.com/search?q=" +
                java.net.URLEncoder.encode(t, "UTF-8")
        }
    }
}
