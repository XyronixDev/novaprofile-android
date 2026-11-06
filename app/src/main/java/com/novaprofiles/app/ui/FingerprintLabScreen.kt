// File-level opt-in: TopAppBar is @ExperimentalMaterial3Api in BOM 2026.08.00.
@file:OptIn(ExperimentalMaterial3Api::class)
package com.novaprofiles.app.ui

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.viewinterop.AndroidView
import com.novaprofiles.app.data.Profile
import com.novaprofiles.app.util.FingerprintJs
import com.novaprofiles.app.util.FingerprintLab

/**
 * Fingerprint Lab (v2.0 Pro): live check of what a site would see for a
 * profile. Runs the profile's fingerprint injection in a hidden WebView,
 * then reads the spoofed values and compares them with the device's real
 * values. Green = spoofed / matches the profile setting. Red = the real
 * device value is exposed. Honest scope: JS-level only, system WebView.
 */
class LabBridge(private val onResult: (String) -> Unit) {
    @JavascriptInterface
    fun postResult(json: String) {
        Handler(Looper.getMainLooper()).post { onResult(json) }
    }
}

private data class LabRow(
    val label: String,
    val detected: String,
    val status: String, // "Spoofed" | "Exposed" | "As set" | "Info"
    val good: Boolean? // true green, false red, null grey
)

@Composable
fun FingerprintLabScreen(profile: Profile, onBack: () -> Unit) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<FingerprintLab.LabResult?>(null) }
    var running by remember { mutableStateOf(false) }
    var hasRun by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val realUa = remember { android.webkit.WebSettings.getDefaultUserAgent(context) }
    val realScreen = remember {
        val m = context.resources.displayMetrics
        "${m.widthPixels}x${m.heightPixels}"
    }
    val realTz = remember { java.util.TimeZone.getDefault().id }
    val realLang = remember { java.util.Locale.getDefault().toLanguageTag() }

    fun runCheck() {
        val wv = webView ?: return
        running = true
        result = null
        wv.evaluateJavascript(FingerprintJs.build(profile)) {
            wv.evaluateJavascript(FingerprintLab.script(), null)
        }
    }

    val rows: List<LabRow> = result?.let { r ->
        val expectedTz = if (profile.timezone == "Auto") realTz else profile.timezone
        val expectedScreen = "${profile.screenWidth}x${profile.screenHeight}"
        listOf(
            LabRow(
                "User-Agent",
                r.userAgent.ifBlank { "(empty)" },
                if (r.userAgent != realUa) "Spoofed" else "Exposed",
                r.userAgent != realUa
            ),
            LabRow(
                "Language",
                r.language.ifBlank { "(empty)" },
                if (r.language != realLang) "Spoofed" else "As set",
                if (r.language != realLang) true else null
            ),
            LabRow(
                "Timezone",
                r.timezone.ifBlank { "(empty)" },
                when {
                    r.timezone == expectedTz && expectedTz != realTz -> "Spoofed"
                    r.timezone == realTz && expectedTz != realTz -> "Exposed"
                    else -> "As set"
                },
                when {
                    r.timezone == expectedTz && expectedTz != realTz -> true
                    r.timezone == realTz && expectedTz != realTz -> false
                    else -> null
                }
            ),
            LabRow(
                "Screen",
                r.screen.ifBlank { "(empty)" },
                when {
                    r.screen.startsWith(expectedScreen) && expectedScreen != realScreen -> "Spoofed"
                    r.screen.startsWith(realScreen) && expectedScreen != realScreen -> "Exposed"
                    else -> "As set"
                },
                when {
                    r.screen.startsWith(expectedScreen) && expectedScreen != realScreen -> true
                    r.screen.startsWith(realScreen) && expectedScreen != realScreen -> false
                    else -> null
                }
            ),
            LabRow(
                "Device memory",
                r.deviceMemory.ifBlank { "(empty)" },
                if (r.deviceMemory == profile.deviceMemory.toString()) "As set" else "Differs",
                if (r.deviceMemory == profile.deviceMemory.toString()) null else false
            ),
            LabRow(
                "CPU cores",
                r.hardwareConcurrency.ifBlank { "(empty)" },
                if (r.hardwareConcurrency == profile.hardwareConcurrency.toString()) "As set" else "Differs",
                if (r.hardwareConcurrency == profile.hardwareConcurrency.toString()) null else false
            ),
            LabRow(
                "Canvas",
                if (r.canvasNoisy) "hash changes every read (${r.canvasHash1})" else "stable hash ${r.canvasHash1}",
                if (r.canvasNoisy) "Spoofed" else "Exposed",
                r.canvasNoisy
            ),
            LabRow(
                "WebGL vendor",
                r.webglVendor.ifBlank { "(empty)" },
                if (r.webglVendor == profile.webglVendor) "As set" else "Differs",
                if (r.webglVendor == profile.webglVendor) null else false
            ),
            LabRow(
                "WebGL renderer",
                r.webglRenderer.ifBlank { "(empty)" },
                if (r.webglRenderer == profile.webglRenderer) "As set" else "Differs",
                if (r.webglRenderer == profile.webglRenderer) null else false
            ),
            LabRow(
                "WebRTC local IP",
                when (r.webrtcLeak) {
                    "none" -> "No local IP leaked"
                    "blocked" -> "RTC blocked"
                    "" -> "(pending)"
                    else -> "Leaked ${r.webrtcLeak}"
                },
                when (r.webrtcLeak) {
                    "none", "blocked" -> "Spoofed"
                    "" -> "Info"
                    else -> "Exposed"
                },
                when (r.webrtcLeak) {
                    "none", "blocked" -> true
                    "" -> null
                    else -> false
                }
            ),
            LabRow(
                "Do Not Track",
                r.doNotTrack.ifBlank { "(off)" },
                if ((r.doNotTrack == "1") == profile.doNotTrack) "As set" else "Differs",
                if ((r.doNotTrack == "1") == profile.doNotTrack) null else false
            ),
            LabRow("Platform", r.platform.ifBlank { "(empty)" }, "Info", null),
            LabRow("Touch points", r.touchPoints.ifBlank { "(empty)" }, "Info", null)
        )
    } ?: emptyList()

    val scored = rows.filter { it.good != null }
    val goodCount = scored.count { it.good == true }
    val score = if (scored.isEmpty()) null else (goodCount * 100 / scored.size)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fingerprint Lab") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp)
        ) {
            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "What a website would see for this profile, checked live.",
                color = GoGreyText
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { runCheck() },
                enabled = !running,
                colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (running) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Checking… (about 3s)")
                } else {
                    Text(if (result == null) "Run fingerprint check" else "Run again")
                }
            }
            Spacer(Modifier.height(12.dp))
            score?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Spoof score: $it%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (it >= 70) GoGreenDark else Color(0xFFD93025)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("$goodCount of ${scored.size} checks spoofed", color = GoGreyText)
                }
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rows.size) { i ->
                    val row = rows[i]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8F9FA))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.label,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val chipColor = when (row.good) {
                                true -> GoGreenDark
                                false -> Color(0xFFD93025)
                                null -> GoGreyText
                            }
                            val chipBg = when (row.good) {
                                true -> Color(0xFFE6F4EA)
                                false -> Color(0xFFFCE8E6)
                                null -> Color(0xFFF1F3F4)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(row.status, fontSize = 12.sp, color = chipColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            row.detected,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF202124)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "JS-level check inside the system WebView — not an engine-level " +
                    "test. Advanced fingerprint services can still detect the device.",
                color = GoGreyText,
                style = MaterialTheme.typography.bodySmall
            )
            // Hidden lab WebView (1dp) — runs the checks, never shown.
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            LabBridge { json ->
                                result = FingerprintLab.parse(json)
                                running = false
                            },
                            "NovaLab"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!hasRun) {
                                    hasRun = true
                                    runCheck()
                                }
                            }
                        }
                        webView = this
                        loadUrl("about:blank")
                    }
                },
                modifier = Modifier.size(1.dp)
            )
        }
    }
}
