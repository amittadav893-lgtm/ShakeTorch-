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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    private var torchOn = false

    private var lastShakeTime = 0L

    private var sensitivity = 65

    companion object {

        private const val CHANNEL_ID =
            "shake_torch_service"

        private const val NOTIFICATION_ID = 1001

        private const val SHAKE_COOLDOWN = 900L

        private const val PREFS_NAME = "shake_torch"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val DEFAULT_SENSITIVITY = 65
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        sensorManager =
            getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager

        accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )!!

        cameraManager =
            getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

        cameraId = findFlashCamera()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val prefs =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        sensitivity =
            if (intent?.hasExtra("sensitivity") == true) {
                intent.getIntExtra(
                    "sensitivity",
                    DEFAULT_SENSITIVITY
                )
            } else {
                prefs.getInt(
                    KEY_SENSITIVITY,
                    DEFAULT_SENSITIVITY
                )
            }

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        return START_STICKY
    }

    private fun findFlashCamera(): String? {

        return try {

            for (id in cameraManager.cameraIdList) {

                val characteristics =
                    cameraManager.getCameraCharacteristics(id)

                val hasFlash =
                    characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true

                val facing =
                    characteristics.get(
                        CameraCharacteristics.LENS_FACING
                    )

                if (
                    hasFlash &&
                    facing ==
                    CameraCharacteristics.LENS_FACING_BACK
                ) {
                    return id
                }
            }

            null

        } catch (e: Exception) {
            null
        }
    }

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration =
            sqrt(
                (x * x) +
                (y * y) +
                (z * z)
            )

        val gravity = 9.81f

        val force =
            kotlin.math.abs(
                acceleration - gravity
            )

        val threshold =
            18f - ((sensitivity - 20) * 0.10f)

        val now =
            System.currentTimeMillis()

        if (
            force > threshold &&
            now - lastShakeTime > SHAKE_COOLDOWN
        ) {

            lastShakeTime = now

            toggleTorch()
        }
    }

    private fun toggleTorch() {

        val id = cameraId ?: return

        try {

            torchOn = !torchOn

            cameraManager.setTorchMode(
                id,
                torchOn
            )

            vibrate()

        } catch (e: Exception) {

            torchOn = false
        }
    }

    private fun vibrate() {

        try {

            if (Build.VERSION.SDK_INT >= 31) {

                val vibratorManager =
                    getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE
                    ) as VibratorManager

                val vibrator =
                    vibratorManager.defaultVibrator

                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        80,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )

            } else {

                @Suppress("DEPRECATION")
                val vibrator =
                    getSystemService(
                        Context.VIBRATOR_SERVICE
                    ) as Vibrator

                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }

        } catch (_: Exception) {
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "Shake Torch is active"
            )
            .setContentText(
                "Shake your phone to toggle the flashlight"
            )
            .setSmallIcon(
                R.drawable.ic_notification
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Shake Torch Service",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Keeps shake detection active in the background"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onDestroy() {

        sensorManager.unregisterListener(this)

        try {

            cameraId?.let {
                cameraManager.setTorchMode(
                    it,
                    false
                )
            }

        } catch (_: Exception) {
        }

        torchOn = false

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}
