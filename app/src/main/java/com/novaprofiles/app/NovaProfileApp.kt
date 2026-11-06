package com.novaprofiles.app

import android.app.Application
import com.novaprofiles.app.data.AppDatabase
import com.novaprofiles.app.data.ProfileRepository

/**
 * NovaProfile — original multi-profile browser.
 *
 * PRO rule (locked): everything is free, unlimited profiles, no billing /
 * subscription code anywhere in the app. Local-only by default — no account
 * is required to use profiles.
 *
 * Honest scope: fingerprint spoofing here is JS-level, applied per profile
 * inside the system WebView. It is NOT a Chromium fork and is detectable by
 * advanced fingerprint tests. See README.md.
 */
class NovaProfileApp : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: ProfileRepository by lazy {
        ProfileRepository(database.profileDao())
    }
}
