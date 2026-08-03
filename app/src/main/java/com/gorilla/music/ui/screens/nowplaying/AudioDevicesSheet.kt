package com.gorilla.music.ui.screens.nowplaying

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.components.ModalSheetScaffold
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable
import com.gorilla.music.ui.theme.rememberHaptic
import kotlin.math.roundToInt

data class AudioOutputDeviceItem(
    val id: Int,
    val name: String,
    val subtext: String,
    val typeTag: String,
    val icon: ImageVector,
    val isCurrent: Boolean,
    val rawDeviceInfo: AudioDeviceInfo?,
)

/**
 * Modern theme-aware audio output destination sheet matching pairnew.html.
 * Allows quick output selection, real-time volume slider control, and pairing new devices.
 */
@Composable
fun AudioDevicesSheet(
    app: AppViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    val cardBg = appColors.bgGlass
    val cardBorder = appColors.borderGlass.copy(alpha = 0.5f)

    // Current volume tracking
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }

    // Devices tracking
    val devices = remember { mutableStateListOf<AudioOutputDeviceItem>() }
    val preferredAudioDeviceId by app.playback.preferredAudioDeviceId.collectAsStateWithLifecycle()

    fun refreshDevices() {
        devices.clear()
        val availableOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        // 1. Single unified entry for internal device speaker
        val internalDevice = availableOutputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } ?: availableOutputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE || it.type == AudioDeviceInfo.TYPE_TELEPHONY
        }
        val automaticallyRoutedBluetooth = availableOutputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
        val currentDeviceId = preferredAudioDeviceId ?: automaticallyRoutedBluetooth?.id

        devices.add(
            AudioOutputDeviceItem(
                id = internalDevice?.id ?: -1,
                name = "This Device",
                subtext = "Internal Speaker",
                typeTag = "System",
                icon = Icons.Rounded.PhoneAndroid,
                isCurrent = currentDeviceId == null || currentDeviceId == internalDevice?.id,
                rawDeviceInfo = internalDevice,
            )
        )

        // 2. Filter and add external audio output devices (Bluetooth, Wired, USB, HDMI)
        val externalTypes = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC,
        )

        val seenExternalNames = mutableSetOf<String>()

        availableOutputs
            .filter { it.type in externalTypes }
            .sortedBy { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 0
                    AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> 1
                    else -> 2
                }
            }
            .forEach { device ->
                val rawName = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "External Device"
                if (seenExternalNames.add(rawName)) {
                    val isCurrent = currentDeviceId == device.id

                    val (subtext, tag, icon) = when (device.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                            Triple("Bluetooth Audio", "Bluetooth", Icons.Rounded.Headset)
                        }
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> {
                            Triple("Wired Headset / USB", "Wired", Icons.Rounded.Headset)
                        }
                        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> {
                            Triple("External Output", "HDMI", Icons.Rounded.SpeakerGroup)
                        }
                        else -> {
                            Triple("Audio Device", "External", Icons.Rounded.Speaker)
                        }
                    }

                    devices.add(
                        AudioOutputDeviceItem(
                            id = device.id,
                            name = rawName,
                            subtext = subtext,
                            typeTag = tag,
                            icon = icon,
                            isCurrent = isCurrent,
                            rawDeviceInfo = device,
                        )
                    )
                }
            }
    }

    LaunchedEffect(preferredAudioDeviceId) {
        refreshDevices()
    }

    val activeDevice = devices.firstOrNull { it.isCurrent } ?: devices.firstOrNull() ?: AudioOutputDeviceItem(
        id = -1,
        name = "This Device",
        subtext = "Internal Speaker",
        typeTag = "System",
        icon = Icons.Rounded.PhoneAndroid,
        isCurrent = true,
        rawDeviceInfo = null,
    )

    val playbackState by app.playback.state.collectAsStateWithLifecycle()
    val playingTrack = playbackState.current

    ModalSheetScaffold(
        onDismiss = onDismiss,
        heightFraction = null,
        surfaceColor = appColors.bgSurface,
        plainSurface = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            // 1. Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SpeakerGroup,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Connected Devices",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appColors.textPrimary,
                            letterSpacing = (-0.2).sp,
                        )
                        Text(
                            text = "Select audio output destination",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .instantClickable(pressedScale = 0.94f) {
                            onDismiss()
                        }
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, cardBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // 2. Active Output Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = if (appColors.isDark) 0.18f else 0.14f),
                                cardBg,
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = activeDevice.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = activeDevice.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = appColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(top = 1.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                    Text(
                                        text = playingTrack?.let { "Playing • ${it.displayArtist}" } ?: "Active Output",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        val volPercent = ((currentVolume.toFloat() / maxVolume) * 100).roundToInt()
                        Text(
                            text = "$volPercent%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Volume Scrubber Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                            contentDescription = "Volume down",
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVol ->
                                val intVol = newVol.roundToInt()
                                currentVolume = intVol
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intVol, 0)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = accent,
                                inactiveTrackColor = appColors.textPrimary.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Volume up",
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 3. Available Audio Output Destinations
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                devices.forEach { device ->
                    val pillShape = RoundedCornerShape(18.dp)
                    val isSelected = device.isCurrent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .instantClickable(pressedScale = 0.94f) {
                                haptic()
                                device.rawDeviceInfo?.let { raw ->
                                    // Undo any call-route override left by older app builds.
                                    audioManager.clearCommunicationDevice()
                                    app.playback.setPreferredAudioDevice(raw.id)
                                } ?: run {
                                    audioManager.clearCommunicationDevice()
                                    app.playback.setPreferredAudioDevice(null)
                                }
                            }
                            .clip(pillShape)
                            .background(cardBg)
                            .border(
                                width = 1.dp,
                                color = cardBorder,
                                shape = pillShape,
                            )
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) accent.copy(alpha = 0.18f) else appColors.textPrimary.copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = device.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) accent else appColors.textPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = device.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) accent else appColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = device.subtext,
                                        fontSize = 11.5.sp,
                                        color = appColors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.18f) else appColors.textPrimary.copy(alpha = 0.08f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = device.typeTag,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) accent else appColors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 4. Pair New / More Settings Button
            val footerBtnShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .instantClickable(pressedScale = 0.94f) {
                        haptic()
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    }
                    .clip(footerBtnShape)
                    .background(cardBg)
                    .border(1.dp, cardBorder, footerBtnShape)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        tint = appColors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Pair New Bluetooth Device",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appColors.textPrimary,
                    )
                }
            }
        }
    }
}
