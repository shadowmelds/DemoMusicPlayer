package dev.shadowmeld.demomusicplayer.media

import android.graphics.Bitmap
import android.net.Uri
import java.io.FileDescriptor

data class MediaItemData(
    val uri: Uri,
    val musicName: String,
    val musicArtist: Bitmap?,
    val musicDuration: Int
)
