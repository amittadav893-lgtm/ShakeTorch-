package com.example.shaketorch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var sensitivityText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var sensitivityBar: SeekBar

    companion object {
        private const val CAMERA_PERMISSION = 100
        private const val NOTIFICATION_PERMISSION = 101

        private const val PREFS_NAME = "shake_torch"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val DEFAULT_SENSITIVITY = 65
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sensitivityText = findViewById(R.id.sensitivityText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        sensitivityBar = findViewById(R.id.sensitivityBar)

        requestPermissionsIfNeeded()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val savedSensitivity =
            prefs.getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

        sensitivityBar.progress = savedSensitivity
        updateSensitivityText(savedSensitivity)

        val isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        startButton.isEnabled = !isEnabled
        stopButton.isEnabled = isEnabled

        sensitivityBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val value = progress.coerceIn(20, 100)

                    sensitivityText.text =
                        "Shake sensitivity: $value"

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_SENSITIVITY, value)
                        .apply()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {}

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {}
            }
        )

        startButton.setOnClickListener {

            val sensitivity =
                sensitivityBar.progress
                    .coerceIn(20, 100)

            val intent =
                Intent(this, ShakeService::class.java)

            intent.putExtra(
                "sensitivity",
                sensitivity
            )
