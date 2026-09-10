package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationData
import com.example.model.StampBgStyle
import com.example.model.StampConfig
import com.example.stamp.MiniMapRenderer
import com.example.ui.theme.Amber400
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate900

@Composable
fun StampOverlayView(
    location: LocationData,
    config: StampConfig,
    modifier: Modifier = Modifier,
    onEditNoteClick: (() -> Unit)? = null
) {
    val isLight = config.bgStyle == StampBgStyle.FROSTED_LIGHT
    val bgColor = when (config.bgStyle) {
        StampBgStyle.TRANSLUCENT_DARK -> Color(0xDD0F172A)
        StampBgStyle.SOLID_BLACK -> Color(0xFF070B14)
        StampBgStyle.FROSTED_LIGHT -> Color(0xE6F8FAFC)
        StampBgStyle.MINIMAL_BORDER -> Color(0xAA0F172A)
    }

    val primaryTextColor = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
    val accentTextColor = if (isLight) Cyan500 else Cyan400
    val borderModifier = if (config.bgStyle == StampBgStyle.MINIMAL_BORDER) {
        Modifier.border(1.5.dp, Cyan400.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
    } else Modifier

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .testTag("stamp_overlay_hud"),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Mini Map & Badge
            if (config.showMiniMap) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    if (config.showBadge && config.badgeText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Cyan600)
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = config.badgeText.uppercase(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    // Mini map bitmap
                    val miniMapBitmap = remember(location.latitude, location.longitude, config.mapStyle) {
                        MiniMapRenderer.generateMiniMapBitmap(
                            180,
                            180,
                            location.latitude,
                            location.longitude,
                            config.mapStyle
                        )
                    }

                    Image(
                        bitmap = miniMapBitmap.asImageBitmap(),
                        contentDescription = "Mini Map with GPS Pin",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    )
                }
            }

            // Right Column: Details & Coordinates
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Title
                if (config.showAddress && location.title.isNotBlank()) {
                    Text(
                        text = location.title,
                        color = primaryTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Address line
                if (config.showAddress && location.addressLine.isNotBlank()) {
                    Text(
                        text = location.addressLine,
                        color = secondaryTextColor,
                        fontSize = 10.5.sp,
                        maxLines = 2,
                        lineHeight = 13.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Date & Time
                if (config.showDateTime) {
                    Text(
                        text = location.getFormattedDateTime(),
                        color = secondaryTextColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Custom Note
                if (config.showNote && config.customNote.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .then(
                                if (onEditNoteClick != null) Modifier.clickable { onEditNoteClick() }
                                else Modifier
                            )
                    ) {
                        Text(
                            text = "Note: ${config.customNote}",
                            color = Amber400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (onEditNoteClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Note",
                                tint = Amber400,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Coordinates (DMS or Decimal)
                if (config.showCoordinates) {
                    Text(
                        text = location.getCoordinatesFormatted(config.coordinateFormat),
                        color = accentTextColor,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metrics Row (Altitude, Compass, Weather, Speed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.showAltitude) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterHdr,
                                contentDescription = null,
                                tint = accentTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = location.getAltitudeFormatted(config.altitudeUnit),
                                color = primaryTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (config.showCompass) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${location.compassDegrees.toInt()}° ${location.compassDirection}",
                                color = primaryTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (config.showWeather) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${location.temperatureC}°C",
                                color = primaryTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
