package com.baidu.carlife.sdk.util.wifip2p.operations

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.util.wifip2p.WifiP2pOperation

class DiscoverPeersOperation(private val wifiP2pManager: WifiP2pManager,
                             private var channel: WifiP2pManager.Channel): WifiP2pOperation {
    override fun updateChannel(channel: WifiP2pManager.Channel){
        this.channel = channel
    }
    @SuppressLint("MissingPermission")
    override fun execute(listener: WifiP2pManager.ActionListener) {
        Log.i(Constants.BLUETOOH_TAG,"DiscoverPeersOperation.execute");
        wifiP2pManager.discoverPeers(channel, listener)
    }
}