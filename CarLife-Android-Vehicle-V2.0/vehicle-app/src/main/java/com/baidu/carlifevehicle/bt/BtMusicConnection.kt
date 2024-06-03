package com.baidu.carlifevehicle.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import com.baidu.carlifevehicle.VehicleApplication
import kotlin.properties.Delegates


@SuppressLint("MissingPermission")
fun Context.isA2DPConnected(): Boolean {
    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter ?: return false
    return bluetoothAdapter.getProfileConnectionState(11) == BluetoothA2dp.STATE_CONNECTED
}

inline val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING)

class BtMusicConnection private constructor(
    private val context: Context,
    service: ComponentName
) {

    companion object {

        private const val TAG = "BtMusicConnection"
        private const val CUSTOM_ACTION_A2DP_PLAYER_FOREGROUND_CHANGED = "autochips.bluetooth.a2dp.CUSTOM_ACTION_PLAYER_STATE_FOREGROUND"
        private const val FOREGROUND_EXTRA_STATE = "autochips.bluetooth.a2dp.foreground.extra.CUSTOM_STATE"

        const val ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED"
        const val A2DP_SINK_PACKAGE = "com.android.bluetooth"
        const val A2DP_SINK_COMPONENT = "com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService"
        val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder().build()
        val EMPTY_METADATA: MediaMetadataCompat = MediaMetadataCompat.Builder().build()


        val instance by lazy {
            BtMusicConnection(
                VehicleApplication.app,
                ComponentName(A2DP_SINK_PACKAGE, A2DP_SINK_COMPONENT)
            )
        }

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
    }

    private val connectionCallback = MediaConnectionCallback()
    private val mediaBrowserCompat =
        MediaBrowserCompat(context, service, connectionCallback, null)
    private lateinit var mediaController: MediaControllerCompat
    private val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls
    var foreground by Delegates.observable(false) { _, oldValue, newValue ->
        if (oldValue == newValue) return@observable

        setPlayerForeground(newValue)
    }

    val connectState = MutableLiveData(STATE_DISCONNECTED)
    val nowPlaying = MutableLiveData(EMPTY_METADATA)
    val playbackState : MutableLiveData<PlaybackStateCompat> = MutableLiveData<PlaybackStateCompat>(EMPTY_PLAYBACK_STATE)
    val a2dpConnected = MutableLiveData(context.isA2DPConnected())

    private val pendingActions = mutableListOf<() -> Unit>()

    private val a2dpStateObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
         //   Debugger.d { "action:${intent?.action}, extras:${intent?.extras.dump()}" }
            when (intent?.action) {
                ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED -> {
                    val isA2dpConnected = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    ) == BluetoothProfile.STATE_CONNECTED
                    a2dpConnected.value = isA2dpConnected
                }
            }
        }
    }

    var onBeforePlayListener: (() -> Unit)? = null

    init {
        context.registerReceiver(a2dpStateObserver, IntentFilter().apply {
            addAction(ACTION_A2DP_SINK_CONNECTION_STATE_CHANGED)
        })
        mediaBrowserCompat.connect()

        connectState.value = STATE_CONNECTING
    }

    fun setPlayerForeground(foreground: Boolean) {
        if (!this::mediaController.isInitialized) return

        val extras = Bundle()
        extras.putBoolean(FOREGROUND_EXTRA_STATE, foreground)
        transportControls.sendCustomAction(CUSTOM_ACTION_A2DP_PLAYER_FOREGROUND_CHANGED, extras)
    }

    fun skipToNext() {
        if (!this::mediaController.isInitialized) return

        transportControls.skipToNext()
        if (playbackState.value?.isPlaying != true) {
            transportControls.play()
        }
    }

    fun skipToPrevious() {
        if (!this::mediaController.isInitialized) return

        transportControls.skipToPrevious()
        if (playbackState.value?.isPlaying != true) {
            transportControls.play()
        }
    }

    fun isPlaying() : Boolean {
        return true == playbackState.value?.isPlaying
    }

    fun play() {
        if (!this::mediaController.isInitialized) return
        onBeforePlayListener?.invoke()
        transportControls.play()
//        unmute()
    }



    fun autoPlay() {
        if (!this::mediaController.isInitialized) {
            pendingActions.add {
                play()
            }
            return
        }

        play()
    }

    fun pause() {
        if (!this::mediaController.isInitialized) return

        transportControls.pause()
    }

    fun stop() {
        if (!this::mediaController.isInitialized) return

        transportControls.stop()
    }

    fun play(mediaId: String) {
        if (!this::mediaController.isInitialized) return

        transportControls.playFromMediaId(mediaId, null)
    }

    fun seekTo(position: Long) {
        if (!this::mediaController.isInitialized) return

        transportControls.seekTo(position)
    }

    inner class MediaConnectionCallback : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
         //   Debugger.d(TAG) { "onConnected, pendingActions:${pendingActions.size}" }
            connectState.value = STATE_CONNECTED
            mediaController =
                MediaControllerCompat(context, mediaBrowserCompat.sessionToken).apply {
                    registerCallback(MediaControllerCallback())
                }
            // execute pending actions
            pendingActions.forEach { action ->
                action()
            }
            pendingActions.clear()
            nowPlaying.value = mediaController.metadata ?: EMPTY_METADATA
            playbackState.value = mediaController.playbackState ?: EMPTY_PLAYBACK_STATE
        }

        override fun onConnectionSuspended() {
          //  Debugger.d(TAG) { "onConnectionSuspended" }
            connectState.value = STATE_DISCONNECTED
        }

        override fun onConnectionFailed() {
          //  Debugger.d(TAG) { "onConnectionFailed" }
            connectState.value = STATE_DISCONNECTED
        }

    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        //    Debugger.v(TAG) { "onPlaybackStateChanged:$state" }
            playbackState.value = state ?: EMPTY_PLAYBACK_STATE
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
         //   Debugger.v(TAG) { "onMetadataChanged:${metadata?.bundle?.dump()}" }
            nowPlaying.value = metadata
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        //    Debugger.d(TAG) { "onSessionDestroyed" }
            nowPlaying.value = EMPTY_METADATA
            playbackState.value = EMPTY_PLAYBACK_STATE
        }

    }
}