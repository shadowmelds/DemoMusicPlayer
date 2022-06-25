package dev.shadowmeld.demomusicplayer.ui

import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import dev.shadowmeld.demomusicplayer.databinding.ActivityMainBinding
import dev.shadowmeld.demomusicplayer.util.log
import dev.shadowmeld.demomusicplayer.media.MediaPlaybackService
import com.google.android.material.composethemeadapter.MdcTheme
import dev.shadowmeld.demomusicplayer.media.DefaultMediaController

@ExperimentalMaterialApi
class MainActivity : AppCompatActivity() {

    /**
     * Activity相关
     */
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    /**
     * mediaBrowser 用于连接服务和与服务通信
     */
    private lateinit var mediaBrowser: MediaBrowserCompat

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.description?.let {
                updateShowMediaInfo(it)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            log("Activity播放状态改变：${state?.state}")
            DefaultMediaController.currentMediaState = state?.state ?: PlaybackStateCompat.STATE_NONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.finishCurrentActivity = {
            finish()
        }
        initComposeView()

        // 初始化MediaBrowser
        initMediaBrowser()

    }

    /**
     * 初始化MediaBrowser
     */
    private fun initMediaBrowser() {

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    // MusicService 连接成功
                    mediaBrowser.sessionToken.also { token ->
                        val mediaController = MediaControllerCompat(
                            this@MainActivity,
                            token
                        )
                        MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                    }

                    subscribe()
                    // Finish building the UI
                    buildTransportControls()
                }

                override fun onConnectionSuspended() {
                    log("MusicService 连接挂起")
                }

                override fun onConnectionFailed() {
                    log("MusicService 连接失败")
                }
            },
            null // optional Bundle
        )
    }

    /**
     * mediaBrowser 和mServiceBinderImpl建立联系
     */
    private fun subscribe() {

        mediaBrowser.root.apply {
            mediaBrowser.unsubscribe(this)
            mediaBrowser.subscribe(this, object : MediaBrowserCompat.SubscriptionCallback() {

                override fun onChildrenLoaded(
                    parentId: String,
                    children: List<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    Log.i("TAG", "onChildrenLoaded: parentId=$parentId children=$children")
                    if (children.isNotEmpty()) {
                        updateShowMediaInfo(children[0].description)
                    }
                }

                override fun onError(parentId: String) {
                    super.onError(parentId)
                    Log.i("TAG", "onError: parentId=$parentId")
                }

            })
        }
    }

    fun buildTransportControls() {

        val mediaController = MediaControllerCompat.getMediaController(this)
        viewModel.setPlayCurrentMusic {

            when(it) {

                PlaybackStateCompat.ACTION_PLAY,
                PlaybackStateCompat.ACTION_PAUSE -> {

                    if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                        mediaController.transportControls.pause()
                        log("播放pause")
                    } else {
                        mediaController.transportControls.play()
                        log("播放play")
                    }
                }

                PlaybackStateCompat.ACTION_SKIP_TO_NEXT -> {
                    mediaController.transportControls.skipToNext()
                }

                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS -> {
                    mediaController.transportControls.skipToPrevious()
                }
            }
        }

        // Display the initial state
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    /**
     * 更新显示媒体信息
     */
    private fun updateShowMediaInfo(description: MediaDescriptionCompat) {
        log("更新显示媒体信息")
//        DefaultMediaController.currentSongInfo = DefaultMediaController.playList?.get(description.)
    }


    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }


    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }


    /**
     * 初始化 Compose 界面
     */
    private fun initComposeView() {

        binding.playUi.setContent {
            MdcTheme {
                ParentPlayer(viewModel = viewModel)
            }
        }
    }
}