package dev.shadowmeld.demomusicplayer.media

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 全局媒体中心
 */
object Media {

    /**
     * MediaSession
     */
    var session: MediaSessionCompat? by mutableStateOf(null)

    /**
     * 全局播放状态
     */
    var currentMediaState: PlaybackStateCompat? by mutableStateOf(null)

    /**
     * 媒体信息
     */
    var currentMediaInfo: MediaItemData? by mutableStateOf(null)

    /**
     * 播放列表
     */
    var playList: Map<String, MediaItemData>? = null
}