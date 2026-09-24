package com.ownnet.syto

import android.app.Application
import com.ownnet.syto.voice.SpikeLog

class SytoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SpikeLog.init(this)
        SpikeLog.log("app", "start ${BuildConfig.VERSION_NAME} ${BuildConfig.GIT_SHA} build=${BuildConfig.BUILD_NUMBER}")
    }
}
