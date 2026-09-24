package com.ownnet.syto.ui

/** Build identity shown on the home screen. Filled from BuildConfig and android.os.Build in MainActivity. */
data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val gitSha: String,
    val buildNumber: String,
    val androidRelease: String,
    val apiLevel: Int,
    val device: String,
)
