package com.heyanle.easybangumi.tv

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.heyanle.easybangumi.R
import com.heyanle.easybangumi.entity.Bangumi
import com.heyanle.easybangumi.source.ISourceParser
import com.heyanle.easybangumi.source.SourceParserFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Loads [MainFragment].
 */
class MainActivity : LeanbackActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }

    }
}