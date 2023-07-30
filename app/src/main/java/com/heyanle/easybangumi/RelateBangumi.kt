package com.heyanle.easybangumi

/**
 * @param line 播放线路
 * @param lineIndex 路线Id
 * @param id 播放的剧集id
 * @param label 当前第几集
 */
data class RelateBangumi(val line:String,val lineIndex:Int,val id:Int, val label:String)