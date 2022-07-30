package com.heyanle.easybangumi.tv


import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.util.Util
import com.heyanle.easybangumi.EasyApplication
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.db.EasyDatabase
import com.heyanle.easybangumi.entity.Bangumi
import com.heyanle.easybangumi.entity.BangumiDetail
import com.heyanle.easybangumi.source.IDetailParser
import com.heyanle.easybangumi.source.IPlayerParser
import com.heyanle.easybangumi.source.SourceParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.LinkedHashMap


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private val UPDATE_DELAY = 16

    private lateinit var mPlayParser: IPlayerParser
    private lateinit var mDetailParser: IDetailParser
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    private var mPlayer: SimpleExoPlayer? = null
    private var mTrackSelector: TrackSelector? = null
    private var mPlaylistActionListener: PlaylistActionListener? = null

    private var mVideo: Bangumi? = null
    private var mBangumiDetail: BangumiDetail?= null
    private var mPlaylist: Playlist? = null
//    private var mVideoLoaderCallbacks: VideoLoaderCallbacks? = null
    private var mVideoCursorAdapter: CursorObjectAdapter? = null
    // 观看路线
    private var mPlayLineIndex = 0
    // 观看集数
    private var mPlayEpisode = 0
    private var mPlayUrl: Array<Array<String>> = emptyArray()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mVideo = requireActivity().intent.getSerializableExtra(PlaybackActivity.VIDEO) as Bangumi

        // 播放
        mPlayParser = SourceParserFactory.play(mVideo!!.source).let {
            if(it == null){
                requireActivity().finish()
                return
            }else{
                it
            }
        }
        // 获取细节
        mDetailParser = SourceParserFactory.detail(mVideo!!.source).let {
            if(it == null){
                requireActivity().finish()
                return
            }else{
                it
            }
        }

        mPlaylist = Playlist()
//        mVideoLoaderCallbacks = VideoLoaderCallbacks(mPlaylist)

        // Loads the playlist.
//        val args = Bundle()
//        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mVideo.category)
//        loaderManager
//            .initLoader(
//                VideoLoaderCallbacks.QUEUE_VIDEOS_LOADER, args,
//                mVideoLoaderCallbacks!!
//            )
//        mVideoCursorAdapter = setupRelatedVideosCursor()
        loadVideoData();
    }

    fun loadVideoData(){
        // 1. 先加载视频详情信息
        lifecycleScope.launch {
            mDetailParser.detail(mVideo!!).complete {
                Log.e("PlaybackVideoFragment", "onCreate:detail ${it.data}", )
                val detail = it.data

                EasyDatabase.AppDB.bangumiDetailDao().findBangumiDetailById(detail.id).forEach { b ->
                    detail.lastProcessTime = b.lastProcessTime
                    detail.lastLine = b.lastLine
                    detail.lastEpisodes = b.lastEpisodes
                    detail.star = b.star
                    detail.lastVisiTime = System.currentTimeMillis()
                    detail.cover = b.cover
                    detail.name = b.name
                    detail.intro = b.intro
                    detail.description = b.description
                }
                mBangumiDetail = detail
                mPlayLineIndex = detail.lastLine
                mPlayEpisode = detail.lastEpisodes
                if(detail.star){
                    EasyDatabase.AppDB.bangumiDetailDao().update(detail)
                }
                withContext(Dispatchers.Main){
                    mPlayerGlue?.star(mBangumiDetail!!.star)
                }
                loadPlayMsg()

            }.error {
                if (it.isParserError){
                     withContext(Dispatchers.Main) {
                        Toast.makeText(EasyApplication.INSTANCE, R.string.source_error, Toast.LENGTH_SHORT).show()
                    }
                }
                it.throwable.printStackTrace()
//                error()
            }
        }





    }

    fun loadPlayMsg(){
        // 2. 加载视频播放信息(多少集)
        lifecycleScope.launch {
            mPlayParser.getPlayMsg(mVideo!!).complete {
                val list = it.data.keys.toList();
//                val data:LinkedHashMap<String, List<String>> =it.data;
                for (index in list.indices){
                    val key = list.get(index)
                    val ls = it.data.get(key)
                    Log.e("PlaybackVideoFragment", "onCreate:playMsg key ${key} ${ls}",)
//                    ls?.let {
//                        for(string in ls){
//                            Log.e("PlaybackVideoFragment", "onCreate:playMsg value ${string}",)
//                        }
//                    }
                }
                mPlayUrl = Array(it.data.size){ po ->
                    val li = it.data[list[po]]?: emptyList()
                    Array(li.size){""}
                }
                withContext(Dispatchers.Main){
                    try {
                        val mRowsAdapter = initializeRelatedVideosRow(it.data)
                        adapter = mRowsAdapter
                    } catch (e: Exception) {
                        Log.e("PlaybackVideoFragment", "loadPlayMsg: ", e)
                    }
                }
                // 获取播放地址准备播放 getPlayUrl
                loadPlayUrl()
            }.error {
                if (it.isParserError){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(EasyApplication.INSTANCE, R.string.source_error, Toast.LENGTH_SHORT).show()
                    }
                }
                it.throwable.printStackTrace()
            }

        }
    }

    fun loadPlayUrl(){
        // 3.加载播放地址，开始播放
        lifecycleScope.launch {
            // 选集顺序有可能是倒序，主要看网站排序
            mPlayParser.getPlayUrl(mVideo!!, mPlayLineIndex, mPlayEpisode)
                .complete {
                    if(it.data == ""){
//                        errorVideo()
                        withContext(Dispatchers.Main){
                            Toast.makeText(EasyApplication.INSTANCE, R.string.source_error, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            mPlayUrl[mPlayLineIndex][mPlayEpisode] = it.data
                            mPlayerGlue?.setTitle(mVideo!!.name)
                            mPlayerGlue?.setSubtitle("第${mPlayEpisode}集")
                            play(it.data)
                        }
                    }
                    Log.e("PlaybackVideoFragment", "onCreate: playUrl ${it}", )
                }.error {
                    if(it.isParserError){
                        withContext(Dispatchers.Main){
                            Toast.makeText(EasyApplication.INSTANCE, R.string.source_error, Toast.LENGTH_SHORT).show()
                        }

                    }
                    it.throwable.printStackTrace()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || mPlayer == null) {
            initializePlayer()
        }
    }

    /** Pauses the player.  */
    @TargetApi(Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        if (mPlayerGlue != null && mPlayerGlue!!.isPlaying()) {
            mPlayerGlue!!.pause()
        }
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
//        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
//        val videoTrackSelectionFactory: TrackSelection.Factory =
//            AdaptiveTrackSelection.Factory(bandwidthMeter)
//        mTrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        mPlayer = SimpleExoPlayer.Builder(requireContext()).build()
//        mPlayer = ExoPlayer.Builder(requireContext()).build()
//        mPlayer = ExoPlayerFactory.newSimpleInstance(activity, mTrackSelector)
        mPlayerAdapter = LeanbackPlayerAdapter(requireActivity(), mPlayer!!, UPDATE_DELAY)
        mPlaylistActionListener = PlaylistActionListener(mPlaylist!!)
        mPlayerGlue = VideoPlayerGlue(activity, mPlayerAdapter, mPlaylistActionListener!!)
        mPlayerGlue?.setHost(VideoSupportFragmentGlueHost(this))
        mPlayerGlue?.playWhenPrepared()
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            mPlayer!!.release()
            mPlayer = null
            mTrackSelector = null
            mPlayerGlue = null
            mPlayerAdapter = null
            mPlaylistActionListener = null
        }
    }
    // 开始播放
    private fun play(url: String) {
        prepareMediaForPlaying(Uri.parse(url))
        mPlayerGlue?.play()
    }

    private fun prepareMediaForPlaying(mediaSourceUri: Uri) {
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(mediaSourceUri)
//            .setMediaId(mediaId)
            .setTag("VideoPlayerGlue")
            .build()
        val mediaSource: MediaSource = DefaultMediaSourceFactory(requireContext()).createMediaSource(mediaItem)

//        mPlayer!!.prepare(mediaSource)
        mPlayer!!.setMediaSource(mediaSource)
    }

    private fun initializeRelatedVideosRow(data:LinkedHashMap<String, List<String>>): ArrayObjectAdapter {
        val presenterSelector = ClassPresenterSelector()
        var rowsAdapter: ArrayObjectAdapter =ArrayObjectAdapter()
        try {

            presenterSelector.addClassPresenter(
                mPlayerGlue!!.getControlsRow()::class.java, mPlayerGlue?.getPlaybackRowPresenter()
            )
            presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
            rowsAdapter = ArrayObjectAdapter(presenterSelector)
            rowsAdapter.add(mPlayerGlue?.getControlsRow())
            val keys = data.keys

            val cardPresenter = CardPresenter()
            for (key in keys){
                Log.e("init", "initializeRelatedVideosRow: ${key}", )
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                val value:List<String>? = data.get(key)
                value?.let {
                    for (index in it.indices){
                        val string = it[index]
                        val bangumi = Bangumi(index.toString(),mVideo!!.source,mVideo!!.detailUrl,mVideo!!.name,mVideo!!.cover,string,0)
                        mPlaylist?.add(bangumi)
                        listRowAdapter.add(bangumi)
                    }
                    val header = HeaderItem(key)
                    val row = ListRow(header, listRowAdapter)
                    rowsAdapter.add(row)
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackVideoFragment", "initializeRelatedVideosRow: ",e )
        }



        setOnItemViewClickedListener(ItemViewClickedListener())
        return rowsAdapter
    }


    fun skipToNext() {
        mPlayerGlue?.next()
    }

    fun skipToPrevious() {
        mPlayerGlue?.previous()
    }

    fun rewind() {
        mPlayerGlue?.rewind()
    }

    fun fastForward() {
        mPlayerGlue?.fastForward()
    }

    /** Opens the video details page when a related video has been clicked.  */
    inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is Bangumi) {
                val video: Bangumi = item as Bangumi
                Log.e("ItemViewClickedListener", "onItemClicked: ${adapter.size()}", )
                Log.e("ItemViewClickedListener", "onItemClicked: ${adapter.get(0)}", )
                Log.e("ItemViewClickedListener", "onItemClicked: ${item}", )
                Log.e("ItemViewClickedListener", "onItemClicked: ${row}", )
                val id = item.id.toIntOrNull()
                if (id!=null){
                    mPlayEpisode= id
                    loadPlayUrl()
                }
            }
        }
    }


    inner class PlaylistActionListener(playlist: Playlist) :
        VideoPlayerGlue.OnActionClickedListener {
        private val mPlaylist: Playlist
        override fun onPrevious() {
            if (mPlayEpisode-1>= 0){
                mPlayEpisode -= 1
                loadPlayUrl()
            }
//            Toast.makeText(EasyApplication.INSTANCE, "上一集", Toast.LENGTH_SHORT).show()
        }

        override fun onNext() {
            Log.e("PlaylistActionListener", "onNext: ${mPlaylist.size()}  mPlayEpisode=${mPlayEpisode}", )
            if (mPlayEpisode+1 < mPlaylist.size()){
                mPlayEpisode+=1
                loadPlayUrl()
            }
//            Toast.makeText(EasyApplication.INSTANCE, "下一集", Toast.LENGTH_SHORT).show()
        }

        override fun star() {
            mBangumiDetail?.let {
                lifecycleScope.launch {
                    if (it.star){
                        it.star = false
                        EasyDatabase.AppDB.bangumiDetailDao().delete(it)
                        withContext(Dispatchers.Main){
                            Toast.makeText(EasyApplication.INSTANCE, "取消追番", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        it.star = true
                        EasyDatabase.AppDB.bangumiDetailDao().insert(it)
                        EasyDatabase.AppDB.bangumiDetailDao().update(it)
                        withContext(Dispatchers.Main){
                            Toast.makeText(EasyApplication.INSTANCE, "已追番", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        init {
            mPlaylist = playlist
        }

    }
}