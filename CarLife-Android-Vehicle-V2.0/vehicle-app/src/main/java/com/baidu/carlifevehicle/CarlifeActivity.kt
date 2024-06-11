package com.baidu.carlifevehicle

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.baidu.carlife.protobuf.CarlifeBTHfpCallStatusCoverProto.CarlifeBTHfpCallStatusCover
import com.baidu.carlife.protobuf.CarlifeCarHardKeyCodeProto
import com.baidu.carlife.protobuf.CarlifeConnectExceptionProto.CarlifeConnectException
import com.baidu.carlife.protobuf.CarlifeMediaInfoProto
import com.baidu.carlife.protobuf.CarlifeMediaProgressBarProto
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.CarLifeModule
import com.baidu.carlife.sdk.Configs
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.Constants.MSG_CHANNEL_TOUCH
import com.baidu.carlife.sdk.Constants.VALUE_PROGRESS_100
import com.baidu.carlife.sdk.WirlessStatusListener
import com.baidu.carlife.sdk.internal.DisplaySpec
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
import com.baidu.carlifevehicle.bt.BtMusicConnection
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
import com.baidu.carlifevehicle.util.CarlifeConfUtil.CFG_AUTO_PLAY_BT_MUSIC
import com.baidu.carlifevehicle.util.CarlifeConfUtil.KEY_INT_AUDIO_TRANSMISSION_MODE
import com.baidu.carlifevehicle.util.CarlifeUtil
import com.baidu.carlifevehicle.util.CommonParams
import com.baidu.carlifevehicle.util.CommonParams.KEYCODE_MAIN
import com.baidu.carlifevehicle.util.DisplayUtils
import com.baidu.carlifevehicle.util.HotspotUtils
import com.baidu.carlifevehicle.util.NaviPos
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
        const val TYPE_SHOW_NAVI_BAR = 1
        const val TYPE_SHOW_STATUS_BAR = 2
        const val TYPE_SETTINGS_RESULT = 0x101
    }


//    val windowInsetsController by lazy {
//        WindowCompat.getWindowInsetsController(window, window.decorView)
//    }

    private val btHandler = Handler(Looper.getMainLooper())

    private val systemBarHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                TYPE_SHOW_NAVI_BAR -> {
                    showNavi()
                }

                TYPE_SHOW_STATUS_BAR -> {
                    showStatus()
                }
            }
        }
    }

    private fun removeAllStatusBarMsg() {
        systemBarHandler.removeCallbacksAndMessages(null)
    }

    private fun sendShowNaviMsg() {
        removeAllStatusBarMsg()
        systemBarHandler.sendEmptyMessageDelayed(TYPE_SHOW_NAVI_BAR, 3000)
    }

    private fun sendShowStatusBarMsg() {
        removeAllStatusBarMsg()
        systemBarHandler.sendEmptyMessageDelayed(TYPE_SHOW_STATUS_BAR, 3000)
    }


// Hide the system bars.


    private fun hideStatusAndNaviBar() {
        hideSystemUi()
//        //<string name="CFG_HIDE_STATUS_BAR">CFG_HIDE_STATUS_BAR</string>
//        //    <string name="CFG_HIDE_NAVI_BAR">CFG_HIDE_NAVI_BAR</string>
//        val statusBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_STATUS_BAR",true)
//        val naviBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_NAVI_BAR",true)
//        if (statusBar && naviBar){
//            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_FULLSCREEN
//                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//            );
//            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        } else if (statusBar){
//            showNavi()
//           // showNavi()
//        }else if (naviBar){
//            hideNavi()
//          //  showStatus()
//        }else {
//            showSystemUI()
//        }

    }

    fun getNavigationBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return (if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0).apply {
            Log.i("-----", "getNavigationBarHeight=$this")
        }
    }

    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        Log.i("-----", "getStatusBarHeight=$result")
        return result
    }

    private fun showStatus() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.show(WindowInsetsCompat.Type.statusBars()) // 隐藏状态栏
        windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars()) // 隐藏导航栏
        if (isInPip()) {
            setDockerViewPadding(0, 0)
        } else {
            setDockerViewPadding(getStatusBarHeight(), 0)
        }

        sendShowStatusBarMsg()
    }

    private fun showNavi() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars()) // 隐藏状态栏
        windowInsetsController?.show(WindowInsetsCompat.Type.navigationBars()) // 隐藏导航栏
        if (isInPip()) {
            setDockerViewPadding(0, 0)
        } else {
            setDockerViewPadding(0, getNavigationBarHeight(this@CarlifeActivity))
        }

        sendShowNaviMsg()


    }

    private fun showSystemUI() {
        //WindowCompat.setDecorFitsSystemWindows(window, false)

        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_VISIBLE)
        if (isInPip()) {
            setDockerViewPadding(0, 0)
        } else {
            setDockerViewPadding(getStatusBarHeight(), getNavigationBarHeight(this@CarlifeActivity))
        }
    }


    private fun getDisplayId(): Int {
        return windowManager.defaultDisplay.displayId
    }

    private fun isInPip(): Boolean {
        return getDisplayId() != 0
    }

    private fun resetRanderDisplayWithPIP() {
        if (getDisplayId() != 0) {
            windowManager.defaultDisplay.apply {
                Logger.d(
                    "VehicleApplication",
                    "CarlifeActivity.onCreate ${this.width}:${this.height}"
                )

                val frameRate =
                    PreferenceUtil.getInstance().getString("CONFIG_VIDEO_FRAME_RATE", "30")
                val displaySpec = DisplaySpec(
                    applicationContext,
                    width,
                    height,
                    frameRate.toInt()
                )
                CarLife.receiver().setDisplaySpec(displaySpec)
            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全屏显示，隐藏状态栏和导航栏，拉出状态栏和导航栏显示一会儿后消失。
        // 全屏显示，隐藏状态栏和导航栏，拉出状态栏和导航栏显示一会儿后消失。
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.i("OnApplyWindowInsetsListener", "$systemBarsInsets")
            insets
        }
        //  hideStatusAndNaviBar()

        //View.SYSTEM_UI_FLAG_IMMERSIVE
        resetRanderDisplayWithPIP()
        setContentView(R.layout.activity_main)
        setDockerViewPadding(0,0)

        mSurfaceView = findViewById(R.id.video_surface_view)
        supportActionBar?.hide()
        //hideSystemUi()

        Log.d(
            "VehicleApplication",
            "CarlifeActivity getDisplayId = ${getDisplayId()}"
        )

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

        AccessibilityUtils.setAccessibilityService(
            this,
            ComponentName(this, MyAccessibilityService::class.java)
        )
        startService(Intent(this, MyAccessibilityService::class.java))
        HotspotUtils.openHot()
        CarlifeMediaSessionService.start(this)
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
        if (PreferenceUtil.getInstance().getBoolean("show_float", true)) {
            FloatWindowManager.show()
        }
    }

    override fun onBackPressed() {
        //  super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == TYPE_SETTINGS_RESULT) {
            if (getDisplayId() == 0) {
                Log.d(
                    "VehicleApplication",
                    "CarlifeActivity.onActivityResult"
                )
                setCarLifeDisplay()
            }

        }
    }

    private fun setCarLifeDisplay() {
        val point = DisplayUtils.getNeedMetrics(this)
        Logger.d(
            "VehicleApplication",
            "CarlifeActivity.onCreate ${point.x}:${point.y}"
        )
        if (!CarLife.receiver().isConnected()) {
            val frameRate =
                PreferenceUtil.getInstance().getString("CONFIG_VIDEO_FRAME_RATE", "30")
            val displaySpec = DisplaySpec(
                applicationContext,
                point.x,
                point.y,
                frameRate.toInt()
            )
            CarLife.receiver().setDisplaySpec(displaySpec)
        }



        mSurfaceView.post {
            val lp = mSurfaceView.layoutParams
            lp.width = point.x
            lp.height = point.y
            mSurfaceView.layoutParams = lp
        }
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
            ServiceTypes.MSG_CMD_MEDIA_INFO -> {
//                val mediaInfo = message.protoPayload as CarlifeMediaInfoProto.CarlifeMediaInfo
//                Log.d("MSG_CMD_MEDIA_INFO","${mediaInfo.song}")
//                Log.d("MSG_CMD_MEDIA_INFO","${mediaInfo.albumArt}")
//                Log.d("MSG_CMD_MEDIA_INFO","${mediaInfo.artist}")
               // DisplayUtils.sendMediaEvent()
            }
            ServiceTypes.MSG_CMD_MEDIA_PROGRESS_BAR-> {
//                val mediaInfo = message.protoPayload as CarlifeMediaProgressBarProto.CarlifeMediaProgressBar
//                Log.d("MSG_CMD_MEDIA_INFO","${mediaInfo.progressBar}")
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
                    "MsgMainActivityHandler handleMessage get msg: " + CommonParams.getMsgName(
                        msg.what
                    )
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
                        if (PreferenceUtil.getInstance()
                                .getBoolean(KEY_INT_AUDIO_TRANSMISSION_MODE, false)
                        ) {
                            if (!BtMusicConnection.instance.isPlaying()) {
                                BtMusicConnection.instance.autoPlay()
                                if (!PreferenceUtil.getInstance()
                                        .getBoolean(CFG_AUTO_PLAY_BT_MUSIC, false)
                                ) {
                                    val am =
                                        getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                    //am.isStreamMute(AudioManager.STREAM_MUSIC)
                                    //am.isMusicActive
                                    val musicVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    if (musicVol != 0) {
                                        am.setStreamMute(AudioManager.STREAM_MUSIC, true)
                                    }

                                    btHandler.postDelayed({
                                        BtMusicConnection.instance.pause()
                                        if (musicVol != 0) {
                                            btHandler.postDelayed({
                                                am.setStreamMute(
                                                    AudioManager.STREAM_MUSIC,
                                                    false
                                                )
                                            }, 150)

                                        }
                                    }, 300)


                                }
                            }


                        }

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


    private fun setDockerViewPadding(top: Int, bottom: Int) {
        Log.d("--------","$top === $bottom")
        window.decorView.post {
            window.decorView.apply {
                if (NaviPos.isNone()) {
                    setPadding(0, top, 0, 0)
                } else if (NaviPos.isLeft()) {
                    setPadding(bottom, top, 0, 0)
                } else {
                    setPadding(0, top, 0, bottom)
                }

            }
        }
    }

    private fun hideSystemUi() {
//        window.addFlags(1024)
//        window.decorView.systemUiVisibility = 4102
        removeAllStatusBarMsg()
        val statusBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_STATUS_BAR", true)
        val naviBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_NAVI_BAR", true)
        if (statusBar && naviBar) {
            window.addFlags(1024)
            window.decorView.systemUiVisibility = 4102
            setDockerViewPadding(0, 0)

        } else if (statusBar) {
            window.clearFlags(1024)
            //   hideStatus()
            showNavi()
        } else if (naviBar) {
            //hideNavi()
            showStatus()
        } else {
            showSystemUI()
        }
    }

    private fun changeSize() {
        mSurfaceView.post {
            Logger.e("VehicleApp--------", "${mSurfaceView.width}:${mSurfaceView.height}")
            val displayMetrics = DisplayUtils.getNeedMetrics(this)
            var w = displayMetrics.x
            Logger.e(
                "VehicleApp--------",
                "displayMetrics:${displayMetrics.x}:${displayMetrics.y}"
            )
            var h = displayMetrics.y
//            applicationContext.resources.displayMetrics.widthPixels = w
//            applicationContext.resources.displayMetrics.heightPixels = h
            val remoteDisplayGLView: RemoteDisplayGLView = this.mSurfaceView
            remoteDisplayGLView.post {
                remoteDisplayGLView.onVideoSizeChanged(w, h)
            }
        }

    }


    fun init() {
        Logger.e(TAG, "++++++++++++++++++++Baidu Carlife Begin++++++++++++++++++++")

        // 根据配置文件设置相应的配置
        CarLife.receiver().run {
            setFeature(
                Configs.FEATURE_CONFIG_VOICE_WAKEUP,
                CarlifeConfUtil.getInstance()
                    .getIntProperty(Configs.FEATURE_CONFIG_VOICE_WAKEUP)
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
                CarlifeConfUtil.getInstance()
                    .getStringFromMap(Configs.CONFIG_TARGET_BLUETOOTH_NAME)
            )
        ) {
            CarLife.receiver().setConfig(
                Configs.CONFIG_TARGET_BLUETOOTH_NAME,
                CarlifeConfUtil.getInstance()
                    .getStringFromMap(Configs.CONFIG_TARGET_BLUETOOTH_NAME)
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