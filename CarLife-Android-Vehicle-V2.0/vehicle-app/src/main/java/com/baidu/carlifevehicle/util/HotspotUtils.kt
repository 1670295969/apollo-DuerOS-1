package com.baidu.carlifevehicle.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlifevehicle.VehicleApplication


/**
 * 时间：2023/8/10 14:40
 * 描述：热点工具类
 */
object HotspotUtils {

    const val TAG = "HotspotUtils"


    private val context by lazy {
        VehicleApplication.app
    }

    private val wifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private var mReservation: LocalOnlyHotspotReservation? = null


    fun openHot() {
        if (CarLifeContext.CONNECTION_TYPE_HOTSPOT != CarLife.receiver().connectionType){
            return
        }
        if (!PreferenceUtil.getInstance().getBoolean("open_hot", false)) {
            return
        }
        try {
            if (isHotspotEnabled()){
                Log.i(TAG,"hot is opened")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startSoftAp()
               // startLocalOnlyHotspot()
            } else {
                enableExistingHotspot()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun closeHot() {
        if (CarLifeContext.CONNECTION_TYPE_WIFIDIRECT == CarLife.receiver().connectionType){
            if (!PreferenceUtil.getInstance().getBoolean("open_hot", false)) {
                return
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    closeHotWithO()
                } else {
                    disableWifiAp()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    private fun enableExistingHotspot() {
        try {
            val existingConfig = getExistingWifiApConfiguration()
            if (existingConfig != null) {
                // Use reflection to enable the existing hotspot
                val method = wifiManager.javaClass.getMethod(
                    "setWifiApEnabled",
                    WifiConfiguration::class.java,
                    Boolean::class.javaPrimitiveType
                )
                val result = method.invoke(wifiManager, existingConfig, true) as Boolean
                if (result) {
                    Log.d("MainActivity", "Hotspot enabled: " + existingConfig.SSID)
                } else {
                    Log.d("MainActivity", "Failed to enable hotspot")
                }
            } else {
                Log.d("MainActivity", "No existing hotspot configuration found")
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error enabling hotspot: " + e.message)
        }
    }

    private fun getExistingWifiApConfiguration(): WifiConfiguration? {
        return try {
            val getConfigMethod = wifiManager.javaClass.getMethod("getWifiApConfiguration")
            getConfigMethod.invoke(wifiManager) as WifiConfiguration
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error retrieving hotspot configuration: " + e.message)
            null
        }
    }

    private fun disableWifiAp() {
        try {
            val method = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(wifiManager, null, false)
            Log.d("MainActivity", "Hotspot disabled")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error disabling hotspot: " + e.message)
        }
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocalOnlyHotspot() {
        getExistingWifiApConfiguration()?.let {
            Log.d(TAG, "SSID: " + it.SSID)
            Log.d(TAG, "Password: " +it.preSharedKey)
            configure5GHzHotspot(it)
            setWifiApConfiguration(it)
        }
        wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                mReservation = reservation
                //reservation.wifiConfiguration?.let { configure5GHzHotspot(it) }
                Log.d(TAG, "Local Hotspot started")
                Log.d(TAG, "SSID: " + reservation.wifiConfiguration!!.SSID)
                Log.d(TAG, "Password: " + reservation.wifiConfiguration!!.preSharedKey)

            }

            override fun onStopped() {
                super.onStopped()
                Log.d(TAG, "Local Hotspot stopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.d(TAG, "Local Hotspot failed to start")
            }
        }, null)
    }

    private fun configure5GHzHotspot(wifiConfig: WifiConfiguration) {
        // Attempt to set 5GHz band
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use new API methods introduced in Android 11 (R)
               // wifiConfig.apBand = 1//WifiConfiguration.AP_BAND_5GHZ
                val apBand = wifiConfig.javaClass.getField("apBand")
                apBand.setInt(wifiConfig,1)
            }
            Log.d(TAG, "Configured to use 5GHz band")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set AP band to 5GHz", e)
        }
    }

    private fun setWifiApConfiguration(wifiConfig: WifiConfiguration) {
        //android.net.wifi.WifiManager, but got android.net.wifi.WifiConfiguration
        try {
            wifiManager.javaClass.getMethod("setWifiApConfiguration",WifiConfiguration::class.java)
                .invoke(wifiManager,wifiConfig)
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun isHotspotEnabled(): Boolean {
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true // Make the method accessible
            method.invoke(wifiManager) as Boolean
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error checking hotspot status: " + e.message)
            false
        }
    }


    private fun closeHotWithO() : Boolean {

        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("stopSoftAp")
            method.isAccessible = true // Make the method accessible
            method.invoke(wifiManager) as Boolean
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error checking hotspot status: " + e.message)
            false
        }
       // mReservation?.close()
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun startSoftAp() : Boolean {

        return try {

            getExistingWifiApConfiguration()?.let {
                Log.d(TAG, "SSID: " + it.SSID)
                Log.d(TAG, "Password: " +it.preSharedKey)
                configure5GHzHotspot(it)
                setWifiApConfiguration(it)
            }
            val method = wifiManager.javaClass.getDeclaredMethod("startSoftAp",WifiConfiguration::class.java)
            method.isAccessible = true // Make the method accessible

            return method.invoke(wifiManager,getExistingWifiApConfiguration()) as Boolean
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error checking hotspot status: " + e.message)
            false
        }
        // mReservation?.close()
    }

}