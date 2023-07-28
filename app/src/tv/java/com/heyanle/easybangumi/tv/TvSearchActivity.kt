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

import android.os.Bundle
import com.heyanle.easybangumi.R
import android.content.Intent
import android.util.Log
import android.view.KeyEvent

/*
 * SearchActivity for SearchFragment
 * 在有搜索数据的情况下，移动到搜索数据最左边会跳到数据源上，暂未找到处理方法
 */
class TvSearchActivity : LeanbackActivity() {
    private var mFragment: TvSearchFragment? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_search)
        mFragment = supportFragmentManager
            .findFragmentById(R.id.search_fragment) as TvSearchFragment?
    }

    override fun onSearchRequested(): Boolean {
        if (mFragment!!.hasResults()) {
            startActivity(Intent(this, TvSearchActivity::class.java))
        } else {
            mFragment!!.startRecognition()
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // If there are no results found, press the left key to reselect the microphone
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !mFragment!!.hasResults()) {
            Log.e("SearchActivity", "onKeyDown: mFragment.focusOnSearch()")
            mFragment!!.focusOnSearch()
        }

        return super.onKeyDown(keyCode, event)
    }
}