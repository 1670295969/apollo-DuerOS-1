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
                performClickAction("接受")
            }
            return
        } else if (event.packageName == "com.android.systemui") {
            performClickAction("确定")
            return
        }


    }

    private fun performClickAction(text: String) {
        val tmp = rootInActiveWindow.findAccessibilityNodeInfosByText(text)
        tmp?.forEach {
            if (it.isClickable) {
                handler.postDelayed({
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }, 350)

                return@forEach
            }
        }
    }

    override fun onInterrupt() {
    }
}
