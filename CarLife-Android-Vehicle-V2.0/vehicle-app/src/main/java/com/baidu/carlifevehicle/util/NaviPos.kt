package com.baidu.carlifevehicle.util;

import com.baidu.carlifevehicle.util.CarlifeConfUtil.CFG_NAVI_POS


public object NaviPos {
    private const val NAVI_BOTTOM = "0"
    private const val NAVI_LEFT = "1"
    private const val NAVI_NONE = "2"

    public fun isNone() : Boolean {
        return NAVI_NONE == PreferenceUtil.getInstance().getString(CFG_NAVI_POS,NAVI_BOTTOM);
    }

    public fun isBottom() : Boolean{
        return NAVI_BOTTOM == PreferenceUtil.getInstance().getString(CFG_NAVI_POS,NAVI_BOTTOM);
    }

    public fun isLeft() :Boolean {
        return NAVI_LEFT == PreferenceUtil.getInstance().getString(CFG_NAVI_POS,NAVI_BOTTOM);
    }

}
