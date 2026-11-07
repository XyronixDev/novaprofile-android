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
    // v2.7.2: the proxy's 2-letter country code (e.g. "CA"). The home
    // card used to guess the flag from the language — but the language
    // only updates when the country parse succeeds, so after an Assign
    // the card kept showing the OLD country while the proxy had already
    // changed. Storing the code on the profile makes the card show the
    // real proxy country, always.
    val proxyCountry: String = "",

    // Fingerprint (JS-level overrides, see util/FingerprintJs.kt)
    val canvasMode: String = "noise",
    val webglVendor: String = "Google Inc. (NVIDIA)",
    val webglRenderer: String = "ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
    val webrtcMode: String = "disable",
    val fontsMasking: Boolean = true,
    val doNotTrack: Boolean = true,

    // v2.6: Extended settings (GoLogin-style sections) — every field is
    // actually applied (FingerprintJs / BrowserActivity), none is a dummy.
    val os: String = "Android",
    val timezoneMode: String = "IP",
    val geoMode: String = "IP",
    val languagesMode: String = "IP",
    val fontsList: String = "Arial,Courier,Georgia,Helvetica,Monaco,Palatino,Tahoma,Times,Times New Roman,Verdana",
    val webglMode: String = "MASKED",
    val audioMode: String = "MASKED",
    val mediaMasked: Boolean = true,
    val saveHistory: Boolean = true,
    val saveBookmarks: Boolean = true,
    val domStorage: Boolean = true,

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
    // v2.2: one-tap country proxy — timezone + language that match the
    // country, so a leak-check site sees ONE identity (IP country =
    // timezone = language). WebRTC is already "disable" by default, so
    // no real-IP leak through the proxy. (Moved out of the companion in
    // v2.6 so the type is addressable as Profile.CountrySettings —
    // companion-nested types are not.)
    data class CountrySettings(
        val code: String,
        val label: String,
        val timezone: String,
        val language: String
    )

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

        val oneTapCountries = listOf("US", "GB", "CA", "AU", "FR", "DE", "NL", "JP")

        // v2.5: country picker list (GoLogin-style Add proxy screen).
        val pickerCountries = listOf(
            "US", "GB", "CA", "AU", "FR", "DE", "NL", "JP",
            "IN", "BR", "IT", "ES", "SE", "CH", "IE", "NZ"
        )

        /** Flag emoji from a 2-letter country code (regional indicators). */
        fun flagEmoji(code: String): String {
            val c = code.uppercase()
            if (c.length != 2) return "🌐"
            val base = 0x1F1E6
            return String(Character.toChars(base + (c[0] - 'A'))) +
                String(Character.toChars(base + (c[1] - 'A')))
        }

        fun countrySettings(code: String): CountrySettings = when (code.uppercase()) {
            "US" -> CountrySettings("US", "United States", "America/New_York", "en-US")
            "GB" -> CountrySettings("GB", "United Kingdom", "Europe/London", "en-GB")
            "CA" -> CountrySettings("CA", "Canada", "America/Toronto", "en-CA")
            "AU" -> CountrySettings("AU", "Australia", "Australia/Sydney", "en-AU")
            "FR" -> CountrySettings("FR", "France", "Europe/Paris", "fr-FR")
            "DE" -> CountrySettings("DE", "Germany", "Europe/Berlin", "de-DE")
            "NL" -> CountrySettings("NL", "Netherlands", "Europe/Amsterdam", "nl-NL")
            "JP" -> CountrySettings("JP", "Japan", "Asia/Tokyo", "ja-JP")
            "IN" -> CountrySettings("IN", "India", "Asia/Kolkata", "en-IN")
            "BR" -> CountrySettings("BR", "Brazil", "America/Sao_Paulo", "pt-BR")
            "IT" -> CountrySettings("IT", "Italy", "Europe/Rome", "it-IT")
            "ES" -> CountrySettings("ES", "Spain", "Europe/Madrid", "es-ES")
            "SE" -> CountrySettings("SE", "Sweden", "Europe/Stockholm", "sv-SE")
            "CH" -> CountrySettings("CH", "Switzerland", "Europe/Zurich", "de-CH")
            "IE" -> CountrySettings("IE", "Ireland", "Europe/Dublin", "en-IE")
            "NZ" -> CountrySettings("NZ", "New Zealand", "Pacific/Auckland", "en-NZ")
            else -> CountrySettings(code.uppercase(), code.uppercase(), "UTC", "en-US")
        }

        // v2.6: Extended settings options.
        val OS_OPTIONS = listOf("Android", "Windows", "macOS", "Linux")

        /** Chrome 151 user-agent for an OS (GoLogin's Browser section). */
        fun osUserAgent(os: String): String = when (os) {
            "Windows" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
            "macOS" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
            "Linux" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36"
            else -> "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36"
        }

        /** navigator.platform for an OS. */
        fun osPlatform(os: String): String = when (os) {
            "Windows" -> "Win32"
            "macOS" -> "MacIntel"
            "Linux" -> "Linux x86_64"
            else -> "Linux armv8l"
        }

        /** (WebGL vendor, renderer) for an OS. */
        fun osWebgl(os: String): Pair<String, String> = when (os) {
            "Windows" -> "Google Inc. (NVIDIA)" to
                "ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 Direct3D11 vs_5_0 ps_5_0, D3D11)"
            "macOS" -> "Google Inc. (Apple)" to
                "ANGLE (Apple, ANGLE Metal Renderer: Apple M1, Unspecified Version)"
            "Linux" -> "Google Inc. (Intel)" to
                "ANGLE (Intel, Mesa Intel(R) UHD Graphics 620 (KBL GT2), OpenGL 4.6)"
            else -> "Google Inc. (ARM)" to
                "ANGLE (ARM, Mali-G57 MC2, OpenGL ES 3.2)"
        }

        val RESOLUTIONS = listOf(
            360 to 800, 390 to 844, 412 to 915, 375 to 812, 414 to 896,
            360 to 760, 402 to 874, 380 to 850, 428 to 926
        )
        val DEVICE_MEMORIES = listOf(1, 2, 4, 8)
        val THREAD_COUNTS = listOf(2, 4, 6, 8, 10, 12, 16, 20, 24, 32, 64)
        val FONT_OPTIONS = listOf(
            "Arial", "Courier", "Courier New", "Georgia", "Helvetica", "Monaco",
            "Palatino", "Tahoma", "Times", "Times New Roman", "Verdana", "Baskerville"
        )
        val TIMEZONE_OPTIONS = listOf(
            "America/New_York", "America/Chicago", "America/Denver",
            "America/Los_Angeles", "Europe/London", "Europe/Paris",
            "Europe/Berlin", "Asia/Tokyo", "Asia/Kolkata",
            "Australia/Sydney", "UTC"
        )

        /** GoLogin's "NEW FINGERPRINT": a fresh random hardware identity
         * for the same OS — resolution, memory, threads, WebGL, UA.
         * Name, folder, proxy, timezone and language stay untouched. */
        fun randomFingerprint(p: Profile): Profile {
            val r = kotlin.random.Random(System.currentTimeMillis())
            val res = RESOLUTIONS[r.nextInt(RESOLUTIONS.size)]
            val (vendor, renderer) = osWebgl(p.os)
            return p.copy(
                screenWidth = res.first,
                screenHeight = res.second,
                deviceMemory = DEVICE_MEMORIES[r.nextInt(DEVICE_MEMORIES.size)],
                hardwareConcurrency = THREAD_COUNTS[r.nextInt(THREAD_COUNTS.size)],
                webglVendor = vendor,
                webglRenderer = renderer,
                userAgent = if (p.userAgentPreset == "Custom") p.userAgent
                else osUserAgent(p.os)
            )
        }
    }
}
