package me.timschneeberger.rootlessjamesdsp.model.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

@Database(entities = [BlockedApp::class], version = 1)
abstract class AppBlocklistDatabase : RoomDatabase() {
    abstract fun appBlocklistDao(): AppBlocklistDao

    private class AppBlocklistDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback()

    companion object {

        @Volatile
        private var INSTANCE: AppBlocklistDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppBlocklistDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppBlocklistDatabase::class.java,
                        "blocked_apps.db"
                    )
                    .addCallback(AppBlocklistDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
