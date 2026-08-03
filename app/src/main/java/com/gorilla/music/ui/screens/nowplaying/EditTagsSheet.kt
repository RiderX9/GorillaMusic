package com.gorilla.music.ui.screens.nowplaying

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gorilla.music.data.model.Track
import com.gorilla.music.ui.components.AlbumArt
import com.gorilla.music.ui.components.ModalSheetScaffold
import com.gorilla.music.ui.theme.LocalAppColors
import com.gorilla.music.ui.theme.LocalDynamicColors
import com.gorilla.music.ui.theme.instantClickable

@Composable
fun EditTagsSheet(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?, String?, Int?, String?) -> Unit,
) {
    var title by remember { mutableStateOf(track.overrideTitle ?: track.title) }
    var artist by remember { mutableStateOf(track.overrideArtist ?: track.artist) }
    var album by remember { mutableStateOf(track.overrideAlbum ?: track.album) }
    var genre by remember { mutableStateOf(track.genre.orEmpty()) }
    var year by remember { mutableStateOf(track.overrideYear?.toString() ?: if (track.year > 0) track.year.toString() else "") }
    var lyrics by remember { mutableStateOf(track.customLyrics.orEmpty()) }

    val appColors = LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    ModalSheetScaffold(
        onDismiss = onDismiss,
        heightFraction = null,
        surfaceColor = appColors.bgSurface,
        plainSurface = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
                // Sheet Header Section
                EditTagsHeader(
                    track = track,
                    onDismiss = onDismiss,
                )

                Spacer(Modifier.height(14.dp))

                val cardBg = appColors.bgGlass
                val cardBorder = appColors.borderGlass.copy(alpha = 0.45f)
                val cardShape = RoundedCornerShape(20.dp)

                // Group 1: Title, Artist, Album Stack
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, cardShape)
                        .border(1.dp, cardBorder, cardShape)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    SingleTagFieldRow(
                        label = "TRACK TITLE",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Track title",
                    )
                    DividerLine()
                    SingleTagFieldRow(
                        label = "ARTIST NAME",
                        value = artist,
                        onValueChange = { artist = it },
                        placeholder = "Artist name",
                    )
                    DividerLine()
                    SingleTagFieldRow(
                        label = "ALBUM NAME",
                        value = album,
                        onValueChange = { album = it },
                        placeholder = "Album title",
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Group 2: Genre & Year Dual Column Pair
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBg, cardShape)
                            .border(1.dp, cardBorder, cardShape)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        SingleTagFieldRow(
                            label = "GENRE",
                            value = genre,
                            onValueChange = { genre = it },
                            placeholder = "e.g. Rock",
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBg, cardShape)
                            .border(1.dp, cardBorder, cardShape)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        SingleTagFieldRow(
                            label = "YEAR",
                            value = year,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) year = it },
                            placeholder = "e.g. 1981",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Group 3: Embedded Lyrics Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBg, cardShape)
                        .border(1.dp, cardBorder, cardShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "EMBEDDED LYRICS",
                        color = appColors.textSecondary.copy(alpha = 0.5f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.08.sp,
                    )
                    BasicTextField(
                        value = lyrics,
                        onValueChange = { lyrics = it },
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = appColors.textPrimary.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                        ),
                        cursorBrush = SolidColor(accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        decorationBox = { inner ->
                            if (lyrics.isEmpty()) {
                                Text(
                                    text = "Enter lyrics...",
                                    color = appColors.textDisabled.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Action Footer Buttons: Cancel + Save Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val cancelBg = appColors.bgGlass
                    val btnShape = RoundedCornerShape(18.dp)

                    // Cancel button with instant touch scale
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .instantClickable(pressedScale = 0.94f) {
                                onDismiss()
                            }
                            .background(cancelBg, btnShape)
                            .border(1.dp, appColors.borderGlass.copy(alpha = 0.55f), btnShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = appColors.textPrimary.copy(alpha = 0.8f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    // Save Tags button with instant touch scale
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = btnShape,
                                ambientColor = accent.copy(alpha = 0.4f),
                                spotColor = accent.copy(alpha = 0.4f),
                            )
                            .instantClickable(pressedScale = 0.94f) {
                                onSave(
                                    title,
                                    artist,
                                    album,
                                    genre,
                                    year.toIntOrNull(),
                                    lyrics
                                )
                            }
                            .background(accent, btnShape)
                            .border(1.dp, accent, btnShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Tags",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
        }
    }
}

@Composable
private fun EditTagsHeader(
    track: Track,
    onDismiss: () -> Unit
) {
    val colors = com.gorilla.music.ui.theme.LocalAppColors.current
    val accent = LocalDynamicColors.current.accent
    val artShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                AlbumArt(
                    albumId = track.albumId,
                    artworkUri = track.artworkUri,
                    shape = artShape,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, colors.borderGlass.copy(alpha = 0.5f), artShape)
                )
                // Small edit badge on album art bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .background(accent, CircleShape)
                        .border(2.dp, colors.bgSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Edit Tags",
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Update ID3 metadata & lyrics",
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        // Circular close icon button with instant touch scale
        Box(
            modifier = Modifier
                .size(32.dp)
                .instantClickable(pressedScale = 0.90f) {
                    onDismiss()
                }
                .background(
                    colors.textPrimary.copy(alpha = 0.08f),
                    CircleShape
                )
                .border(1.dp, colors.borderGlass.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = colors.textPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SingleTagFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = com.gorilla.music.ui.theme.LocalAppColors.current
    val accent = LocalDynamicColors.current.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.08.sp,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.textDisabled.copy(alpha = 0.4f),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun DividerLine() {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.borderGlass.copy(alpha = 0.25f))
    )
}
