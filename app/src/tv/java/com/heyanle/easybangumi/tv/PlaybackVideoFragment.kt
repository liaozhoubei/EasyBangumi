package com.heyanle.easybangumi.tv


import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.util.Util
import com.heyanle.bangumi_source_api.api.IDetailParser
import com.heyanle.bangumi_source_api.api.IPlayerParser
import com.heyanle.bangumi_source_api.api.entity.Bangumi
import com.heyanle.bangumi_source_api.api.entity.BangumiDetail
import com.heyanle.bangumi_source_api.api.entity.BangumiSummary
import com.heyanle.easybangumi.BangumiApp
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.RelateBangumi
import com.heyanle.easybangumi.db.EasyDB
import com.heyanle.easybangumi.db.entity.BangumiStar
import com.heyanle.easybangumi.source.AnimSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private val UPDATE_DELAY = 16

    private lateinit var mPlayParser: IPlayerParser
    private lateinit var mDetailParser: IDetailParser
    private var mPlayerGlue: VideoPlayerGlue? = null
    private var mPlayerAdapter: LeanbackPlayerAdapter? = null
    private var mPlayer: ExoPlayer? = null
    private var mTrackSelector: TrackSelector? = null
    private var mPlaylistActionListener: PlaylistActionListener? = null
    private var mExoPlayerListener: ExoPlayerListener? = null

    private var mVideo: Bangumi? = null
    private var mBangumiDetail: BangumiDetail? = null
    var mCurrentBangumiStar: BangumiStar? = null

    // 播放视频的地址
    private var mPlayerUrl: String = "";
    private var mPlaylist: Playlist? = null

    //    private var mVideoLoaderCallbacks: VideoLoaderCallbacks? = null
//    private var mVideoCursorAdapter: CursorObjectAdapter? = null

    // 观看路线
    private var mPlayLineIndex = 0

    // 观看集数
    private var mPlayEpisode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mVideo = requireActivity().intent.getSerializableExtra(PlaybackActivity.VIDEO) as Bangumi

        // 播放
        mPlayParser = AnimSourceFactory.play(mVideo!!.source).let {
            if (it == null) {
                requireActivity().finish()
                return
            } else {
                it
            }
        }
        // 获取细节
        mDetailParser = AnimSourceFactory.detail(mVideo!!.source).let {
            if (it == null) {
                requireActivity().finish()
                return
            } else {
                it
            }
        }

        mPlaylist = Playlist()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
        loadVideoData();
    }

    fun loadVideoData() {
        // 1. 先加载视频详情信息
        lifecycleScope.launch(Dispatchers.IO) {
            val summary = BangumiSummary(mVideo!!.id, mVideo!!.source, mVideo!!.detailUrl)
            mDetailParser.detail(summary).complete {
                val detail = it.data
                val bangumiStar: BangumiStar? = EasyDB.database.bangumiStar.getBySourceDetailUrl(
                    detail.id,
                    detail.source,
                    detail.detailUrl
                )
                bangumiStar?.let {
                    detail.star = true
                }
                mPlayerGlue?.setTitle(mVideo!!.name)
                mBangumiDetail = detail
                mPlayLineIndex = detail.lastLine


                if (detail.star) {
                    mCurrentBangumiStar = BangumiStar(
                        bangumiId = detail.id, name = detail.name, cover = detail.cover,
                        source = detail.source, detailUrl = detail.detailUrl,
                        playId = bangumiStar?.playId ?: 0,
                        createTime = bangumiStar?.createTime ?: 0
                    )

                    mPlayEpisode = mCurrentBangumiStar!!.playId
                }
                withContext(Dispatchers.Main) {
                    mPlayerGlue?.star(mBangumiDetail!!.star)
                }
                if (bangumiStar != null) {
                    loadPlayMsg(true)
                } else {
                    loadPlayMsg(false)
                }


            }.error {
                if (it.isParserError) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            BangumiApp.INSTANCE,
                            R.string.source_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                it.throwable.printStackTrace()
//                error()
            }
        }


    }

    // 当前路线和剧集数量
    var mCurrentPlayData: LinkedHashMap<String, List<String>>? = null
    fun loadPlayMsg(star: Boolean) {
        // 2. 加载视频播放信息(多少集)
        lifecycleScope.launch {
            val summary = BangumiSummary(mVideo!!.id, mVideo!!.source, mVideo!!.detailUrl)
            mPlayParser.getPlayMsg(summary).complete {
                val list = it.data.keys.toList();
//                val data:LinkedHashMap<String, List<String>> =it.data;
                for (index in list.indices) {
                    val key = list.get(index)
                    val ls = it.data.get(key)
                }
                mCurrentPlayData = it.data

                withContext(Dispatchers.Main) {
                    try {
                        val mRowsAdapter = initializeRelatedVideosRow(it.data)
                        adapter = mRowsAdapter
                    } catch (e: Exception) {
                        Log.e("PlaybackVideoFragment", "loadPlayMsg: ", e)
                    }
                }
                if (list.isNotEmpty()) {
                    // 当前播放路线
                    val key = list.get(0)
                    // 当前播放路线的剧集
                    val value: List<String>? = it.data[key]
                    if (star) {
                        if (value != null) {
                            val position = mCurrentBangumiStar?.playId ?: (value.size - 1)
                            // 设置为之前看过的那一集
                            mPlayEpisode = position
                        }
                    }

                }

                // 获取播放地址准备播放 getPlayUrl
                loadPlayUrl()
            }.error {
                if (it.isParserError) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            BangumiApp.INSTANCE,
                            R.string.source_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                it.throwable.printStackTrace()
            }

        }
    }

    fun loadPlayUrl() {
        // 3.加载播放地址，开始播放
        lifecycleScope.launch(Dispatchers.IO) {
            val summary = BangumiSummary(mVideo!!.id, mVideo!!.source, mVideo!!.detailUrl)
            // 选集顺序有可能是倒序，主要看网站排序
            mPlayParser.getPlayUrl(summary, mPlayLineIndex, mPlayEpisode)
                .complete {
                    val data: IPlayerParser.PlayerInfo = it.data
                    if (data.uri == "") {
//                        errorVideo()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                BangumiApp.INSTANCE,
                                R.string.source_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        var subtitle = ""
                        var value: List<String>? = null

                        mCurrentPlayData?.let {
                            val list = it.keys.toList();
                            val key = list.get(mPlayLineIndex)
                            value = it[key]
                            // 获取当前播放视频的副标题
                            subtitle = value?.get(mPlayEpisode) ?: ""
                        }
                        mPlayerGlue?.subtitle = subtitle

                        withContext(Dispatchers.Main) {
                            mPlayerUrl = data.uri
                            play(data.uri)
                        }
                    }
                    Log.i("PlaybackVideoFragment", "loadPlayUrl: playUrl ${it}")
                }.error {
                    if (it.isParserError) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                BangumiApp.INSTANCE,
                                R.string.source_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                    it.throwable.printStackTrace()
                }
        }
    }

    override fun onStart() {
        super.onStart()

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

        mPlayer = ExoPlayer.Builder(requireContext()).build()
        mPlayerAdapter = LeanbackPlayerAdapter(requireActivity(), mPlayer!!, UPDATE_DELAY)
        mPlaylistActionListener = PlaylistActionListener(mPlaylist!!)
        mPlayerGlue = VideoPlayerGlue(activity, mPlayerAdapter, mPlaylistActionListener!!)
        mPlayerGlue?.setHost(VideoSupportFragmentGlueHost(this))
        mPlayerGlue?.playWhenPrepared()
        mExoPlayerListener = ExoPlayerListener()
        mPlayer?.addListener(mExoPlayerListener!!)
    }


    private fun releasePlayer() {
        if (mPlayer != null) {
            mCurrentBangumiStar?.let {
                val position = mPlayer!!.getCurrentPosition()
                Thread() {
                    EasyDB.database.bangumiStar.update(it.bangumiId, mPlayEpisode, position)
                }.start()

            }

            mPlayer?.removeListener(mExoPlayerListener!!)
            mPlayer!!.release()
            mPlayer = null
            mTrackSelector = null
            mPlayerGlue = null
            mPlayerAdapter = null
            mPlaylistActionListener = null
            mExoPlayerListener = null
        }
    }

    // 开始播放
    private fun play(url: String) {
//        val ura = "http://v16m-default.akamaized.net/40cd327469af133bcd29c1358e3a23fe/64c593f3/video/tos/alisg/tos-alisg-ve-0051c001-sg/osozp7CchLDTErfwyIZAaNJtZoAmAiBYBIAJwD/?mime_type=video_mp4"
        prepareMediaForPlaying(Uri.parse(url))
        mPlayerGlue?.play()
        mCurrentBangumiStar?.let {
            if (it.playId == mPlayEpisode)
                // 如果看过这一集就跳至上次观看位置
                mPlayer?.seekTo(it.createTime)

        }

    }

    private fun prepareMediaForPlaying(mediaSourceUri: Uri) {
        Log.i("PlaybackVideoFragment", "prepareMediaForPlaying: $mediaSourceUri")
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(mediaSourceUri)
//            .setMediaId(mediaId)
            .setTag("VideoPlayerGlue")
            .build()

        val mediaSource = DefaultMediaSourceFactory(requireContext()).createMediaSource(mediaItem)

//        mPlayer!!.prepare(mediaSource)
        DefaultMediaSourceFactory(requireContext())
//        mPlayer!!.setMediaSource()
        mPlayer!!.setMediaSource(mediaSource)
    }

    private fun initializeRelatedVideosRow(data: LinkedHashMap<String, List<String>>): ArrayObjectAdapter {
        val presenterSelector = ClassPresenterSelector()
        var rowsAdapter: ArrayObjectAdapter = ArrayObjectAdapter()
        try {

            presenterSelector.addClassPresenter(
                mPlayerGlue!!.getControlsRow()::class.java, mPlayerGlue?.getPlaybackRowPresenter()
            )
            presenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
            rowsAdapter = ArrayObjectAdapter(presenterSelector)
            rowsAdapter.add(mPlayerGlue?.getControlsRow())
            val keys: List<String> = data.keys.toList()
            val cardPresenter = GridItemPresenter()
            for (lindIndex in keys.indices) {
                // 获取当前路线
                val key = keys.get(lindIndex)
                Log.i("init", "initializeRelatedVideosRow: ${key}")
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                // 当前路线集数
                val value: List<String>? = data.get(key)
                value?.let {
                    for (index in it.indices) {
                        val relateBangumi = RelateBangumi(key, lindIndex, index, it[index])
                        listRowAdapter.add(relateBangumi)
                        mPlaylist?.add(relateBangumi)
                    }
                    val header = HeaderItem(key)
                    val row = ListRow(header, listRowAdapter)
                    rowsAdapter.add(row)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlaybackVideoFragment", "initializeRelatedVideosRow: ", e)
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
            if (item is RelateBangumi) {
                Log.i("PlaybackVideoFragment", "onItemClicked: item:$item")
                mPlayLineIndex = item.lineIndex
                mPlayerGlue?.subtitle = item.label
                mPlayEpisode = item.id
                loadPlayUrl()
            }
        }
    }

    inner class ExoPlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)

        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (Player.STATE_ENDED == playbackState) {
                mPlaylistActionListener?.onNext()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Toast.makeText(activity, "播放错误：${error.toString()}", Toast.LENGTH_SHORT).show()
            Log.e("PlaybackVideoFragment", "onPlayerError: 播放错误", error)
        }

    }


    inner class PlaylistActionListener(playlist: Playlist) :
        VideoPlayerGlue.OnActionClickedListener {
        private val mPlaylist: Playlist
        override fun onPrevious() {
            if (mPlayEpisode - 1 >= 0) {
                mPlayEpisode -= 1
                loadPlayUrl()
            }
//            Toast.makeText(EasyApplication.INSTANCE, "上一集", Toast.LENGTH_SHORT).show()
        }

        override fun onNext() {
            Log.e(
                "PlaylistActionListener",
                "onNext: ${mPlaylist.size()}  mPlayEpisode=${mPlayEpisode}",
            )
            if (mPlayEpisode + 1 < mPlaylist.size()) {
                mPlayEpisode += 1
                loadPlayUrl()
            }
//            Toast.makeText(EasyApplication.INSTANCE, "下一集", Toast.LENGTH_SHORT).show()
        }

        override fun star() {
            mBangumiDetail?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val star = BangumiStar(
                        bangumiId = it.id,
                        name = it.name,
                        cover = it.cover,
                        source = it.source,
                        detailUrl = it.detailUrl,
                        playId = mPlayEpisode,
                        createTime = it.createTime
                    )
                    if (it.star) {
                        it.star = false
                        // BangumiStar 中有自增长的id ,因此只能先查询在删除
                        val deleteStar = EasyDB.database.bangumiStarDao()
                            .getBySourceDetailUrl(it.id, it.source, it.detailUrl)
                        deleteStar?.let {
                            EasyDB.database.bangumiStarDao().delete(deleteStar)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(BangumiApp.INSTANCE, "取消追番", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        it.star = true
                        EasyDB.database.bangumiStarDao().insert(star)
//                        EasyDB.database.bangumiStarDao().update(it)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(BangumiApp.INSTANCE, "已追番", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        override fun openOutPlayer() {
            Log.e("openOutPlayer", "openOutPlayer: $mVideo")
            if (mPlayerUrl.isNotBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val type = "video/*"
                    val uri = Uri.parse(mPlayerUrl)
                    intent.setDataAndType(uri, type)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "无法使用外部播放器:${e.toString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

        init {
            mPlaylist = playlist
        }

    }

    private inner class GridItemPresenter : Presenter() {
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(
                GRID_ITEM_WIDTH,
                GRID_ITEM_HEIGHT
            )
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {

            (viewHolder.view as TextView).text = (item as RelateBangumi).label
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

}