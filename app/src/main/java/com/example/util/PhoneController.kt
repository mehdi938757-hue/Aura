package com.example.util

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.AlarmClock
import android.util.Log

class PhoneController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun turnOnFlashlight(on: Boolean): String {
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, on)
                if (on && vibrator != null) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                "Flashlight is now ${if (on) "ON" else "OFF"}."
            } else {
                "Flashlight not available on this device."
            }
        } catch (e: Exception) {
            Log.e("PhoneController", "Flashlight error", e)
            "Failed to toggle flashlight: ${e.message}"
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Alarm set for $hour:$minute with message '$message'."
        } catch (e: Exception) {
            Log.e("PhoneController", "Alarm error", e)
            // Fallback intent without skip ui
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                "Alarm set for $hour:$minute."
            } catch (ex: Exception) {
                "Failed to set alarm: ${ex.message}"
            }
        }
    }

    fun adjustVolume(level: Int): String {
        return try {
            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 15
            val targetVolume = ((level / 100f) * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, targetVolume, AudioManager.FLAG_SHOW_UI)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            "Volume adjusted to $level%."
        } catch (e: Exception) {
            Log.e("PhoneController", "Volume error", e)
            "Failed to adjust volume: ${e.message}"
        }
    }

    fun getBatteryLevel(): String {
        return try {
            val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            "Battery is at $pct% ${if (isCharging) "and currently charging" else ""}"
        } catch (e: Exception) {
            Log.e("PhoneController", "Battery error", e)
            "Could not retrieve battery info."
        }
    }

    fun openApp(appName: String): String {
        val query = appName.lowercase()
        return try {
            val intent = when {
                query.contains("camera") -> Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                query.contains("browser") || query.contains("web") -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                query.contains("settings") -> Intent(android.provider.Settings.ACTION_SETTINGS)
                query.contains("dial") || query.contains("phone") -> Intent(Intent.ACTION_DIAL)
                else -> context.packageManager.getLaunchIntentForPackage("com.android.settings")
            }
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "Opened $appName for you."
            } else {
                "Could not find or open app: $appName"
            }
        } catch (e: Exception) {
            Log.e("PhoneController", "Open app error", e)
            "Failed to open $appName: ${e.message}"
        }
    }
}
