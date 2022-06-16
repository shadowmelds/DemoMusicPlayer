package dev.shadowmeld.demomusicplayer.ui

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.ViewModel
import dev.shadowmeld.demomusicplayer.data.DefaultMusicInfoRepository
import dev.shadowmeld.demomusicplayer.data.MusicRepository
import dev.shadowmeld.demomusicplayer.media.MediaItemData

class MainViewModel : ViewModel() {

    private lateinit var musicInfoRepository: MusicRepository

    fun setMusicInfoRepository(context: Context) {
        musicInfoRepository = DefaultMusicInfoRepository(context)
    }

    var playbackAction: ((Long) -> Unit)? = null

    fun setPlayCurrentMusic(
        playbackAction: (Long) -> Unit
    ) {
        this.playbackAction = playbackAction
    }

    fun getMusicInfo(): Map<String, MediaItemData>? {
        musicInfoRepository.getMusicInfo()
        return musicInfoRepository.observerResult.value
    }

    var finishCurrentActivity: (() -> Unit)? = null

    fun finishCurrentActivity() {
        finishCurrentActivity?.invoke()
    }
}