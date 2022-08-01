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
import com.heyanle.easybangumi.EasyApplication
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.entity.Bangumi
import com.heyanle.easybangumi.source.ISearchParser
import com.heyanle.easybangumi.source.ISourceParser
import com.heyanle.easybangumi.source.SourceParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * This class demonstrates how to do in-app search
 */
class SearchFragment : TvSearchSupportFragment(), TvSearchSupportFragment.SearchResultProvider {
    private val mHandler = Handler()
    private var mRowsAdapter: ArrayObjectAdapter? = null
    private var mQuery: String? = null
    private val mResultsFound = false
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
        setSourceLabel(SourceParserFactory.homeLabel())
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

    override fun onQueryTextSubmit(query: String, key:String): Boolean {
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


    private fun loadQuery(query: String, label:String) {
        if (!TextUtils.isEmpty(query) && query != "nil") {
            mQuery = query
            val searchKeys = SourceParserFactory.searchKeys()
            for (key in searchKeys){
                val source = SourceParserFactory.parser(key)
                if (source!= null){
                    if (source.getLabel() === label){
                        val sourceKey = source.getKey();
                        val searchParser = SourceParserFactory.search(source.getKey())
                        if (searchParser!= null){
                            searchData(searchParser)
                        }
                    }
                }

            }
        }
    }

    private fun searchData(searchParser: ISearchParser) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = searchParser.search("废柴", 0)

            if (result is ISourceParser.ParserResult.Complete) {
                val data = result.data
//                Log.e(TAG, "loadQuery: ${data.first} ${data.second}")
                val bangumis = data.second
                var titleRes: Int
                if (bangumis.isEmpty()) {
                    titleRes = R.string.no_search_results;
                } else {
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
                withContext(Dispatchers.Main){
                    Toast.makeText(EasyApplication.INSTANCE, result.throwable.stackTraceToString(), Toast.LENGTH_SHORT).show()
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