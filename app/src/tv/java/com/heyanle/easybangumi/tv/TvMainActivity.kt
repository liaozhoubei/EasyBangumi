package com.heyanle.easybangumi.tv

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.heyanle.easybangumi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Loads [TvMainFragment].
 */
class TvMainActivity : LeanbackActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        lifecycleScope.launch(Dispatchers.IO){
//            SourceParserFactory.init()
//        }

        setContentView(R.layout.activity_tv_main)


    }
}