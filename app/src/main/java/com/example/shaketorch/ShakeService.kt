package com.example.shaketorch

import android.app.*
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

class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var vibrator: Vibrator
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    companion object {
        var sensitivity: Float = 50f
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val channelId = "shake_torch_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Shake Torch", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Shake Torch Active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val curTime = System.currentTimeMillis()
            if ((curTime - lastUpdate) > 100) {
                val diffTime = curTime - lastUpdate
                lastUpdate = curTime
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                if (speed > (200 - sensitivity)) {
                    sendBroadcast(Intent("TOGGLE_FLASHLIGHT"))
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                lastX = x; lastY = y; lastZ = z
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensitivity = intent?.getFloatExtra("sensitivity", 50f) ?: 50f
        return START_STICKY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
}
