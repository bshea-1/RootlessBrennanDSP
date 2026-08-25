package me.timschneeberger.rootlessjamesdsp.session.shared

import android.content.Context
import android.os.Process.myUid
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionDumpEntry
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import timber.log.Timber

abstract class BaseSessionDatabase(protected val context: Context) {

    val sessionList = hashMapOf<Int, IEffectSession>()
    private var isDisposing = false
    private val missedPollCounts = hashMapOf<Int, Int>()
    private val changeCallbacks = mutableListOf<OnSessionChangeListener>()
    private var excludedUids = arrayOf<Int>()

    protected open val excludedPackages = arrayOf(
        context.packageName
    )
    protected abstract fun shouldAcceptSessionDump(id: Int, session: AudioSessionDumpEntry): Boolean
    protected abstract fun shouldAddSession(id: Int, uid: Int, packageName: String): Boolean
    protected abstract fun createSession(id: Int, uid: Int, packageName: String): IEffectSession?
    protected abstract fun onSessionRemoved(item: IEffectSession)

    open fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        missedPollCounts.clear()
        sessionList.forEach { (_, session) -> onSessionRemoved(session) }
        sessionList.clear()
    }

    fun update(dump: ISessionInfoDump)
    {
        if(isDisposing) {
            Timber.d("update: SessionDatabase is disposing; ignoring dump")
            return
        }

        val removedSessions = sessionList.filter {
            !dump.sessions.contains(it.key)
        }
        val addedSessions = dump.sessions.filter {
            !sessionList.contains(it.key) && !excludedUids.contains(it.value.uid)
        }

        sessionList.keys.forEach { sid ->
            if (dump.sessions.contains(sid))
                missedPollCounts.remove(sid)
        }

        addedSessions.forEach next@ {
            val sid = it.key
            val data = it.value
            val name = context.packageManager.getNameForUid(it.value.uid)
            if (data.uid == myUid() || excludedPackages.contains(name)) {
                Timber.d("Skipped session $sid due to package name $name ($data)")
                return@next
            }
            if (sid == 0) {
                Timber.w("Session 0 skipped ($data)")
                return@next
            }

            if(shouldAcceptSessionDump(sid, data)) {
                addSession(sid, data.uid, data.packageName)
            }
        }

        removedSessions.forEach { (sid, _) ->
            val misses = (missedPollCounts[sid] ?: 0) + 1
            if (misses >= SESSION_REMOVAL_MISSED_POLLS) {
                Timber.d("Removing session $sid after $misses consecutive missed polls")
                missedPollCounts.remove(sid)
                removeSession(sid)
            }
            else {
                missedPollCounts[sid] = misses
                Timber.d("Session $sid missing from dump ($misses/${SESSION_REMOVAL_MISSED_POLLS}); keeping alive")
            }
        }
    }

    fun addSession(sid: Int, uid: Int, packageName: String, replace: Boolean = false){
        if(!shouldAddSession(sid, uid, packageName)) {
            return
        }

        if(excludedUids.contains(uid)) {
            Timber.d("Rejected session $sid from excluded uid $uid ($packageName)")
            return
        }

        if(replace) {

            sessionList
                .filter { it.value.packageName == packageName }
                .keys
                .forEach(::removeSession)
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }

        Timber.d("Found new session: sid=$sid; $packageName")
        sessionList[sid] = createSession(sid, uid, packageName) ?: return
        Timber.d("Successfully added session $sid")

        changeCallbacks.forEach { it.onSessionChanged(sessionList) }
    }

    fun removeSession(sid: Int) {
        missedPollCounts.remove(sid)
        sessionList[sid]?.let { it ->
            Timber.d("Removed session: session ${sid}; data: $it")
            onSessionRemoved(it)
            sessionList.remove(sid)
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }
    }

    fun setExcludedUids(uids: Array<Int>) {
        excludedUids = uids

        val excludedSessions = sessionList.filter {
            excludedUids.contains(it.value.uid)
        }
        val notify = excludedSessions.isNotEmpty()
        excludedSessions.forEach { (_, session) -> onSessionRemoved(session) }
        excludedSessions.map { it.key }.forEach { sid -> sessionList.remove(sid) }
        if(notify)
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
    }

    fun registerOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.add(changeListener)
        changeListener.onSessionChanged(sessionList)
    }

    fun unregisterOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.remove(changeListener)
    }

    interface OnSessionChangeListener {
        fun onSessionChanged(sessionList: HashMap<Int, IEffectSession>)
    }

    companion object {
        const val SESSION_REMOVAL_MISSED_POLLS = 3
    }
}
