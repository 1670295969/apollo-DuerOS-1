//package com.baidu.carlifevehicle
//
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import android.support.v4.media.session.MediaControllerCompat
//import android.support.v4.media.session.MediaSessionCompat
//import android.util.Log
//
//class MesiaSessionService : Service() {
//    override fun onBind(p0: Intent?): IBinder? {
//        TODO("Not yet implemented")
//    }
//
//    private fun initMedia(){
//        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
//
//        mediaSessionCompat.isActive = true
//        MediaControllerCompat.setMediaController(this, mediaSessionCompat.controller)
//
//        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
//            override fun onMediaButtonEvent(intent: Intent?): Boolean {
//                Log.d(TAG, "intent=$intent")
//                var keyEvent: KeyEvent? = null
//                if (intent != null) {
//                    if ("android.intent.action.MEDIA_BUTTON" == intent.action) {
//                        keyEvent = intent.getParcelableExtra(
//                            "android.intent.extra.KEY_EVENT"
//                        ) as KeyEvent?
//                        if (keyEvent != null) {
//                            val action = keyEvent.action
//                            val keyCode = keyEvent.keyCode
//                            if (action == KeyEvent.ACTION_UP) {
//                                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
//                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_ADD)
//                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
//                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_SUB)
//                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
//                                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
//                                ) {
//                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_STOP)
//                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
//                                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
//                                ) {
//                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_START)
//                                }
//                            }
//                        }
//
//                    }
//                }
//
//                return super.onMediaButtonEvent(intent)
//            }
//
//        }, null)
//
//    }
//}