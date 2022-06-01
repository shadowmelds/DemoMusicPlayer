package dev.shadowmeld.demomusicplayer.data

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.R
import dev.shadowmeld.demomusicplayer.media.MediaItemData

interface MusicRepository {

    fun getMusicInfo()

    val observerResult: MutableLiveData<List<MediaItemData>>
}

class DefaultMusicInfoRepository(
    private val context: Context?
) : MusicRepository {

    private val result: MutableLiveData<List<MediaItemData>> by lazy {
        MutableLiveData<List<MediaItemData>>()
    }

    override fun getMusicInfo() {
        context?.resources?.openRawResourceFd(R.raw.a1)?.fileDescriptor

        result.value = mutableListOf<MediaItemData>().apply {
            add(MediaItemData(
                context?.resources?.openRawResourceFd(R.raw.a1)?.fileDescriptor,
                "A1",
                "Test",
            ))
            add(MediaItemData(
                context?.resources?.openRawResourceFd(R.raw.a2)?.fileDescriptor,
                "A2",
                "Test",
            ))
            add(MediaItemData(
                context?.resources?.openRawResourceFd(R.raw.a3)?.fileDescriptor,
                "A3",
                "Test",
            ))
        }
    }

    override val observerResult: MutableLiveData<List<MediaItemData>>
        get() = result

}