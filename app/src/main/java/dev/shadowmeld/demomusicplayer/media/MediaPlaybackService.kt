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
import dev.shadowmeld.demomusicplayer.util.log


class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "Daydream Music"
        const val MEDIA_ROOT_ID = "media_root_id"
        const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
        const val ACTION_FAVORITE: Long = 1 shl 111
    }


    override fun onCreate() {
        super.onCreate()

        // 获取播放列表
        setPlayList(baseContext)

        // 初始化 ExoPlayer
        DefaultMediaController.initExoPlayer(baseContext)

        // 初始化 MediaSession
        initMediaSession()

        // 给ExoPlayer 添加状态监听
        initExoPlayerListener(DefaultMediaController.exoPlayer)
    }

    /**
     * 设置播放列表数据
     */
    private fun setPlayList(context: Context) {
        DefaultMediaController.musicInfoRepository = DefaultMusicInfoRepository(context)
        DefaultMediaController.musicInfoRepository.getMusicInfo()
        DefaultMediaController.playList = DefaultMediaController.musicInfoRepository.observerResult.value
    }

    /**
     * 初始化 MediaSession
     */
    private fun initMediaSession() {
        DefaultMediaController.session = MediaSessionCompat(
            baseContext,
            MediaPlaybackService::class.java.simpleName
        ).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

            setPlaybackState(
                PlaybackStateCompat.Builder().setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT).build()
            )
            setCallback(MediaSessionCallback())
            controller?.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    DefaultMediaController.currentMediaState = state?.state ?: PlaybackStateCompat.STATE_NONE
                    log("Service播放状态改变：${state?.state}， ${DefaultMediaController.currentMediaState == PlaybackStateCompat.STATE_PLAYING}")

                    if (state?.state == PlaybackStateCompat.STATE_PLAYING || state?.state == PlaybackStateCompat.STATE_PAUSED || state?.state == PlaybackStateCompat.STATE_NONE) {
                        startForeground(1, createNotification(baseContext, NOTIFICATION_CHANNEL_ID))
                    }
                }
            })
            setSessionToken(sessionToken)

            isActive = true
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(getString(R.string.app_name), null)
    }

    private val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        // 不允许浏览
        if (EMPTY_MEDIA_ROOT_ID == parentId) {
            result.sendResult(null)
            log("不允许浏览")
            return
        }

        DefaultMediaController.playList?.let {
            for ((index, musicEntity) in it.withIndex()) {
                val metadataCompat: MediaMetadataCompat = buildMediaMetadata(musicEntity, index)
                DefaultMediaController.updateMediaInfo(buildMediaMetadata(musicEntity, index))

                mediaItems.add(
                    MediaBrowserCompat.MediaItem(
                        metadataCompat.description,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
                )
                DefaultMediaController.exoPlayer.addMediaItem(MediaItem.Builder().setMediaId(index.toString()).setUri(RawResourceDataSource.buildRawResourceUri(musicEntity.source)).build())
            }
            log("走到这一步 ${mediaItems.size}")
        }
        result.sendResult(mediaItems)
    }

    /**
     * BroadcastReceiver 会将 Intent 转发给您的服务，键码转换为相应的会话回调方法
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(DefaultMediaController.session, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    fun createNotification(context: Context, channelId: String): Notification {

        createNotificationChannel(channelId, "Daydream Player")

        val controller = DefaultMediaController.session.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        return NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(description?.title)
            setContentText(description?.subtitle)
            setSubText(description?.description)
            setLargeIcon(description?.iconBitmap)
            setContentIntent(DefaultMediaController.session.controller?.sessionActivity)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_round_play_arrow)
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
                if (DefaultMediaController.currentMediaState == PlaybackStateCompat.STATE_PLAYING) {
                    NotificationCompat.Action(
                        R.drawable.ic_baseline_pause_24,
                        "Pause",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                } else {
                    NotificationCompat.Action(
                        R.drawable.ic_baseline_play_arrow_24,
                        "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_PLAY,
                        )
                    )
                }

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
                    .setMediaSession(DefaultMediaController.session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

            color = Color.BLACK
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

                Log.i(
                    "TAG",
                    "onPlaybackStateChanged: currentPosition=$currentPosition duration=$duration state=$state"
                )

                val playbackState: Int = when (state) {
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

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                newPosition.mediaItem?.mediaId?.toInt()?.let {
                    DefaultMediaController.playList?.get(it)?.let { mediaItem ->
                        DefaultMediaController.updateMediaInfo(buildMediaMetadata(mediaItem, it))
                        DefaultMediaController.currentSongInfo = mediaItem
                    }

                }
            }
        })
        exoPlayer.addAnalyticsListener(EventLogger(DefaultTrackSelector()))
    }

    private fun setPlaybackState(playbackState: Int) {
        val speed = DefaultMediaController.exoPlayer.playbackParameters.speed
        DefaultMediaController.session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(playbackState, DefaultMediaController.exoPlayer.currentPosition, speed).setActions(PlaybackStateCompat.ACTION_SEEK_TO).build()
        )
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            DefaultMediaController.onPlay()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            DefaultMediaController.onNext()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            DefaultMediaController.onPrevious()
        }

        override fun onPause() {
            super.onPause()
            DefaultMediaController.onPause()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            DefaultMediaController.onSeekTo(pos)
        }
    }

    /**
     * 单曲数据 MediaItemData to MediaMetadataCompat
     */
    private fun buildMediaMetadata(musicEntity: MediaItemData, index: Int): MediaMetadataCompat {

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, index.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicEntity.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicEntity.artist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, musicEntity.image)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicEntity.duration.toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicEntity.title)
            .build()
    }
}