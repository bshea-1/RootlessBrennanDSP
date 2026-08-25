package me.timschneeberger.rootlessjamesdsp.model.room

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class AppBlocklistRepository(private val appBlocklistDao: AppBlocklistDao) {
    val blocklist: Flow<List<BlockedApp>> = appBlocklistDao.getAll()

    @WorkerThread
    suspend fun insert(app: BlockedApp) {
        appBlocklistDao.insertAll(app)
    }

    @WorkerThread
    suspend fun delete(app: BlockedApp) {
        appBlocklistDao.delete(app)
    }
}
