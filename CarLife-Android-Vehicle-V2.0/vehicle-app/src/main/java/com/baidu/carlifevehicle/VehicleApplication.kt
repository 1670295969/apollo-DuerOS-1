package com.baidu.carlifevehicle

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Point
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.Configs.*
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.Constants.TAG
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlife.sdk.internal.DisplaySpec
import com.baidu.carlife.sdk.util.Logger
import com.baidu.carlifevehicle.audio.recorder.VoiceManager
import com.baidu.carlifevehicle.audio.recorder.VoiceMessageHandler
import com.baidu.carlifevehicle.protocol.ControllerHandler
import com.baidu.carlifevehicle.util.*
import com.baidu.carlifevehicle.util.CarlifeConfUtil.CONNECT_SUCCESS_SHOW_UI
import com.baidu.carlifevehicle.util.CarlifeConfUtil.KEY_INT_AUDIO_TRANSMISSION_MODE
import com.baidu.carlifevehicle.util.CommonParams.CONNECT_TYPE_SHARED_PREFERENCES

class VehicleApplication : Application() {

    var vehicleBind: VehicleService.VehicleBind? = null

    companion object {
        lateinit var app: Application

        const val VID_ACCESSORY = 0x18D1
        const val PID_ACCESSORY_ONLY = 0x2D00
        const val PID_ACCESSORY_AUDIO_ADB_BULK = 0x2D05

    }

//    private fun getDisplayId() : Int {
//        return display?.displayId ?: 0
//    }


    public fun getNowDisplayId() : Int {
        val wm  = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.defaultDisplay.displayId
    }

    fun getNeedMetrics() : Point {
        return DisplayUtils.getNeedMetrics(this)
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        Log.d(
            "VehicleApplication",
            "VehicleApplication getNowDisplayId = ${getNowDisplayId()}"
        )


        PreferenceUtil.getInstance().init(this)
        resetUsbDeviceIfNecessary()
        CarlifeConfUtil.getInstance().init()
        initReceiver()
        bindVehicleService()

        val sharedPreferences = PreferenceUtil.getInstance().preferences
        val result = sharedPreferences?.getBoolean(CONNECT_SUCCESS_SHOW_UI, false) ?: false
        if (result) {
            CarLife.receiver().connect()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, CarlifeMediaSessionService::class.java))
        } else {
            startService(Intent(this, CarlifeMediaSessionService::class.java))
        }

        HotspotUtils.openHot()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initReceiver() {

        /**
         * 获取屏幕的宽高，默认采用16：9的分辨率
         * 如车厂为宽屏车机，车厂可自定义修改为8：3
         */
        val screenPoint = getNeedMetrics();
        val screenWidth = screenPoint.x
        val screenHeight = screenPoint.y
        Log.d(
            "VehicleApplication",
            "VehicleApplication.oncreate ${screenWidth}:${screenHeight}"
        )
        val frameRate = PreferenceUtil.getInstance().getString("CONFIG_VIDEO_FRAME_RATE","30")
        val displaySpec = DisplaySpec(
            this,
            screenWidth,
            screenHeight,
            frameRate.toInt()
        )

        if (!PreferenceUtil.getInstance().contains(CONNECT_TYPE_SHARED_PREFERENCES)){
            PreferenceUtil.getInstance()
                .putInt(CONNECT_TYPE_SHARED_PREFERENCES, CarLifeContext.CONNECTION_TYPE_AOA)
        }
        /**
         * 连接方式默认直连连接
         */
        val type = PreferenceUtil.getInstance()
            .getInt(CONNECT_TYPE_SHARED_PREFERENCES, CarLifeContext.CONNECTION_TYPE_WIFIDIRECT)

        val audioTransmissionMode = PreferenceUtil.getInstance()
            .getBoolean(KEY_INT_AUDIO_TRANSMISSION_MODE, false)

        val audioMode = if (audioTransmissionMode) 1 else 0

        val accSupportEnable = PreferenceUtil.getInstance().getBoolean("AAC_SUPPORT",false)
        val accSupport = if (accSupportEnable) 1 else 0
        /**
         * 车机端的支持的Feature，会通过协议传给手机端
         */
        val features = mapOf(
            FEATURE_CONFIG_USB_MTU to 32 * 1024,
            FEATURE_CONFIG_I_FRAME_INTERVAL to 300,
            FEATURE_CONFIG_CONNECT_TYPE to type,
            FEATURE_CONFIG_AAC_SUPPORT to accSupport,
            FEATURE_CONFIG_AUDIO_TRANSMISSION_MODE to audioMode,
            FEATURE_CONFIG_MUSIC_HUD to 1
        )

        /**
         * 车机端本地的一些配置
         */
        val configs = mapOf(
            CONFIG_LOG_LEVEL to Log.DEBUG,
            CONFIG_USE_ASYNC_USB_MODE to false,
            CONFIG_PROTOCOL_VERSION to 4
        )

        Logger.d(
            Constants.TAG,
            "VehicleApplication initReceiver $screenWidth, $screenHeight $displaySpec"
        )

        /**
         * 初始化CarLifeReceiver,需要在主线程调用
         */
        CarLife.init(
            this,
            "20029999",
            "12345678",
            features,
            CarlifeActivity::class.java,
            configs
        )

        // 车机录音初始化
        VoiceManager.init(CarLife.receiver())

        // 设置车机分辨率，这里会传递给手机端，手机端会根据此分辨率传递视频帧到车机
        CarLife.receiver().setDisplaySpec(displaySpec)

        // 注册接受车机收音消息逻辑
        CarLife.receiver().registerTransportListener(VoiceMessageHandler())

        // 注册other消息处理
        CarLife.receiver().registerTransportListener(ControllerHandler())



        /**
         * 下面这段代码用于示范消息订阅相关实例
         * 比如车机订阅手机的GPS信息等
        CarLife.receiver().addSubscriber(AssistantGuideSubscriber(CarLife.receiver()))
        CarLife.receiver().addSubscriber(TurnByTurnSubscriber(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataGPSSubscribable(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataVelocitySubscribable(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataGyroscopeSubscribable(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataAccelerationSubscribable(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataGearSubscribable(CarLife.receiver()))
        CarLife.receiver().addSubscribable(CarDataOilSubscribable(CarLife.receiver()))
         */
    }

    val vehicleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
           // ("Not yet implemented")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            vehicleBind = service as VehicleService.VehicleBind
        }
    }

    /**
     * CarLife起来时，拉起VehicleService服务，用于CarLife后台连接成功时可以自动被拉到前台显示，
     * 车厂可根据需求选择是否需要此服务.
     */
    private fun bindVehicleService() {
        val intent = Intent(this, VehicleService::class.java)
        bindService(intent, vehicleConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 1、重置一下USB设备端口，防止又脏数据；
     * 2、如车厂有自己的重置USB方式，此方法可以不调用。
     */
    private fun resetUsbDeviceIfNecessary() {

        val usbManager = applicationContext.getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in usbManager.deviceList.values) {
            if (isAccessory(device)) {
                try {
                    val usbConnection = usbManager.openDevice(device)
                    usbConnection.releaseInterface(device.getInterface(0))
                    val resetDevice = UsbDeviceConnection::class.java.getMethod("resetDevice")
                    val result = resetDevice.invoke(usbConnection) // 调用反射方法，得到返回值
                    Log.e(TAG, "USB Device resetDevice result: $result")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 判断当前设备是否处于AOA配件模式；
     * 此判断用于重置usb时使用，如车厂有其他方式重置，可忽略此方法.
     * 详情可参考：https://source.android.com/devices/accessories/aoa
     */
    private fun isAccessory(usbDevice: UsbDevice): Boolean {
        return usbDevice.vendorId == VID_ACCESSORY
                && usbDevice.productId >= PID_ACCESSORY_ONLY
                && usbDevice.productId <= PID_ACCESSORY_AUDIO_ADB_BULK
    }
}