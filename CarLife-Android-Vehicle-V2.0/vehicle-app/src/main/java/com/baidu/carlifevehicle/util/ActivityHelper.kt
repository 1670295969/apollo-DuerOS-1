package com.baidu.carlifevehicle.util

import com.baidu.carlifevehicle.CarlifeActivity
import com.baidu.carlifevehicle.module.MusicModule
import java.lang.ref.WeakReference

object ActivityHelper {

    private var carlifeActivityRef : WeakReference<CarlifeActivity?>? = null

    fun setActivity(activity: CarlifeActivity){
        carlifeActivityRef = WeakReference(activity)
    }

    fun removeActivity(){
        carlifeActivityRef?.clear()
        carlifeActivityRef = null
    }

    fun handle(block : (MusicModule)->Unit){

        carlifeActivityRef?.get()
            ?.mMusicModule
            ?.let {
                block(it)
            }

    }

    fun isPlayIng() : Boolean {
        return true == carlifeActivityRef?.get()?.mMusicModule?.isPlaying()
    }



}