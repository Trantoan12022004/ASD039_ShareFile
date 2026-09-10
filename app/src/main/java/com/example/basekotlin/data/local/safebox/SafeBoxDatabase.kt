package com.example.basekotlin.data.local.safebox

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.basekotlin.data.local.safebox.converter.SafeBoxTypeConverters
import com.example.basekotlin.data.local.safebox.dao.SafeBoxDao
import com.example.basekotlin.data.local.safebox.entity.SafeBoxFile

@Database(
    entities = [SafeBoxFile::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SafeBoxTypeConverters::class)
abstract class SafeBoxDatabase : RoomDatabase() {

    abstract fun safeBoxDao(): SafeBoxDao

    companion object {
        @Volatile
        private var instance: SafeBoxDatabase? = null

        fun getInstance(context: Context): SafeBoxDatabase {
            val existing = instance
            if (existing != null) {
                return existing
            }
            return synchronized(this) {
                val current = instance
                if (current != null) {
                    current
                } else {
                    val created = Room.databaseBuilder(
                        context.applicationContext,
                        SafeBoxDatabase::class.java,
                        "safebox_database"
                    ).build()
                    instance = created
                    created
                }
            }
        }
    }
}
