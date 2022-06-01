package dev.shadowmeld.demomusicplayer.media

import java.io.FileDescriptor

data class MediaItemData(
    val media: FileDescriptor?,
    val title: String,
    val subtitle: String,
)
