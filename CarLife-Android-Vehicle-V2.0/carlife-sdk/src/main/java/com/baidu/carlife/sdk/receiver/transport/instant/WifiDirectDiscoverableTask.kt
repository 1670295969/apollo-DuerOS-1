package com.baidu.carlife.sdk.receiver.transport.instant

import android.net.wifi.p2p.WifiP2pManager
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.util.Logger
import com.baidu.carlife.sdk.util.TimerUtils
import com.baidu.carlife.sdk.util.wifip2p.WifiP2pOperationSequence
import com.baidu.carlife.sdk.util.wifip2p.operations.DiscoverPeersOperation
import com.baidu.carlife.sdk.util.wifip2p.operations.StopPeerDiscoverOperation

class WifiDirectDiscoverableTask(val wifiP2pManager: WifiP2pManager,
                                 var channel: WifiP2pManager.Channel)
    : WifiP2pManager.ActionListener {

    private val operationSequence = WifiP2pOperationSequence(this)

    init {
        operationSequence.add(StopPeerDiscoverOperation(wifiP2pManager, channel))
        operationSequence.add(DiscoverPeersOperation(wifiP2pManager, channel))
    }

    fun updateChannel(channel : WifiP2pManager.Channel){
        this.channel = channel
    }
    fun discoverable() {
        operationSequence.execute()
    }

    fun stopDiscoverable() {
        TimerUtils.stop(mDiscoverPeers)
    }

    fun terminate() {
        stopDiscoverable()
        wifiP2pManager.cancelConnect(channel, object :WifiP2pManager.ActionListener{
            override fun onSuccess() {
                Logger.d(WifiDirectManager.TAG, "cancelConnect onSuccess")
            }

            override fun onFailure(reason: Int) {
                Logger.d(WifiDirectManager.TAG, "cancelConnect onFailure")
            }
        })
    }

    // WifiP2pOperationSequence.Callback
    override fun onSuccess() {
        Logger.d(WifiDirectManager.TAG, "WifiDirectDiscoverableTask onSuccess")
        TimerUtils.stop(mDiscoverPeers)
    }

    override fun onFailure(error: Int) {
        Logger.d(WifiDirectManager.TAG, "WifiDirectDiscoverableTask onFailure ", error)

        // 扫描失败后，两秒之后继续扫描
        TimerUtils.schedule(mDiscoverPeers, 2000, 2000)
    }

    private val mDiscoverPeers = Runnable {
        // 每隔2秒扫描一次
        Logger.d(WifiDirectManager.TAG, "WifiDirectDiscoverableTask DiscoverPeers")
        operationSequence.execute()
    }
}