package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionDumpEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.AudioServiceDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.AudioFlingerServiceDumpUtils
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.getPackageNameFromUid
import timber.log.Timber

class AudioServiceDumpProvider : ISessionDumpProvider {

    override fun dump(context: Context): ISessionInfoDump? {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        dump ?: return null

        return process(context, dump)
    }

    private fun process(context: Context, dump: String): ISessionInfoDump {
        val playbackConfRegex31 = """AudioPlaybackConfiguration.*u/pid:(\d+)/(\d+).*usage=(\w+).*content=(\w+).*sessionId:(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        val playbackConfRegex30 = """AudioPlaybackConfiguration.*u/pid:(\d+)/(\d+).*usage=(\w+).*content=(\w+)""".toRegex(RegexOption.IGNORE_CASE)
        val playbackConfRegex29 = """ID:\s*\d+.*u/pid:(\d+)/(\d+).*usage=(\w+).*content=(\w+)""".toRegex(RegexOption.IGNORE_CASE)

        val sidPidLookupMap = mutableMapOf<Int, Int>()
        val globalSessionRefs = AudioFlingerServiceDumpUtils.dump(context)
        globalSessionRefs?.forEach {
            if(sidPidLookupMap.contains(it.pid))
            {
                Timber.w("SID/PID map: Duplicated PID (pid=${it.pid}; sid=${it.sid})")
            }
            else
            {
                Timber.d("SID/PID map: AudioFlinger: pid=${it.pid}; sid=${it.sid}")
            }

            sidPidLookupMap[it.pid] = it.sid
        }

        val sessions = hashMapOf<Int, AudioSessionDumpEntry>()

        dump.lineSequence().forEach { line ->
            if (!line.contains("u/pid", ignoreCase = true) && !line.contains("AudioPlaybackConfiguration", ignoreCase = true)) {
                return@forEach
            }

            val match = playbackConfRegex31.find(line)
                ?: playbackConfRegex30.find(line)
                ?: playbackConfRegex29.find(line)
                ?: return@forEach

            try {
                val uid = match.groups[1]?.value?.toIntOrNull()
                val pid = match.groups[2]?.value?.toIntOrNull()
                val usage = match.groups[3]?.value
                val content = match.groups[4]?.value ?: "CONTENT_TYPE_UNKNOWN"

                if (pid == null || uid == null || usage == null) {
                    return@forEach
                }

                var sid = if (match.groups.size > 5) {
                    match.groups[5]?.value?.toIntOrNull()
                } else null

                if (sid == null && sidPidLookupMap.containsKey(pid)) {
                    sid = sidPidLookupMap[pid]
                }

                if (sid == null) {
                    return@forEach
                }

                val pkg = context.getPackageNameFromUid(uid) ?: uid.toString()
                sessions[sid] = AudioSessionDumpEntry(uid, pkg, usage, content)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }

        Timber.d("Dump processed: found ${sessions.size} active sessions")
        return AudioServiceDump(sessions)
    }

    override fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        sb.append(process(context, dump ?: ""))
        sb.append("\n\n")
        sb.append(AudioFlingerServiceDumpUtils.dumpString(context))

        return sb.toString()
    }

    companion object {
        const val TARGET_SERVICE = "audio"
    }
}
