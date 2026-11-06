package com.novaprofiles.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One isolated browser profile.
 *
 * Field shapes follow the behaviour observed in the REA spec
 * (GOLOGIN_REA_SPEC.md §3.4 ProxyModel / §7.1) — re-implemented here as
 * our own data class with our own code. No GoLogin code, names or
 * endpoints are used.
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val folder: String = "General",
    val homeUrl: String = "https://www.google.com",

    // Identity
    val userAgentPreset: String = "Chrome Android",
    val userAgent: String = "",
    val language: String = "en-US",
    val timezone: String = "Auto",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geoAccuracy: Int = 100,
    val screenWidth: Int = 1080,
    val screenHeight: Int = 2400,
    val deviceMemory: Int = 8,
    val hardwareConcurrency: Int = 8,

    // Proxy — NONE, HTTP or SOCKS5. Users bring their own proxies (or none).
    val proxyMode: String = "NONE",
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val proxyUser: String = "",
    val proxyPass: String = "",

    // Fingerprint (JS-level overrides, see util/FingerprintJs.kt)
    val canvasMode: String = "noise",
    val webglVendor: String = "Google Inc. (NVIDIA)",
    val webglRenderer: String = "ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
    val webrtcMode: String = "disable",
    val fontsMasking: Boolean = true,
    val doNotTrack: Boolean = true,

    val notes: String = "",

    // Persisted cookies for this profile, as "url<TAB>cookie" lines.
    // Saved on stop, restored on launch — this is what isolates profiles
    // at the cookie level in v1 (see util/CookieStore.kt).
    val cookiesData: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0,
    val deletedAt: Long? = null
) {
    companion object {
        val USER_AGENT_PRESETS = listOf(
            "Chrome Android",
            "Chrome Desktop",
            "Firefox Android",
            "Safari iOS",
            "Custom"
        )

        // v2.1: Chrome 151 (matches Orbita's Chromium 151, so leak-check
        // sites report "Chrome 151" instead of the system WebView's 126).
        fun defaultUserAgent(preset: String): String = when (preset) {
            "Chrome Desktop" ->
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
            "Firefox Android" ->
                "Mozilla/5.0 (Android 14; Mobile; rv:126.0) Gecko/126.0 Firefox/126.0"
            "Safari iOS" ->
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 " +
                    "Mobile/15E148 Safari/604.1"
            else ->
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36"
        }

        fun new(name: String): Profile = Profile(name = name.ifBlank { "Profile 1" })

        // v2.2: one-tap country proxy — timezone + language that match the
        // country, so a leak-check site sees ONE identity (IP country =
        // timezone = language). WebRTC is already "disable" by default, so
        // no real-IP leak through the proxy.
        data class CountrySettings(
            val code: String,
            val label: String,
            val timezone: String,
            val language: String
        )

        val oneTapCountries = listOf("US", "GB", "CA", "AU", "FR", "DE", "NL", "JP")

        fun countrySettings(code: String): CountrySettings = when (code.uppercase()) {
            "US" -> CountrySettings("US", "United States", "America/New_York", "en-US")
            "GB" -> CountrySettings("GB", "United Kingdom", "Europe/London", "en-GB")
            "CA" -> CountrySettings("CA", "Canada", "America/Toronto", "en-CA")
            "AU" -> CountrySettings("AU", "Australia", "Australia/Sydney", "en-AU")
            "FR" -> CountrySettings("FR", "France", "Europe/Paris", "fr-FR")
            "DE" -> CountrySettings("DE", "Germany", "Europe/Berlin", "de-DE")
            "NL" -> CountrySettings("NL", "Netherlands", "Europe/Amsterdam", "nl-NL")
            "JP" -> CountrySettings("JP", "Japan", "Asia/Tokyo", "ja-JP")
            else -> CountrySettings(code.uppercase(), code.uppercase(), "UTC", "en-US")
        }
    }
}
