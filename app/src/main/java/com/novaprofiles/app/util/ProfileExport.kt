package com.novaprofiles.app.util

import com.novaprofiles.app.data.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local export / import of profiles as a single .novaprofile zip file.
 * The zip contains one profiles.json (a JSON array of profile objects,
 * cookies included) — no server, no account, files stay on the device.
 * Imported profiles get fresh ids and empty last-run state.
 */
object ProfileExport {

    private fun profileToJson(p: Profile): JSONObject = JSONObject().apply {
        put("name", p.name)
        put("folder", p.folder)
        put("homeUrl", p.homeUrl)
        put("userAgentPreset", p.userAgentPreset)
        put("userAgent", p.userAgent)
        put("language", p.language)
        put("timezone", p.timezone)
        if (p.latitude != null) put("latitude", p.latitude)
        if (p.longitude != null) put("longitude", p.longitude)
        put("geoAccuracy", p.geoAccuracy)
        put("screenWidth", p.screenWidth)
        put("screenHeight", p.screenHeight)
        put("deviceMemory", p.deviceMemory)
        put("hardwareConcurrency", p.hardwareConcurrency)
        put("proxyMode", p.proxyMode)
        put("proxyHost", p.proxyHost)
        put("proxyPort", p.proxyPort)
        put("proxyUser", p.proxyUser)
        put("proxyPass", p.proxyPass)
        put("proxyCountry", p.proxyCountry)
        put("canvasMode", p.canvasMode)
        put("webglVendor", p.webglVendor)
        put("webglRenderer", p.webglRenderer)
        put("webrtcMode", p.webrtcMode)
        put("fontsMasking", p.fontsMasking)
        put("doNotTrack", p.doNotTrack)
        // v2.6: Extended settings.
        put("os", p.os)
        put("timezoneMode", p.timezoneMode)
        put("geoMode", p.geoMode)
        put("languagesMode", p.languagesMode)
        put("fontsList", p.fontsList)
        put("webglMode", p.webglMode)
        put("audioMode", p.audioMode)
        put("mediaMasked", p.mediaMasked)
        put("saveHistory", p.saveHistory)
        put("saveBookmarks", p.saveBookmarks)
        put("domStorage", p.domStorage)
        put("notes", p.notes)
        put("cookiesData", p.cookiesData)
    }

    private fun profileFromJson(o: JSONObject): Profile = Profile(
        name = o.optString("name", "Imported profile"),
        folder = o.optString("folder", "General"),
        homeUrl = o.optString("homeUrl", "https://www.google.com"),
        userAgentPreset = o.optString("userAgentPreset", "Chrome Android"),
        userAgent = o.optString("userAgent", ""),
        language = o.optString("language", "en-US"),
        timezone = o.optString("timezone", "Auto"),
        latitude = if (o.has("latitude")) o.optDouble("latitude") else null,
        longitude = if (o.has("longitude")) o.optDouble("longitude") else null,
        geoAccuracy = o.optInt("geoAccuracy", 100),
        screenWidth = o.optInt("screenWidth", 1080),
        screenHeight = o.optInt("screenHeight", 2400),
        deviceMemory = o.optInt("deviceMemory", 8),
        hardwareConcurrency = o.optInt("hardwareConcurrency", 8),
        proxyMode = o.optString("proxyMode", "NONE"),
        proxyHost = o.optString("proxyHost", ""),
        proxyPort = o.optInt("proxyPort", 8080),
        proxyUser = o.optString("proxyUser", ""),
        proxyPass = o.optString("proxyPass", ""),
        proxyCountry = o.optString("proxyCountry", ""),
        canvasMode = o.optString("canvasMode", "noise"),
        webglVendor = o.optString("webglVendor", ""),
        webglRenderer = o.optString("webglRenderer", ""),
        webrtcMode = o.optString("webrtcMode", "disable"),
        fontsMasking = o.optBoolean("fontsMasking", true),
        doNotTrack = o.optBoolean("doNotTrack", true),
        // v2.6: Extended settings.
        os = o.optString("os", "Android"),
        timezoneMode = o.optString("timezoneMode", "IP"),
        geoMode = o.optString("geoMode", "IP"),
        languagesMode = o.optString("languagesMode", "IP"),
        fontsList = o.optString("fontsList", "Arial,Courier,Georgia,Helvetica,Monaco,Palatino,Tahoma,Times,Times New Roman,Verdana"),
        webglMode = o.optString("webglMode", "MASKED"),
        audioMode = o.optString("audioMode", "MASKED"),
        mediaMasked = o.optBoolean("mediaMasked", true),
        saveHistory = o.optBoolean("saveHistory", true),
        saveBookmarks = o.optBoolean("saveBookmarks", true),
        domStorage = o.optBoolean("domStorage", true),
        notes = o.optString("notes", ""),
        cookiesData = o.optString("cookiesData", "")
    )

    fun export(profiles: List<Profile>, out: OutputStream) {
        val arr = JSONArray()
        for (p in profiles) arr.put(profileToJson(p))
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry("profiles.json"))
            zip.write(arr.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun import(input: InputStream): List<Profile> {
        val out = mutableListOf<Profile>()
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "profiles.json") {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    val arr = JSONArray(text)
                    for (i in 0 until arr.length()) {
                        out.add(profileFromJson(arr.getJSONObject(i)))
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out
    }
}
