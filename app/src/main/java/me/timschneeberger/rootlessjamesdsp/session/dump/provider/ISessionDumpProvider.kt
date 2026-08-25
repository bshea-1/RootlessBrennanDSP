package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump

interface ISessionDumpProvider : IDumpProvider {

    fun dump(context: Context): ISessionInfoDump?

    override fun dumpString(context: Context): String
}
