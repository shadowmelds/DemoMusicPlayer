package dev.shadowmeld.demomusicplayer.media

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.exoplayer2.ExoPlayer
import dev.shadowmeld.demomusicplayer.data.MusicRepository

/**
 * 全局媒体中心
 */
object Media {

    /**
     * MediaSession
     */
    lateinit var session: MediaSessionCompat

    /**
     * 数据源
     */
    lateinit var musicInfoRepository: MusicRepository

    /**
     * 全局播放状态
     */
    var currentMediaState: Int? by mutableStateOf(null)

    /**
     * 媒体信息
     */
    var currentMediaInfo: MediaItemData? by mutableStateOf(null)

    /**
     * 播放列表
     */
    var playList: Map<String, MediaItemData>? = null

    lateinit var exoPlayer: ExoPlayer

    fun initExoPlayer(context: Context) {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            // 设置ExoPlayer 为重复播放
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }
}