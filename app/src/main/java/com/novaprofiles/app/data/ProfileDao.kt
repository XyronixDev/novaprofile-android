package com.novaprofiles.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrash(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Profile?

    @Query("SELECT DISTINCT folder FROM profiles WHERE deletedAt IS NULL ORDER BY folder ASC")
    fun getFolders(): Flow<List<String>>

    @Query("SELECT * FROM profiles WHERE deletedAt IS NULL AND (name LIKE '%' || :q || '%' OR folder LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<Profile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
