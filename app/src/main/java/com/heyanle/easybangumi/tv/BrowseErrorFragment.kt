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

import androidx.leanback.app.ErrorSupportFragment
import android.os.Bundle
import android.os.Handler
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.tv.BrowseErrorFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment

/*
 * This class demonstrates how to extend ErrorFragment to create an error dialog.
 */
class BrowseErrorFragment : ErrorSupportFragment() {
    private val mHandler = Handler()
    private var mSpinnerFragment: SpinnerFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = resources.getString(R.string.app_name)
        mSpinnerFragment = SpinnerFragment()
        requireFragmentManager().beginTransaction().add(R.id.main_frame, mSpinnerFragment!!).commit()
    }

    override fun onStart() {
        super.onStart()
        mHandler.postDelayed({
            requireFragmentManager().beginTransaction().remove(mSpinnerFragment!!).commit()
            setErrorContent()
        }, TIMER_DELAY.toLong())
    }

    override fun onStop() {
        super.onStop()
        mHandler.removeCallbacksAndMessages(null)
        requireFragmentManager().beginTransaction().remove(mSpinnerFragment!!).commit()
    }

    private fun setErrorContent() {
        imageDrawable = resources.getDrawable(androidx.leanback.R.drawable.lb_ic_sad_cloud, null)
        message = resources.getString(R.string.error_fragment_message)
        setDefaultBackground(TRANSLUCENT)
        buttonText = resources.getString(R.string.dismiss_error)
        buttonClickListener = View.OnClickListener {
            requireFragmentManager().beginTransaction().remove(this@BrowseErrorFragment).commit()
            requireFragmentManager().popBackStack()
        }
    }

    class SpinnerFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val progressBar = ProgressBar(container!!.context)
            if (container is FrameLayout) {
                val res = resources
                val width = res.getDimensionPixelSize(R.dimen.spinner_width)
                val height = res.getDimensionPixelSize(R.dimen.spinner_height)
                val layoutParams = FrameLayout.LayoutParams(width, height, Gravity.CENTER)
                progressBar.layoutParams = layoutParams
            }
            return progressBar
        }
    }

    companion object {
        private const val TRANSLUCENT = true
        private const val TIMER_DELAY = 1000
    }
}