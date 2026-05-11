package com.offmind.runtimeshaders.gl

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class BackgroundCapture(
    private val window: Window
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val enabled = AtomicBoolean(false)
    private val dirty = AtomicBoolean(true)
    private val inFlight = AtomicBoolean(false)
    private val sourceRect = AtomicReference<Rect?>(null)
    private val latestBitmap = AtomicReference<Bitmap?>(null)

    fun updateSourceRect(rect: Rect) {
        val previous = sourceRect.get()
        if (previous != rect) {
            sourceRect.set(Rect(rect))
            markDirty()
        }
    }

    fun markDirty() {
        dirty.set(true)
    }

    fun submitBitmap(bitmap: Bitmap) {
        latestBitmap.getAndSet(bitmap)?.recycle()
    }

    fun enable() {
        enabled.set(true)
        markDirty()
    }

    fun shouldCapture(): Boolean {
        val rect = sourceRect.get()
        return enabled.get() &&
            dirty.get() &&
            !inFlight.get() &&
            rect != null &&
            rect.width() > 0 &&
            rect.height() > 0
    }

    fun isCaptureInFlight(): Boolean = inFlight.get()

    fun requestCapture() {
        if (!dirty.compareAndSet(true, false)) return
        if (!inFlight.compareAndSet(false, true)) {
            dirty.set(true)
            return
        }

        val rect = sourceRect.get()
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
            inFlight.set(false)
            return
        }

        val destination = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        PixelCopy.request(window, rect, destination, { result ->
            inFlight.set(false)
            if (result == PixelCopy.SUCCESS) {
                latestBitmap.getAndSet(destination)?.recycle()
            } else {
                destination.recycle()
                dirty.set(true)
            }
        }, mainHandler)
    }

    fun pollLatestBitmap(): Bitmap? = latestBitmap.getAndSet(null)

    fun release() {
        latestBitmap.getAndSet(null)?.recycle()
    }

    private companion object {
    }
}
