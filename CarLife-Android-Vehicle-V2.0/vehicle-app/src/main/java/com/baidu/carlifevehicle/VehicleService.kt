package com.baidu.carlifevehicle

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.internal.transport.TransportListener
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlife.sdk.util.Logger
import com.baidu.carlifevehicle.util.CarlifeConfUtil
import com.baidu.carlifevehicle.util.PreferenceUtil


class VehicleService : Service(), TransportListener {

    companion object {
        private val NOTIFICATION_ID = 1
        private val CHANNEL_ID = "foreground_service_channel"
    }

    override fun onBind(intent: Intent?): IBinder? {
        Logger.d("mtg_carlife", "VehicleService onBind")
        return VehicleBind()
    }

    override fun onCreate() {
        super.onCreate()
        CarLife.receiver().registerTransportListener(this)
        Logger.d("mtg_carlife", "VehicleService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForegroundNotification()
        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    override fun onConnectionEstablished(context: CarLifeContext) {
        super.onConnectionEstablished(context)

        Logger.d("VideoRender", "onConnectionEstablished start CarlifeActivity")
        // 这里先注释掉，如车厂有需求，可以放开
        val sharedPreferences = PreferenceUtil.getInstance().preferences
        val result = sharedPreferences?.getBoolean(CarlifeConfUtil.CONNECT_SUCCESS_SHOW_UI, false) ?: false
        if (result){
            showFront()
        }

    }

    /**
     * 此方法用于当CarLife在后台时，此时用户连接成功，则需要把CarLife拉到前台显示出来
     */
    fun showFront() {
        val mAm = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val taskList = mAm.getRunningTasks(100)

        for (rti in taskList) {
            Logger.d("VideoRender", "topActivity:", rti.topActivity!!.className)
            // 找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
            if (rti.topActivity!!.className.contains("CarlifeActivity")) {
                mAm.moveTaskToFront(rti.id, 0)
                Logger.d("VideoRender", "CarlifeActivity moveTaskToFront")
                return
            }
        }

        val intent = Intent(this, CarlifeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    class VehicleBind : Binder() {
        fun serviceStatus(status: Int) {

        }
    }







    private fun showForegroundNotification() {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("百度Carlife")
            .setContentText("服务正在运行")
            .setSmallIcon(R.drawable.ic_launcher) // 替换为你的通知图标资源ID
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Channel name", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = builder.build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务被销毁时移除通知
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

}