package com.boox.einkdraw

import android.os.Handler
import android.os.Looper

object RapidRawPointRelay {
    private var listener: ((Float, Float, Float, Int, Long) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun setListener(cb: ((Float, Float, Float, Int, Long) -> Unit)?) {
        listener = cb
    }

    @JvmStatic
    fun publish(x: Float, y: Float, pressure: Float, action: Int, time: Long) {
        handler.post {
            listener?.invoke(x, y, pressure, action, time)
        }
    }
}
