package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.animation.ValueAnimator
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.DynamicColors
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

fun String.crc(): Int = this.toByteArray()
        .fold(-0x1) { crc, byte ->
            (0 until 8).fold(crc xor byte.toInt()) { acc, _ ->
                val mask = -(acc and 1)
                (acc shr 1) xor (-0x12477ce0 and mask)
            }
        }
        .inv()

fun String.asHtml(): Spanned = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)

fun Double.equalsDelta(other: Double) = abs(this - other) < 0.00001 * max(abs(this), abs(other))

fun Double.prettyNumberFormat(): String {
    if( this == 0.0 ) return "0"

    val prefix = if( this < 0 ) "-" else ""
    val num = abs(this)

    val pow = floor(log10(num) /3).roundToInt()
    val base = num / 10.0.pow(pow * 3)

    val roundedDown = floor(base*100) /100.0

    var baseStr = BigDecimal(roundedDown)
        .setScale(1, RoundingMode.HALF_EVEN)
        .toString()

    baseStr = baseStr.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }

    val suffixes = listOf("","k","M","B","T")

    return when {
        pow < suffixes.size -> "$prefix$baseStr${suffixes[pow]}"
        else -> "${prefix}∞"
    }
}

fun Boolean.toShort() = (if (this) 1 else 0).toShort()

val String.md5: ByteArray
    get() {
        return MessageDigest.getInstance("MD5").digest(this.toByteArray())
    }

private val isSamsung by lazy {
    Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}

val isDynamicColorAvailable by lazy {
    DynamicColors.isDynamicColorAvailable() || (isSamsung && SdkCheck.isSnowCake)
}

fun View.setBackgroundFromAttribute(@AttrRes attrRes: Int) {
    val a = TypedValue()
    context.theme.resolveAttribute(attrRes, a, true)
    if (SdkCheck.isQ && a.isColorType) {
        setBackgroundColor(a.data)
    } else {
        background = ResourcesCompat.getDrawable(context.resources, a.resourceId, context.theme)
    }
}

fun File.ensureIsDirectory() = if(isDirectory) this else null
fun File.ensureIsFile() = if(isFile) this else null

inline fun <reified T> ValueAnimator.animatedValueAs(): T? = this.animatedValue as? T
