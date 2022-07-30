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
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
//        lifecycleScope.launch {
//            val keyList = SourceParserFactory.homeKeys()
//            val result =SourceParserFactory.home(keyList[0])?.home();
//            if(result is ISourceParser.ParserResult.Complete){
//                 val data:LinkedHashMap<String, List<Bangumi>> = result.data;
//                val keys = data.keys
//                for (key in keys){
//                    val bangumis:List<Bangumi>? = data.get(key)
//                    Log.e("MainActivity", "onCreate: key=${key}", )
//                    bangumis?.let {
//                        for (bangumi in it){
//                            Log.e("MainActivity", "onCreate: source=${bangumi.source} name=${bangumi.name}", )
//                        }
//                    }
//                }
//            }
//            when(result){
//                is ISourceParser.ParserResult.Complete->{}
//                is ISourceParser.ParserResult.Error ->{}
//                else->{}
//            }
//        }

    }
}