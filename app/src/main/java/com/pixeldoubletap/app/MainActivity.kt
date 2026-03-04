package com.yourpackage.pixeldoubletap

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val info = TextView(this).apply {
            text = "PixelDoubleTap\n\nGrant two permissions:\n1) Display over other apps (overlay).\n2) Accessibility service (to lock + replay taps).\n\nWe only use these to implement a fast double-tap lock and to replay single taps so your home screen works normally."
        }
        layout.addView(info)

        val btnOverlay = Button(this).apply {
            text = "Grant overlay (Display over other apps)"
            setOnClickListener { openOverlaySettings() }
        }
        layout.addView(btnOverlay)

        val btnA11y = Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(btnA11y)

        val btnStart = Button(this).apply {
            text = "Start service (Foreground)"
            setOnClickListener {
                startService(Intent(this@MainActivity, OverlayService::class.java).apply { action = OverlayService.ACTION_START })
            }
        }
        layout.addView(btnStart)

        val btnStop = Button(this).apply {
            text = "Stop service"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }
        layout.addView(btnStop)

        setContentView(layout)
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }
}