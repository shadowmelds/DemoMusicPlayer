package dev.shadowmeld.demomusicplayer.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.example.myapplication.R
import com.example.myapplication.logger
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.EventLogger


class MediaPlaybackService : MediaBrowserServiceCompat() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "Daydream Music"
        const val MEDIA_ROOT_ID = "media_root_id"
        const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
    }

    private var mediaSession: MediaSessionCompat? = null

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(
            baseContext,
            MediaPlaybackService::class.java.simpleName
        ).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

            setPlaybackState(
                PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
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
                }
            })
            setSessionToken(sessionToken)
        }
        exoPlayer = ExoPlayer.Builder(baseContext).build()

        initExoPlayerListener(exoPlayer!!)
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

        val musicEntityList = getMusicEntityList()
        for (i in musicEntityList.indices) {
            val musicEntity = musicEntityList[i]
            val metadataCompat: MediaMetadataCompat = buildMediaMetadata(musicEntity)
            if (i == 0) {
                mediaSession!!.setMetadata(metadataCompat)
            }
            mediaItems.add(
                MediaBrowserCompat.MediaItem(
                    metadataCompat.description,
                    MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
            )
            exoPlayer!!.addMediaItem(MediaItem.fromUri(musicEntity.source))
        }
        logger("走到这一步 ${mediaItems.size}")
        result.sendResult(mediaItems)
    }

    private fun createNotificationChannel(channelId: String, name: String) {
        ContextCompat.getSystemService(this, NotificationManager::class.java)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
                createNotificationChannel(channel)
            }
        }
    }

    fun createNotification(context: Context, channelId: String): Notification {

        createNotificationChannel(channelId, "Daydream Player")

        val controller = mediaSession!!.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        return NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(description?.title)
            setContentText(description?.subtitle)
            setSubText(description?.description)
            setLargeIcon(description?.iconBitmap)
            setContentIntent(mediaSession?.controller?.sessionActivity)
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            color = Color.BLACK
            addAction(
                R.drawable.ic_baseline_skip_previous_24,
                "Forward",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )

            // Add a pause button
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_pause_24,
                    "暂停",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
//            addAction(
//                if (exoPlayer?.isPlayingState == true) R.drawable.ic_baseline_pause_24
//                else R.drawable.ic_baseline_play_arrow_24,
//                if (exoPlayer?.isPlayingState == true) "Pause" else "Play",
//                MediaButtonReceiver.buildMediaButtonPendingIntent(
//                    context,
//                    PlaybackStateCompat.ACTION_PLAY_PAUSE
//                )
//            )
            addAction(
                R.drawable.ic_baseline_skip_next_24,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(1)
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

            fun onPlayerError(error: ExoPlaybackException) {
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i("TAG", "onIsPlayingChanged: isPlaying=$isPlaying")
                if (isPlaying) {
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                } else {
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.i("TAG", "onPositionDiscontinuity: reason=$reason")
            }
        })
        exoPlayer.addAnalyticsListener(EventLogger(DefaultTrackSelector()))
    }

    private fun setPlaybackState(playbackState: Int) {
        val speed =
            if (exoPlayer?.playbackParameters == null) 1f else exoPlayer!!.playbackParameters.speed
        mediaSession!!.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(playbackState, exoPlayer!!.currentPosition, speed).build()
        )
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            logger("MediaSessionCallback Play() 执行 exoPlayer = ${exoPlayer == null}")
            super.onPlay()
            exoPlayer?.play()
            exoPlayer?.prepare()
            //资源加载后立即播放
            exoPlayer?.playWhenReady = true
        }

        override fun onPause() {
            super.onPause()
            exoPlayer?.pause()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            exoPlayer?.seekTo(pos)
        }
    }

    private fun getMusicEntityList(): List<MusicEntity> {

        return mutableListOf<MusicEntity>().apply {
            add(
                MusicEntity(
                id = "wake_up_02",
                title = "Geisha",
                album = "Wake Up",
                artist = "Media Right Productions",
                genre = "Electronic",
                source = "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/02_-_Geisha.mp3",
                image = "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
                trackNumber = 2,
                totalTrackCount = 13,
                duration = 267,
                site = "http://freemusicarchive.org/music/The_Kyoto_Connection/Wake_Up_1957/"
            )
            )
        }
    }

    private fun buildMediaMetadata(musicEntity: MusicEntity): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicEntity.id)
            .putString("__SOURCE__", musicEntity.source)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, musicEntity.album)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicEntity.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, musicEntity.duration.toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, musicEntity.genre)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, musicEntity.image)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicEntity.title)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                musicEntity.trackNumber.toLong()
            )
            .putLong(
                MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                musicEntity.totalTrackCount.toLong()
            )
            .build()
    }

}