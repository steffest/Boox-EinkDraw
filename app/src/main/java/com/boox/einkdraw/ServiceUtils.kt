package com.boox.einkdraw

import android.app.ActivityManager
import android.content.Context

@Suppress("DEPRECATION")
fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    return am.getRunningServices(Int.MAX_VALUE)
        .asSequence()
        .map { it.service.className }
        .any { it == serviceClass.name }
}
