package com.novaprofiles.app.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "novaprofile_settings")

/**
 * App-level settings (DataStore). Nothing here is a purchase, a lock or a
 * paywall — the app has no billing code at all (PRO rule).
 */
class SettingsStore(private val context: Context) {
    companion object {
        private val KEY_SHOW_TRASH = booleanPreferencesKey("show_trash")
        private val KEY_PROXY_CHECK_URL = stringPreferencesKey("proxy_check_url")
        private val KEY_DEFAULT_FOLDER = stringPreferencesKey("default_folder")
        // v2.7: Webshare API key. Stored only on this phone (DataStore),
        // sent only to Webshare's own API. The free-plan key and a paid
        // key work the same way — paid is optional, never required.
        private val KEY_WEBSHARE_API_KEY = stringPreferencesKey("webshare_api_key")
    }

    val showTrash: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_TRASH] ?: false }
    val proxyCheckUrl: Flow<String> =
        context.dataStore.data.map { it[KEY_PROXY_CHECK_URL] ?: "https://api.ipify.org" }
    val defaultFolder: Flow<String> =
        context.dataStore.data.map { it[KEY_DEFAULT_FOLDER] ?: "General" }
    val webshareApiKey: Flow<String> =
        context.dataStore.data.map { it[KEY_WEBSHARE_API_KEY] ?: "" }

    suspend fun setShowTrash(v: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_TRASH] = v }
    }

    suspend fun setProxyCheckUrl(v: String) {
        context.dataStore.edit { it[KEY_PROXY_CHECK_URL] = v }
    }

    suspend fun setDefaultFolder(v: String) {
        context.dataStore.edit { it[KEY_DEFAULT_FOLDER] = v }
    }

    suspend fun setWebshareApiKey(v: String) {
        context.dataStore.edit { it[KEY_WEBSHARE_API_KEY] = v }
    }
}
