package com.yourpackage.pixeldoubletap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.hypot

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "pixeldoubletap_overlay"
        const val NOTIF_ID = 1001

        const val ACTION_START = "com.yourpackage.pixeldoubletap.ACTION_START"
        const val ACTION_STOP = "com.yourpackage.pixeldoubletap.ACTION_STOP"

        // Tuneable params:
        const val DOUBLE_TAP_TIMEOUT_MS = 200L
        const val MAX_SLOP_DP = 40f
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var replayRunnable: Runnable? = null

    private var slopPx = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        slopPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_SLOP_DP, dm)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundWithOverlay()
            ACTION_STOP -> stopSelf()
            else -> startForegroundWithOverlay()
        }
        return START_STICKY
    }

    private fun startForegroundWithOverlay() {
        createNotificationChannel()
        val notifIntent = Intent(this, MainActivity::class.java)
        val p = PendingIntent.getActivity(
            this, 0, notifIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PixelDoubleTap running")
            .setContentText("Double-tap anywhere to lock the screen.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(p)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)

        if (overlayView == null) addOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Overlay service", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun addOverlay() {
        // layout params for full screen overlay
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // keep it touchable so we can intercept taps, but let system manage focus
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT
        )

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // A minimal view (transparent)
        overlayView = View(this).apply {
            isClickable = true
            isFocusable = false
            setBackgroundColor(0x00000000) // fully transparent
        }

        // GestureDetector for basic double-tap detection & coordinates
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                // Handled in touch listener with coords for replay
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                // directly handled below
                return true
            }
        })

        overlayView?.setOnTouchListener { v, event ->
            // Only respond to ACTION_UP to get stable coordinates
            if (event.action == MotionEvent.ACTION_UP) {
                val now = System.currentTimeMillis()
                val x = event.rawX
                val y = event.rawY

                // determine if this is within slop of last tap
                val dt = now - lastTapTime
                val distance = hypot((x - lastTapX).toDouble(), (y - lastTapY).toDouble())

                if (dt <= DOUBLE_TAP_TIMEOUT_MS && distance <= slopPx) {
                    // double tap detected -> cancel scheduled replay, broadcast lock
                    replayRunnable?.let { handler.removeCallbacks(it) }
                    replayRunnable = null
                    sendLockBroadcast()
                    lastTapTime = 0L
                } else {
                    // schedule a replay after DOUBLE_TAP_TIMEOUT_MS (if second tap doesn't occur)
                    lastTapTime = now
                    lastTapX = x
                    lastTapY = y

                    // cancel existing runnable if any
                    replayRunnable?.let { handler.removeCallbacks(it) }

                    replayRunnable = Runnable {
                        // No second tap — request AccessibilityService to inject a single tap at (x,y)
                        sendInjectTapBroadcast(x.toInt(), y.toInt())
                        replayRunnable = null
                    }
                    handler.postDelayed(replayRunnable!!, DOUBLE_TAP_TIMEOUT_MS)
                }
            }

            // Return true to signal we handled it (we will replay the single tap later)
            true
        }

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            // failed to add overlay (permissions or other) — stop service
            stopSelf()
        }
    }

    private fun sendLockBroadcast() {
        val i = Intent(ScreenLockAccessibilityService.ACTION_LOCK)
        sendBroadcast(i)
    }

    private fun sendInjectTapBroadcast(x: Int, y: Int) {
        val i = Intent(ScreenLockAccessibilityService.ACTION_INJECT_TAP)
        i.putExtra("x", x)
        i.putExtra("y", y)
        sendBroadcast(i)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}