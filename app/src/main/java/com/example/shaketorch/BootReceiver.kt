package com.example.shaketorch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action ==
            Intent.ACTION_BOOT_COMPLETED
        ) {

            val preferences =
                context.getSharedPreferences(
                    "shake_torch",
                    Context.MODE_PRIVATE
                )

            val enabled =
                preferences.getBoolean(
                    "enabled",
                    false
                )

            if (enabled) {

                val serviceIntent =
                    Intent(
                        context,
                        ShakeService::class.java
                    )

                ContextCompat.startForegroundService(
                    context,
                    serviceIntent
                )
            }
        }
    }
}
