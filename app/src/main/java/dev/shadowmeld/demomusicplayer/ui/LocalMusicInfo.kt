package dev.shadowmeld.demomusicplayer.ui

import android.graphics.Bitmap
import android.net.Uri

data class LocalMusicInfo(
    val source: Uri?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val image: Bitmap?,
    val duration: Int?
)
