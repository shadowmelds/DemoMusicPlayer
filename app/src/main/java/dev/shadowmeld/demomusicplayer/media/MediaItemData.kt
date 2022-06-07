package dev.shadowmeld.demomusicplayer.media

import android.graphics.Bitmap
import android.net.Uri
import java.io.FileDescriptor

data class MediaItemData(
    val uri: Uri,
    val source: Int,
    val title: String,
    val artist: String,
    val album: String,
    val image: Bitmap?,
    val duration: Int
)
