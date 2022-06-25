package dev.shadowmeld.demomusicplayer.media
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media.session.MediaButtonReceiver
import dev.shadowmeld.demomusicplayer.util.log

class MediaButtonReceiver : MediaButtonReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        when (val keyCode = (intent?.extras?.get("android.intent.extra.KEY_EVENT") as KeyEvent).keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                log("媒体组件点击：PLAY_PAUSE")
                DefaultMediaController.session.controller?.transportControls?.pause()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                log("媒体组件点击：PLAY")
                DefaultMediaController.session.controller?.transportControls?.play()
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                log("媒体组件点击：NEXT")
                DefaultMediaController.session.controller?.transportControls?.skipToNext()
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                log("媒体组件点击：PREVIOUS")
                DefaultMediaController.session.controller?.transportControls?.skipToPrevious()
            }
            MediaPlaybackService.ACTION_FAVORITE.toInt() -> {
                log("媒体组件点击：喜欢这首歌")
            }
            else -> {
                log("媒体组件点击：${keyCode}")
            }
        }
    }
}