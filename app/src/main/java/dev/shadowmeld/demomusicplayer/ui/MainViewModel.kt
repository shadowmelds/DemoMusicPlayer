package dev.shadowmeld.demomusicplayer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.shadowmeld.demomusicplayer.data.DefaultMusicInfoRepository
import dev.shadowmeld.demomusicplayer.data.MusicRepository
import dev.shadowmeld.demomusicplayer.media.MediaItemData

class MainViewModel : ViewModel() {

    var currentMusic: ((musicData: LocalMusicInfo) -> Unit)? = null

    private lateinit var musicInfoRepository: MusicRepository

    fun setMusicInfoRepository(context: Context) {
        musicInfoRepository = DefaultMusicInfoRepository(context)
    }

    fun setUpdateCurrentMusic(
        currentMusic: (musicData: LocalMusicInfo) -> Unit
    ) {
        this.currentMusic = currentMusic
    }

    var playButton: (() -> Unit)? = null

    fun setPlayCurrentMusic(
        playButton: () -> Unit
    ) {
        this.playButton = playButton
    }

    fun getMusicInfo(): List<MediaItemData>? {
        musicInfoRepository.getMusicInfo()
        return musicInfoRepository.observerResult.value
    }

    var finishCurrentActivity: (() -> Unit)? = null

    fun finishCurrentActivity() {
        finishCurrentActivity?.invoke()
    }
}