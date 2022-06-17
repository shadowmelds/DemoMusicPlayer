package dev.shadowmeld.demomusicplayer.media
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.media.session.MediaButtonReceiver
import dev.shadowmeld.demomusicplayer.util.logger

class MediaButtonReceiver : MediaButtonReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        when (val keyCode = (intent?.extras?.get("android.intent.extra.KEY_EVENT") as KeyEvent).keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                logger("媒体组件点击：PLAY_PAUSE")
                Media.session?.controller?.transportControls?.pause()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                logger("媒体组件点击：PLAY")
                Media.session?.controller?.transportControls?.play()
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                logger("媒体组件点击：NEXT")
                Media.session?.controller?.transportControls?.skipToNext()
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                logger("媒体组件点击：PREVIOUS")
                Media.session?.controller?.transportControls?.skipToPrevious()
            }
            MediaPlaybackService.ACTION_FAVORITE.toInt() -> {
                logger("媒体组件点击：喜欢这首歌")
            }
            else -> {
                logger("媒体组件点击：${keyCode}")
            }
        }
    }
}