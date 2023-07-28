/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.heyanle.easybangumi.tv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import com.heyanle.bangumi_source_api.api.ISearchParser
import com.heyanle.bangumi_source_api.api.ISourceParser
import com.heyanle.bangumi_source_api.api.entity.Bangumi
import com.heyanle.easybangumi.BangumiApp
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.source.AnimSourceFactory
import com.heyanle.easybangumi.source.AnimSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * This class demonstrates how to do in-app search
 */
class TvSearchFragment : TvSearchSupportFragment(), TvSearchSupportFragment.SearchResultProvider {
    private val mHandler = Handler()
    private var mRowsAdapter: ArrayObjectAdapter? = null
    private var mQuery: String? = null
    private var mResultsFound = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        setSearchResultProvider(this)
        setOnItemViewClickedListener(ItemViewClickedListener())

        if (DEBUG) {
            Log.d(
                TAG, "User is initiating a search. Do we have RECORD_AUDIO permission? " +
                        hasPermission(Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val sources = AnimSourceFactory.parsers() as MutableStateFlow<AnimSources>
        val parsers = sources.value.homeParsers()
        val labels = parsers.map { it.getLabel() }
        var defaultFocus = 0
        for (i in 0..parsers.size){
            val parser = parsers.get(i)
           if( parser.getKey() === "yhdm"){
               defaultFocus = i
               break
           }
        }
        // 设置label ,它们是 onQueryTextSubmit 中的key
        setSourceLabel(labels, defaultFocus)


    }

    override fun onPause() {
        mHandler.removeCallbacksAndMessages(null)
        super.onPause()
    }


    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter!!
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        if (DEBUG) Log.i(TAG, String.format("Search text changed: %s", newQuery))
        return true
    }

    override fun onQueryTextSubmit(query: String, key: String): Boolean {
        if (DEBUG) Log.i(TAG, String.format("Search text submitted: %s", query))
        loadQuery(query, key)
        return true
    }

    fun hasResults(): Boolean {
        return mRowsAdapter!!.size() > 0 && mResultsFound
    }

    private fun hasPermission(permission: String): Boolean {
        val context: Context? = activity
        return PackageManager.PERMISSION_GRANTED == context!!.packageManager.checkPermission(
            permission, context.packageName
        )
    }


    private fun loadQuery(query: String, label: String) {
        Log.e(TAG, "loadQuery: $query, $label")
        if (!TextUtils.isEmpty(query) && query != "nil") {
            mQuery = query
            val sources = AnimSourceFactory.parsers() as MutableStateFlow<AnimSources>
            val parsers: List<ISearchParser> = sources.value.searchParsers()
            parsers.forEach {
                if (it.getLabel() === label) {
                    searchData(query, it)
                }
            }
        }
    }

    private fun searchData(query: String, searchParser: ISearchParser) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = searchParser.search(query, 0)

            if (result is ISourceParser.ParserResult.Complete) {
                val data = result.data
//                Log.e(TAG, "loadQuery: ${data.first} ${data.second}")
                val bangumis = data.second
                var titleRes: Int
                if (bangumis.isEmpty()) {
                    mResultsFound = false;
                    titleRes = R.string.no_search_results;
                } else {
                    mResultsFound = true;
                    titleRes = R.string.search_results;
                }
                val header = HeaderItem(getString(titleRes, mQuery));
                val cardPresenter = CardPresenter()
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                for (bangumi in bangumis) {
                    listRowAdapter.add(bangumi)
                }

                mRowsAdapter?.clear()
                val row = ListRow(header, listRowAdapter)
                mRowsAdapter?.add(row)
            }
            if (result is ISourceParser.ParserResult.Error) {
                var titleRes: Int = R.string.no_search_results;
                val header = HeaderItem(getString(titleRes, mQuery));
                val cardPresenter = CardPresenter()
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                mRowsAdapter?.clear()
                val row = ListRow(header, listRowAdapter)
                mRowsAdapter?.add(row)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        BangumiApp.INSTANCE,
                        result.throwable.stackTraceToString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun focusOnSearch() {
        requireView().findViewById<View>(R.id.lb_search_bar).requestFocus()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder, item: Any,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is Bangumi) {
                val intent = Intent(requireActivity(), PlaybackActivity::class.java)
                intent.putExtra(PlaybackActivity.VIDEO, item)
                startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "SearchFragment"
        private const val DEBUG = true
        private const val FINISH_ON_RECOGNIZER_CANCELED = true
        private const val REQUEST_SPEECH = 0x00000010
    }
}