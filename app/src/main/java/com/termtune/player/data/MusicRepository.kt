package com.termtune.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of a library scan, so the UI can tell "empty" from "blocked". */
sealed interface LibraryResult {
    data class Success(val songs: List<Song>) : LibraryResult
    data object PermissionDenied : LibraryResult
    data class Failure(val message: String) : LibraryResult
}

/** Reads the device's local audio files through MediaStore. */
class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): LibraryResult = withContext(Dispatchers.IO) {
        if (!MediaPermissions.hasAudioAccess(context)) {
            return@withContext LibraryResult.PermissionDenied
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )

        // Skip ringtones, notifications and anything with no real duration.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val songs = mutableListOf<Song>()
            context.contentResolver
                .query(collection, projection, selection, null, sortOrder)
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val rawTitle = cursor.getString(titleColumn)
                        val fileName = cursor.getString(nameColumn)
                        val title = rawTitle?.takeIf { it.isNotBlank() }
                            ?: fileName?.substringBeforeLast('.')
                            ?: "track $id"

                        songs += Song(
                            id = id,
                            title = title,
                            artist = cursor.getString(artistColumn)
                                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                ?: "unknown artist",
                            album = cursor.getString(albumColumn)
                                ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                ?: "unknown album",
                            durationMs = cursor.getLong(durationColumn),
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                        )
                    }
                }
                ?: return@withContext LibraryResult.Failure("MediaStore query returned no cursor")

            LibraryResult.Success(songs)
        } catch (error: SecurityException) {
            LibraryResult.PermissionDenied
        } catch (error: Exception) {
            LibraryResult.Failure(error.message ?: error::class.java.simpleName)
        }
    }
}
