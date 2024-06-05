package com.baidu.carlifevehicle.access;

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

public class MyAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "MyAccessibilityService";
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        Log.i("MyAccessibilityService", "" + event)
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return


        Log.i("MyAccessibilityService", "" + event)
        if (event.packageName == "android") {
            val nodeInfo = event.source

            if (nodeInfo != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                try {
                    val tmp = rootInActiveWindow.findAccessibilityNodeInfosByText("接受")
                    tmp?.forEach {
                        Log.i("MyAccessibilityService", it.viewIdResourceName)
                        it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }catch (e:java.lang.Exception){
                    e.printStackTrace()
                }

            }
            return
        } else if (event.packageName == "com.android.systemui") {
            //
            try{
                val tmp = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.systemui:id/mAlwaysUse")
                tmp?.forEach {
                    Log.i("MyAccessibilityService", "mAlwaysUse:${it.className}")
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
            try {
                performClickAction("确定")
            }catch (e:Exception){
                e.printStackTrace()
            }

            return
        }


    }

    private fun performClickAction(text: String) {
     //   handler.postDelayed({
            val tmp = rootInActiveWindow.findAccessibilityNodeInfosByText(text)
            tmp?.forEach {
                Log.i("MyAccessibilityService", it.viewIdResourceName)
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
     //   },300)

    }

    override fun onInterrupt() {
    }
}
