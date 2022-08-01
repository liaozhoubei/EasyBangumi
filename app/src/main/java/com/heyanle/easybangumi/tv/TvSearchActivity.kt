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
import android.view.KeyEvent
import com.heyanle.easybangumi.source.SourceParserFactory

/*
 * SearchActivity for SearchFragment
 */
class TvSearchActivity : LeanbackActivity() {
    private var mFragment: SearchFragment? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)
        mFragment = supportFragmentManager
            .findFragmentById(R.id.search_fragment) as SearchFragment?
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
            mFragment!!.focusOnSearch()
        }
        return super.onKeyDown(keyCode, event)
    }
}