package com.closetstudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.RoomSpec
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Embedded / Inline Dimensions Panel:
 * Used in split-screen mode:
 * - Mobile: lower half of screen
 * - Tablet: right side panel
 */
@Composable
fun RoomDimensionsPanel(
    room: RoomSpec,
    onRoomChange: (RoomSpec) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Checkbox state for symmetrical/opposite walls (default true = 90° rectangular box)
    var symmetricOppositeWalls by remember { mutableStateOf(true) }

    // Checkbox for centered door (default true = D and E are equal)
    var doorCentered by remember { mutableStateOf(room.isDoorCentered) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFF2C55B5),
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "Plano y Dimensiones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Paredes A, B, C y Tramos D - E (Puerta)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                    )
                }
            }

            // 1. Switch for Symmetrical Opposite Walls
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Paredes opuestas simétricas",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (symmetricOppositeWalls) "Pared A = (D + Puerta + E) • 90°" else "Modo libre: lados independientes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = symmetricOppositeWalls,
                        onCheckedChange = { symmetricOppositeWalls = it },
                    )
                }
            }

            HorizontalDivider()

            // 2. Pared A (Frente / Norte)
            WallHeaderBadge(letter = "A", name = "Pared Norte (Frente)")
            DimensionSlider(
                label = if (symmetricOppositeWalls) "Largo Pared A (y Cara D-E)" else "Largo Pared A",
                valueMm = room.clearWidthMm,
                rangeMm = 2000f..6000f,
                stepMm = 50f,
                onValueChange = { newW ->
                    val updated = room.copy(clearWidthMm = newW)
                    onRoomChange(if (doorCentered) updated.withCenteredDoor() else updated)
                },
            )

            // 3. Paredes B y C (Laterales Este y Oeste)
            WallHeaderBadge(letter = "B / C", name = "Paredes Laterales (Este y Oeste)")
            DimensionSlider(
                label = "Profundidad (Paredes B y C)",
                valueMm = room.clearDepthMm,
                rangeMm = 2000f..6000f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearDepthMm = it)) },
            )

            HorizontalDivider()

            // 4. Pared con Puerta: Tramos D y E
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WallBadge(letter = "D")
                            Icon(Icons.Default.DoorFront, contentDescription = null, tint = Color(0xFF8B5A2B), modifier = Modifier.size(18.dp))
                            WallBadge(letter = "E")
                            Text(
                                text = "Pared de la Puerta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = doorCentered,
                                onCheckedChange = { checked ->
                                    doorCentered = checked
                                    if (checked) {
                                        onRoomChange(room.withCenteredDoor())
                                    }
                                },
                            )
                            Text(
                                text = "Centrada",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }

                    // Visual breakdown pill: [ D ] + [ Puerta ] + [ E ] = Total
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "D: ${room.segmentDMm.roundToInt()} mm",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C55B5),
                            )
                            Text(
                                text = "+  🚪 ${room.door.widthMm.roundToInt()} mm  +",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5A2B),
                            )
                            Text(
                                text = "E: ${room.segmentEMm.roundToInt()} mm",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C55B5),
                            )
                            Text(
                                text = "= ${room.doorWallLengthMm.roundToInt()} mm",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (!doorCentered) {
                        // Slider for Tramo D (adapts E)
                        val maxD = (room.doorWallLengthMm - room.door.widthMm).coerceAtLeast(100f)
                        DimensionSlider(
                            label = "Tramo D (Izquierda de la puerta)",
                            valueMm = room.segmentDMm,
                            rangeMm = 0f..maxD,
                            stepMm = 50f,
                            onValueChange = { onRoomChange(room.withSegmentD(it)) },
                        )

                        // Slider for Tramo E (adapts D)
                        DimensionSlider(
                            label = "Tramo E (Derecha de la puerta)",
                            valueMm = room.segmentEMm,
                            rangeMm = 0f..maxD,
                            stepMm = 50f,
                            onValueChange = { onRoomChange(room.withSegmentE(it)) },
                        )
                    }

                    // Slider for Door Width (immutable for segments D & E)
                    DimensionSlider(
                        label = "Ancho de la Puerta (Inmutable al mover lados)",
                        valueMm = room.door.widthMm,
                        rangeMm = 600f..1200f,
                        stepMm = 50f,
                        onValueChange = { onRoomChange(room.withDoorWidth(it, keepCentered = doorCentered)) },
                    )
                }
            }

            HorizontalDivider()

            // 5. Ángulos de Esquinas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.SquareFoot, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Ángulos entre Paredes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "90.0° (Orto)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // 6. Altura (Y)
            DimensionSlider(
                label = "Altura de Paredes (Y)",
                valueMm = room.clearHeightMm,
                rangeMm = 2000f..3500f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearHeightMm = it)) },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Listo (Volver a Vista 3D)")
            }
        }
    }
}

@Composable
private fun WallHeaderBadge(letter: String, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WallBadge(letter = letter)
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun WallBadge(letter: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF2C55B5),
    ) {
        Text(
            text = " $letter ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DimensionSlider(
    label: String,
    valueMm: Float,
    rangeMm: ClosedFloatingPointRange<Float>,
    stepMm: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${valueMm.roundToInt()} mm (${String.format(Locale.US, "%.2f", valueMm / 1000f)} m)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2C55B5),
            )
        }
        Slider(
            value = valueMm.coerceIn(rangeMm.start, rangeMm.endInclusive),
            onValueChange = { raw ->
                val stepped = ((raw - rangeMm.start) / stepMm).roundToInt() * stepMm + rangeMm.start
                onValueChange(stepped.coerceIn(rangeMm.start, rangeMm.endInclusive))
            },
            valueRange = rangeMm,
        )
    }
}
