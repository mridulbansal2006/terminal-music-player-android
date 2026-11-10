package com.termtune.player.data

/**
 * A single audio track discovered through [android.provider.MediaStore].
 *
 * The content URI is kept as a [String] so the model stays free of Android
 * framework types and can be exercised by plain JVM unit tests.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
)
