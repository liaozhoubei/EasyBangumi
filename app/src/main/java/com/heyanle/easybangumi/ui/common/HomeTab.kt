package com.heyanle.easybangumi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyanle.easybangumi.theme.EasyThemeController

/**
 * Created by HeYanLe on 2023/1/9 22:12.
 * https://github.com/heyanLE
 */
@Composable
fun HomeTabRow(
    selectedTabIndex: Int,
    tabs: @Composable () -> Unit
){
    val themeState by remember {
        EasyThemeController.easyThemeState
    }

    val isUseSecondary = themeState.isDark() && !(themeState.isDynamicColor && EasyThemeController.isSupportDynamicColor())

    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.primary,
    ){
        ScrollableTabRow(
            edgePadding = 0.dp,
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            tabs = tabs,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = if(isUseSecondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                )
            },)
    }

}

@Composable
fun HomeTabItem(
    selected: Boolean,
    text: (@Composable ()->Unit)? = null,
    icon: (@Composable ()->Unit)? = null,
    onClick: ()->Unit,
){

    Tab(
        selected = selected,
        onClick = onClick,
        text = text,
        icon = icon,
        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
        unselectedContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
    )
}

@Composable
fun KeyTabRow(
    selectedTabIndex: Int,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.background,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onBackground,
    textList: List<String>,
    onItemClick: (Int)->Unit,
){

    LazyRow(
        modifier = Modifier.padding(2.dp, 0.dp),

    ){
        items(textList.size){
            val selected = it == selectedTabIndex
            Surface(
                shadowElevation = 4.dp,
                shape = CircleShape,
                modifier =
                    Modifier
                        .padding(2.dp, 8.dp)
                        ,
                color = if (selected) selectedContainerColor else unselectedContainerColor,
            ) {
                Text(
                    modifier = Modifier.clip(CircleShape).clickable {
                        onItemClick(it)
                    }.padding(8.dp, 4.dp),
                    color = if (selected) selectedContentColor else unselectedContentColor,
                    fontWeight = FontWeight.W900,
                    text = textList[it],
                    fontSize = 12.sp,
                )
            }
        }
    }

}