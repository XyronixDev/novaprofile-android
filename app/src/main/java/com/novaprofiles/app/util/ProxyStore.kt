package com.novaprofiles.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import com.novaprofiles.app.data.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.UUID

/**
 * Proxy Manager storage (v2.0 Pro): the user's own proxies, kept locally
 * in SharedPreferences as JSON. No Room change, no migration, no data loss.
 *
 * A proxy check opens https://api.ipify.org THROUGH the proxy (real IP the
 * sites would see), then looks up that IP's country via ip-api.com, and
 * times the round trip.
 */
data class ProxyEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mode: String, // HTTP or SOCKS5
    val host: String,
    val port: Int,
    val user: String = "",
    val pass: String = "",
    val country: String = "",
    val lastIp: String = "",
    val lastMs: Long = 0,
    val ok: Boolean = false,
    // v2.5: the IP's real timezone from ip-api.com (e.g. a Seattle proxy
    // reports America/Los_Angeles, not the country default New York).
    // Profiles take this over the country default so timezone == IP.
    val lastTimezone: String = "",
    // v2.6: the IP's coordinates (Mountain View for the Google Cloud
    // free proxies) — the profile's geolocation spoof target.
    val lastLat: Double = 0.0,
    val lastLon: Double = 0.0
)

class ProxyStore(context: Context) {
    private val prefs = context.getSharedPreferences("novaprofile_proxies", Context.MODE_PRIVATE)

    private val _proxies = MutableStateFlow(load())
    val proxies: StateFlow<List<ProxyEntry>> = _proxies

    private fun load(): List<ProxyEntry> {
        return try {
            val raw = prefs.getString("list", "[]") ?: "[]"
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ProxyEntry(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name"),
                    mode = o.optString("mode", "HTTP"),
                    host = o.optString("host"),
                    port = o.optInt("port", 8080),
                    user = o.optString("user"),
                    pass = o.optString("pass"),
                    country = o.optString("country"),
                    lastIp = o.optString("lastIp"),
                    lastMs = o.optLong("lastMs"),
                    ok = o.optBoolean("ok"),
                    lastTimezone = o.optString("lastTimezone"),
                    lastLat = o.optDouble("lastLat", 0.0),
                    lastLon = o.optDouble("lastLon", 0.0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<ProxyEntry>) {
        // v2.7.2 safety net: drop any host:port duplicates already on
        // the phone (keep the last entry for each), so old duplicates
        // disappear on the next write even if they predate add()'s fix.
        val deduped = list.reversed().distinctBy { it.host to it.port }.reversed()
        val arr = JSONArray()
        for (p in deduped) {
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("mode", p.mode)
                    put("host", p.host)
                    put("port", p.port)
                    put("user", p.user)
                    put("pass", p.pass)
                    put("country", p.country)
                    put("lastIp", p.lastIp)
                    put("lastMs", p.lastMs)
                    put("ok", p.ok)
                    put("lastTimezone", p.lastTimezone)
                    put("lastLat", p.lastLat)
                    put("lastLon", p.lastLon)
                }
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
        _proxies.value = deduped
    }

    // v2.7.2: same proxy picked twice must not stack up as two saved
    // entries (the phone showed 3x "Free IN 10"). Adding a host:port
    // that is already saved replaces it in place with the fresh test
    // result instead of appending a duplicate.
    fun add(entry: ProxyEntry) {
        val existing = _proxies.value.firstOrNull { it.host == entry.host && it.port == entry.port }
        if (existing != null) {
            persist(_proxies.value.map { if (it.id == existing.id) entry.copy(id = existing.id) else it })
        } else {
            persist(_proxies.value + entry)
        }
    }

    fun remove(id: String) {
        persist(_proxies.value.filter { it.id != id })
    }

    fun update(entry: ProxyEntry) {
        persist(_proxies.value.map { if (it.id == entry.id) entry else it })
    }

    /**
     * Free proxies (v2.1, rebuilt v2.3): fetches public HTTP proxies for a
     * country so the user can test a foreign IP without buying one.
     *
     * v2.3 lesson (measured 2026-11-07: 14/14 free proxies from the old
     * single source were dead — TCP open, CONNECT refused/hung): one
     * source is never enough. This merges FOUR sources in parallel —
     * Geonode (tested, latency-sorted, elite only), ProxyScrape (country
     * filtered), free-proxy-list.net (plain HTML table, elite/anonymous
     * only, country filtered) — dedupes, and returns up to [limit]
     * candidates for parallel testing. GitHub raw list stays as the
     * last resort when all three return nothing. Free proxies are
     * still best-effort: most die within hours, so the caller must test
     * them (see checkFirstWorking) and the user may need to retry.
     */
    suspend fun fetchFreeProxies(country: String = "US", limit: Int = 10): List<ProxyEntry> =
        withContext(Dispatchers.IO) {
            val cc = country.uppercase()
            val geonode = async {
                try {
                    val body = URL(
                        "https://proxylist.geonode.com/api/proxy-list" +
                            "?limit=50&page=1&sort_by=latency&sort_type=asc" +
                            "&country=$cc&anonymityLevel=elite" +
                            "&protocols=http%2Chttps"
                    ).readText()
                    val arr = JSONObject(body).optJSONArray("data") ?: return@async emptyList<String>()
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        val host = o.optString("ip")
                        val port = o.optInt("port", 0)
                        if (host.isBlank() || port <= 0) return@mapNotNull null
                        "$host:$port"
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val scrape = async {
                try {
                    URL(
                        "https://api.proxyscrape.com/v2/?request=getproxies" +
                            "&protocol=http&timeout=5000&country=$cc" +
                            "&ssl=all&anonymity=elite,anonymous"
                    ).readText().lines().filter { it.contains(":") }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            // v2.7: free-proxy-list.net — a plain server-rendered HTML
            // table (verified 2026-11-07: ~300 rows, IPs in plain text,
            // "Last Checked" seconds ago). No API, no speed column, HTTP
            // only — the parallel test is what proves speed. The row
            // pattern tolerates class attributes on every <td>.
            val fpl = async {
                try {
                    val conn = URL("https://free-proxy-list.net/")
                        .openConnection() as HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    conn.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36"
                    )
                    if (conn.responseCode != 200) {
                        conn.disconnect()
                        return@async emptyList()
                    }
                    val html = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val rowRe = Regex(
                        "<tr><td>(\\d+\\.\\d+\\.\\d+\\.\\d+)</td><td>(\\d+)</td>" +
                            "<td>([A-Z]{2})</td><td[^>]*>([^<]*)</td><td[^>]*>([^<]*)</td>"
                    )
                    rowRe.findAll(html).mapNotNull { m ->
                        val (ip, port, code, _, anon) = m.destructured
                        if (code != cc) return@mapNotNull null
                        val a = anon.lowercase()
                        if (!a.contains("elite") && !a.contains("anonymous")) {
                            return@mapNotNull null
                        }
                        "$ip:$port"
                    }.take(10).toList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val merged = (geonode.await() + scrape.await() + fpl.await())
                .map { it.trim() }.filter { it.contains(":") }.distinct()
            val source = if (merged.isNotEmpty()) merged else try {
                URL("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt")
                    .readText().lines().filter { it.contains(":") }
            } catch (e: Exception) {
                emptyList()
            }
            source.take(limit).mapIndexedNotNull { i, line ->
                val parts = line.split(":")
                if (parts.size < 2) return@mapIndexedNotNull null
                val port = parts[1].toIntOrNull() ?: return@mapIndexedNotNull null
                ProxyEntry(
                    name = "Free $cc ${i + 1}",
                    mode = "HTTP",
                    host = parts[0],
                    port = port
                )
            }
        }

    /**
     * v2.7: Webshare proxies via the official API. Free plan = 10 stable
     * proxies + 1GB/month, renewing monthly; paid ($2.99/mo) is optional
     * and never required — a free-plan key works exactly the same way.
     * The user creates an API key in the Webshare dashboard
     * (Integrations -> API Keys) and pastes it into Settings; the key is
     * stored only on this phone (DataStore) and is sent only to
     * Webshare's own API. Direct mode: connect to proxy_address + port
     * with the per-proxy username/password the API returns. Defensive
     * parsing: every field is opt* with fallbacks, so an API shape
     * change degrades to an empty list, never a crash — and a wrong key
     * (non-200) also returns empty, which the UI reports as "check the
     * key". Free Webshare proxies are shared datacenter IPs: stable,
     * but the whoer ~90% ceiling still applies (residential needed for
     * 100%) and heavy use eats the 1GB cap.
     */
    suspend fun fetchWebshareProxies(apiKey: String): List<ProxyEntry> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext emptyList()
            try {
                val conn = URL(
                    "https://proxy.webshare.io/api/v2/proxy/list/" +
                        "?mode=direct&page=1&page_size=100"
                ).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Authorization", "Token ${apiKey.trim()}")
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@withContext emptyList()
                }
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val arr = JSONObject(body).optJSONArray("results")
                    ?: return@withContext emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val host = o.optString("proxy_address").ifBlank { o.optString("ip") }
                    val port = o.optInt("port", 0)
                    if (host.isBlank() || port <= 0) return@mapNotNull null
                    if (!o.optBoolean("valid", true)) return@mapNotNull null
                    val cc = o.optString("country_code").uppercase()
                    val label = if (cc.isBlank()) "" else Profile.countrySettings(cc).label
                    ProxyEntry(
                        name = "Webshare • " + label.ifBlank { host },
                        // Dashboard quick-start uses HTTP; the same port
                        // also speaks SOCKS5, but HTTP is the safe default.
                        mode = "HTTP",
                        host = host,
                        port = port,
                        user = o.optString("username"),
                        pass = o.optString("password"),
                        country = if (cc.isBlank()) "" else "$cc • $label"
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Parallel fastest-working test (v2.3, rebuilt v2.4): tests all
     * [candidates] at once and returns the FASTEST one that actually
     * proxies. v2.3 stopped at the FIRST working proxy — which could be
     * a slow one (the phone test: a Google Cloud proxy that took ~4s
     * to load whoer). Now, once the first working proxy appears, faster
     * ones get up to 2.5s more to finish, and the lowest-latency winner
     * is picked. Worst case is still the 12s deadline.
     */
    suspend fun checkFirstWorking(candidates: List<ProxyEntry>): ProxyEntry? =
        withContext(Dispatchers.IO) {
            val deferreds = candidates.map { async { check(it) } }
            val deadline = System.currentTimeMillis() + 12_000
            var firstOkAt = 0L
            var working: ProxyEntry? = null
            while (working == null && System.currentTimeMillis() < deadline) {
                val done = deferreds.filter { it.isCompleted }
                    .mapNotNull { runCatching { it.await() }.getOrNull() }
                    .filter { it.ok }
                if (done.isNotEmpty()) {
                    if (firstOkAt == 0L) firstOkAt = System.currentTimeMillis()
                    if (System.currentTimeMillis() - firstOkAt >= 2_500 ||
                        deferreds.all { it.isCompleted }
                    ) {
                        working = done.minByOrNull { it.lastMs }
                        break
                    }
                }
                if (deferreds.all { it.isCompleted }) break
                delay(200)
            }
            deferreds.forEach { it.cancel() }
            working
        }

    /** Live check through the proxy. Returns the entry with fresh results. */
    suspend fun check(entry: ProxyEntry): ProxyEntry = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            val type = if (entry.mode == "SOCKS5") Proxy.Type.SOCKS else Proxy.Type.HTTP
            val proxy = Proxy(type, InetSocketAddress(entry.host, entry.port))
            val conn = URL("https://api.ipify.org").openConnection(proxy) as HttpURLConnection
            // v2.3: 6s — a proxy slower than this to answer ipify is
            // unusable for browsing anyway, and parallel batches must
            // resolve fast.
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            if (entry.user.isNotBlank()) {
                val creds = android.util.Base64.encodeToString(
                    "${entry.user}:${entry.pass}".toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
                conn.setRequestProperty("Proxy-Authorization", "Basic $creds")
            }
            val ip = conn.inputStream.bufferedReader().readText().trim()
            conn.disconnect()
            if (conn.responseCode != 200 || ip.isBlank() || !ip.contains(".")) {
                return@withContext entry.copy(ok = false, lastMs = System.currentTimeMillis() - started)
            }
            // Country + timezone + coordinates lookup for that IP (also
            // through the proxy). The timezone is the v2.5 anti-detect
            // fix: the IP's own zone (e.g. America/Los_Angeles for
            // Mountain View) must be the profile's zone, not the country
            // capital's. Coordinates feed the v2.6 geolocation spoof.
            var country = ""
            var tz = ""
            var lat = 0.0
            var lon = 0.0
            try {
                val c2 = URL("http://ip-api.com/json/$ip?fields=status,country,countryCode,timezone,lat,lon")
                    .openConnection(proxy) as HttpURLConnection
                c2.connectTimeout = 6_000
                c2.readTimeout = 6_000
                val body = c2.inputStream.bufferedReader().readText()
                c2.disconnect()
                val o = JSONObject(body)
                if (o.optString("status") == "success") {
                    country = "${o.optString("countryCode")} • ${o.optString("country")}"
                    tz = o.optString("timezone")
                    lat = o.optDouble("lat", 0.0)
                    lon = o.optDouble("lon", 0.0)
                }
            } catch (e: Exception) {
            }
            entry.copy(
                ok = true,
                country = country,
                lastIp = ip,
                lastMs = System.currentTimeMillis() - started,
                lastTimezone = tz,
                lastLat = lat,
                lastLon = lon
            )
        } catch (e: Exception) {
            entry.copy(ok = false, lastMs = System.currentTimeMillis() - started)
        }
    }

    /**
     * v2.6 smart 90% fix: ONE ip-api call through the proxy, returning the
     * proxy IP's current (ip, timezone, lat, lon). Run time calls this
     * before opening the browser, so an old profile — or a free proxy
     * whose IP rotated — never runs with a stale timezone: the free US
     * proxies sit in Mountain View (America/Los_Angeles), while the
     * country default is New York, a 3-hour mismatch whoer deducts for.
     * Returns null on any failure; the caller then launches with the
     * profile's stored values instead of blocking.
     */
    data class Location(val ip: String, val timezone: String, val lat: Double, val lon: Double)

    suspend fun resolveLocation(entry: ProxyEntry): Location? =
        withContext(Dispatchers.IO) {
            try {
                val proxy = Proxy(
                    if (entry.mode == "SOCKS5") Proxy.Type.SOCKS else Proxy.Type.HTTP,
                    InetSocketAddress(entry.host, entry.port)
                )
                val c = URL("http://ip-api.com/json/?fields=status,query,timezone,lat,lon")
                    .openConnection(proxy) as HttpURLConnection
                c.connectTimeout = 6_000
                c.readTimeout = 6_000
                val body = c.inputStream.bufferedReader().readText()
                c.disconnect()
                val o = JSONObject(body)
                if (o.optString("status") != "success") return@withContext null
                val tz = o.optString("timezone")
                if (tz.isBlank()) return@withContext null
                Location(
                    ip = o.optString("query"),
                    timezone = tz,
                    lat = o.optDouble("lat", 0.0),
                    lon = o.optDouble("lon", 0.0)
                )
            } catch (e: Exception) {
                null
            }
        }
}
