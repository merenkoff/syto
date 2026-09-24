package com.ownnet.syto.voice

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase-1 spike journal: one plain-text file in filesDir, rotated once at 1 MB.
 * Line format: `HH:mm:ss.SSS | tag | message`. Every line is mirrored to logcat under the tag "Syto".
 * The log screen shows the tail of this file and shares it via ACTION_SEND; that is the only way
 * to get logs off the phone without adb.
 */
object SpikeLog {
    private const val LOGCAT_TAG = "Syto"
    private const val MAX_BYTES = 1L shl 20

    private val lock = Any()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US) // guarded by [lock]
    private var file: File? = null
    private var rotated: File? = null

    fun init(context: Context) {
        synchronized(lock) {
            file = File(context.filesDir, "spike.log")
            rotated = File(context.filesDir, "spike.log.1")
        }
    }

    fun log(tag: String, message: String) {
        Log.d(LOGCAT_TAG, "$tag | $message")
        synchronized(lock) {
            val f = file ?: return
            val line = "${timeFormat.format(Date())} | $tag | $message\n"
            try {
                if (f.exists() && f.length() + line.length > MAX_BYTES) {
                    rotated?.delete()
                    rotated?.let { f.renameTo(it) }
                }
                f.appendText(line)
            } catch (e: IOException) {
                Log.w(LOGCAT_TAG, "cannot write spike log", e)
            }
        }
    }

    /** Last [maxLines] lines, oldest first. Reads the whole file; it is at most 1 MB. */
    fun tail(maxLines: Int = 200): List<String> = synchronized(lock) {
        val f = file ?: return emptyList()
        if (!f.exists()) return emptyList()
        try {
            f.readLines().takeLast(maxLines)
        } catch (e: IOException) {
            listOf("cannot read spike log: $e")
        }
    }

    fun file(): File? = synchronized(lock) { file?.takeIf { it.exists() } }

    fun clear() {
        synchronized(lock) {
            file?.delete()
            rotated?.delete()
        }
        log("log", "cleared")
    }
}
