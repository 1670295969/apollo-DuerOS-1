package com.baidu.carlifevehicle.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.baidu.carlife.protobuf.CarlifeCarHardKeyCodeProto
import com.baidu.carlife.sdk.Constants
import com.baidu.carlife.sdk.internal.protocol.CarLifeMessage
import com.baidu.carlife.sdk.internal.protocol.ServiceTypes
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlifevehicle.CarlifeActivity
import com.baidu.carlifevehicle.R
import com.baidu.carlifevehicle.VehicleService

object DisplayUtils {

    fun getNavigationBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return (if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0).apply {
            Log.i("-----","getNavigationBarHeight=$this")
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        Log.i("-----","getStatusBarHeight=$result")
        return result
    }

     fun getNeedMetrics(context: Context) : Point {
        val hideStatusBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_STATUS_BAR",true)
        val hideNaviBar = PreferenceUtil.getInstance().getBoolean("CFG_HIDE_NAVI_BAR",true)
        val naviHeight = DisplayUtils.getNavigationBarHeight(context)
        val statusHeight = DisplayUtils.getStatusBarHeight(context)
        val displayMetrics = DisplayMetrics()
        val wm  = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(displayMetrics)
        var needHeight = displayMetrics.heightPixels
        var needWidth = displayMetrics.widthPixels
        if (hideStatusBar && hideNaviBar){
            needHeight = displayMetrics.heightPixels
            needWidth = displayMetrics.widthPixels
        }else if (hideStatusBar){
            needHeight -= statusHeight
        }else if (hideNaviBar){
            if (NaviPos.isBottom()){
                needHeight -= naviHeight
            }else if(NaviPos.isLeft()){
                needWidth -= naviHeight
            }
        }else {
            needHeight -= statusHeight
            if (NaviPos.isBottom()){
                needHeight -= naviHeight
            }else if(NaviPos.isLeft()){
                needWidth -= naviHeight
            }
        }
        return Point(needWidth,needHeight)
    }

    @JvmStatic
    fun sendHardKeyCodeEvent(keycode: Int) {
        try {
            Log.d(CarlifeActivity.TAG, "sendHardKeyCodeEvent: keycode = $keycode")
            val message = CarLifeMessage.obtain(
                Constants.MSG_CHANNEL_TOUCH,
                ServiceTypes.MSG_TOUCH_CAR_HARD_KEY_CODE,
                0
            )
            message.serviceType = CommonParams.MSG_TOUCH_CAR_HARD_KEY_CODE
            message.payload(
                CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder()
                    .setKeycode(keycode)
                    .build()
            )
            CarLife.receiver().postMessage(message)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun showForegroundNotification(context: Service,channelId : String,notificationID : Int) {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context,
            channelId
        )
            .setContentTitle("百度Carlife媒体服务")
            .setContentText("服务正在运行")
            .setSmallIcon(R.drawable.ic_launcher) // 替换为你的通知图标资源ID
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel name", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = builder.build()
        context.startForeground(notificationID, notification)
    }

}