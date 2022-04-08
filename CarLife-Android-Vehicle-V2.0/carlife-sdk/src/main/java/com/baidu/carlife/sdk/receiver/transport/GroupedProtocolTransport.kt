package com.baidu.carlife.sdk.receiver.transport

import android.content.Intent
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.Configs.FEATURE_CONFIG_CONNECT_TYPE
import com.baidu.carlife.sdk.internal.protocol.CarLifeMessage
import com.baidu.carlife.sdk.internal.transport.MessageDispatcher
import com.baidu.carlife.sdk.internal.transport.ProtocolTransport
import com.baidu.carlife.sdk.internal.transport.TransportListener
import com.baidu.carlife.sdk.receiver.transport.aoa.AOAProtocolTransport
import com.baidu.carlife.sdk.receiver.transport.wirless.WirlessP2PProtocolTransport
import com.baidu.carlife.sdk.receiver.transport.wirless.WirlessAPProtocolTransport

class GroupedProtocolTransport(private val context: CarLifeContext) :
    ProtocolTransport.ConnectionListener, TransportListener {
    private val transports = mutableListOf<ProtocolTransport>()

    private var messageDispatcher: MessageDispatcher? = null

    init {
        // init transports
        configConnectType()
    }

    fun connect() {
        if (transports.isEmpty()) {
            configConnectType()
        }
        transports.forEach {
            it.connect()
        }
    }

    fun stopConnect() {
        while (transports.isNotEmpty()) {
            transports.removeFirst().terminate()
        }
    }

    fun configConnectType() {
        stopConnect()
        when (context.getFeature(FEATURE_CONFIG_CONNECT_TYPE, CarLifeContext.CONNECTION_TYPE_AOA)) {
            CarLifeContext.CONNECTION_TYPE_HOTSPOT -> {
                transports.add(WirlessAPProtocolTransport(context, this))
                context.connectionType = CarLifeContext.CONNECTION_TYPE_HOTSPOT
            }
            CarLifeContext.CONNECTION_TYPE_WIFIDIRECT -> {
                transports.add(WirlessP2PProtocolTransport(context, this))
                context.connectionType = CarLifeContext.CONNECTION_TYPE_WIFIDIRECT
            }
            else -> {
                transports.add(AOAProtocolTransport(context, this))
                context.connectionType = CarLifeContext.CONNECTION_TYPE_AOA
            }
        }
    }

    fun ready() {
        transports.forEach { it.ready() }
    }

    fun onNewIntent(intent: Intent) {
        transports.forEach { it.onNewIntent(intent) }
    }

    fun terminate() {
        transports.forEach { it.terminate() }
    }

    fun postMessage(message: CarLifeMessage) {
        messageDispatcher?.postMessage(message);
    }

    fun sendMessage(message: CarLifeMessage) {
        messageDispatcher?.sendMessage(message)
    }

    /**
     * this function only use for simulate receive message
     */
    fun dispatchMessage(message: CarLifeMessage) {
        messageDispatcher?.dispatchMessage(message)
    }

    override fun onConnectionAttached(transport: ProtocolTransport) {
        transports.forEach {
            if (transport === it) {
                messageDispatcher = MessageDispatcher(context, transport)
                if (transport is AOAProtocolTransport) {
                    context.connectionType = CarLifeContext.CONNECTION_TYPE_AOA
                } else if (transport is WirlessAPProtocolTransport) {
                    context.connectionType = CarLifeContext.CONNECTION_TYPE_HOTSPOT
                } else if (transport is WirlessP2PProtocolTransport) {
                    context.connectionType = CarLifeContext.CONNECTION_TYPE_WIFIDIRECT
                }
            } else {
                // 关掉其他连接方式，避免不必要的资源浪费
                it.terminate()
            }
        }
        context.isVersionSupport = true
        context.onConnectionAttached()
        messageDispatcher?.start()
    }

    override fun onConnectionDetached(transport: ProtocolTransport) {
        if (transport == messageDispatcher?.transport) {
            // 是当前transport才回调detach
            messageDispatcher?.terminate()
            messageDispatcher = null

            // 连接断开，一秒之后重新连接
            // 如果车机与手机版本不匹配，则不触发重新连接
            if (context.isVersionSupport) {
                context.postDelayed({ connect() }, 1000)
            } else {
                ready()
            }

            context.onConnectionDetached()
        }
    }
}