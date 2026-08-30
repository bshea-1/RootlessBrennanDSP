package me.timschneeberger.rootlessjamesdsp.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-performance, zero-allocation Single-Producer Single-Consumer (SPSC)
 * circular audio ring buffer for real-time audio stream decoupling.
 *
 * Designed to decouple AudioRecord (capture producer) and AudioTrack (render consumer)
 * into independent real-time threads without mutex lock contention.
 */
class SpscAudioRingBuffer(
    val slotCount: Int = 16,
    val chunkSizeBytes: Int
) {
    init {
        require(slotCount > 0 && (slotCount and (slotCount - 1)) == 0) {
            "slotCount must be a power of 2 (got $slotCount)"
        }
    }

    private val mask = slotCount - 1
    private val writeIndex = AtomicInteger(0)
    private val readIndex = AtomicInteger(0)

    // Pre-allocated direct ByteBuffers for zero-copy memory transfers
    private val buffers: Array<ByteBuffer> = Array(slotCount) {
        ByteBuffer.allocateDirect(chunkSizeBytes).order(ByteOrder.nativeOrder())
    }

    /**
     * Number of chunks available for the consumer to read.
     */
    fun availableToRead(): Int {
        val w = writeIndex.get()
        val r = readIndex.get()
        return (w - r) and 0x7FFFFFFF
    }

    /**
     * Number of slots available for the producer to write.
     */
    fun availableToWrite(): Int {
        return slotCount - availableToRead() - 1
    }

    /**
     * Producer: Get the next direct buffer to write into.
     * Returns null if buffer is full.
     */
    fun acquireWriteBuffer(): ByteBuffer? {
        val w = writeIndex.get()
        val r = readIndex.get()
        if (((w - r) and 0x7FFFFFFF) >= slotCount - 1) {
            return null // Full
        }
        val slot = w and mask
        val buf = buffers[slot]
        buf.clear()
        return buf
    }

    /**
     * Producer: Commit written bytes and advance write pointer.
     */
    fun commitWrite(bytesWritten: Int) {
        val w = writeIndex.get()
        val slot = w and mask
        buffers[slot].position(0)
        buffers[slot].limit(bytesWritten)
        writeIndex.set(w + 1)
    }

    /**
     * Consumer: Get the next buffer to read and process.
     * Returns null if buffer is empty.
     */
    fun acquireReadBuffer(): ByteBuffer? {
        val r = readIndex.get()
        val w = writeIndex.get()
        if (r == w) {
            return null // Empty
        }
        val slot = r and mask
        val buf = buffers[slot]
        buf.position(0)
        return buf
    }

    /**
     * Consumer: Finish processing current buffer and advance read pointer.
     */
    fun commitRead() {
        val r = readIndex.get()
        readIndex.set(r + 1)
    }

    /**
     * Trim excess backlog down to [targetWatermark] chunks.
     * Drops oldest unconsumed chunks to maintain ultra-low latency.
     */
    fun trimToWatermark(targetWatermark: Int): Int {
        val w = writeIndex.get()
        val r = readIndex.get()
        val currentDepth = (w - r) and 0x7FFFFFFF
        if (currentDepth > targetWatermark) {
            val dropCount = currentDepth - targetWatermark
            readIndex.set(r + dropCount)
            return dropCount
        }
        return 0
    }

    /**
     * Flush all unconsumed audio frames and reset pointers to 0.
     */
    fun flush() {
        val w = writeIndex.get()
        readIndex.set(w)
    }
}
