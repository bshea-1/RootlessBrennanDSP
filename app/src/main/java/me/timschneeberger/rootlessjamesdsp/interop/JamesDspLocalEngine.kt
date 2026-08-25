package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import android.content.Intent
import me.timschneeberger.rootlessjamesdsp.interop.structure.EelVmVariable
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class JamesDspLocalEngine(context: Context, callbacks: JamesDspWrapper.JamesDspCallbacks? = null) : JamesDspBaseEngine(context, callbacks) {
    @Volatile
    var handle: JamesDspHandle = JamesDspWrapper.alloc(callbacks ?: DummyCallbacks())
    private val isDisposed = AtomicBoolean(false)
    private val processLock = Any()

    override var sampleRate: Float
        set(value) {
            super.sampleRate = value
            synchronized(processLock) {
                if (handle != 0L) {
                    JamesDspWrapper.setSamplingRate(handle, value, false)
                }
            }
            context.sendLocalBroadcast(Intent(Constants.ACTION_SAMPLE_RATE_UPDATED))
        }
        get() = super.sampleRate
    override var enabled: Boolean = true

    init {
        if(BenchmarkManager.hasBenchmarksCached())
            BenchmarkManager.loadBenchmarksFromCache()
    }

    override fun close() {
        if (!isDisposed.compareAndSet(false, true)) return
        super.close()

        val oldHandle: JamesDspHandle
        synchronized(processLock) {
            oldHandle = handle
            handle = 0L
        }

        if (oldHandle != 0L) {
            JamesDspWrapper.free(oldHandle)
            Timber.d("Handle $oldHandle has been freed synchronously")
        }
    }

    fun processDirectFloat(input: ByteBuffer, output: ByteBuffer, frameCount: Int) {
        if (!enabled || handle == 0L || isDisposed.get()) {
            input.position(0)
            output.position(0)
            output.put(input)
            input.position(0)
            output.position(0)
        } else {
            synchronized(processLock) {
                if (handle != 0L && !isDisposed.get()) {
                    JamesDspWrapper.processDirectBuffer(handle, input, output, frameCount)
                } else {
                    input.position(0)
                    output.position(0)
                    output.put(input)
                    input.position(0)
                    output.position(0)
                }
            }
        }
    }

    fun processInt16(input: ShortArray, output: ShortArray, offset: Int = -1, length: Int = -1)
    {
        if(!enabled || handle == 0L || isDisposed.get())
        {
            if(offset < 0 && length < 0) {
                input.copyInto(output)
            }
            else {
                input.copyInto(output, 0, offset, offset + length)
            }
        }
        else {
            synchronized(processLock) {
                if (handle != 0L && !isDisposed.get()) {
                    JamesDspWrapper.processInt16(handle, input, output, offset, length)
                } else {
                    if (offset < 0 && length < 0) input.copyInto(output)
                    else input.copyInto(output, 0, offset, offset + length)
                }
            }
        }
    }

    fun processInt32(input: IntArray, output: IntArray, offset: Int = -1, length: Int = -1)
    {
        if(!enabled || handle == 0L || isDisposed.get())
        {
            if(offset < 0 && length < 0) {
                input.copyInto(output)
            }
            else {
                input.copyInto(output, 0, offset, offset + length)
            }
        }
        else {
            synchronized(processLock) {
                if (handle != 0L && !isDisposed.get()) {
                    JamesDspWrapper.processInt32(handle, input, output, offset, length)
                } else {
                    if (offset < 0 && length < 0) input.copyInto(output)
                    else input.copyInto(output, 0, offset, offset + length)
                }
            }
        }
    }

    fun processFloat(input: FloatArray, output: FloatArray, offset: Int = -1, length: Int = -1)
    {
        if(!enabled || handle == 0L || isDisposed.get())
        {
            if(offset < 0 && length < 0) {
                input.copyInto(output)
            }
            else {
                input.copyInto(output, 0, offset, offset + length)
            }
        }
        else {
            synchronized(processLock) {
                if (handle != 0L && !isDisposed.get()) {
                    JamesDspWrapper.processFloat(handle, input, output, offset, length)
                } else {
                    if (offset < 0 && length < 0) input.copyInto(output)
                    else input.copyInto(output, 0, offset, offset + length)
                }
            }
        }
    }

    override fun setOutputControl(threshold: Float, release: Float, postGain: Float): Boolean {
        return JamesDspWrapper.setLimiter(handle, threshold, release) and JamesDspWrapper.setPostGain(handle, postGain)
    }

    override fun setReverb(enable: Boolean, preset: Int): Boolean
    {
        return JamesDspWrapper.setReverb(handle, enable, preset)
    }

    override fun setCrossfeed(enable: Boolean, mode: Int): Boolean
    {
        synchronized(processLock) {
            return JamesDspWrapper.setCrossfeed(handle, enable, mode, 0, 0)
        }
    }

    override fun setCrossfeedCustom(enable: Boolean, fcut: Int, feed: Int): Boolean
    {
        synchronized(processLock) {
            return JamesDspWrapper.setCrossfeed(handle, enable, 99, fcut, feed)
        }
    }

    override fun setBassBoost(enable: Boolean, maxGain: Float): Boolean
    {
        return JamesDspWrapper.setBassBoost(handle, enable, maxGain)
    }

    override fun setStereoEnhancement(enable: Boolean, level: Float): Boolean
    {
        return JamesDspWrapper.setStereoEnhancement(handle, enable, level)
    }

    override fun setVacuumTube(enable: Boolean, level: Float): Boolean
    {
        return JamesDspWrapper.setVacuumTube(handle, enable, level)
    }

    override fun setMultiEqualizerInternal(
        enable: Boolean,
        filterType: Int,
        interpolationMode: Int,
        bands: DoubleArray
    ): Boolean {
        return JamesDspWrapper.setMultiEqualizer(handle, enable, filterType, interpolationMode, bands)
    }

    override fun setCompanderInternal(
        enable: Boolean,
        timeConstant: Float,
        granularity: Int,
        tfTransforms: Int,
        bands: DoubleArray
    ): Boolean {
        return JamesDspWrapper.setCompander(handle, enable, timeConstant, granularity, tfTransforms, bands)
    }

    override fun setVdcInternal(enable: Boolean, vdc: String): Boolean {
        return JamesDspWrapper.setVdc(handle, enable, vdc)
    }

    override fun setConvolverInternal(
        enable: Boolean,
        impulseResponse: FloatArray,
        irChannels: Int,
        irFrames: Int,
        irCrc: Int,
        irSampleRate: Int,
    ): Boolean {
        return JamesDspWrapper.setConvolver(handle, enable, impulseResponse, irChannels, irFrames)
    }

    override fun setGraphicEqInternal(enable: Boolean, bands: String): Boolean {
        return JamesDspWrapper.setGraphicEq(handle, enable, bands)
    }

    override fun setLiveprogInternal(enable: Boolean, name: String, script: String): Boolean {
        return JamesDspWrapper.setLiveprog(handle, enable, name, script)
    }

    override fun supportsEelVmAccess(): Boolean { return true }
    override fun supportsCustomCrossfeed(): Boolean { return true }

    override fun enumerateEelVariables(): ArrayList<EelVmVariable>
    {
        return JamesDspWrapper.enumerateEelVariables(handle)
    }

    override fun manipulateEelVariable(name: String, value: Float): Boolean
    {
        return JamesDspWrapper.manipulateEelVariable(handle, name, value)
    }

    override fun freezeLiveprogExecution(freeze: Boolean)
    {
        JamesDspWrapper.freezeLiveprogExecution(handle, freeze)
    }
}
