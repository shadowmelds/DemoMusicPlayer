package dev.shadowmeld.demomusicplayer.media

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.exoplayer2.ExoPlayer
import dev.shadowmeld.demomusicplayer.data.MusicRepository
import dev.shadowmeld.demomusicplayer.util.log

interface MediaController {

    /**
     * 播放列表
     */
    var playList: MutableList<MediaItemData>?

    /**
     * 播放器
     */
    var exoPlayer: ExoPlayer

    /**
     * MediaSession
     */
    var session: MediaSessionCompat

    /**
     * 媒体信息
     */
    var currentSongInfo: MediaItemData?

    /**
     * 全局播放状态
     */
    var currentMediaState: Int

    /**
     * 播放模式
     */
    var playMode: PlayMode

    /**
     * 添加歌到播放列表
     */
    fun addSongs(mediaItems: ArrayList<MediaItemData>)

    /**
     * 从播放列表移除歌
     */
    fun removeSongs(mediaItems: ArrayList<MediaItemData>)

    /**
     * 暂停音乐
     */
    fun onPause()

    /**
     * 播放音乐
     */
    fun onPlay()

    /**
     * 上一曲
     */
    fun onPrevious()

    /**
     * 下一曲
     */
    fun onNext()

    /**
     * 播放至..
     */
    fun onSeekTo(pos: Long)

    /**
     * 喜爱
     */
    fun onFavorite()

    /**
     * 更新音乐信息
     */
    fun updateMediaInfo(metadataCompat: MediaMetadataCompat)
}

enum class PlayMode {
    REPEAT, SHUFFLE, SINGLE
}


object DefaultMediaController : MediaController {

    /**
     * 数据源
     */
    lateinit var musicInfoRepository: MusicRepository

    fun initExoPlayer(context: Context) {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            // 设置ExoPlayer 为重复播放
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }

    override var playList: MutableList<MediaItemData>? = null

    override lateinit var exoPlayer: ExoPlayer

    override lateinit var session: MediaSessionCompat

    override var currentSongInfo: MediaItemData? by mutableStateOf(null)

    override var currentMediaState: Int by mutableStateOf(PlaybackStateCompat.STATE_NONE)

    override var playMode: PlayMode = PlayMode.REPEAT
    set(value) {
        field = value
        when (value) {
            PlayMode.REPEAT -> {
                exoPlayer.shuffleModeEnabled = false
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            }

            PlayMode.SHUFFLE -> {
                exoPlayer.shuffleModeEnabled = true
            }

            PlayMode.SINGLE -> {
                exoPlayer.shuffleModeEnabled = false
                exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
        }
    }

    override fun addSongs(mediaItems: ArrayList<MediaItemData>) {
        playList?.addAll(mediaItems)
    }

    override fun removeSongs(mediaItems: ArrayList<MediaItemData>) {
        playList?.removeAll(mediaItems.toSet())
    }

    override fun onPause() {
        log("暂停")
        exoPlayer.pause()
    }

    override fun onPlay() {
        log("播放")
        exoPlayer.play()
        exoPlayer.prepare()
    }

    override fun onPrevious() {
        log("上一曲")
        exoPlayer.seekToPreviousMediaItem()
    }

    override fun onNext() {
        log("下一曲")
        exoPlayer.seekToNextMediaItem()
    }

    override fun onSeekTo(pos: Long) {
        log("播放至：${pos}")
        exoPlayer.seekTo(pos)
    }

    override fun onFavorite() {
        log("喜欢")
    }

    override fun updateMediaInfo(metadataCompat: MediaMetadataCompat) {
        session.setMetadata(metadataCompat)
    }
}