package com.boox.einkdraw

import android.app.Application
import android.os.Build
import com.onyx.android.sdk.rx.RxManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BooxEinkDrawApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { RxManager.Builder.initAppContext(this) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("") }
        }
    }
}
