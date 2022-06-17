package dev.shadowmeld.demomusicplayer.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.EventLogger
import dev.shadowmeld.demomusicplayer.R
import dev.shadowmeld.demomusicplayer.data.DefaultMusicInfoRepository
import dev.shadowmeld.demomusicplayer.data.MusicRepository
import dev.shadowmeld.demomusicplayer.util.logger


class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "Daydream Music"
        const val MEDIA_ROOT_ID = "media_root_id"
        const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        const val ACTION_FAVORITE: Long = 1 shl 111
    }

    /**
     * EXO播放器
     */
    private lateinit var exoPlayer: ExoPlayer

    /**
     * 数据源
     */
    private lateinit var musicInfoRepository: MusicRepository

    override fun onCreate() {
        super.onCreate()

        // 获取播放列表
        musicInfoRepository = DefaultMusicInfoRepository(this)
        musicInfoRepository.getMusicInfo()
        Media.playList = musicInfoRepository.observerResult.value

        // 初始化 MediaSession
        initMediaSession()
        // 初始化 ExoPlayer
        exoPlayer = ExoPlayer.Builder(baseContext).build()
        // 给ExoPlayer添加监听
        initExoPlayerListener(exoPlayer)
    }

    /**
     * 初始化 MediaSession
     */
    private fun initMediaSession() {
        Media.session = MediaSessionCompat(
            baseContext,
            MediaPlaybackService::class.java.simpleName
        ).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

            setPlaybackState(
                PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SEEK_TO).build()
            )

            setCallback(MediaSessionCallback())
            controller?.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    logger("state = ${state?.state}")
                    if (state?.state == PlaybackStateCompat.STATE_PLAYING || state?.state == PlaybackStateCompat.STATE_PAUSED || state?.state == PlaybackStateCompat.STATE_NONE) {
                        logger("启动通知")

                        startForeground(1, createNotification(baseContext, NOTIFICATION_CHANNEL_ID))
                    }

                    logger("Service播放状态改变：${state?.state}")
                    Media.currentMediaState = state?.state
                }


            })
            setSessionToken(sessionToken)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(getString(R.string.app_name), null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        // 不允许浏览
        if (EMPTY_MEDIA_ROOT_ID == parentId) {
            result.sendResult(null)
            logger("不允许浏览")
            return
        }
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        Media.playList?.let {
            for (musicEntity in it.entries) {
                val metadataCompat: MediaMetadataCompat = buildMediaMetadata(musicEntity)
                Media.session!!.setMetadata(metadataCompat)
                mediaItems.add(
                    MediaBrowserCompat.MediaItem(
                        metadataCompat.description,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
                )
                exoPlayer.addMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(musicEntity.value.source)))
            }
            logger("走到这一步 ${mediaItems.size}")
        }
        result.sendResult(mediaItems)
    }

    /**
     * BroadcastReceiver 会将 Intent 转发给您的服务，键码转换为相应的会话回调方法
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(Media.session, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    fun createNotification(context: Context, channelId: String): Notification {

        createNotificationChannel(channelId, "Daydream Player")

        val controller = Media.session!!.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        return NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(description?.title)
            setContentText(description?.subtitle)
            setSubText(description?.description)
            setLargeIcon(description?.iconBitmap)
            setContentIntent(Media.session?.controller?.sessionActivity)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            color = Color.BLACK
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_previous_24,
                    "Forward",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )

            )
            addAction(
                NotificationCompat.Action(
                    if (Media.currentMediaState == PlaybackStateCompat.STATE_PLAYING) R.drawable.ic_baseline_pause_24
                    else R.drawable.ic_baseline_play_arrow_24,
                    if (Media.currentMediaState == PlaybackStateCompat.STATE_PLAYING) "Pause" else "Play",
                    if (Media.currentMediaState == PlaybackStateCompat.STATE_PLAYING) MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                    else MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY,
                    )
                )
            )
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_next_24,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_round_favorite_border_24,
                    "Favorite",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        ACTION_FAVORITE
                    )
                )
            )
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(Media.session?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        }.build()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel(channelId: String, name: String) {
        ContextCompat.getSystemService(this, NotificationManager::class.java)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
                createNotificationChannel(channel)
            }
        }
    }

    private fun initExoPlayerListener(exoPlayer: ExoPlayer) {
        exoPlayer.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                val currentPosition = exoPlayer.currentPosition
                val duration = exoPlayer.duration

                //状态改变（播放器内部发生状态变化的回调，
                // 包括
                // 1. 用户触发的  比如： 手动切歌曲、暂停、播放、seek等；
                // 2. 播放器内部触发 比如： 播放结束、自动切歌曲等）

                //该如何通知给ui业务层呐？？好些只能通过回调
                //那有该如何 --》查看源码得知通过setPlaybackState设置
                Log.i(
                    "TAG",
                    "onPlaybackStateChanged: currentPosition=$currentPosition duration=$duration state=$state"
                )
                val playbackState: Int
                playbackState = when (state) {
                    Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
                    Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                    Player.STATE_READY -> if (exoPlayer.playWhenReady) {
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        PlaybackStateCompat.STATE_PAUSED
                    }
                    Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                    else -> PlaybackStateCompat.STATE_NONE
                }
                //播放器的状态变化，通过mediasession告诉在ui业务层注册的MediaControllerCompat.Callback进行回调
                setPlaybackState(playbackState)
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Log.i("TAG", "onPlayerError: error=" + error.message)
                setPlaybackState(PlaybackStateCompat.STATE_ERROR)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i("TAG", "onIsPlayingChanged: isPlaying=$isPlaying")
                if (isPlaying) {
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }
        })
        exoPlayer.addAnalyticsListener(EventLogger(DefaultTrackSelector()))
    }

    private fun setPlaybackState(playbackState: Int) {
        val speed = exoPlayer.playbackParameters.speed
        Media.session!!.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(playbackState, exoPlayer.currentPosition, speed).build()
        )
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            logger("MediaSessionCallback Play() 执行")
            super.onPlay()
            exoPlayer.play()
            exoPlayer.prepare()
            //资源加载后立即播放
            exoPlayer.playWhenReady = true
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            logger("下一曲")
            exoPlayer.seekToNextMediaItem()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            logger("上一曲")
            exoPlayer.seekToPreviousMediaItem()
        }

        override fun onPause() {
            super.onPause()
            exoPlayer.pause()
            logger("onPause ${Media.currentMediaState == PlaybackStateCompat.STATE_PLAYING}")
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            exoPlayer.seekTo(pos)
        }
    }

    /**
     * 单曲数据 MediaItemData to MediaMetadataCompat
     */
    private fun buildMediaMetadata(musicEntity: Map.Entry<String, MediaItemData>): MediaMetadataCompat {

        val mediaItemData = musicEntity.value

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicEntity.key)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mediaItemData.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaItemData.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaItemData.duration.toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaItemData.title)
            .build()
    }

}