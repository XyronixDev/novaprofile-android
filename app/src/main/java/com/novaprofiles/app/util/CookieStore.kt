package com.novaprofiles.app.util

import android.webkit.CookieManager
import org.json.JSONObject

/**
 * Per-profile cookie isolation for v1.
 *
 * The system WebView shares one CookieManager per process, so NovaProfile
 * saves the running profile's cookies on stop and restores them on launch:
 * launch = clear all cookies, then set this profile's saved cookies;
 * stop = read back cookies for the visited hosts and persist them.
 *
 * Additionally BrowserActivity calls WebView.setDataDirectorySuffix()
 * ("p<profileId>") before the first WebView is created in the process,
 * which gives each profile its own WebView storage directory on first
 * launch. Both layers together are the v1 isolation model — see README.
 */
object CookieStore {

    /** Serialize saved cookies: JSONObject of host -> cookie header string. */
    fun serialize(cookies: Map<String, String>): String {
        val obj = JSONObject()
        for ((k, v) in cookies) obj.put(k, v)
        return obj.toString()
    }

    fun deserialize(data: String): Map<String, String> {
        if (data.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(data)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optString(k, "")
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Read the cookie header for each host from the CookieManager. */
    fun readAll(hosts: Set<String>): Map<String, String> {
        val cm = CookieManager.getInstance()
        val out = mutableMapOf<String, String>()
        for (host in hosts) {
            val cookie = cm.getCookie("https://$host") ?: cm.getCookie("http://$host")
            if (!cookie.isNullOrBlank()) out[host] = cookie
        }
        return out
    }

    /** Clear everything, then restore this profile's cookies. */
    fun restore(saved: Map<String, String>, onDone: () -> Unit) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.removeAllCookies {
            for ((host, cookie) in saved) {
                cm.setCookie("https://$host", cookie)
                cm.setCookie("http://$host", cookie)
            }
            cm.flush()
            onDone()
        }
    }

    fun clearAll(onDone: () -> Unit = {}) {
        CookieManager.getInstance().removeAllCookies { onDone() }
    }
}
