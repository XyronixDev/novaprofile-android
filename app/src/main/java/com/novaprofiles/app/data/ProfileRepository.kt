package com.novaprofiles.app.data

import kotlinx.coroutines.flow.Flow

/**
 * Thin repository over [ProfileDao]. All profile operations are local
 * (Room) — there is no cloud sync, no account and no server in v1.
 */
class ProfileRepository(private val dao: ProfileDao) {
    val allProfiles: Flow<List<Profile>> = dao.getAll()
    val trash: Flow<List<Profile>> = dao.getTrash()
    val folders: Flow<List<String>> = dao.getFolders()

    fun search(q: String): Flow<List<Profile>> = dao.search(q)

    suspend fun getById(id: Long): Profile? = dao.getById(id)

    suspend fun create(profile: Profile): Long {
        val now = System.currentTimeMillis()
        return dao.insert(profile.copy(createdAt = now, updatedAt = now))
    }

    suspend fun update(profile: Profile) {
        dao.update(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveCookies(id: Long, cookiesData: String) {
        val p = dao.getById(id) ?: return
        dao.update(p.copy(cookiesData = cookiesData, updatedAt = System.currentTimeMillis()))
    }

    suspend fun markRun(id: Long) {
        val p = dao.getById(id) ?: return
        dao.update(p.copy(lastRunAt = System.currentTimeMillis()))
    }

    suspend fun duplicate(profile: Profile): Long {
        val copy = profile.copy(
            id = 0,
            name = "${profile.name} copy",
            cookiesData = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastRunAt = 0,
            deletedAt = null
        )
        return dao.insert(copy)
    }

    suspend fun moveToTrash(profile: Profile) {
        dao.update(profile.copy(deletedAt = System.currentTimeMillis()))
    }

    suspend fun restore(profile: Profile) {
        dao.update(profile.copy(deletedAt = null))
    }

    suspend fun deleteForever(profile: Profile) {
        dao.delete(profile)
    }
}
