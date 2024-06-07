package com.baidu.carlifevehicle.util

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

object DisplayUtils {

    fun getNavigationBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return (if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0).apply {
            Log.i("-----","getNavigationBarHeight=$this")
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        Log.i("-----","getStatusBarHeight=$result")
        return result
    }

     fun getNeedMetrics(context: Context) : Point {
        val hideStatusBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_STATUS_BAR",true)
        val hideNaviBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_NAVI_BAR",true)
        val naviHeight = DisplayUtils.getNavigationBarHeight(context)
        val statusHeight = DisplayUtils.getStatusBarHeight(context)
        val displayMetrics = DisplayMetrics()
        val wm  = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(displayMetrics)
        var needHeight = displayMetrics.heightPixels
        var needWidth = displayMetrics.widthPixels
        if (hideStatusBar && hideNaviBar){
            needHeight = displayMetrics.heightPixels
            needWidth = displayMetrics.widthPixels
        }else if (hideStatusBar){
            needHeight -= statusHeight
        }else if (hideNaviBar){
            if (NaviPos.isBottom()){
                needHeight -= naviHeight
            }else if(NaviPos.isLeft()){
                needWidth -= naviHeight
            }
        }else {
            needHeight -= statusHeight
            if (NaviPos.isBottom()){
                needHeight -= naviHeight
            }else if(NaviPos.isLeft()){
                needWidth -= naviHeight
            }
        }
        return Point(needWidth,needHeight)
    }

}