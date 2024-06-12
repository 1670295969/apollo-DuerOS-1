package com.baidu.carlifevehicle.util

import android.util.Log
import android.view.KeyEvent
import com.baidu.carlife.sdk.receiver.CarLife

object HookCustomKey {

    fun handleCustomKey(from :String,keyCode: Int, isPlayIng: Boolean): Boolean {
        if(!CarLife.receiver().isConnected()){
            return false
        }
        val keyCodePlayPause = try {
            PreferenceUtil.getInstance().getString("KEYCODE_PLAY_PAUSE", "-1").toInt()//303
        } catch (e: Exception) {
            -1
        }
        val keyCodeVoice =
            try {
                PreferenceUtil.getInstance().getString("KEYCODE_VOICE", "-1").toInt() // 302
            } catch (e: Exception) {
                -1
            }
        Log.d("HookCustomKey","$from:$keyCode")
        when (keyCode) {
            keyCodePlayPause, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (isPlayIng) {
                    DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_STOP)
                } else {
                    DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_START)
                }
                return true
            }
            keyCodeVoice -> {
                DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_VR_START)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_STOP)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_START)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_ADD)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                DisplayUtils.sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_SUB)
                return true
            }
            else -> return false
        }
    }

}