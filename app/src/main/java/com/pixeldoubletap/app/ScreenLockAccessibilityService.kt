package com.yourpackage.pixeldoubletap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ScreenLockAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "ScreenLockA11y"
        const val ACTION_LOCK = "com.yourpackage.pixeldoubletap.ACTION_LOCK"
        const val ACTION_INJECT_TAP = "com.yourpackage.pixeldoubletap.ACTION_INJECT_TAP"
    }

    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_LOCK -> {
                    try {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lock failed: ${e.message}")
                    }
                }
                ACTION_INJECT_TAP -> {
                    val x = intent.getIntExtra("x", -1)
                    val y = intent.getIntExtra("y", -1)
                    if (x >= 0 && y >= 0) {
                        injectTap(x, y)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply {
            addAction(ACTION_LOCK)
            addAction(ACTION_INJECT_TAP)
        }
        registerReceiver(br, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We set accessibilityEventTypes="typeNone" so nothing arrives here.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(br)
        } catch (_: IllegalArgumentException) { }
    }

    private fun injectTap(x: Int, y: Int) {
        try {
            // Build a tiny gesture at (x,y)
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val desc = GestureDescription.StrokeDescription(path, 0, 20) // 20ms tap
            val gd = GestureDescription.Builder().addStroke(desc).build()
            dispatchGesture(gd, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    // success
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    // failed
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "injectTap failed: ${e.message}")
        }
    }
}