package me.timschneeberger.rootlessjamesdsp.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.ProcessorMessageHandler
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession
import me.timschneeberger.rootlessjamesdsp.model.preference.AudioEncoding
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp
import me.timschneeberger.rootlessjamesdsp.model.rootless.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.session.rootless.OnRootlessSessionChangeListener
import me.timschneeberger.rootlessjamesdsp.session.rootless.RootlessSessionDatabase
import me.timschneeberger.rootlessjamesdsp.session.rootless.RootlessSessionManager
import me.timschneeberger.rootlessjamesdsp.session.rootless.SessionRecordingPolicyManager
import me.timschneeberger.rootlessjamesdsp.session.shared.BaseSessionDatabase
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PREFERENCES_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SAMPLE_RATE_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_HARD_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_RELOAD_LIVEPROG
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_SOFT_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasRecordPermission
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications
import me.timschneeberger.rootlessjamesdsp.utils.notifications.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.hiddenapi_impl.CapturePolicy
import me.timschneeberger.hiddenapi_impl.ShizukuSystemServerApi
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RequiresApi(Build.VERSION_CODES.Q)
class RootlessAudioProcessorService : BaseAudioProcessorService() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionStartIntent: Intent? = null

    private var recreateRecorderRequested = false
    private var recorderThread: Thread? = null
    private lateinit var engine: JamesDspLocalEngine
    private val isRunning: Boolean
        get() = recorderThread != null

    private lateinit var sessionManager: RootlessSessionManager
    private var sessionLossRetryCount = 0
    @Volatile
    private var notificationSessions: Array<IEffectSession> = emptyArray()

    private var isProcessorIdle = false
    private var suspendOnIdle = false

    private var excludeRestrictedSessions = false

    private var isProcessorDisposing = false
    private var isServiceDisposing = false

    private val preferences: Preferences.App by inject()
    private val preferencesVar: Preferences.Var by inject()

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val blockedAppDatabase by lazy { AppBlocklistDatabase.getDatabase(this, applicationScope) }
    private val blockedAppRepository by lazy { AppBlocklistRepository(blockedAppDatabase.appBlocklistDao()) }
    private val blockedApps by lazy { blockedAppRepository.blocklist.asLiveData() }
    private val blockedAppObserver = Observer<List<BlockedApp>?> {
        Timber.d("blockedAppObserver: Database changed; ignored=${!isRunning}")
        if(isRunning)
            recreateRecorderRequested = true
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService<AudioManager>()!!
        mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
        notificationManager = getSystemService<NotificationManager>()!!

        val powerManager = getSystemService<PowerManager>()
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RootlessBrennanDSP:AudioProcessor")?.apply {
            setReferenceCounted(false)
        }

        sessionManager = RootlessSessionManager(this)
        sessionManager.sessionDatabase.setOnSessionLossListener(onSessionLossListener)
        sessionManager.sessionDatabase.setOnAppProblemListener(onAppProblemListener)
        sessionManager.sessionDatabase.registerOnSessionChangeListener(onSessionChangeListener)
        sessionManager.sessionPolicyDatabase.registerOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)

        engine = JamesDspLocalEngine(this, ProcessorMessageHandler())
        engine.syncWithPreferences()

        val filter = IntentFilter()
        filter.addAction(ACTION_PREFERENCES_UPDATED)
        filter.addAction(ACTION_SAMPLE_RATE_UPDATED)
        filter.addAction(ACTION_SERVICE_RELOAD_LIVEPROG)
        filter.addAction(ACTION_SERVICE_HARD_REBOOT_CORE)
        filter.addAction(ACTION_SERVICE_SOFT_REBOOT_CORE)
        registerLocalReceiver(broadcastReceiver, filter)

        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
        loadFromPreferences(getString(R.string.key_powersave_suspend))
        loadFromPreferences(getString(R.string.key_session_exclude_restricted))

        blockedApps.observeForever(blockedAppObserver)

        notificationManager.cancel(Notifications.ID_SERVICE_STARTUP)

        recreateRecorderRequested = false

        startForeground(
            Notifications.ID_SERVICE_STATUS,
            ServiceNotificationHelper.createServiceNotification(this, arrayOf()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Timber.d("onStartCommand")

        when (intent.action) {
            null -> {
                Timber.wtf("onStartCommand: intent.action is null")
            }
            ACTION_START -> {
                Timber.d("Starting service")
            }
            ACTION_STOP -> {
                Timber.d("Stopping service")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isRunning) {
            return START_NOT_STICKY
        }

        notificationManager.cancel(Notifications.ID_SERVICE_SESSION_LOSS)
        notificationManager.cancel(Notifications.ID_SERVICE_APPCOMPAT)

        mediaProjectionStartIntent = intent.extras?.getParcelableAs(EXTRA_MEDIA_PROJECTION_DATA)

        mediaProjection = try {
            mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                mediaProjectionStartIntent!!
            )
        }
        catch (ex: Exception) {
            Timber.e("Failed to acquire media projection")
            sendLocalBroadcast(Intent(Constants.ACTION_DISCARD_AUTHORIZATION))
            Timber.e(ex)
            null
        }

        mediaProjection?.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        if (mediaProjection != null) {
            startRecording()
            sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STARTED))
        } else {
            Timber.w("Failed to capture audio")
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        isServiceDisposing = true

        stopRecording()
        engine.close()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)

        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        blockedApps.removeObserver(blockedAppObserver)

        unregisterLocalReceiver(broadcastReceiver)
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection = null

        sessionManager.sessionPolicyDatabase.unregisterOnRestrictedSessionChangeListener(onSessionPolicyChangeListener)
        sessionManager.sessionDatabase.unregisterOnSessionChangeListener(onSessionChangeListener)
        sessionManager.destroy()

        preferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)
        notificationManager.cancel(Notifications.ID_SERVICE_STATUS)

        stopSelf()
        super.onDestroy()
    }

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
            _, key ->
        loadFromPreferences(key)
    }

    private val projectionCallback = object: MediaProjection.Callback() {
        override fun onStop() {
            if(isServiceDisposing) {
                return
            }

            if(restartWithFreshProjection()) {
                return
            }

            Timber.w("Capture permission revoked. Stopping service.")

            sendLocalBroadcast(Intent(Constants.ACTION_DISCARD_AUTHORIZATION))

            this@RootlessAudioProcessorService.toast(getString(R.string.capture_permission_revoked_toast))

            notificationManager.cancel(Notifications.ID_SERVICE_STATUS)
            stopSelf()
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SAMPLE_RATE_UPDATED -> engine.syncWithPreferences(arrayOf(Constants.PREF_CONVOLVER))
                ACTION_PREFERENCES_UPDATED -> engine.syncWithPreferences()
                ACTION_SERVICE_RELOAD_LIVEPROG -> engine.syncWithPreferences(arrayOf(Constants.PREF_LIVEPROG))
                ACTION_SERVICE_HARD_REBOOT_CORE -> restartRecording()
                ACTION_SERVICE_SOFT_REBOOT_CORE -> requestAudioRecordRecreation()
            }
        }
    }

    private val onSessionLossListener = object: RootlessSessionDatabase.OnSessionLossListener {
        override fun onSessionLost(sid: Int) {
            if(!preferences.get<Boolean>(R.string.key_session_loss_ignore)) {
                if(sessionLossRetryCount < SESSION_LOSS_MAX_RETRIES) {
                    sessionLossRetryCount++
                    Timber.d("Session lost. Retry count: $sessionLossRetryCount/$SESSION_LOSS_MAX_RETRIES")
                    sessionManager.pollOnce(false)
                    restartRecording()
                    return
                }
                else {
                    Timber.e("Session loss limit reached. Notifying user...")
                    ServiceNotificationHelper.pushSessionLossNotification(this@RootlessAudioProcessorService, mediaProjectionStartIntent)
                }
            }
            else {
                Timber.d("Session lost. Ignoring...")
            }
        }
    }

    private val onSessionChangeListener = object : BaseSessionDatabase.OnSessionChangeListener {
        override fun onSessionChanged(sessionList: HashMap<Int, IEffectSession>) {
            isProcessorIdle = sessionList.size == 0
            Timber.d("onSessionChanged: isProcessorIdle=$isProcessorIdle")

            if (try { rikka.shizuku.Shizuku.pingBinder() } catch (_: Throwable) { false }) {
                sessionList.values.forEach { session ->
                    try {
                        ShizukuSystemServerApi.AudioPolicyService_setAllowedCapturePolicy(
                            session.uid,
                            CapturePolicy.ALLOW_CAPTURE_BY_ALL
                        )
                    } catch (e: Throwable) {
                        Timber.d("Could not set audio policy for uid %d: %s", session.uid, e.message)
                    }
                }
            }

            notificationSessions = sessionList.values.toTypedArray()

            ServiceNotificationHelper.pushServiceNotification(
                this@RootlessAudioProcessorService,
                notificationSessions
            )
        }
    }

    private val onAppProblemListener = object : RootlessSessionDatabase.OnAppProblemListener {
        override fun onAppProblemDetected(uid: Int) {
            if(!preferences.get<Boolean>(R.string.key_session_app_problem_ignore)) {
                notificationManager.cancel(Notifications.ID_SERVICE_STATUS)
                if(preferencesVar.get<Boolean>(R.string.key_is_activity_active) ||
                    preferencesVar.get<Boolean>(R.string.key_is_app_compat_activity_active)) {
                    startActivity(
                        ServiceNotificationHelper.createAppTroubleshootIntent(
                            this@RootlessAudioProcessorService,
                            mediaProjectionStartIntent,
                            uid,
                            directLaunch = true
                        )
                    )
                    notificationManager.cancel(Notifications.ID_SERVICE_APPCOMPAT)
                }
                else
                    ServiceNotificationHelper.pushAppIssueNotification(this@RootlessAudioProcessorService, mediaProjectionStartIntent, uid)

                this@RootlessAudioProcessorService.toast(getString(R.string.session_app_compat_toast), false)
                Timber.w("Terminating service due to app incompatibility; redirect user to troubleshooting options")
                stopSelf()
            }
        }
    }

    private val onSessionPolicyChangeListener = object : SessionRecordingPolicyManager.OnSessionRecordingPolicyChangeListener {
        override fun onSessionRecordingPolicyChanged(sessionList: HashMap<String, SessionRecordingPolicyEntry>, isMinorUpdate: Boolean) {
            Timber.d("onSessionRecordingPolicyChanged: keeping active capture stream continuous")
        }
    }

    private fun restartWithFreshProjection(): Boolean {
        return try {
            Timber.d("Re-acquiring MediaProjection")
            var newProjection: MediaProjection? = null

            val startIntent = mediaProjectionStartIntent
            if (startIntent != null) {
                try {
                    newProjection = mediaProjectionManager.getMediaProjection(android.app.Activity.RESULT_OK, startIntent)
                } catch (e: Exception) {
                    Timber.d("getMediaProjection with startIntent failed: ${e.message}")
                }
            }

            if (newProjection == null) {
                val myUid = android.os.Process.myUid()
                for (method in MediaProjectionManager::class.java.declaredMethods.filter { it.name == "createProjection" }) {
                    try {
                        method.isAccessible = true
                        val types = method.parameterTypes
                        val args = Array<Any?>(types.size) { null }
                        var intCount = 0
                        for (i in types.indices) {
                            when (types[i]) {
                                Int::class.javaPrimitiveType, Int::class.javaObjectType -> {
                                    args[i] = when (intCount) {
                                        0 -> myUid
                                        1 -> 1
                                        else -> 0
                                    }
                                    intCount++
                                }
                                String::class.java -> args[i] = packageName
                                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> args[i] = false
                                else -> args[i] = null
                            }
                        }
                        val result = method.invoke(mediaProjectionManager, *args)
                        if (result is MediaProjection) {
                            newProjection = result
                            break
                        }
                    } catch (_: Exception) {}
                }
            }

            if (newProjection == null) {
                val displayManager = getSystemService<android.hardware.display.DisplayManager>()
                val displayId = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.displayId ?: 0
                for (method in MediaProjectionManager::class.java.declaredMethods.filter { it.name == "createProjection" }) {
                    try {
                        method.isAccessible = true
                        val types = method.parameterTypes
                        val args = Array<Any?>(types.size) { null }
                        var intCount = 0
                        for (i in types.indices) {
                            when (types[i]) {
                                Int::class.javaPrimitiveType, Int::class.javaObjectType -> {
                                    args[i] = when (intCount) {
                                        0 -> displayId
                                        1 -> 1
                                        else -> 0
                                    }
                                    intCount++
                                }
                                String::class.java -> args[i] = packageName
                                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> args[i] = false
                                else -> args[i] = null
                            }
                        }
                        val result = method.invoke(mediaProjectionManager, *args)
                        if (result is MediaProjection) {
                            newProjection = result
                            break
                        }
                    } catch (_: Exception) {}
                }
            }

            if (newProjection == null) {
                return false
            }

            try { mediaProjection?.unregisterCallback(projectionCallback) } catch (_: Exception) {}
            mediaProjection = newProjection
            newProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            requestAudioRecordRecreation()
            Timber.i("MediaProjection re-acquired silently; capture resumed")
            true
        }
        catch (e: Exception) {
            Timber.e(e, "Silent projection re-acquisition failed")
            false
        }
    }

    private fun loadFromPreferences(key: String?){
        when (key) {
            getString(R.string.key_powersave_suspend) -> {
                suspendOnIdle = preferences.get<Boolean>(R.string.key_powersave_suspend)
                Timber.d("Suspend on idle set to $suspendOnIdle")
            }
            getString(R.string.key_session_exclude_restricted) -> {
                excludeRestrictedSessions = preferences.get<Boolean>(R.string.key_session_exclude_restricted)
                Timber.d("Exclude restricted set to $excludeRestrictedSessions")
            }
        }
    }

    fun requestAudioRecordRecreation() {
        if(isProcessorDisposing || isServiceDisposing) {
            Timber.e("recreateAudioRecorder: service or processor already disposing")
            return
        }

        recreateRecorderRequested = true
    }

    @SuppressLint("BinaryOperationInTimber")
    private fun startRecording() {

        if (!hasRecordPermission()) {
            Timber.e("Record audio permission missing. Can't record")
            stopSelf()
            return
        }

        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Timber.d("Acquired PARTIAL_WAKE_LOCK for audio processing")
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not acquire wake lock")
        }

        val encoding = AudioEncoding.fromInt(
            preferences.get<String>(R.string.key_audioformat_encoding).toIntOrNull() ?: 1
        )
        val encodingFormat = when (encoding) {
            AudioEncoding.PcmShort -> AudioFormat.ENCODING_PCM_16BIT
            else -> AudioFormat.ENCODING_PCM_FLOAT
        }

        val sampleRate = 48000
        val frameSizeBytes = if (encoding == AudioEncoding.PcmShort) 2 * Short.SIZE_BYTES else 2 * Float.SIZE_BYTES

        // Chunk size: align with HAL burst for optimal scheduling (~4-5ms on Pixel)
        val halBurstFrames = determineBufferSize()  // typically 192 or 240 on Pixel
        val framesPerChunk = halBurstFrames.coerceIn(128, 512)
        val bufferSizeBytes = framesPerChunk * frameSizeBytes
        val bufferElements = framesPerChunk * 2

        val minTrackBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, encodingFormat)
        val minRecBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, encodingFormat)

        // Hardware buffer: 3× minimum — enough headroom without adding latency.
        // The partial-write loop prevents data loss, so we don't need massive buffers.
        val recHwBuf = maxOf(minRecBuf * 3, bufferSizeBytes * 4)
        val trackHwBuf = maxOf(minTrackBuf * 3, bufferSizeBytes * 4)

        Timber.i("Sample rate: $sampleRate; Encoding: ${encoding.name}; " +
                "Chunk frames: $framesPerChunk; Chunk bytes: $bufferSizeBytes; " +
                "Track HW Buf: $trackHwBuf; Rec HW Buf: $recHwBuf; " +
                "HAL burst frames: $halBurstFrames")

        var recorder: AudioRecord
        val track: AudioTrack
        try {
            recorder = buildAudioRecord(encodingFormat, sampleRate, recHwBuf)
            track = buildAudioTrack(encodingFormat, sampleRate, trackHwBuf)
        }
        catch(ex: Exception) {
            Timber.e("Failed to create initial audio record/track")
            Timber.e(ex)
            stopSelf()
            return
        }

        if(engine.sampleRate.toInt() != sampleRate) {
            Timber.d("Sampling rate changed to ${sampleRate}Hz")
            engine.sampleRate = sampleRate.toFloat()
        }

        recorderThread = Thread {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

                ServiceNotificationHelper.pushServiceNotification(
                    applicationContext,
                    notificationSessions,
                )

                val directInBuf = ByteBuffer.allocateDirect(bufferSizeBytes).order(ByteOrder.nativeOrder())
                val directOutBuf = ByteBuffer.allocateDirect(bufferSizeBytes).order(ByteOrder.nativeOrder())
                val shortBuffer = ShortArray(bufferElements)
                val shortOutBuffer = ShortArray(bufferElements)

                recorder.startRecording()
                // Fade-in ramp: 30ms worth of stereo samples for smooth transition
                val totalFadeInSamples = (sampleRate * 0.030).toInt() * 2
                var fadeInSamplesRemaining = totalFadeInSamples

                while (!isProcessorDisposing) {
                    if(recreateRecorderRequested) {
                        Timber.d("Recreating AudioRecord due to permission grant")
                        recreateRecorderRequested = false
                        try {
                            recorder.stop()
                            recorder.release()
                        } catch (e: Exception) {
                            Timber.e(e, "Error releasing old recorder")
                        }
                        try {
                            recorder = buildAudioRecord(encodingFormat, sampleRate, recHwBuf)
                            recorder.startRecording()
                            fadeInSamplesRemaining = totalFadeInSamples
                            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                track.pause()
                                track.flush()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to recreate recorder")
                        }
                    }
                    if (mediaProjection == null) {
                        Timber.e("Media projection handle is null, stopping service")
                        stopSelf()
                        return@Thread
                    }

                    if(isProcessorIdle && suspendOnIdle)
                    {
                        if(recorder.state == AudioRecord.STATE_INITIALIZED &&
                            recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                            recorder.stop()
                        if(track.state == AudioTrack.STATE_INITIALIZED &&
                            track.playState != AudioTrack.PLAYSTATE_STOPPED)
                            track.stop()

                        try {
                            Thread.sleep(50)
                        }
                        catch(e: InterruptedException) {
                            break
                        }
                        continue
                    }

                    if(recorder.state == AudioRecord.STATE_INITIALIZED && recorder.recordingState == AudioRecord.RECORDSTATE_STOPPED) {
                        recorder.startRecording()
                    }
                    // Start playback immediately — the fade-in ramp handles the transition
                    if(track.state == AudioTrack.STATE_INITIALIZED && track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                        Timber.d("AudioTrack playing (low-latency start)")
                    }

                    if(encoding == AudioEncoding.PcmShort) {
                        val readShorts = recorder.read(shortBuffer, 0, shortBuffer.size, AudioRecord.READ_BLOCKING)
                        if (readShorts > 0) {
                            val validShorts = readShorts - (readShorts % 2)
                            engine.processInt16(shortBuffer, shortOutBuffer)

                            // Apply fade-in ramp for smooth transition on startup
                            if (fadeInSamplesRemaining > 0) {
                                for (i in 0 until validShorts) {
                                    val sampleIndex = totalFadeInSamples - fadeInSamplesRemaining + i
                                    if (sampleIndex < totalFadeInSamples) {
                                        val gain = sampleIndex.toFloat() / totalFadeInSamples
                                        shortOutBuffer[i] = (shortOutBuffer[i] * gain).toInt().toShort()
                                    }
                                }
                                fadeInSamplesRemaining = (fadeInSamplesRemaining - validShorts).coerceAtLeast(0)
                            }

                            directOutBuf.clear()
                            directOutBuf.asShortBuffer().put(shortOutBuffer, 0, validShorts)
                            directOutBuf.position(0)
                            directOutBuf.limit(validShorts * Short.SIZE_BYTES)
                            var bytesRemaining = validShorts * Short.SIZE_BYTES
                            while (bytesRemaining > 0 && !isProcessorDisposing) {
                                val written = track.write(directOutBuf, bytesRemaining, AudioTrack.WRITE_BLOCKING)
                                if (written > 0) {
                                    bytesRemaining -= written
                                } else {
                                    Timber.e("AudioTrack write short error: $written")
                                    break
                                }
                            }
                        }
                    }
                    else {
                        directInBuf.clear()
                        val readStartNs = System.nanoTime()
                        val readBytes = recorder.read(directInBuf, bufferSizeBytes, AudioRecord.READ_BLOCKING)
                        val readMs = (System.nanoTime() - readStartNs) / 1_000_000L
                        if (readMs > 20) Timber.w("Slow capture read: ${readMs}ms ($readBytes bytes)")
                        if (readBytes <= 0) { Timber.e("AudioRecord read returned error: $readBytes") }
                        if (readBytes > 0) {
                            val frameCount = readBytes / (2 * Float.SIZE_BYTES)
                            val validBytes = frameCount * (2 * Float.SIZE_BYTES)
                            if (validBytes > 0) {
                                directInBuf.position(0)
                                directOutBuf.clear()
                                directOutBuf.position(0)
                                engine.processDirectFloat(directInBuf, directOutBuf, frameCount)

                                // Apply fade-in ramp for smooth transition on startup
                                if (fadeInSamplesRemaining > 0) {
                                    val floatView = directOutBuf.asFloatBuffer()
                                    val totalFloats = frameCount * 2
                                    for (i in 0 until totalFloats) {
                                        val sampleIndex = totalFadeInSamples - fadeInSamplesRemaining + i
                                        if (sampleIndex < totalFadeInSamples) {
                                            val gain = sampleIndex.toFloat() / totalFadeInSamples
                                            floatView.put(i, floatView.get(i) * gain)
                                        }
                                    }
                                    fadeInSamplesRemaining = (fadeInSamplesRemaining - totalFloats).coerceAtLeast(0)
                                }

                                directOutBuf.position(0)
                                directOutBuf.limit(validBytes)
                                val writeStartNs = System.nanoTime()
                                var bytesRemaining = validBytes
                                while (bytesRemaining > 0 && !isProcessorDisposing) {
                                    val written = track.write(directOutBuf, bytesRemaining, AudioTrack.WRITE_BLOCKING)
                                    if (written > 0) {
                                        bytesRemaining -= written
                                    } else {
                                        Timber.e("AudioTrack write float error: $written")
                                        break
                                    }
                                }
                                val writeMs = (System.nanoTime() - writeStartNs) / 1_000_000L
                                if (writeMs > 20) Timber.w("Slow track write: ${writeMs}ms")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Timber.w(e)
            } catch (e: Exception) {
                Timber.e("Exception in recorderThread raised")
                Timber.e(e)
                stopSelf()
            } finally {
                

                if(recorder.state != AudioRecord.STATE_UNINITIALIZED) {
                    recorder.stop()
                }
                if(track.state != AudioTrack.STATE_UNINITIALIZED) {
                    track.stop()
                }

                recorder.release()
                track.release()
            }
        }
        recorderThread!!.start()
    }

    fun stopRecording() {
        if (recorderThread != null) {
            isProcessorDisposing = true
            recorderThread!!.interrupt()
            recorderThread!!.join(500)
            recorderThread = null
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Timber.d("Released PARTIAL_WAKE_LOCK")
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not release wake lock")
        }
    }

    fun restartRecording() {
        if(isProcessorDisposing || isServiceDisposing) {
            Timber.e("restartRecording: service or processor already disposing")
            return
        }

        stopRecording()
        isProcessorDisposing = false
        recreateRecorderRequested = false
        startRecording()
    }

    private fun buildAudioTrack(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioTrack {
        val attributesBuilder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)

        sdkAbove(Build.VERSION_CODES.Q) {
            attributesBuilder.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        }
        sdkAbove(Build.VERSION_CODES.S) {
            attributesBuilder.setSpatializationBehavior(AudioAttributes.SPATIALIZATION_BEHAVIOR_NEVER)
        }
        val format = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .build()

        val frameSizeInBytes: Int = if (encoding == AudioFormat.ENCODING_PCM_16BIT) {
            2 * 2
        } else {
            2 * 4
        }

        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, encoding)
        val targetSize = maxOf(bufferSizeBytes, minBuf)
        val bufferSize = if (((targetSize % frameSizeInBytes) != 0 || targetSize < 1)) {
            frameSizeInBytes * (targetSize / frameSizeInBytes)
        } else targetSize

        Timber.d("Using AudioTrack buffer size $bufferSize (min: $minBuf)")

        return AudioTrack.Builder()
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioAttributes(attributesBuilder.build())
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun buildAudioRecord(encoding: Int, sampleRate: Int, bufferSizeBytes: Int): AudioRecord {
        if (!hasRecordPermission()) {
            Timber.e("buildAudioRecord: RECORD_AUDIO not granted")
            throw RuntimeException("RECORD_AUDIO not granted")
        }

        val format = AudioFormat.Builder()
            .setEncoding(encoding)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val configBuilder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)

        val excluded = (if(excludeRestrictedSessions)
            sessionManager.sessionPolicyDatabase.getRestrictedUids().toList()
        else {
            sessionManager.pollOnce(false)
            emptyList()
        }).toMutableList()

        blockedApps.value?.map { it.uid }?.let {
            excluded += it
        }
        excluded += Process.myUid()

        excluded.forEach { configBuilder.excludeUid(it) }
        sessionManager.sessionDatabase.setExcludedUids(excluded.toTypedArray())
        sessionManager.pollOnce(false)

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, encoding)
        val targetSize = maxOf(bufferSizeBytes, minBuf)
        val frameSizeInBytes: Int = if (encoding == AudioFormat.ENCODING_PCM_16BIT) 4 else 8
        val bufferSize = if (((targetSize % frameSizeInBytes) != 0 || targetSize < 1)) {
            frameSizeInBytes * (targetSize / frameSizeInBytes)
        } else targetSize

        Timber.d("buildAudioRecord: buffer size $bufferSize (min: $minBuf); Excluded UIDs: ${excluded.joinToString("; ")}")

        return AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(configBuilder.build())
            .build()
    }

    private fun determineSamplingRate(): Int {
        val nativeRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        if (nativeRate > 0) {
            Timber.i("Native AudioTrack sampling rate is $nativeRate")
            return nativeRate
        }
        val sampleRateStr: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val srate = sampleRateStr?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 48000
        Timber.i("Real HAL sampling rate is $srate")
        return srate
    }

    private fun determineBufferSize(): Int {
        val framesPerBuffer: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        return framesPerBuffer?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 256
    }

    companion object {
        const val SESSION_LOSS_MAX_RETRIES = 1

        const val ACTION_START = BuildConfig.APPLICATION_ID + ".rootless.service.START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".rootless.service.STOP"
        const val EXTRA_MEDIA_PROJECTION_DATA = "mediaProjectionData"
        const val EXTRA_APP_UID = "uid"
        const val EXTRA_APP_COMPAT_INTERNAL_CALL = "appCompatInternalCall"

        fun start(context: Context, data: Intent?) {
            try {
                context.startForegroundService(ServiceNotificationHelper.createStartIntent(context, data))
            }
            catch(ex: Exception) {
                CrashlyticsImpl.recordException(ex)
            }
        }

        fun stop(context: Context) {
            try {
                context.startForegroundService(ServiceNotificationHelper.createStopIntent(context))
            }
            catch(ex: Exception) {
                CrashlyticsImpl.recordException(ex)
            }
        }
    }
}
