package com.baidu.carlife.sdk.receiver.transport.instant

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Build
import android.os.Looper
import android.util.Log
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.CarLifeContext.Companion.CONNECTION_TYPE_WIFIDIRECT
import com.baidu.carlife.sdk.Configs
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.Constants.TAG
import com.baidu.carlife.sdk.util.Logger

class WifiDirectManager(
    private val context: CarLifeContext,
    private val callbacks: Callbacks
) : BroadcastReceiver() {
    interface Callbacks {
        fun onDeviceConnected(info: WifiP2pInfo) {}
    }
    companion object{
        const val TAG = Constants.BLUETOOH_TAG + "_WifiDirectManager"
    }

    private val wifiP2pManager =
        context.applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var channel: WifiP2pManager.Channel

    private val discoverableTask: WifiDirectDiscoverableTask


    @Volatile
    var isConnected = false
        private set

    //    private fun reqDirInfo(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
//            wifiP2pManager.requestDeviceInfo(channel) {
//                Logger.d(Constants.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION it: $it")
//                it?.let {
//                    context.setConfig(Configs.CONFIG_WIFI_DIRECT_NAME,it.deviceName)
//                }
//            }
//        }
////
//    }


    init {
        channel = wifiP2pManager.initialize(
            context.applicationContext,
            Looper.getMainLooper(),
            object : WifiP2pManager.ChannelListener {
                override fun onChannelDisconnected() {
                    Log.w(TAG,"wifiDirectManager:onChannelDisconnected")
                    // 如果channel出现异常的话，需要重新初始化
                    channel = wifiP2pManager.initialize(
                        context.applicationContext,
                        Looper.getMainLooper(),
                        this
                    )
                    wifiP2pManager.requestConnectionInfo(channel) {
                        Logger.d(TAG, "re requestConnectionInfo ", it)
                        if (it != null && it.groupFormed) {
                            isConnected = true
                        }
                    }
                   // updateChannel()
                    //reqDirInfo()
                }
            })
        //reqDirInfo()
        discoverableTask = WifiDirectDiscoverableTask(wifiP2pManager, channel)

        val filter = IntentFilter()
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        //filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        context.applicationContext.registerReceiver(this, filter)

        wifiP2pManager.requestConnectionInfo(channel) {
            Logger.d(TAG, "requestConnectionInfo ", it)
            if (it != null && it.groupFormed) {
                isConnected = true
            }
        }
    }

    fun discoverable() {
        discoverableTask.discoverable()
    }

    fun terminate() {
        Logger.d(TAG, "WifiP2pManager terminate")
        discoverableTask.terminate()
    }

    override fun onReceive(context1: Context, intent: Intent) {
        //Logger.d(TAG, "WIFI_P2P onReceive intent.action: ", intent.action)
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                val list = intent.getParcelableExtra<WifiP2pDeviceList>(EXTRA_P2P_DEVICE_LIST)
                list?.deviceList?.forEach {
                   // Log.d(TAG, "----${it.deviceName}")
//                    Log.d("---------", "${it.deviceAddress}")
//                    if (it.deviceName == "liuk"){
//
//                                            wifiP2pManager.connect(
//                        channel,
//                        WifiP2pConfig().apply {
//                                              this.deviceAddress = it.deviceAddress
//                        },null
//                    )
//                    }

                }

            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device = intent.getParcelableExtra<WifiP2pDevice>(EXTRA_WIFI_P2P_DEVICE)
                Logger.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION device: ${device?.deviceName}")
                context.setConfig(Configs.CONFIG_WIFI_DIRECT_NAME, device?.deviceName ?: "")
                context.sharedPreferences
                    .edit()
                    .putString(Configs.CONFIG_WIFI_DIRECT_NAME, device?.deviceName ?: "")
                    .commit()

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                val info =
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO) as? WifiP2pInfo
                //Logger.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION info: $info")
                if (info != null && info.groupFormed) {
                    isConnected = true
                    callbacks.onDeviceConnected(info)
                    val groupInfo =
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP) as? WifiP2pGroup
                    if (groupInfo != null) {
                        Logger.d(TAG, "WifiDirectManager group: ", groupInfo.networkName)
                    }
                } else {
                    isConnected = false

                    // p2p连接断开后，使当前设备可被发现
                    if (context.connectionType == CONNECTION_TYPE_WIFIDIRECT) {
                        discoverableTask.discoverable()
                    }
                }
              //  Logger.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION ", isConnected)
            }
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val p2pIsEnable = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    WifiP2pManager.WIFI_P2P_STATE_DISABLED
                )
                if (p2pIsEnable != WifiP2pManager.WIFI_P2P_STATE_ENABLED) isConnected = false
            }
        }
    }
}