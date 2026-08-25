package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context

interface IDumpProvider {

    fun dumpString(context: Context): String
}
