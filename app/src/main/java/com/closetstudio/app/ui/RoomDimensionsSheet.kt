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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Embedded / Inline Dimensions Panel with Parametric Constraint Solver controls:
 * - Independent wall lengths for A, B, C, and South (D + Door + E)
 * - Lock / Unlock toggles (🔒 / 🔓) per wall and per corner angle
 * - Independent corner angle adjustment (65°..115°) with automatic polygon closure
 */
@Composable
fun RoomDimensionsPanel(
    room: RoomSpec,
    onRoomChange: (RoomSpec) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var symmetricOppositeWalls by remember { mutableStateOf(false) }
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
                            text = "Plano 2D y Restricciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Paredes independientes (A, B, C, D-E) y ángulos",
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

            // 1. Switch for Symmetrical Opposite Walls vs Independent Walls
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
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
                            text = "Paredes opuestas iguales (Rectángulo)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (symmetricOppositeWalls) {
                                "B = C y A = Sur • Esquinas a 90°"
                            } else {
                                "Modo libre: cada pared y esquina se edita por separado"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = symmetricOppositeWalls,
                        onCheckedChange = { checked ->
                            symmetricOppositeWalls = checked
                            if (checked) {
                                onRoomChange(room.withOrthogonalReset(doorCentered))
                            }
                        },
                    )
                }
            }

            HorizontalDivider()

            // 2. Pared A (Frente / Norte)
            WallHeaderWithLock(
                letter = "A",
                name = "Pared Norte (Frente)",
                isLocked = room.lockWallA,
                onLockToggle = { onRoomChange(room.copy(lockWallA = !room.lockWallA)) },
            )
            DimensionSlider(
                label = "Largo Pared A",
                valueMm = room.wallAMm,
                rangeMm = 1500f..6000f,
                stepMm = 50f,
                onValueChange = { newA ->
                    onRoomChange(room.withWallA(newA, doorCentered))
                },
            )

            // 3. Pared B (Lateral Este / Derecha) - Independent!
            WallHeaderWithLock(
                letter = "B",
                name = "Pared Este (Derecha)",
                isLocked = room.lockWallB,
                onLockToggle = { onRoomChange(room.copy(lockWallB = !room.lockWallB)) },
            )
            DimensionSlider(
                label = if (symmetricOppositeWalls) "Largo Pared B (sincronizada con C)" else "Largo Pared B (independiente)",
                valueMm = room.wallBMm,
                rangeMm = 1500f..6000f,
                stepMm = 50f,
                onValueChange = { newB ->
                    if (symmetricOppositeWalls) {
                        onRoomChange(
                            room.copy(clearDepthMm = newB, wallBMm = newB)
                                .let { if (doorCentered) it.withCenteredDoor() else it },
                        )
                    } else {
                        onRoomChange(room.withWallB(newB, doorCentered))
                    }
                },
            )

            // 4. Pared C (Lateral Oeste / Izquierda) - Independent!
            WallHeaderWithLock(
                letter = "C",
                name = "Pared Oeste (Izquierda)",
                isLocked = room.lockWallC,
                onLockToggle = { onRoomChange(room.copy(lockWallC = !room.lockWallC)) },
            )
            DimensionSlider(
                label = if (symmetricOppositeWalls) "Largo Pared C (sincronizada con B)" else "Largo Pared C (independiente)",
                valueMm = room.wallCMm,
                rangeMm = 1500f..6000f,
                stepMm = 50f,
                onValueChange = { newC ->
                    if (symmetricOppositeWalls) {
                        onRoomChange(
                            room.copy(clearDepthMm = newC, wallBMm = newC)
                                .let { if (doorCentered) it.withCenteredDoor() else it },
                        )
                    } else {
                        onRoomChange(room.withWallC(newC, doorCentered))
                    }
                },
            )

            HorizontalDivider()

            // 5. Corner Angles Card (Independent checkbox per corner to unlock/edit angle)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                ),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ángulos de Esquinas (marcar check para editar)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            val sumAngles = room.angleACDeg + room.angleABDeg + room.angleBEDeg + room.angleCDDeg
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "AC: %.1f° • AB: %.1f° • CD: %.1f° • BE: %.1f° (Σ=%.0f°)",
                                    room.angleACDeg,
                                    room.angleABDeg,
                                    room.angleCDDeg,
                                    room.angleBEDeg,
                                    sumAngles,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        TextButton(
                            onClick = {
                                onRoomChange(
                                    room.copy(angleACDeg = 90f, angleABDeg = 90f)
                                        .let { if (doorCentered) it.withCenteredDoor() else it },
                                )
                            },
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" 90°", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Corner A-C (North-West)
                    AngleSliderRow(
                        cornerLabel = "Esquina A-C (Arriba Izq.)",
                        angleDeg = room.angleACDeg,
                        isLocked = room.lockAngleAC,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAC = !room.lockAngleAC)) },
                        onAngleChange = { newDeg ->
                            symmetricOppositeWalls = false
                            onRoomChange(room.withAngleAC(newDeg, doorCentered))
                        },
                    )

                    // Corner A-B (North-East)
                    AngleSliderRow(
                        cornerLabel = "Esquina A-B (Arriba Der.)",
                        angleDeg = room.angleABDeg,
                        isLocked = room.lockAngleAB,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAB = !room.lockAngleAB)) },
                        onAngleChange = { newDeg ->
                            symmetricOppositeWalls = false
                            onRoomChange(room.withAngleAB(newDeg, doorCentered))
                        },
                    )

                    // Corner C-D (South-West)
                    AngleSliderRow(
                        cornerLabel = "Esquina C-D (Abajo Izq. • adapta B/Sur)",
                        angleDeg = room.angleCDDeg,
                        isLocked = room.lockAngleCD,
                        onLockToggle = { onRoomChange(room.copy(lockAngleCD = !room.lockAngleCD)) },
                        onAngleChange = { newDeg ->
                            symmetricOppositeWalls = false
                            onRoomChange(room.withAngleCD(newDeg, doorCentered))
                        },
                    )

                    // Corner B-E (South-East)
                    AngleSliderRow(
                        cornerLabel = "Esquina B-E (Abajo Der. • adapta C/Sur)",
                        angleDeg = room.angleBEDeg,
                        isLocked = room.lockAngleBE,
                        onLockToggle = { onRoomChange(room.copy(lockAngleBE = !room.lockAngleBE)) },
                        onAngleChange = { newDeg ->
                            symmetricOppositeWalls = false
                            onRoomChange(room.withAngleBE(newDeg, doorCentered))
                        },
                    )
                }
            }

            HorizontalDivider()

            // 6. Pared con Puerta: Tramos D y E (South Wall)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            WallBadge(letter = "D")
                            Icon(
                                Icons.Default.DoorFront,
                                contentDescription = null,
                                tint = Color(0xFF8B5A2B),
                                modifier = Modifier.size(18.dp),
                            )
                            WallBadge(letter = "E")
                            Text(
                                text = "Pared Puerta (Sur)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = room.lockWallSouth,
                                onClick = { onRoomChange(room.copy(lockWallSouth = !room.lockWallSouth)) },
                                label = {
                                    Text(
                                        text = if (room.lockWallSouth) "Fija 🔒" else "Cierre 🔓",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (room.lockWallSouth) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                },
                            )
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
                                style = MaterialTheme.typography.labelSmall,
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
                                text = "+ 🚪 ${room.door.widthMm.roundToInt()} mm +",
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

                    // Immutable Door Width slider
                    DimensionSlider(
                        label = "Ancho de Puerta (Inmutable al mover lados)",
                        valueMm = room.door.widthMm,
                        rangeMm = 600f..1400f,
                        stepMm = 50f,
                        onValueChange = { newDoorW ->
                            onRoomChange(room.withDoorWidth(newDoorW, keepCentered = doorCentered))
                        },
                    )

                    if (!doorCentered) {
                        val maxSide = room.maxDoorOffset().coerceAtLeast(100f)
                        DimensionSlider(
                            label = "Tramo D (Izquierda • E se adapta solo)",
                            valueMm = room.segmentDMm,
                            rangeMm = 0f..maxSide,
                            stepMm = 25f,
                            onValueChange = { newD ->
                                onRoomChange(room.withSegmentD(newD))
                            },
                        )

                        DimensionSlider(
                            label = "Tramo E (Derecha • D se adapta solo)",
                            valueMm = room.segmentEMm,
                            rangeMm = 0f..maxSide,
                            stepMm = 25f,
                            onValueChange = { newE ->
                                onRoomChange(room.withSegmentE(newE))
                            },
                        )
                    }
                }
            }

            // 7. Ceiling Height (Y)
            DimensionSlider(
                label = "Altura del Techo (Eje Y)",
                valueMm = room.clearHeightMm,
                rangeMm = 2000f..3500f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearHeightMm = it)) },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Listo • Ver en 3D", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WallHeaderWithLock(
    letter: String,
    name: String,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
        FilterChip(
            selected = isLocked,
            onClick = onLockToggle,
            label = {
                Text(
                    text = if (isLocked) "Bloqueado 🔒" else "Adaptable 🔓",
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
        )
    }
}

@Composable
private fun AngleSliderRow(
    cornerLabel: String,
    angleDeg: Float,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onAngleChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Checkbox(
                    checked = !isLocked,
                    onCheckedChange = { onLockToggle() },
                )
                Text(
                    text = cornerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f°", angleDeg),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (abs(angleDeg - 90f) < 0.3f) Color(0xFF2C55B5) else Color(0xFFD97706),
                )
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.size(26.dp),
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Bloquear esquina",
                        tint = if (isLocked) Color(0xFF2C55B5) else Color(0xFFD97706),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Slider(
            value = angleDeg.coerceIn(65f, 115f),
            enabled = !isLocked,
            onValueChange = { raw ->
                val snapped = (raw * 2f).roundToInt() / 2f
                onAngleChange(snapped)
            },
            valueRange = 65f..115f,
        )
    }
}

@Composable
private fun WallBadge(letter: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF2C55B5),
    ) {
        Text(
            text = " $letter ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
    val steps = ((rangeMm.endInclusive - rangeMm.start) / stepMm).toInt() - 1

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${valueMm.roundToInt()} mm (${String.format(Locale.US, "%.2f", valueMm / 1000f)} m)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C55B5),
            )
        }
        Slider(
            value = valueMm.coerceIn(rangeMm.start, rangeMm.endInclusive),
            onValueChange = { raw ->
                val snapped = (raw / stepMm).roundToInt() * stepMm
                onValueChange(snapped.coerceIn(rangeMm.start, rangeMm.endInclusive))
            },
            valueRange = rangeMm,
            steps = steps.coerceAtLeast(0),
        )
    }
}
