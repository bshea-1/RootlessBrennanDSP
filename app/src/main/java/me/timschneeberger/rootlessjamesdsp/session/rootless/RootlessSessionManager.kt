package me.timschneeberger.rootlessjamesdsp.session.rootless

import android.content.*
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionPolicyInfoDump
import me.timschneeberger.rootlessjamesdsp.session.shared.BaseSessionManager

class RootlessSessionManager(context: Context) : BaseSessionManager(context)
{

    val sessionDatabase: RootlessSessionDatabase = RootlessSessionDatabase(context)

    val sessionPolicyDatabase: SessionRecordingPolicyManager = SessionRecordingPolicyManager(context)

    override fun destroy()
    {
        super.destroy()
        sessionDatabase.destroy()
        sessionPolicyDatabase.destroy()
    }

    override fun handleSessionDump(sessionDump: ISessionInfoDump?, policyDump: ISessionPolicyInfoDump?) {
        if(sessionDump is ISessionPolicyInfoDump) {
            sessionPolicyDatabase.update(sessionDump)
        }
        else {
            policyDump?.let { sessionPolicyDatabase.update(it) }
        }

        sessionDump?.let { sessionDatabase.update(it) }
    }

    override fun onActivePlaybackDetected(sid: Int, uid: Int, packageName: String) {
        sessionDatabase.addSession(sid, uid, packageName)
    }

    override fun onDumpMethodChange(method: DumpManager.Method) {
        sessionDatabase.clearSessions()
        sessionPolicyDatabase.clearSessions()
        super.onDumpMethodChange(method)
    }
}
