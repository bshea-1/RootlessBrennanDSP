package me.timschneeberger.rootlessjamesdsp.utils.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import kotlin.reflect.KClass

class Preferences(val context: Context) {

    inner class App : AbstractPreferences(context) {
        override fun namespace() = Constants.PREF_APP
    }

    inner class Var : AbstractPreferences(context) {
        override fun namespace() = Constants.PREF_VAR
    }

    abstract class AbstractPreferences(val context: Context) {
        abstract fun namespace(): String

        val preferences: SharedPreferences by lazy {
            context.getSharedPreferences(namespace(), Context.MODE_PRIVATE)
        }

        private val defaultCache: HashMap<String, Any> = hashMapOf()

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> get(@StringRes nameRes: Int, default: T? = null, type: KClass<T>): T {
            val key = context.getString(nameRes)
            val defValue = default ?: getDefault(nameRes, type)

            val result: T = try {
                when(type) {
                    Boolean::class -> preferences.getBoolean(key, defValue as Boolean) as T
                    String::class -> preferences.getString(key, defValue as String) as T
                    Int::class -> preferences.getInt(key, defValue as Int) as T
                    Long::class -> preferences.getLong(key, defValue as Long) as T
                    Float::class -> preferences.getFloat(key, defValue as Float) as T
                    else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
                }
            } catch (_: Throwable) {
                try {
                    when(type) {
                        Boolean::class -> (preferences.getString(key, defValue.toString())?.toBooleanStrictOrNull() ?: (defValue as Boolean)) as T
                        Float::class -> (preferences.getString(key, defValue.toString())?.toFloatOrNull() ?: (preferences.all[key]?.toString()?.toFloatOrNull() ?: (defValue as Float))) as T
                        Int::class -> (preferences.getString(key, defValue.toString())?.toIntOrNull() ?: (preferences.all[key]?.toString()?.toIntOrNull() ?: (defValue as Int))) as T
                        Long::class -> (preferences.getString(key, defValue.toString())?.toLongOrNull() ?: (preferences.all[key]?.toString()?.toLongOrNull() ?: (defValue as Long))) as T
                        String::class -> (preferences.all[key]?.toString() ?: (defValue as String)) as T
                        else -> defValue
                    }
                } catch (_: Throwable) {
                    defValue
                }
            }

            return result.also {
                CrashlyticsImpl.setCustomKey("${namespace()}_$key", it.toString())
            }
        }

        inline fun <reified T : Any> get(@StringRes nameRes: Int): T {
            return get<T>(nameRes, null, T::class)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> getDefault(@StringRes nameRes: Int, type: KClass<T>): T {
            val key = context.getString(nameRes)
            return if(defaultCache.containsKey(key)) {
                defaultCache[key] as T
            }
            else {
                @SuppressLint("DiscouragedApi")
                val defaultRes = context.resources.getIdentifier(
                    "default_$key",
                    when(type)
                    {
                        Boolean::class -> "bool"
                        String::class -> "string"
                        Int::class -> "integer"
                        Long::class -> "integer"
                        Float::class -> "integer"
                        else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
                    },
                    context.packageName
                )

                if(defaultRes == 0) {
                    throw IllegalStateException("Preference key '$key' has no default set")
                }

                (when(type) {
                    Boolean::class -> context.resources.getBoolean(defaultRes)
                    String::class -> context.resources.getString(defaultRes)
                    Int::class -> context.resources.getInteger(defaultRes)
                    Long::class -> context.resources.getInteger(defaultRes).toLong()
                    Float::class -> context.resources.getInteger(defaultRes).toFloat()
                    else -> throw IllegalArgumentException("Unknown type")
                } as T).also {
                    defaultCache[key] = it as Any
                }
            }
        }

        inline fun <reified T : Any> getDefault(@StringRes nameRes: Int): T {
            return getDefault(nameRes, T::class)
        }

        @SuppressLint("ApplySharedPref")
        fun <T : Any> reset(@StringRes nameRes: Int, async: Boolean = true, type: KClass<T>) {
            set(nameRes, getDefault(nameRes, type), async, type)
        }

        inline fun <reified T : Any> reset(@StringRes nameRes: Int, async: Boolean = true) {
            return reset(nameRes, async, T::class)
        }

        @SuppressLint("ApplySharedPref")
        fun <T : Any> set(@StringRes nameRes: Int, value: T, async: Boolean = true, type: KClass<T>) {
            val key = context.getString(nameRes)
            val edit = preferences.edit()
            CrashlyticsImpl.setCustomKey("${namespace()}_$key", value.toString())

            when(type) {
                Boolean::class -> edit.putBoolean(key, value as Boolean)
                String::class -> edit.putString(key, value as String)
                Int::class -> edit.putInt(key, value as Int)
                Long::class -> edit.putLong(key, value as Long)
                Float::class -> edit.putFloat(key, value as Float)
                else -> throw IllegalArgumentException("Unknown type ${type.qualifiedName}")
            }.run {
                if(async)
                    apply()
                else
                    commit()
            }
        }

        inline fun <reified T : Any> set(@StringRes nameRes: Int, value: T, async: Boolean = true) {
            set(nameRes, value, async, T::class)
        }

        open fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            preferences.registerOnSharedPreferenceChangeListener(listener)
        }

        open fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
