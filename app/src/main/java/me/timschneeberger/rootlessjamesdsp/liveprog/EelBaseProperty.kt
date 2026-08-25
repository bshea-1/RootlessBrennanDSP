package me.timschneeberger.rootlessjamesdsp.liveprog

abstract class EelBaseProperty(val key: String, val description: String) {

    abstract fun hasDefault(): Boolean

    abstract fun isDefault(): Boolean

    abstract fun restoreDefaults()

    abstract fun valueAsString(): String

    abstract fun manipulateProperty(contents: String): String?
}
