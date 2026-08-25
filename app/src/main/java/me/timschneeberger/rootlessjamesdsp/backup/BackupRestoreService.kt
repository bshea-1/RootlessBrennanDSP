package me.timschneeberger.rootlessjamesdsp.backup

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.acquireWakeLock
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.isServiceRunning
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications
import timber.log.Timber

class BackupRestoreService : Service() {

    companion object {
        private const val EXTRA_URI = "${BuildConfig.APPLICATION_ID}.BackupRestoreServices.EXTRA_URI"
        private const val EXTRA_DIRTY = "${BuildConfig.APPLICATION_ID}.BackupRestoreServices.DIRTY"

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(BackupRestoreService::class.java)

        fun start(context: Context, uri: Uri, dirtyRestore: Boolean) {
            if (!isRunning(context)) {
                val intent = Intent(context, BackupRestoreService::class.java).apply {
                    putExtra(EXTRA_URI, uri)
                    putExtra(EXTRA_DIRTY, dirtyRestore)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var ioScope: CoroutineScope
    private var backupManager: BackupManager? = null
    private lateinit var notifier: BackupNotifier

    override fun onCreate() {
        super.onCreate()

        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        notifier = BackupNotifier(this)
        wakeLock = acquireWakeLock(javaClass.name)

        ServiceCompat.startForeground(
            this,
            Notifications.ID_RESTORE_PROGRESS,
            notifier.showRestoreProgress().build(),
            if(SdkCheck.isQ) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
    }

    override fun stopService(name: Intent?): Boolean {
        destroyJob()
        return super.stopService(name)
    }

    override fun onDestroy() {
        destroyJob()
        super.onDestroy()
    }

    private fun destroyJob() {
        backupManager?.job?.cancel()
        ioScope.cancel()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.extras?.getParcelableAs<Uri>(EXTRA_URI) ?: return START_NOT_STICKY
        val dirtyRestore = intent.extras?.getBoolean(EXTRA_DIRTY) ?: return START_NOT_STICKY

        backupManager?.job?.cancel()

        backupManager = BackupManager(this)

        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.i(exception)

            notifier.showRestoreError(exception.message)
            stopSelf(startId)
        }

        ioScope.launch(handler) {
            notifier.showRestoreProgress()
            backupManager?.restoreBackup(uri, dirtyRestore)
            notifier.showRestoreComplete()
        }.apply {
            invokeOnCompletion { stopSelf(startId) }
            backupManager?.job = this
        }

        return START_NOT_STICKY
    }
}
