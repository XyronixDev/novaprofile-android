package com.novaprofiles.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
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
    val ok: Boolean = false
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
                    ok = o.optBoolean("ok")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<ProxyEntry>) {
        val arr = JSONArray()
        for (p in list) {
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
                }
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
        _proxies.value = list
    }

    fun add(entry: ProxyEntry) {
        persist(_proxies.value + entry)
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
     * source is never enough. This merges THREE sources in parallel —
     * Geonode (tested, latency-sorted, elite only), ProxyScrape (country
     * filtered), GitHub raw list as last resort — dedupes, and returns
     * up to [limit] candidates for parallel testing. Free proxies are
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
            val merged = (geonode.await() + scrape.await())
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
            // Country lookup for that IP (also through the proxy).
            var country = ""
            try {
                val c2 = URL("http://ip-api.com/json/$ip?fields=status,country,countryCode")
                    .openConnection(proxy) as HttpURLConnection
                c2.connectTimeout = 6_000
                c2.readTimeout = 6_000
                val body = c2.inputStream.bufferedReader().readText()
                c2.disconnect()
                val o = JSONObject(body)
                if (o.optString("status") == "success") {
                    country = "${o.optString("countryCode")} • ${o.optString("country")}"
                }
            } catch (e: Exception) {
            }
            entry.copy(
                ok = true,
                country = country,
                lastIp = ip,
                lastMs = System.currentTimeMillis() - started
            )
        } catch (e: Exception) {
            entry.copy(ok = false, lastMs = System.currentTimeMillis() - started)
        }
    }
}
