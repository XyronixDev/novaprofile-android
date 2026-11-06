package com.novaprofiles.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
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
     * Free proxies (v2.1): fetches a few public US HTTP proxies so the user
     * can test a foreign IP without buying one. Free proxies are
     * best-effort — most are dead or slow, which is why every entry must
     * be Tested before use. Primary: ProxyScrape free API (US only);
     * fallback: a public GitHub proxy list (any country).
     */
    suspend fun fetchFreeProxies(country: String = "US"): List<ProxyEntry> =
        withContext(Dispatchers.IO) {
            val lines = try {
                URL(
                    "https://api.proxyscrape.com/v2/?request=getproxies" +
                        "&protocol=http&timeout=5000&country=$country" +
                        "&ssl=all&anonymity=elite,anonymous"
                ).readText().lines()
            } catch (e: Exception) {
                emptyList()
            }.filter { it.contains(":") }
            val source = if (lines.isNotEmpty()) lines else try {
                URL("https://raw.githubusercontent.com/TheSpeedX/PROXY-List/master/http.txt")
                    .readText().lines().filter { it.contains(":") }
            } catch (e: Exception) {
                emptyList()
            }
            source.take(5).mapIndexedNotNull { i, line ->
                val parts = line.trim().split(":")
                if (parts.size < 2) return@mapIndexedNotNull null
                val port = parts[1].toIntOrNull() ?: return@mapIndexedNotNull null
                ProxyEntry(
                    name = "Free ${country.uppercase()} ${i + 1}",
                    mode = "HTTP",
                    host = parts[0],
                    port = port
                )
            }
        }

    /** Live check through the proxy. Returns the entry with fresh results. */
    suspend fun check(entry: ProxyEntry): ProxyEntry = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            val type = if (entry.mode == "SOCKS5") Proxy.Type.SOCKS else Proxy.Type.HTTP
            val proxy = Proxy(type, InetSocketAddress(entry.host, entry.port))
            val conn = URL("https://api.ipify.org").openConnection(proxy) as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
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
                c2.connectTimeout = 10_000
                c2.readTimeout = 10_000
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
