package dev.shadowmeld.demomusicplayer.ui

import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.logger
import dev.shadowmeld.demomusicplayer.media.MediaPlaybackService
import com.google.android.material.composethemeadapter.MdcTheme

@ExperimentalMaterialApi
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initComposeView()

        // 初始化MediaBrowser
        initMediaBrowser()

    }

    /**
     * 初始化MediaBrowser
     */
    private fun initMediaBrowser() {

        val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                // MusicService 连接成功
                mediaBrowser.sessionToken.also { token ->
                    val mediaController = MediaControllerCompat(
                        this@MainActivity,
                        token
                    )
//                    mediaController.transportControls.playFromMediaId(R.raw.overture.toString(), null)

                    MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                }

                subscribe()
                // Finish building the UI
                buildTransportControls()
            }

            override fun onConnectionSuspended() {
                logger("MusicService 连接挂起")
            }

            override fun onConnectionFailed() {
                logger("MusicService 连接失败")
            }
        }

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            null // optional Bundle
        )
    }

    private fun subscribe() {
        val mediaId = mediaBrowser.root
        mediaBrowser.unsubscribe(mediaId)
        if (mSubscriptionCallback == null) {
            //mediaBrowser 和mServiceBinderImpl建立联系
            mSubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: List<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    Log.i("TAG", "onChildrenLoaded: parentId=$parentId children=$children")
                    if (children != null && children.size > 0) {
//                        updateShowMediaInfo(children[0].description)
                    }
                }

                override fun onError(parentId: String) {
                    super.onError(parentId)
                    Log.i("TAG", "onError: parentId=$parentId")
                }
            }
        }
        mediaBrowser.subscribe(mediaId, mSubscriptionCallback!!)
    }

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        // Grab the view for the play/pause button
//        binding.play.apply {
//            setOnClickListener {
//                // Since this is a play/pause button, you'll need to test the current state
//                // and choose the action accordingly
//
//            }
//        }

        viewModel.setPlayCurrentMusic {

            val pbState = mediaController.playbackState.state

            logger("当前播放状态 $pbState")
            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                mediaController.transportControls.pause()
                logger("播放pause")
            } else {
                mediaController.transportControls.play()
                logger("播放play")
            }
        }

        // Display the initial state
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
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

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {}

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}
    }


    private fun initComposeView() {

        binding.playUi.setContent {
            MdcTheme {
                ParentPlayer(viewModel = viewModel)
            }
        }
    }
}