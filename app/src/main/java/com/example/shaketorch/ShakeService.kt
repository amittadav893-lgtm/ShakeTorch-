package com.example.shaketorch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null

    private var lastShakeTime: Long = 0
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    companion object {
        var isRunning = false
        var sensitivity: Float = 50f // Default medium sensitivity
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        startForegroundServiceWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Slider se aane wali sensitivity value ko yahan read kiya jata hai
        intent?.let {
            if (it.hasExtra("sensitivity")) {
                val sentValue = it.getIntExtra("sensitivity", 50)
                // Yahan value ko invert/scale karte hain taaki logic sahi bane:
                // Slider jitna kam hoga, threshold utna hi bada hoga (zor se hilana padega)
                sensitivity = (200 - sentValue).toFloat().coerceAtLeast(10f)
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "ShakeTorchChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ShakeTorch Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("ShakeTorch Active")
                .setContentText("Shake sensitivity: ${sensitivity.toInt()}")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ShakeTorch Active")
                .setContentText("Shake sensitivity")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        }

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 70) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Speed calculation for shake motion
                val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                // Agar speed hamari set ki gayi sensitivity threshold se zyada hai
                if (speed > sensitivity) {
                    if ((currentTime - lastShakeTime) > 1200) { // 1.2 second cooldown taaki bar bar trigger na ho
                        lastShakeTime = currentTime
                        triggerAction()
                    }
                }

                lastX = x
                lastY = y
                lastZ = z
            }
        }
    }

    private fun triggerAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(150)
        }

        val intent = Intent("TOGGLE_FLASHLIGHT")
        sendBroadcast(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        sensorManager.unregisterListener(this)
    }
}
