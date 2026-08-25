package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context

interface ISessionPolicyDumpProvider : IDumpProvider {

    fun dump(context: Context): ISessionPolicyDumpProvider?

    override fun dumpString(context: Context): String
}
