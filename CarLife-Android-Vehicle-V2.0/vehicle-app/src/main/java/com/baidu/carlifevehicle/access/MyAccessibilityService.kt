package com.baidu.carlifevehicle.access;

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.material.tabs.TabLayout.TabGravity

public class MyAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "MyAccessibilityService";
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        Log.i("MyAccessibilityService",""+event)
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        Log.i("MyAccessibilityService",""+event)


        if (event.packageName != "android"){
            return
        }

        Log.i("MyAccessibilityService",""+event.packageName)
        Log.i("MyAccessibilityService",""+event.className)
        Log.i("MyAccessibilityService",""+event.text)

        val nodeInfo = event.source


    if (nodeInfo != null && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val tmp = rootInActiveWindow.findAccessibilityNodeInfosByText("接受")
            tmp?.forEach {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

    }

    override fun onInterrupt() {
    }
}
