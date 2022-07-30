package com.heyanle.easybangumi.tv

import java.util.Collections
import java.util.Timer
import java.util.TimerTask

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.db.EasyDatabase
import com.heyanle.easybangumi.entity.Bangumi
import com.heyanle.easybangumi.source.ISourceParser
import com.heyanle.easybangumi.source.SourceParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseSupportFragment() {

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private var starIndex:Int = 0;

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()

        loadRows()

        setupEventListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private fun prepareBackgroundManager() {
        // BackgroundManager 用于管理多个activity 之间连续的背景
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mDefaultBackground = ContextCompat.getDrawable(requireActivity(),
            R.drawable.default_background
        )
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        val labelList = SourceParserFactory.homeLabel()
        title = labelList[1]
        // over title
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color 左边侧滑菜单颜色
        brandColor = ContextCompat.getColor(requireActivity(), R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.search_opaque)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO){

        }
    }

    private fun loadRows() {
        lifecycleScope.launch(Dispatchers.IO) {
            val keyList = SourceParserFactory.homeKeys()
            val result = SourceParserFactory.home("yhdm")?.home();
            if (result is ISourceParser.ParserResult.Complete) {
                var index = 0;
                val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
                val cardPresenter = CardPresenter()

                val data: LinkedHashMap<String, List<Bangumi>> = result.data;
                val keys = data.keys

                val listStarRowAdapter = ArrayObjectAdapter(cardPresenter)
                val detals = EasyDatabase.AppDB.bangumiDetailDao().findStarBangumiDetails()
                for (detal in detals){
                    val bangumi = Bangumi(detal.id,detal.source,detal.detailUrl,detal.name,detal.cover,detal.intro,0)
                    listStarRowAdapter.add(bangumi)
                }

                starIndex = index
                val header = HeaderItem(index.toLong(), "我的追番")
                rowsAdapter.add(ListRow(header, listStarRowAdapter))

                index++
                for (key in keys) {
                    val bangumis: List<Bangumi>? = data.get(key)
                    Log.e("MainActivity", "onCreate: key=${key}",)
                    // 右侧影片信息
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    bangumis?.let {
                        for (bangumi in it) {
                            listRowAdapter.add(bangumi)
                            Log.e(
                                "MainActivity",
                                "onCreate: source=${bangumi} ",
                            )
                        }
                    }
                    val header = HeaderItem(index.toLong(), key)
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                    index++
                }
                withContext(Dispatchers.Main){
                    adapter = rowsAdapter
                }

            }else if (result is ISourceParser.ParserResult.Error){
                result.throwable.printStackTrace()
                Log.e(TAG, "loadRows: ${result.throwable.stackTraceToString()}" )
            }

        }

//        val list = MovieList.list
//
//        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
//        val cardPresenter = CardPresenter()
//
//        for (i in 0 until NUM_ROWS) {
//            if (i != 0) {
//                // 随机排序
//                Collections.shuffle(list)
//            }
//            // 右侧影片信息
//            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
//            for (j in 0 until NUM_COLS) {
//                listRowAdapter.add(list[j % 5])
//            }
//            // 左侧菜单
//            val header = HeaderItem(i.toLong(), MovieList.MOVIE_CATEGORY[i])
//            rowsAdapter.add(ListRow(header, listRowAdapter))
//        }

        // 最后的个人信息
//        val gridHeader = HeaderItem(NUM_ROWS.toLong(), "PREFERENCES")
//
//        val mGridPresenter = GridItemPresenter()
//        val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
//        gridRowAdapter.add(resources.getString(R.string.grid_view))
//        gridRowAdapter.add(getString(R.string.error_fragment))
//        gridRowAdapter.add(resources.getString(R.string.personal_settings))
//        rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))
//
//        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(requireActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                .show()
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {

            if (item is Bangumi) {
                Log.d(TAG, "Item: " + item)
//                val intent = Intent(activity!!, PlaybackActivity::class.java)
//                intent.putExtra(PlaybackActivity.MOVIE, item)
//
//                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                    activity!!,
//                    (itemViewHolder.view as ImageCardView).mainImageView,
//                    DetailsActivity.SHARED_ELEMENT_NAME
//                )
//                    .toBundle()
//                startActivity(intent, bundle)
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.VIDEO, item)
                startActivity(intent)
            } else if (item is String) {
//                if (item.contains(getString(R.string.error_fragment))) {
//                    val intent = Intent(activity!!, BrowseErrorActivity::class.java)
//                    startActivity(intent)
//                } else {
//                    Toast.makeText(activity!!, item, Toast.LENGTH_SHORT).show()
//                }
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            // 更换背景图
//            if (item is Movie) {
//                mBackgroundUri = item.backgroundImageUrl
//                startBackgroundTimer()
//            }
        }
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(requireActivity())
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_ROWS = 6
        private val NUM_COLS = 15
    }
}