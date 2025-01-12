package com.heyanle.lib_anim.bimibimi

import android.util.Log
import com.google.gson.JsonParser
import com.heyanle.bangumi_source_api.api.*
import com.heyanle.bangumi_source_api.api.IPlayerParser.PlayerInfo.Companion.TYPE_HLS
import com.heyanle.bangumi_source_api.api.entity.Bangumi
import com.heyanle.bangumi_source_api.api.entity.BangumiDetail
import com.heyanle.bangumi_source_api.api.entity.BangumiSummary
import com.heyanle.lib_anim.utils.SourceUtils
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.lang.Exception
import java.lang.IndexOutOfBoundsException

/**
 * Created by HeYanLe on 2021/10/21 11:38.
 * https://github.com/heyanLE
 */
class BimibimiParser : ISourceParser, IHomeParser, IDetailParser, IPlayerParser, ISearchParser {

    companion object {
        const val ROOT_URL = "http://www.bimiacg5.net"
        const val PROXY_URL =
            "https://proxy-tf-all-ws.bilivideo.com/?url=" // https://proxy-tf-all-ws.bilivideo.com/?url=
    }

    private fun url(source: String): String {
        return SourceUtils.urlParser(ROOT_URL, source)
    }

    override fun getKey(): String {
        return "Bimibimi"
    }

    override fun getLabel(): String {
        return "哔咪动漫"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun getVersionCode(): Int {
        return 0
    }

    private fun defaultGET(url: String): String {
//        val req =
//            GET(PROXY_URL + url, Headers.headersOf("User-Agent", networkHelper.defaultLinuxUA))
        val req =
            GET(url, Headers.headersOf("User-Agent", networkHelper.defaultLinuxUA))
        return networkHelper.client.newCall(req).execute().body?.string() ?: ""
    }

    override suspend fun home(): ISourceParser.ParserResult<LinkedHashMap<String, List<Bangumi>>> {
        return withContext(Dispatchers.IO) {
            val map = LinkedHashMap<String, List<Bangumi>>()
            val doc = runCatching {
                Jsoup.parse(defaultGET(ROOT_URL))
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }
            kotlin.runCatching {

                val elements = doc.select("div.area-cont")

                fun load(element: Element) {
                    val columnTitle = element.getElementsByClass("title")[0].child(1).text()
                    val uls = element.getElementsByClass("tab-cont")
                    val list = arrayListOf<Bangumi>()
                    val ul = uls[0]
                    Log.i("Bimi", "home load ul.children.size: ${ul.children().size}", )
                    ul.children().forEach { ele ->
                        val detailUrl = url(ele.child(0).attr("href"))
                        val imgTag = ele.getElementsByTag("img")
                        var imgUrl:String
                        if(imgTag.size > 0){
                            imgUrl = url(imgTag[0].attr("src"))
                        }else{
                            imgUrl = ""
                            Log.e("Bimi", "load imgTag error: $imgTag", )
                        }
                        val title = ele.child(1).child(0).text()
                        val intro = ele.child(1).child(1).text()
                        val bangumi = Bangumi(
                            id = "${getLabel()}-$detailUrl",
                            source = getKey(),
                            name = title,
                            cover = imgUrl,
                            intro = intro,
                            detailUrl = detailUrl,
                            visitTime = System.currentTimeMillis()
                        )
                        list.add(bangumi)
                    }
                    map[columnTitle] = list

                }

                // 今日热播
                load(elements[0])

                // 新番放送
                load(elements[1])

                // 大陆动漫
                load(elements[2])

                // 番组计划
                load(elements[3])

                // 剧场动画
                load(elements[4])

            }.onFailure {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, true)
            }.onSuccess {
                return@withContext ISourceParser.ParserResult.Complete(map)
            }

            return@withContext ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
    }

    override fun firstKey(): Int {
        return 1
    }

    override suspend fun search(
        keyword: String,
        key: Int
    ): ISourceParser.ParserResult<Pair<Int?, List<Bangumi>>> {
        return withContext(Dispatchers.IO) {
            val doc = runCatching {
                val url = url("/vod/search/wd/$keyword/page/$key")
                Jsoup.parse(defaultGET(url))
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }

            runCatching {
                val r = arrayListOf<Bangumi>()
                doc.select("div.v_tb ul.drama-module.clearfix.tab-cont li.item").forEach {
                    val detailUrl = url(it.child(0).attr("href"))
                    val b = Bangumi(
                        id = "${getLabel()}-$detailUrl",
                        name = it.child(1).child(0).text(),
                        detailUrl = detailUrl,
                        intro = it.child(1).child(1).text(),
//                        cover = PROXY_URL + url(it.child(0).child(0).attr("src")),
                        cover = url(it.child(0).child(0).attr("src")),
                        visitTime = System.currentTimeMillis(),
                        source = getKey(),
                    )
                    r.add(b)
                }
                val pages = doc.select("div.pages ul.pagination li a.next.pagegbk")
                if (pages.isEmpty()) {
                    return@withContext ISourceParser.ParserResult.Complete(Pair(null, r))
                } else {
                    return@withContext ISourceParser.ParserResult.Complete(Pair(key + 1, r))
                }
            }.onFailure {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, true)
            }

            return@withContext ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
    }

    override suspend fun detail(bangumi: BangumiSummary): ISourceParser.ParserResult<BangumiDetail> {
        return withContext(Dispatchers.IO) {
            val doc = runCatching {
                Jsoup.parse(defaultGET(bangumi.detailUrl))
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }
            kotlin.runCatching {
                val id = "${getLabel()}-${bangumi.detailUrl}"
                val tit = doc.select("div.txt_intro_con div.tit")[0]
                val name = tit.child(0).text()
                val intro = tit.child(1).text()
                val img = doc.select("div.poster_placeholder div.v_pic img")
                var cover:String = ""
                if (img.size > 0){
                    cover = url(img[0].attr("src"))
                }else{
                    Log.e("Bimi", "detail: no cover: $cover", )
                }
//                    PROXY_URL + url(doc.select("div.poster_placeholder div.v_pic img")[0].attr("src"))
                val description = doc.getElementsByClass("vod-jianjie")[0].text()
                return@withContext ISourceParser.ParserResult.Complete(
                    BangumiDetail(
                        id = id,
                        source = getKey(),
                        name = name,
                        cover = cover,
                        intro = intro,
                        detailUrl = bangumi.detailUrl,
                        description = description
                    )
                )
            }.onFailure {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }
            return@withContext ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
    }

    private var bangumi: BangumiSummary? = null
    private val temp: ArrayList<ArrayList<String>> = arrayListOf()

    override suspend fun getPlayMsg(bangumi: BangumiSummary): ISourceParser.ParserResult<LinkedHashMap<String, List<String>>> {
        this@BimibimiParser.temp.clear()
        return withContext(Dispatchers.IO) {
            val map = LinkedHashMap<String, List<String>>()
            val doc = runCatching {
                Jsoup.parse(defaultGET(bangumi.detailUrl))
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }

            kotlin.runCatching {
                val sourceDiv = doc.getElementsByClass("play_source_tab")[0]
                val ite = sourceDiv.getElementsByTag("a").iterator()
                val playBoxIte = doc.getElementsByClass("play_box").iterator()
                while (ite.hasNext() && playBoxIte.hasNext()) {
                    val sourceA = ite.next()
                    val list = arrayListOf<String>()
                    val urlList = arrayListOf<String>()

                    val playBox = playBoxIte.next()
                    playBox.getElementsByTag("a").forEach {
                        list.add(it.text())
                        urlList.add(url(it.attr("href")))
                    }

                    map[sourceA.text()] = list
                    this@BimibimiParser.temp.add(urlList)
                }
                this@BimibimiParser.bangumi = bangumi
                return@withContext ISourceParser.ParserResult.Complete(map)
            }.onFailure {
                this@BimibimiParser.bangumi = null
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, true)
            }
            this@BimibimiParser.bangumi = null
            return@withContext ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
    }

    override suspend fun getPlayUrl(
        bangumi: BangumiSummary,
        lineIndex: Int,
        episodes: Int
    ): ISourceParser.ParserResult<IPlayerParser.PlayerInfo> {

        if (lineIndex < 0 || episodes < 0) {
            return ISourceParser.ParserResult.Error(IndexOutOfBoundsException(), false)
        }
        var url = ""
        if (bangumi != this.bangumi
            || lineIndex >= temp.size
            || episodes >= temp[lineIndex].size
            || temp[lineIndex][episodes] == ""
        ) {
            getPlayMsg(bangumi).error {
                return@getPlayUrl ISourceParser.ParserResult.Error(it.throwable, it.isParserError)
            }.complete {
                runCatching {
                    url = temp[lineIndex][episodes]
                }.onFailure {
                    return@getPlayUrl ISourceParser.ParserResult.Error(it, true)
                }
            }
        } else {
            url = temp[lineIndex][episodes]
        }

        if (url.isEmpty()) {
            return ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
        return withContext(Dispatchers.IO) {
            val doc = runCatching {
                Log.e("bimibimi", "getPlayUrlxxx:$url", )
                val html = defaultGET(url)
                Jsoup.parse(html)
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }

            val videoHtmlUrl = runCatching {
                val jsonData = doc.getElementById("video").toString().run {
                    substring(indexOf("{"), lastIndexOf("}") + 1)
                }
                val jsonObject = JsonParser.parseString(jsonData).asJsonObject
                val jsonUrl = jsonObject.get("url").asString

                if (jsonUrl.contains("http")) {
                    return@withContext ISourceParser.ParserResult.Complete(
                        IPlayerParser.PlayerInfo(
                            uri = jsonUrl
                        )
                    )
                } else {
                    var from = jsonObject.get("from").asString
                    from = when (from) {
                        "wei" -> {
                            "wy"
                        }

                        "ksyun" -> {
                            "ksyun"
                        }

                        "danmakk" -> {
                            "pic"
                        }

                        "pic" -> {
                            "pic"
                        }

                        else -> {
                            "play"
                        }
                    }
                    "$ROOT_URL/static/danmu/$from.php?url=$jsonUrl"// &myurl=$url
                }


            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, true)
            }
            val d = runCatching {
                Jsoup.parse(
                    networkHelper.client.newCall(GET( url(videoHtmlUrl)))
//                    networkHelper.client.newCall(GET(PROXY_URL + url(videoHtmlUrl)))
                        .execute().body?.string() ?: ""
                )
            }.getOrElse {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, false)
            }
            runCatching {
                var src = d.select("video#video source")[0].attr("src")
                if (src.startsWith("./")) {
                    src = src.replace("./", "$ROOT_URL/static/danmu/")
//                    src = src.replace("./", "$PROXY_URL$ROOT_URL/static/danmu/")
                } else if (!src.startsWith("http://") && !src.startsWith("https://")) {
                    src = "${"$ROOT_URL/static/danmu/"}${src}"
//                    src = "${"$PROXY_URL$ROOT_URL/static/danmu/"}${src}"
                }
                Log.e("Bimibimi", "getPlayUrl: $src", )
                return@withContext ISourceParser.ParserResult.Complete(
                    IPlayerParser.PlayerInfo(
                        type = TYPE_HLS,
                        uri = src
                    )
                )
            }.onFailure {
                it.printStackTrace()
                return@withContext ISourceParser.ParserResult.Error(it, true)
            }

            return@withContext ISourceParser.ParserResult.Error(Exception("Unknown Error"), true)
        }
        //return super.getPlayUrl(bangumi, lineIndex, episodes)
    }

}