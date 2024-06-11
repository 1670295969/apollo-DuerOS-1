package com.baidu.carlifevehicle

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import com.baidu.carlifevehicle.util.CommonParams
import com.baidu.carlifevehicle.util.DisplayUtils

class MediaSessionCallBack(private val myMediaSessionService : CarlifeMediaSessionService) : MediaSessionCompat.Callback() {


    override fun onMediaButtonEvent(intent: Intent): Boolean {
        val keyEvent = intent.getParcelableExtra<KeyEvent>("android.intent.extra.KEY_EVENT")!!
        Log.i(CarlifeMediaSessionService.TAG, "keyEvent : $keyEvent")
        if (keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_NEXT
            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PREVIOUS
            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY
            && keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PAUSE
            ) {//&& keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            return super.onMediaButtonEvent(intent);
        }
        if (keyEvent.action == 1) {
            when(keyEvent.keyCode){
                KeyEvent.KEYCODE_MEDIA_NEXT-> {
                    sendKeyEvent(CommonParams.KEYCODE_SEEK_ADD)
                }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS->{
                        sendKeyEvent(CommonParams.KEYCODE_SEEK_SUB)
                    }
                      KeyEvent.KEYCODE_MEDIA_PLAY->{
                          sendKeyEvent(CommonParams.KEYCODE_MEDIA_START)
                      }
                      KeyEvent.KEYCODE_MEDIA_PAUSE->{
                          sendKeyEvent(CommonParams.KEYCODE_MEDIA_STOP)
                      }
            }
        }
        return true;
    }

    private fun sendKeyEvent(keycode:Int){
        DisplayUtils.sendHardKeyCodeEvent(keycode)
    }



}