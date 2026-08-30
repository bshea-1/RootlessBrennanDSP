package me.timschneeberger.rootlessjamesdsp.preference
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.Slider
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import timber.log.Timber
import java.math.BigDecimal
import java.math.MathContext
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class MaterialSeekbarPreference : Preference {
    var mSeekBarValue = 0f
    var mMin = 0f
    private var mMax = 0f
    private var mSeekBarIncrement = 0f
    var mTrackingTouch = false
    lateinit var mSeekBar: Slider
    private var mSeekBarValueTextView: TextView? = null

    var mUnit: String = ""
    var mPrecision: Int = 2
    var mLabelMinWidth: Int = 0

    var mAdjustable = false

    private var mShowSeekBarValue = false

    var mUpdatesContinuously = false

    var valueLabelOverride: ((Float) -> String)? = null

    private val mSeekBarChangeListener =
        Slider.OnChangeListener { slider, value, fromUser ->
            if (fromUser && mUpdatesContinuously || !mTrackingTouch) {
                syncValueInternal(slider)
            } else {

                updateLabelValue(value)
            }
        }
    private val mSeekBarTouchListener = object :  Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(seekBar: Slider) {
            mTrackingTouch = true
        }

        override fun onStopTrackingTouch(seekBar: Slider) {
            mTrackingTouch = false
            if (seekBar.value != mSeekBarValue) {
                syncValueInternal(seekBar)
            }
        }
    }

    private val mSeekBarKeyListener =
        View.OnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@OnKeyListener false
            }
            if (!mAdjustable && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {

                return@OnKeyListener false
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return@OnKeyListener false
            }
            mSeekBar.onKeyDown(keyCode, event)
        }

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        layoutResource = R.layout.preference_materialslider

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MaterialSeekbarPreference, defStyleAttr, defStyleRes
        )

        mMin = a.getFloat(R.styleable.MaterialSeekbarPreference_minValue, 0f)
        setMax(a.getFloat(R.styleable.MaterialSeekbarPreference_maxValue, 100f))
        setSeekBarIncrement(a.getFloat(R.styleable.MaterialSeekbarPreference_seekBarIncrement, 0f))
        mShowSeekBarValue = a.getBoolean(R.styleable.MaterialSeekbarPreference_showSeekBarValue, false)
        mUpdatesContinuously = a.getBoolean(
            R.styleable.MaterialSeekbarPreference_updatesContinuously,
            false
        )

        mLabelMinWidth = a.getDimensionPixelSize(R.styleable.MaterialSeekbarPreference_labelMinWidth, 0)
        mUnit = a.getString(R.styleable.MaterialSeekbarPreference_unit) ?: ""
        mPrecision = a.getInt(R.styleable.MaterialSeekbarPreference_precision, 2)

        a.recycle()
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context, attrs: AttributeSet?,
    ) : this(context, attrs, R.attr.seekBarStyle)

    constructor(
        context: Context,
    ) : this(context, null)

    private fun validateValue(value: Float): Float {
        if (mSeekBarIncrement > 0 && !valueLandsOnTick(value)) {
            val newValue = mSeekBarIncrement * ((value / mSeekBarIncrement).roundToInt())
            Timber.w("setValueInternal: value corrected $value to $newValue")
            return newValue
        }
        return value
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mSeekBar = holder.findViewById(R.id.seekbar) as Slider
        mSeekBarValueTextView = holder.findViewById(R.id.seekbar_value) as TextView
        holder.itemView.setOnKeyListener(mSeekBarKeyListener)

        if(mLabelMinWidth > 0) {
            mSeekBarValueTextView!!.minWidth = mLabelMinWidth
        }

        if (mShowSeekBarValue) {
            mSeekBarValueTextView!!.visibility = View.VISIBLE
        } else {
            mSeekBarValueTextView!!.visibility = View.GONE
            mSeekBarValueTextView = null
        }

        mSeekBar.clearOnChangeListeners()
        mSeekBar.clearOnSliderTouchListeners()
        mSeekBar.addOnChangeListener(mSeekBarChangeListener)
        mSeekBar.addOnSliderTouchListener(mSeekBarTouchListener)
        mSeekBar.valueFrom = mMin
        mSeekBar.valueTo = mMax

        mSeekBar.stepSize = mSeekBarIncrement

        mSeekBar.value = validateValue(mSeekBarValue)
        updateLabelValue(mSeekBarValue)
        mSeekBar.isEnabled = isEnabled

        this.setOnPreferenceClickListener {
            context.showInputAlert(
                LayoutInflater.from(context),
                context.getString(R.string.slider_dialog_title),
                title?.toString(),
                "%.${mPrecision}f".format(Locale.ROOT, getValue()),
                true,
                mUnit
            ) {
                it ?: return@showInputAlert
                try {
                    if(mSeekBar.stepSize <= 0 || valueLandsOnTick(it.toFloat())) {
                        setValue(it.toFloat())
                    }
                    else {
                        context.toast(
                            context.getString(R.string.slider_dialog_step_error, mSeekBar.stepSize.roundToInt()),
                            false
                        )
                    }
                }
                catch (ex: Exception) {
                    Timber.e("Failed to parse number input")
                    Timber.d(ex)
                    context.toast(
                        context.getString(R.string.slider_dialog_format_error),
                        false
                    )
                }
            }
            true
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val def = (defaultValue as? Float) ?: 0f
        val currentVal = try {
            getPersistedFloat(def)
        } catch (_: Throwable) {
            try {
                preferenceManager.sharedPreferences?.getInt(key, def.toInt())?.toFloat() ?: def
            } catch (_: Throwable) {
                try {
                    preferenceManager.sharedPreferences?.getString(key, def.toString())?.toFloatOrNull() ?: def
                } catch (_: Throwable) {
                    def
                }
            }
        }
        setValue(currentVal)
        try {
            persistFloat(currentVal)
        } catch (_: Throwable) {}
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return validateValue(a.getFloat(index, 0f))
    }

    fun getMin(): Float {
        return mMin
    }

    fun setMin(_min: Float) {
        var min = _min
        if (min > mMax) {
            min = mMax
        }
        if (min != mMin) {
            mMin = min
            notifyChanged()
        }
    }

    fun getSeekBarIncrement(): Float {
        return mSeekBarIncrement
    }

    fun setSeekBarIncrement(seekBarIncrement: Float) {
        if (seekBarIncrement != mSeekBarIncrement) {
            mSeekBarIncrement = min(mMax - mMin, abs(seekBarIncrement))
            notifyChanged()
        }
    }

    fun getMax(): Float {
        return mMax
    }

    fun setMax(_max: Float) {
        var max = _max
        if (max < mMin) {
            max = mMin
        }
        if (max != mMax) {
            mMax = max
            notifyChanged()
        }
    }

    fun isAdjustable(): Boolean {
        return mAdjustable
    }

    fun setAdjustable(adjustable: Boolean) {
        mAdjustable = adjustable
    }

    fun getUpdatesContinuously(): Boolean {
        return mUpdatesContinuously
    }

    fun setUpdatesContinuously(updatesContinuously: Boolean) {
        mUpdatesContinuously = updatesContinuously
    }

    fun getShowSeekBarValue(): Boolean {
        return mShowSeekBarValue
    }

    fun setShowSeekBarValue(showSeekBarValue: Boolean) {
        mShowSeekBarValue = showSeekBarValue
        notifyChanged()
    }

    private fun setValueInternal(_seekBarValue: Float, notifyChanged: Boolean) {
        var seekBarValue = _seekBarValue
        if (seekBarValue < mMin) {
            seekBarValue = mMin
        }
        if (seekBarValue > mMax) {
            seekBarValue = mMax
        }

        seekBarValue = validateValue(seekBarValue)
        if (mSeekBarIncrement > 0 && !valueLandsOnTick(seekBarValue)) {
            seekBarValue = mSeekBarIncrement * ((seekBarValue / mSeekBarIncrement).roundToInt())
            Timber.w("setValueInternal: value corrected $_seekBarValue to $seekBarValue")
        }

        if (seekBarValue != mSeekBarValue) {
            mSeekBarValue = seekBarValue
            updateLabelValue(mSeekBarValue)
            persistFloat(seekBarValue)
            if (notifyChanged) {
                notifyChanged()
            }
        }
    }

    fun getValue(): Float {
        return mSeekBarValue
    }

    fun setValue(seekBarValue: Float) {
        setValueInternal(seekBarValue, true)
    }

    fun  syncValueInternal(seekBar: Slider) {
        val seekBarValue = seekBar.value
        if (seekBarValue != mSeekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false)
            } else {
                seekBar.value = validateValue(mSeekBarValue)
                updateLabelValue(mSeekBarValue)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun  updateLabelValue(value: Float) {
        if (mSeekBarValueTextView != null) {

            if(valueLabelOverride == null)
            {
                mSeekBarValueTextView!!.text = "%.${mPrecision}f${mUnit}".format(Locale.ROOT, value)
            }
            else
            {
                mSeekBarValueTextView!!.text = valueLabelOverride!!(value)
            }
        }
    }

    private fun valueLandsOnTick(value: Float): Boolean {

        return isMultipleOfStepSize(value - mMin)
    }

    private fun isMultipleOfStepSize(value: Float): Boolean {

        val result = BigDecimal(value.toString())
            .divide(BigDecimal(mSeekBarIncrement.toString()), MathContext.DECIMAL64)
            .toDouble()

        return abs(result.roundToInt() - result) < 1.0E-4
    }
}
