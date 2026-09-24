package com.ownnet.syto.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

object ContactsLookup {
    /**
     * True if [number] matches a contact, false if it does not,
     * null if READ_CONTACTS is not granted (caller treats it as "unknown number").
     */
    fun isKnown(context: Context, number: String?): Boolean? {
        if (number.isNullOrBlank()) return false
        val granted = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver
                .query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (e: SecurityException) {
            SpikeLog.log("contacts", "lookup failed: $e")
            null
        }
    }
}
