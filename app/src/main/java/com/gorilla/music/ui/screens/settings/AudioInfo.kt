package com.gorilla.music.ui.screens.settings

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

data class AudioOutputInfo(
    val deviceName: String,
    val sampleRate: String,
)

/** Reads the current audio output route + the device's preferred sample rate. */
fun readAudioOutput(context: Context): AudioOutputInfo {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val active = devices.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
    } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

    val name = when (active?.type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headphones"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB audio"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
        else -> active?.productName?.toString() ?: "Default output"
    }

    val rateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    val rate = rateStr?.toIntOrNull()
    val sampleRate = if (rate != null && rate > 0) String.format("%.1f kHz", rate / 1000.0) else "—"

    return AudioOutputInfo(deviceName = name, sampleRate = sampleRate)
}
