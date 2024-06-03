package com.baidu.carlifevehicle

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.baidu.carlife.protobuf.CarlifeBTHfpCallStatusCoverProto.CarlifeBTHfpCallStatusCover
import com.baidu.carlife.protobuf.CarlifeCarHardKeyCodeProto
import com.baidu.carlife.protobuf.CarlifeConnectExceptionProto.CarlifeConnectException
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.CarLifeModule
import com.baidu.carlife.sdk.Configs
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.Constants.MSG_CHANNEL_TOUCH
import com.baidu.carlife.sdk.Constants.VALUE_PROGRESS_100
import com.baidu.carlife.sdk.WirlessStatusListener
import com.baidu.carlife.sdk.internal.protocol.CarLifeMessage
import com.baidu.carlife.sdk.internal.protocol.CarLifeMessage.Companion.obtain
import com.baidu.carlife.sdk.internal.protocol.ServiceTypes
import com.baidu.carlife.sdk.internal.transport.TransportListener
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlife.sdk.receiver.CarLife.receiver
import com.baidu.carlife.sdk.receiver.ConnectProgressListener
import com.baidu.carlife.sdk.receiver.FileTransferListener
import com.baidu.carlife.sdk.receiver.OnPhoneStateChangeListener
import com.baidu.carlife.sdk.receiver.view.RemoteDisplayGLView
import com.baidu.carlife.sdk.util.Logger
import com.baidu.carlifevehicle.access.AccessibilityUtils
import com.baidu.carlifevehicle.access.MyAccessibilityService
import com.baidu.carlifevehicle.audio.recorder.VoiceManager
import com.baidu.carlifevehicle.fragment.BaseFragment
import com.baidu.carlifevehicle.fragment.CarLifeFragmentManager
import com.baidu.carlifevehicle.fragment.ExceptionFragment
import com.baidu.carlifevehicle.fragment.LaunchFragment
import com.baidu.carlifevehicle.fragment.MainFragment
import com.baidu.carlifevehicle.fragment.NewUserGuideFragment
import com.baidu.carlifevehicle.fragment.TouchFragment
import com.baidu.carlifevehicle.message.MsgBaseHandler
import com.baidu.carlifevehicle.message.MsgHandlerCenter
import com.baidu.carlifevehicle.module.MusicModule
import com.baidu.carlifevehicle.module.NavModule
import com.baidu.carlifevehicle.module.PhoneModule
import com.baidu.carlifevehicle.module.VRModule
import com.baidu.carlifevehicle.util.CarlifeConfUtil
import com.baidu.carlifevehicle.util.CarlifeUtil
import com.baidu.carlifevehicle.util.CommonParams
import com.baidu.carlifevehicle.util.CommonParams.KEYCODE_MAIN
import com.baidu.carlifevehicle.util.HotspotUtils
import com.baidu.carlifevehicle.util.HotspotUtils.openHot
import com.baidu.carlifevehicle.util.PreferenceUtil
import com.baidu.carlifevehicle.view.CarlifeMessageDialog
import com.baidu.carlifevehicle.view.FloatWindowManager
import com.permissionx.guolindev.PermissionX


class CarlifeActivity : AppCompatActivity(), ConnectProgressListener,
    TransportListener, View.OnClickListener, OnPhoneStateChangeListener, WirlessStatusListener {

    public var mIsConnectException = false
    private lateinit var mSurfaceView: RemoteDisplayGLView
    private var mSurface: Surface? = null
    private lateinit var mRootView: ViewGroup
    private lateinit var btHardKeyCode: Button
    private lateinit var mVehicleControlHandler: TransportListener
    private lateinit var mPhoneModule: CarLifeModule
    private lateinit var mMusicModule: CarLifeModule
    private lateinit var mNavModule: CarLifeModule
    private lateinit var mVRModule: CarLifeModule
    private var mCarLifeFragmentManager: CarLifeFragmentManager? = null
    private var mExitAppDialog: CarlifeMessageDialog? = null

    private var mHasEverConnect = false
    private var mMainActivityHandler: MsgBaseHandler? = null
    private var mIsCallCoverShowed = false
    private var callStatus = 0
    private var phoneNum = ""
    private var phoneName = ""
    private var mIsCalling: Boolean = false
    private var mIsCallComing: Boolean = false
    private var mIsInitConfig: Boolean = false

    companion object {
        const val TAG = "CarlifeActivity"
    }

    private val mediaSessionCompat by lazy {
        MediaSessionCompat(this, "CarlifeActivity")
    }

//    val windowInsetsController by lazy {
//        WindowCompat.getWindowInsetsController(window, window.decorView)
//    }


// Hide the system bars.


    private fun hideStatusAndNaviBar(){
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全屏显示，隐藏状态栏和导航栏，拉出状态栏和导航栏显示一会儿后消失。
        // 全屏显示，隐藏状态栏和导航栏，拉出状态栏和导航栏显示一会儿后消失。
        hideStatusAndNaviBar()

        //View.SYSTEM_UI_FLAG_IMMERSIVE
        setContentView(R.layout.activity_main)
        mSurfaceView = findViewById(R.id.video_surface_view)

        mRootView = findViewById(R.id.root_view)

        mCarLifeFragmentManager = CarLifeFragmentManager(this)
        // initialize basefragment, must be called before using it's subclass
        BaseFragment.initBeforeAll(this)
        mCarLifeFragmentManager!!.showFragment(LaunchFragment.getInstance())

        btHardKeyCode = findViewById(R.id.bt_hard)
        btHardKeyCode.setOnClickListener(this)

        mPhoneModule = PhoneModule(CarLife.receiver(), this)
        mMusicModule = MusicModule(CarLife.receiver())
        mNavModule = NavModule(CarLife.receiver())
        mVRModule = VRModule(CarLife.receiver())
        CarLife.receiver().addModule(mPhoneModule)
        CarLife.receiver().addModule(mMusicModule)
        CarLife.receiver().addModule(mNavModule)
        CarLife.receiver().addModule(mVRModule)
        CarLife.receiver().addConnectProgressListener(this)
        CarLife.receiver().registerTransportListener(this)
        CarLife.receiver().registerWirlessStatusListeners(this)
        mVehicleControlHandler = VehicleControlHandler()
        CarLife.receiver().registerTransportListener(mVehicleControlHandler)
        CarLife.receiver().setFileTransferListener(FileTransferListener { file ->
            Logger.d("zwh", "file>>>>>", file.absolutePath)
            ApkInstall.installApk(this@CarlifeActivity, file.path)
        })

        requestPermission()
        initCarBluetoothInfo()
        ControlTestWindow.getInstance().init(this@CarlifeActivity, mRootView)

        mMainActivityHandler = MsgMainActivityHandler()
        MsgHandlerCenter.registerMessageHandler(mMainActivityHandler)
        (mMainActivityHandler as MsgMainActivityHandler).sendEmptyMessageDelayed(
            CommonParams.MSG_CONNECT_INIT,
            500
        )

        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSessionCompat.isActive = true
        MediaControllerCompat.setMediaController(this, mediaSessionCompat.controller)

        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(intent: Intent?): Boolean {
                Log.d(TAG, "intent=$intent")
                var keyEvent: KeyEvent? = null
                if (intent != null) {
                    if ("android.intent.action.MEDIA_BUTTON" == intent.action) {
                        keyEvent = intent.getParcelableExtra(
                            "android.intent.extra.KEY_EVENT"
                        ) as KeyEvent?
                        if (keyEvent != null) {
                            val action = keyEvent.action
                            val keyCode = keyEvent.keyCode
                            if (action == KeyEvent.ACTION_UP) {
                                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_ADD)
                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_SUB)
                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                                ) {
                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_STOP)
                                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                                ) {
                                    sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_START)
                                }
                            }
                        }

                    }
                }

                return super.onMediaButtonEvent(intent)
            }

        }, null)

        AccessibilityUtils.setAccessibilityService(this, ComponentName(this,MyAccessibilityService::class.java))
        startService(Intent(this,MyAccessibilityService::class.java))
        HotspotUtils.openHot()


    }



    fun sendHardKeyCodeEvent(keycode: Int) {
        try {
            Log.d(TAG, "sendHardKeyCodeEvent: keycode = $keycode")
            val message = obtain(
                MSG_CHANNEL_TOUCH,
                ServiceTypes.MSG_TOUCH_CAR_HARD_KEY_CODE,
                0
            )
            message.serviceType = CommonParams.MSG_TOUCH_CAR_HARD_KEY_CODE
            message.payload(
                CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder()
                    .setKeycode(keycode)
                    .build()
            )
            receiver().postMessage(message)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun getCarLifeVehicleFragmentManager(): CarLifeFragmentManager? {
        return mCarLifeFragmentManager
    }

    private fun initCarBluetoothInfo() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        PreferenceUtil.getInstance().putString(Configs.CONFIG_HU_BT_NAME, bluetoothAdapter.name)
        receiver().setConfig(Configs.CONFIG_HU_BT_NAME, bluetoothAdapter.name)
        receiver().setConfig(Configs.CONFIG_HU_BT_MAC, bluetoothAdapter.address)
//        bluetoothAdapter.name
//        bluetoothAdapter.address
    }

    private fun requestPermission() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.CAMERA
                )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "打开权限",
                    "确定",
                    "取消"
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (!allGranted) {
                    Toast.makeText(
                        this,
                        "权限未打开: $deniedList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // 配置语音录音的请求权限，暂时现这样处理，后面封装成统一的工具类
    private fun requestRecordPermission(permissionString: String?): Boolean {

        val result = ContextCompat.checkSelfPermission(this@CarlifeActivity, permissionString!!)
        Logger.e(Constants.TAG, "requestPermission>>>", result)
        var hasPermisson = false
        hasPermisson = if (result == PackageManager.PERMISSION_GRANTED) { // 没有获得权限
            true
        } else {
            Logger.e(Constants.TAG, "requestPermission>>>", permissionString)
            requestPermissions(this, arrayOf<String>(permissionString), 100)
            false
        }
        return hasPermisson
    }

    override fun onDestroy() {
        super.onDestroy()
        mSurfaceView.onDestroy()
        Logger.d(Constants.TAG, "MainActivity onDestroy")
        CarLife.receiver().removeConnectProgressListener(this)
        CarLife.receiver().unregisterTransportListener(this)
        CarLife.receiver().unregisterTransportListener(mVehicleControlHandler)
        CarLife.receiver().setFileTransferListener(null)
        CarLife.receiver().removeModule(mPhoneModule)
        CarLife.receiver().removeModule(mMusicModule)
        CarLife.receiver().removeModule(mNavModule)
        CarLife.receiver().removeModule(mVRModule)

        CarLife.receiver().unregisterWirlessStatusListeners(this)
        MsgHandlerCenter.unRegisterMessageHandler(mMainActivityHandler)


    }

    override fun onProgress(progress: Int) {
        MsgHandlerCenter.dispatchMessage(
            CommonParams.MSG_CONNECT_CHANGE_PROGRESS_NUMBER,
            progress,
            0,
            null
        )
        if (progress == VALUE_PROGRESS_100) {
            MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CMD_VIDEO_ENCODER_START)
        }
    }

    override fun onStart() {
        super.onStart()
        VoiceManager.onActivityStart()
        CarLife.receiver().onActivityStarted()
        FloatWindowManager.dismiss()
    }

    override fun onStop() {
        super.onStop()
        VoiceManager.onActivityStop()
        CarLife.receiver().onActivityStopped()
        if(PreferenceUtil.getInstance().getBoolean("show_float",true)){
            FloatWindowManager.show()
        }
    }

    override fun onBackPressed() {
        //  super.onBackPressed()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.i(TAG, "onKeyUp=$event")
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val message = obtain(
                MSG_CHANNEL_TOUCH,
                ServiceTypes.MSG_TOUCH_CAR_HARD_KEY_CODE,
                0
            )
            message.payload(
                CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder()
                    .setKeycode(KEYCODE_MAIN)
                    .build()
            )
            receiver().postMessage(message)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onConnectionAuthenFailed(context: CarLifeContext) {
        MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CONNECT_FAIL_AUTHEN_FAILED)
    }

    override fun onConnectionVersionNotSupprt(context: CarLifeContext) {
        MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CONNECT_FAIL_NOT_SURPPORT)
    }

    override fun onConnectionEstablished(context: CarLifeContext) {
        MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CONNECT_STATUS_ESTABLISHED)
    }

    override fun onConnectionAttached(context: CarLifeContext) {
        MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CONNECT_STATUS_CONNECTED)
       // hideStatusAndNaviBar()
    }

    override fun onConnectionReattached(context: CarLifeContext) {
    }

    override fun onConnectionDetached(context: CarLifeContext) {
        MsgHandlerCenter.dispatchMessage(CommonParams.MSG_CONNECT_STATUS_DISCONNECTED)
    }

    override fun onReceiveMessage(context: CarLifeContext, message: CarLifeMessage): Boolean {
        when (message.serviceType) {
            ServiceTypes.MSG_CMD_CONNECT_EXCEPTION -> {
                val response = message.protoPayload as CarlifeConnectException
                handleConnectException(response)
            }
        }
        return false
    }

    private var numClickCount = 0

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.bt_hard -> {
                if (++numClickCount == 3) {
                    ControlTestWindow.getInstance().displayWindow()
                    numClickCount = 0
                }
            }
        }
    }

    private fun openExitAppDialogOnReadConfFail() {
        mExitAppDialog = CarlifeMessageDialog(this).setTitleText(R.string.alert_quit)
            .setMessage(R.string.conf_init_fail)
            .setOnFirstBtnClickListener { exitApp() }
            .setFirstBtnText(R.string.alert_confirm)
        mExitAppDialog?.setOnDismissListener(DialogInterface.OnDismissListener {
            mExitAppDialog = null
        })

        if (!mExitAppDialog!!.isShowing) {
            try {
                mExitAppDialog?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun exitApp() {
        CarLife.receiver().disconnect()
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun openExitAppDialog() {
        mExitAppDialog = CarlifeMessageDialog(this).setTitleText(R.string.alert_quit)
            .setMessage(R.string.alert_quit_app_content).setFirstBtnText(R.string.alert_confirm)
            .setFirstBtnTextColorHighLight().setOnFirstBtnClickListener { exitApp() }
            .setSecondBtnText(R.string.alert_cancel)
        mExitAppDialog?.setOnDismissListener(DialogInterface.OnDismissListener {
            mExitAppDialog = null
        })
        if (!mExitAppDialog!!.isShowing) {
            try {
                mExitAppDialog?.show()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class MsgMainActivityHandler : MsgBaseHandler() {

        override fun handleMessage(msg: Message) {
            try {
                Logger.d(
                    TAG,
                    "MsgMainActivityHandler handleMessage get msg: " + CommonParams.getMsgName(msg.what)
                )
                when (msg.what) {
                    CommonParams.MSG_CONNECT_INIT -> {
                        if (CarlifeConfUtil.getInstance().readConfStatus) {
                            init()
                        } else {
                            if (!CarlifeConfUtil.getInstance().isReadMaxTime) {
                                Logger.d(TAG, "read conf again")
                                CarlifeConfUtil.getInstance().init()
                                sendEmptyMessageDelayed(CommonParams.MSG_CONNECT_INIT, 500)
                            } else {
                                openExitAppDialogOnReadConfFail()
                            }
                        }
                    }

                    CommonParams.MSG_CONNECT_STATUS_CONNECTED -> {
                        saveConnectStatus(true)
                        hideSystemUi()
                        //TODO remove
                        //  mCarLifeFragmentManager?.removeCurrentFragment()
                    }

                    CommonParams.MSG_MAIN_DISPLAY_USER_GUIDE_FRAGMENT -> {
                        if (!mHasEverConnect) {
                            mCarLifeFragmentManager?.showFragment(NewUserGuideFragment.getInstance())
                        }
                    }

                    CommonParams.MSG_MAIN_DISPLAY_MAIN_FRAGMENT -> {
                        if (!mIsInitConfig) {
                            // 如果配置加载失败，则延时500再回来执行。保证配置加载成功再去连接手机端
                            Logger.d(TAG, "mIsInitConfig is $mIsInitConfig, so delay 500")
                            MsgHandlerCenter.dispatchMessageDelay(
                                CommonParams.MSG_MAIN_DISPLAY_MAIN_FRAGMENT,
                                500
                            )
                            return
                        }

                        if (CarLife.receiver().isAttached()) {
                            MsgHandlerCenter.dispatchMessage(CommonParams.MSG_MAIN_DISPLAY_TOUCH_FRAGMENT)
                        } else {
                            if (mCarLifeFragmentManager != null) {
                                mCarLifeFragmentManager!!.showFragment(MainFragment.getInstance())
                            }
                        }
                    }

                    CommonParams.MSG_MAIN_DISPLAY_EXCEPTION_FRAGMENT -> {
                        if (mCarLifeFragmentManager == null) {
                            return
                        }
                        mCarLifeFragmentManager!!.showFragment(ExceptionFragment.getInstance())
                        Logger.d(TAG, "mIsCalling=$mIsCalling")
                        if (mIsCalling) {
                            if (mIsCallComing) {
                                ExceptionFragment.getInstance()
                                    .changeTipsCallback(resources.getString(R.string.line_is_coming))
                                ExceptionFragment.getInstance()
                                    .changeDrawableCallback(R.drawable.car_ic_incoming)
                                ExceptionFragment.getInstance().setStartAppBtnHide()
                            } else {
                                ExceptionFragment.getInstance()
                                    .changeTipsCallback(resources.getString(R.string.line_is_busy))
                                ExceptionFragment.getInstance()
                                    .changeDrawableCallback(R.drawable.car_ic_calling)
                                ExceptionFragment.getInstance().setStartAppBtnHide()
                            }
                        } else {
                            ExceptionFragment.getInstance()
                                .changeTipsCallback(resources.getString(R.string.connect_screenoff_hint))
                            ExceptionFragment.getInstance().setStartAppBtnVisible()
                            ExceptionFragment.getInstance()
                                .changeDrawableCallback(R.drawable.car_ic_click)
                        }
                    }

                    CommonParams.MSG_MAIN_DISPLAY_TOUCH_FRAGMENT -> {
                        // Recover call status cover page only when in ringing
                        if (!CarLife.receiver().isAttached()) return
                        if (mIsCallCoverShowed && callStatus == 1) {
                            val builder = CarlifeBTHfpCallStatusCover.newBuilder()
                            if (builder != null) {
                                Logger.d(
                                    "Bt",
                                    "Recover callstatus cover on reception of foreground message"
                                )
                                builder.state = callStatus
                                if (TextUtils.isEmpty(phoneNum)) {
                                    builder.phoneNum = ""
                                } else {
                                    builder.phoneNum = phoneNum
                                }
                                if (TextUtils.isEmpty(phoneName)) {
                                    builder.name = ""
                                } else {
                                    builder.name = phoneName
                                }

                                var message = CarLifeMessage.obtain(
                                    Constants.MSG_CHANNEL_CMD,
                                    ServiceTypes.MSG_CMD_BT_HFP_CALL_STATUS_COVER
                                )
                                message.payload(builder.build())

                                CarLife.receiver().postMessage(message)
                                changeSize()
                            }
                        } else {
                            if (mCarLifeFragmentManager != null) {
                                mCarLifeFragmentManager!!.showFragment(TouchFragment.getInstance())
                            }
                        }
                    }

                    CommonParams.MSG_CONNECT_STATUS_DISCONNECTED -> {
                        mIsConnectException = false
                        if (mCarLifeFragmentManager != null) {
                            mCarLifeFragmentManager!!.showFragment(MainFragment.getInstance())
                        }
                    }
                }
            } catch (ex: java.lang.Exception) {
                Logger.e(TAG, "handle message exception", ex)
                ex.printStackTrace()
            }
        }

        override fun careAbout() {
            addMsg(CommonParams.MSG_CONNECT_INIT)
            addMsg(CommonParams.MSG_CONNECT_STATUS_CONNECTED)
            addMsg(CommonParams.MSG_CONNECT_STATUS_DISCONNECTED)
            addMsg(CommonParams.MSG_MAIN_DISPLAY_USER_GUIDE_FRAGMENT)
            addMsg(CommonParams.MSG_MAIN_DISPLAY_MAIN_FRAGMENT)
            addMsg(CommonParams.MSG_MAIN_DISPLAY_TOUCH_FRAGMENT)
            addMsg(CommonParams.MSG_MAIN_DISPLAY_EXCEPTION_FRAGMENT)
        }

    }

    override fun onResume() {
        super.onResume()
        changeSize()
        hideSystemUi()
        //hideStatusAndNaviBar()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun hideSystemUi() {
        window.addFlags(1024)
        window.decorView.systemUiVisibility = 4102
    }

    private fun changeSize() {
        val sharedPreferences: SharedPreferences = PreferenceUtil.getInstance().preferences
        val forceFullScreen = sharedPreferences.getBoolean("FORCE_FULL_SCREEN", false)
        if (forceFullScreen) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            var w = displayMetrics.widthPixels.toString()
            val forceWidth = sharedPreferences.getString("FORCE_FULL_SCREEN_WIDTH", w)!!

            var h = displayMetrics.heightPixels.toString()
            val forceHigh = sharedPreferences.getString("FORCE_FULL_SCREEN_HEIGHT", h)!!
            applicationContext.resources.displayMetrics.widthPixels = forceWidth.toInt()
            applicationContext.resources.displayMetrics.heightPixels = forceHigh.toInt()
            val remoteDisplayGLView: RemoteDisplayGLView = this.mSurfaceView
            remoteDisplayGLView.post {
                remoteDisplayGLView.onVideoSizeChanged(forceWidth.toInt(), forceHigh.toInt())
            }
        }


    }


    fun init() {
        Logger.e(TAG, "++++++++++++++++++++Baidu Carlife Begin++++++++++++++++++++")

        // 根据配置文件设置相应的配置
        CarLife.receiver().run {
            setFeature(
                Configs.FEATURE_CONFIG_VOICE_WAKEUP,
                CarlifeConfUtil.getInstance().getIntProperty(Configs.FEATURE_CONFIG_VOICE_WAKEUP)
            )
            setFeature(
                Configs.FEATURE_CONFIG_VOICE_MIC,
                CarlifeConfUtil.getInstance().getIntProperty(Configs.FEATURE_CONFIG_VOICE_MIC)
            )
            setFeature(
                Configs.FEATURE_CONFIG_BLUETOOTH_INTERNAL_UI,
                CarlifeConfUtil.getInstance()
                    .getIntProperty(Configs.FEATURE_CONFIG_BLUETOOTH_INTERNAL_UI)
            )
            setFeature(
                Configs.FEATURE_CONFIG_FOCUS_UI,
                CarlifeConfUtil.getInstance().getIntProperty(Configs.FEATURE_CONFIG_FOCUS_UI)
            )
        }

        if (!TextUtils.isEmpty(
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_WIFI_DIRECT_NAME)
            )
        ) {
            CarLife.receiver().setConfig(
                Configs.CONFIG_WIFI_DIRECT_NAME,
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_WIFI_DIRECT_NAME)
            )
        }

        if (!TextUtils.isEmpty(
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_TARGET_BLUETOOTH_NAME)
            )
        ) {
            CarLife.receiver().setConfig(
                Configs.CONFIG_TARGET_BLUETOOTH_NAME,
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_TARGET_BLUETOOTH_NAME)
            )
        }

        if (!TextUtils.isEmpty(
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_HU_BT_NAME)
            )
        ) {
            CarLife.receiver().setConfig(
                Configs.CONFIG_HU_BT_NAME,
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_HU_BT_NAME)
            )
        }

        if (!TextUtils.isEmpty(
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_HU_BT_MAC)
            )
        ) {
            CarLife.receiver().setConfig(
                Configs.CONFIG_HU_BT_MAC,
                CarlifeConfUtil.getInstance().getStringFromMap(Configs.CONFIG_HU_BT_MAC)
            )
        }

        CarLife.receiver().setConfig(
            Configs.CONFIG_LOG_LEVEL,
            CarlifeConfUtil.getInstance().getIntProperty(Configs.CONFIG_LOG_LEVEL)
        )
        CarLife.receiver().setConfig(
            Configs.CONFIG_WIRLESS_TYPE,
            CarlifeConfUtil.getInstance().getIntProperty(Configs.CONFIG_WIRLESS_TYPE)
        )
        CarLife.receiver().setConfig(
            Configs.CONFIG_WIRLESS_FREQUENCY,
            CarlifeConfUtil.getInstance().getIntProperty(Configs.CONFIG_WIRLESS_FREQUENCY)
        )
        CarLife.receiver().setConfig(
            Configs.CONFIG_SAVE_AUDIO_FILE,
            CarlifeConfUtil.getInstance().getBooleanProperty(Configs.CONFIG_SAVE_AUDIO_FILE)
        )

        // 根据渠道号构建static info 信息
        CarLife.receiver().initStatisticsInfo(CommonParams.vehicleChannel, "12345678")
        mIsInitConfig = true;

    }

    fun saveConnectStatus(status: Boolean) {
        try {
            PreferenceUtil.getInstance().putBoolean(
                CommonParams.CONNECT_STATUS_SHARED_PREFERENCES,
                CommonParams.CONNECT_STATUS, status
            )
        } catch (ex: java.lang.Exception) {
            Logger.e(TAG, "save connect status error")
            ex.printStackTrace()
        }
    }

    override fun onStateChanged(isCalling: Boolean, isCallComing: Boolean) {
        mIsCalling = isCalling
        mIsCallComing = isCallComing
    }

    private fun handleConnectException(exception: CarlifeConnectException) {
        var hintResStr: String? = null
        when (exception.exceptionType) {
            Constants.VIDEO_PERMISSION_DENIED -> {
                hintResStr = resources.getString(R.string.carlife_video_permission_denied_hint)
                mIsConnectException = true
            }

            Constants.VIDEO_ENCODER_EROOR -> {
                hintResStr = resources.getString(R.string.carlife_phone_not_support_hint)
                mIsConnectException = true
            }

            Constants.VIDEO_PAUSE_BY_SCREENSHARE_REQUEST -> {
                hintResStr = resources.getString(R.string.carlife_video_permission_hint)
            }
        }

        if (hintResStr == null) {
            return
        }

        if (mCarLifeFragmentManager != null) {
            mCarLifeFragmentManager!!.showFragment(MainFragment.getInstance())
        }

        MainFragment.getInstance().updateExceptionTips(hintResStr)
    }

    override fun onDeviceWirlessStatus(status: Int) {
        when (status) {
            Constants.VALUE_LOW_POWER -> {
                CarlifeUtil.showToastInUIThread(R.string.carlife_toast_low_power)
            }

            Constants.VALUE_NO_WIFI -> {
                CarlifeUtil.showToastInUIThread(R.string.carlife_toast_no_wifi)
            }
        }
    }
}