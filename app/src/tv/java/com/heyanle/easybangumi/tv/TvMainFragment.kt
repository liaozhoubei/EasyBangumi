package com.heyanle.easybangumi.tv

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.heyanle.bangumi_source_api.api.IHomeParser
import com.heyanle.bangumi_source_api.api.ISourceParser
import com.heyanle.bangumi_source_api.api.entity.Bangumi
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.db.EasyDB
import com.heyanle.easybangumi.source.AnimSourceFactory
import com.heyanle.easybangumi.source.AnimSourceLibrary
import com.heyanle.easybangumi.source.AnimSources
import com.heyanle.easybangumi.utils.loge
import com.heyanle.lib_anim.yhdm.YhdmParser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Loads a grid of cards with movies to browse.
 */
class TvMainFragment : BrowseSupportFragment() {

    private val mHandler = Handler(Looper.myLooper()!!)
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private var starIndex: Int = 0;
    private lateinit var sharedPreferences: SharedPreferences

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        prepareBackgroundManager()

        setupUIElements()

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
        mDefaultBackground = ContextCompat.getDrawable(
            requireActivity(),
            R.drawable.default_background
        )
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
//        val labelList = SourceParserFactory.homeLabel()

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

        lifecycleScope.launch(Dispatchers.IO) {
            val parser = AnimSourceFactory.parsers()
            parser.collectLatest() { animSources ->
                val parsers = animSources.homeParsers()

                if (parsers.isNotEmpty()) {
                    var key = sharedPreferences.getString(getString(R.string.pref_key_source), null)
                    if (key == null) {
                        val edit = sharedPreferences.edit()
                        val parser = AnimSourceFactory.home("yhdm")
                        key = parser!!.getKey()
                        edit.putString(getString(R.string.pref_key_source), key)
                    }
                    val parser = AnimSourceFactory.home(key)
                    val label = parser!!.getLabel()
                    withContext(Dispatchers.Main) {
                        title = label
                    }

                    loadRows(key)
                }
            }
        }

    }

    private fun loadRows(key:String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val parser = AnimSourceFactory.home(key)
//            val key = sharedPreferences.getString(getString(R.string.pref_key_source), AnimSources.homeKeys()[0])
//            Log.e(TAG, "loadRows: ${key}", )
            var result = parser?.home();
            var index = 0;
            starIndex = index

            val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
            val cardPresenter = CardPresenter()

            val listStarRowAdapter = ArrayObjectAdapter(cardPresenter)
            val star = EasyDB.database.bangumiStar.getAllStar();
//            val detals = EasyDatabase.AppDB.bangumiDetailDao().findStarBangumiDetails()
            for (detal in star) {
                val bangumi = Bangumi(
                    detal.bangumiId,
                    detal.source,
                    detal.detailUrl,
                    detal.name,
                    detal.cover,
                    "",
                    0
                )
                listStarRowAdapter.add(bangumi)
            }

            val header = HeaderItem(index.toLong(), "我的追番")
            rowsAdapter.add(ListRow(header, listStarRowAdapter))


            if (result is ISourceParser.ParserResult.Complete) {

                val data: LinkedHashMap<String, List<Bangumi>> = result.data;
                val keys = data.keys

                index++
                for (key in keys) {
                    val bangumis: List<Bangumi>? = data.get(key)
                    // 右侧影片信息
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    bangumis?.let {
                        for (bangumi in it) {
                            listRowAdapter.add(bangumi)
                        }
                    }
                    val header = HeaderItem(index.toLong(), key)
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                    index++
                }

            } else if (result is ISourceParser.ParserResult.Error) {
                result.throwable.printStackTrace()
                Log.e(TAG, "loadRows: ${result.throwable.stackTraceToString()}")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireActivity(),
                        result.throwable.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    val errorFragment = BrowseErrorFragment()
                    val bundle = Bundle()
                    bundle.putString(BrowseErrorFragment.ERROR_MSG, result.throwable.toString())
                    errorFragment.arguments = bundle
                    requireFragmentManager().beginTransaction()
                        .replace(R.id.main_browse_fragment, errorFragment)
                        .addToBackStack(null).commit()
                }
            }
            withContext(Dispatchers.Main) {
                // 最后的设置页面
                val gridHeader = HeaderItem(NUM_ROWS.toLong(), getString(R.string.setting))
                val mGridPresenter = GridItemPresenter()
                val gridRowAdapter = ArrayObjectAdapter(mGridPresenter)
                gridRowAdapter.add(resources.getString(R.string.setting))
                rowsAdapter.add(ListRow(gridHeader, gridRowAdapter))

                adapter = rowsAdapter
            }

        }
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            val intent = Intent(activity, TvSearchActivity::class.java)
            startActivity(intent)
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
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.VIDEO, item)
                startActivity(intent)
            } else if ((item as String).contains(getString(R.string.setting))) {
                val intent = Intent(activity, TvSettingsActivity::class.java)
                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity())
                    .toBundle()
                startActivity(intent, bundle)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            // 更换背景图
            if (item is Bangumi) {
                // 图片像素太差。。。
//                mBackgroundUri = item.cover
//                startBackgroundTimer()
            }

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