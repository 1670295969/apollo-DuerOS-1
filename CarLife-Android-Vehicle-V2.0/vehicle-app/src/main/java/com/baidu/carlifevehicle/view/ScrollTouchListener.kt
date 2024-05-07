package com.baidu.carlifevehicle.view;

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class ScrollTouchListener(
    private val wm: WindowManager
) : View.OnTouchListener {
    companion object {
        const val MIN_DIS = 10
    }
    private var x = 0
    private var y = 0
    private var downX = 0;
    private var downY = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                x = motionEvent.rawX.toInt()
                y = motionEvent.rawY.toInt()

                downX = motionEvent.rawX.toInt()
                downY = motionEvent.rawY.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                val nowX = motionEvent.rawX.toInt()
                val nowY = motionEvent.rawY.toInt()
                val movedX = nowX - x
                val movedY = nowY - y
                x = nowX
                y = nowY
                (view.layoutParams as? WindowManager.LayoutParams)?.apply {
                    x += movedX
                    y += movedY
                    wm.updateViewLayout(view, this)
                }
            }
            else -> {

            }
        }
        if (motionEvent.action == MotionEvent.ACTION_UP){
            val nowUpX = motionEvent.rawX.toInt()
            val nowUpY = motionEvent.rawY.toInt()
            if (abs(nowUpX - downX) > MIN_DIS || abs(nowUpY - downY) > MIN_DIS){
                return true
            }
        }
        return false
    }
}