package com.novaprofiles.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Profile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2.6: Extended-settings columns. A real migration (not the
        // destructive fallback) so existing profiles survive the update.
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN os TEXT NOT NULL DEFAULT 'Android'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN timezoneMode TEXT NOT NULL DEFAULT 'IP'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN geoMode TEXT NOT NULL DEFAULT 'IP'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN languagesMode TEXT NOT NULL DEFAULT 'IP'")
                db.execSQL(
                    "ALTER TABLE profiles ADD COLUMN fontsList TEXT NOT NULL DEFAULT " +
                        "'Arial,Courier,Georgia,Helvetica,Monaco,Palatino,Tahoma,Times,Times New Roman,Verdana'"
                )
                db.execSQL("ALTER TABLE profiles ADD COLUMN webglMode TEXT NOT NULL DEFAULT 'MASKED'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN audioMode TEXT NOT NULL DEFAULT 'MASKED'")
                db.execSQL("ALTER TABLE profiles ADD COLUMN mediaMasked INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE profiles ADD COLUMN saveHistory INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE profiles ADD COLUMN saveBookmarks INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE profiles ADD COLUMN domStorage INTEGER NOT NULL DEFAULT 1")
            }
        }

        // v2.7.2: proxyCountry column (see Profile.kt) — real migration
        // so existing profiles survive the update.
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN proxyCountry TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "novaprofile.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
