package com.baidu.carlifevehicle.view

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.baidu.carlifevehicle.R
import com.baidu.carlifevehicle.VehicleApplication

object FloatWindowManager {


    private val wm by lazy {
        VehicleApplication.app.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    }

    private val view by lazy {
        View.inflate(VehicleApplication.app, R.layout.float_window,null).apply {
            setOnClickListener {
                VehicleApplication.app.packageManager.getLaunchIntentForPackage(VehicleApplication.app.packageName)
                    ?.let {
                        VehicleApplication.app.startActivity(it)
                    }
            }
        }
    }

    private val lp by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.LEFT or Gravity.TOP
            x = 100
            y = 200
        }
    }

    fun show(){
        try {
            if (view.parent == null){
                wm.addView(view, lp)
            }
            view.setOnTouchListener(ScrollTouchListener(wm))
        }catch (e : java.lang.Exception){
            e.printStackTrace()
        }

    }
    fun dismiss(){
        try {
            if (view.parent!=null){
                wm.removeViewImmediate(view)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

    }





}