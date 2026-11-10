package com.termtune.player.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralises the Android-version split for media permissions.
 *
 * Android 13 (API 33) replaced the broad storage read with the scoped
 * `READ_MEDIA_AUDIO`, and added a runtime permission for notifications.
 */
object MediaPermissions {

    /** The single permission that gates MediaStore audio access. */
    val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** Everything worth asking for in one dialog batch. */
    fun requestedPermissions(): Array<String> = buildList {
        add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun hasAudioAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED

    /** Human-readable name used in terminal output and the settings screen. */
    fun audioPermissionLabel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "READ_MEDIA_AUDIO"
        } else {
            "READ_EXTERNAL_STORAGE"
        }
}
