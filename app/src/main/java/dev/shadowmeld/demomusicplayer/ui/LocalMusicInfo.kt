package dev.shadowmeld.demomusicplayer.ui

import android.net.Uri

data class LocalMusicInfo(
    val uri: Uri,
    val musicName: String,
    val musicArtist: String,
    val musicDuration: Int,
    val size: Int
)
