package com.baidu.carlifevehicle.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.baidu.carlifevehicle.CarlifeActivity
import com.baidu.carlifevehicle.VehicleService
import com.baidu.carlifevehicle.util.PreferenceUtil
import com.baidu.carlifevehicle.view.FloatWindowManager
import java.util.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("CarLifeBootReceiver", "启动广播:${intent?.action}");
        val sharedPreferences = PreferenceUtil.getInstance().preferences;
        val result = sharedPreferences?.getBoolean("START_ON_SYSTEM_BOOT", false) ?: false

        if(PreferenceUtil.getInstance().getBoolean("show_float",true)){
            FloatWindowManager.show()
        }
        if (!result) {
            return

        }

        val serviceIntent = Intent(context, VehicleService::class.java);
        if (Build.VERSION.SDK_INT >= 26) {
            context?.startForegroundService(serviceIntent);
        } else {
            context?.startService(serviceIntent);
        }

        val showUI = sharedPreferences?.getBoolean("START_ON_SYSTEM_BOOT_SHOW_UI", false) ?: false
        if (showUI){
            context?.startActivity(Intent(context,CarlifeActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            })
        }

    }
}