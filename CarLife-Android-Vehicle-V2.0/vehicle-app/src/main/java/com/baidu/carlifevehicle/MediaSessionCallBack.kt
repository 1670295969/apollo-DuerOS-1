package com.baidu.carlifevehicle

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import com.baidu.carlifevehicle.util.ActivityHelper
import com.baidu.carlifevehicle.util.CommonParams
import com.baidu.carlifevehicle.util.DisplayUtils
import com.baidu.carlifevehicle.util.HookCustomKey

class MediaSessionCallBack(private val myMediaSessionService: CarlifeMediaSessionService) :
    MediaSessionCompat.Callback() {


    override fun onMediaButtonEvent(intent: Intent): Boolean {
        try {
            val keyEvent = intent.getParcelableExtra<KeyEvent>("android.intent.extra.KEY_EVENT")!!
            Log.i(CarlifeMediaSessionService.TAG, "keyEvent : $keyEvent")
//        if (keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_NEXT
//            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PREVIOUS
//            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY
//            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE
//            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
//        ) {//
//            return super.onMediaButtonEvent(intent);
//        }
            if (keyEvent.action == 1) {
                if (HookCustomKey.handleCustomKey("service",keyEvent.keyCode,ActivityHelper.isPlayIng())){
                    return true
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

        return super.onMediaButtonEvent(intent);
    }


    private fun sendKeyEvent(keycode: Int) {
        DisplayUtils.sendHardKeyCodeEvent(keycode)
    }


}