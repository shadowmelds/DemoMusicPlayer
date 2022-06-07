package dev.shadowmeld.demomusicplayer.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.R
import dev.shadowmeld.demomusicplayer.media.MediaItemData
import java.io.FileDescriptor

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

        result.value = mutableListOf<MediaItemData>().apply {
            context?.let { add(musicInfo(it, Uri.parse("android.resource://"+ it.packageName +"/raw/a1"), R.raw.a1))}
            context?.let { add(musicInfo(it, Uri.parse("android.resource://"+ it.packageName +"/raw/a2"), R.raw.a2))}
            context?.let { add(musicInfo(it, Uri.parse("android.resource://"+ it.packageName +"/raw/a3"), R.raw.a3))}
        }
    }

    override val observerResult: MutableLiveData<List<MediaItemData>>
        get() = result

    private fun musicInfo(context: Context, media: Uri, resource: Int): MediaItemData {
        val mediaMetaData = MediaMetadataRetriever()
        mediaMetaData.setDataSource(context, media)
        val cover = mediaMetaData.embeddedPicture
        return MediaItemData(
            media,
            resource,
            mediaMetaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "没有标题信息",
            mediaMetaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "没有音乐人信息",
            mediaMetaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "没有专辑信息",
            cover?.size?.let { BitmapFactory.decodeByteArray(cover, 0, it) },
            mediaMetaData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0,
        )
    }

}