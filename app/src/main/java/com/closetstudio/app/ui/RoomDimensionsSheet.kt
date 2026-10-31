package com.closetstudio.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.RoomSpec
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Contextual Property Inspector for the 2D CAD Floorplan:
 * Displays ONLY the controls for the wall or corner selected on the 2D plan,
 * avoiding a cluttered list of all sliders at once.
 */
@Composable
fun RoomDimensionsPanel(
    room: RoomSpec,
    selectedElement: FloorplanElement,
    onSelectElement: (FloorplanElement) -> Unit,
    onRoomChange: (RoomSpec) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var doorCentered by remember(room.isDoorCentered) { mutableStateOf(room.isDoorCentered) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Compact Header + Quick Reset to Orthogonal 90°
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFF2C55B5),
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text = "Inspector del Plano 2D",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Tocá una pared o esquina en el dibujo para editarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextButton(
                    onClick = { onRoomChange(room.withOrthogonalReset(doorCentered)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Escuadra 90°", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(20.dp))
                }
            }

            // 2. Quick Selector Strip (synced with 2D floorplan tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ElementChip("Pared A", selectedElement == FloorplanElement.WallA) {
                    onSelectElement(FloorplanElement.WallA)
                }
                ElementChip("Pared B", selectedElement == FloorplanElement.WallB) {
                    onSelectElement(FloorplanElement.WallB)
                }
                ElementChip("Pared C", selectedElement == FloorplanElement.WallC) {
                    onSelectElement(FloorplanElement.WallC)
                }
                ElementChip("Puerta E-D", selectedElement == FloorplanElement.WallSouthDoor) {
                    onSelectElement(FloorplanElement.WallSouthDoor)
                }
                ElementChip("∠ A-B", selectedElement == FloorplanElement.CornerAC, isCorner = true) {
                    onSelectElement(FloorplanElement.CornerAC)
                }
                ElementChip("∠ B-C", selectedElement == FloorplanElement.CornerAB, isCorner = true) {
                    onSelectElement(FloorplanElement.CornerAB)
                }
                ElementChip("∠ C-D", selectedElement == FloorplanElement.CornerBE, isCorner = true) {
                    onSelectElement(FloorplanElement.CornerBE)
                }
                ElementChip("∠ A-E", selectedElement == FloorplanElement.CornerCD, isCorner = true) {
                    onSelectElement(FloorplanElement.CornerCD)
                }
                ElementChip("Altura Y", selectedElement == FloorplanElement.CeilingHeight) {
                    onSelectElement(FloorplanElement.CeilingHeight)
                }
            }

            HorizontalDivider()

            // 3. Focused Contextual Inspector Card for the selected element
            // Clockwise wall mapping: A = Left (wallCMm), B = Top (wallAMm), C = Right (wallBMm), E = Bottom-Left (segmentDMm), D = Bottom-Right (segmentEMm)
            when (selectedElement) {
                FloorplanElement.WallA -> {
                    WallInspectorCard(
                        letter = "A",
                        title = "Pared A",
                        subtitle = "Se redimensiona sola sin obligar a Pared C",
                        valueMm = room.wallCMm,
                        isLocked = room.lockWallC,
                        onLockToggle = { onRoomChange(room.copy(lockWallC = !room.lockWallC)) },
                        onValueChange = { onRoomChange(room.withWallC(it, doorCentered)) },
                        oppositeButtonLabel = "Igualar A con Pared C (${room.wallBMm.roundToInt()} mm)",
                        onMatchOpposite = { onRoomChange(room.withWallC(room.wallBMm, doorCentered)) },
                    )
                }

                FloorplanElement.WallB -> {
                    WallInspectorCard(
                        letter = "B",
                        title = "Pared B",
                        subtitle = "Pared principal frente a la puerta",
                        valueMm = room.wallAMm,
                        isLocked = room.lockWallA,
                        onLockToggle = { onRoomChange(room.copy(lockWallA = !room.lockWallA)) },
                        onValueChange = { onRoomChange(room.withWallA(it, doorCentered)) },
                        oppositeButtonLabel = "Igualar B con Pared E-D (${room.wallSouthMm.roundToInt()} mm)",
                        onMatchOpposite = { onRoomChange(room.withWallA(room.wallSouthMm, doorCentered)) },
                    )
                }

                FloorplanElement.WallC -> {
                    WallInspectorCard(
                        letter = "C",
                        title = "Pared C",
                        subtitle = "Se redimensiona sola sin obligar a Pared A",
                        valueMm = room.wallBMm,
                        isLocked = room.lockWallB,
                        onLockToggle = { onRoomChange(room.copy(lockWallB = !room.lockWallB)) },
                        onValueChange = { onRoomChange(room.withWallB(it, doorCentered)) },
                        oppositeButtonLabel = "Igualar C con Pared A (${room.wallCMm.roundToInt()} mm)",
                        onMatchOpposite = { onRoomChange(room.withWallB(room.wallCMm, doorCentered)) },
                    )
                }

                FloorplanElement.WallSouthDoor -> {
                    DoorWallInspectorCard(
                        room = room,
                        doorCentered = doorCentered,
                        onDoorCenteredChange = { checked ->
                            doorCentered = checked
                            if (checked) {
                                onRoomChange(room.withCenteredDoor())
                            }
                        },
                        onRoomChange = onRoomChange,
                    )
                }

                FloorplanElement.CornerAC -> {
                    val otherLockedCount = listOf(room.lockAngleAB, room.lockAngleCD, room.lockAngleBE).count { it }
                    CornerInspectorCard(
                        cornerCode = "A-B",
                        title = "Esquina A-B",
                        subtitle = "Ángulo interior entre Pared A y Pared B",
                        angleDeg = room.angleACDeg,
                        isLocked = room.lockAngleAC,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAC = !room.lockAngleAC)) },
                        onAngleChange = { onRoomChange(room.withAngleAC(it, doorCentered)) },
                    )
                }

                FloorplanElement.CornerAB -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleCD, room.lockAngleBE).count { it }
                    CornerInspectorCard(
                        cornerCode = "B-C",
                        title = "Esquina B-C",
                        subtitle = "Ángulo interior entre Pared B y Pared C",
                        angleDeg = room.angleABDeg,
                        isLocked = room.lockAngleAB,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAB = !room.lockAngleAB)) },
                        onAngleChange = { onRoomChange(room.withAngleAB(it, doorCentered)) },
                    )
                }

                FloorplanElement.CornerBE -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleAB, room.lockAngleCD).count { it }
                    CornerInspectorCard(
                        cornerCode = "C-D",
                        title = "Esquina C-D",
                        subtitle = "Ángulo interior entre Pared C y Tramo D",
                        angleDeg = room.angleBEDeg,
                        isLocked = room.lockAngleBE,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleBE = !room.lockAngleBE)) },
                        onAngleChange = { onRoomChange(room.withAngleBE(it, doorCentered)) },
                    )
                }

                FloorplanElement.CornerCD -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleAB, room.lockAngleBE).count { it }
                    CornerInspectorCard(
                        cornerCode = "A-E",
                        title = "Esquina A-E",
                        subtitle = "Ángulo interior entre Pared A y Tramo E",
                        angleDeg = room.angleCDDeg,
                        isLocked = room.lockAngleCD,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleCD = !room.lockAngleCD)) },
                        onAngleChange = { onRoomChange(room.withAngleCD(it, doorCentered)) },
                    )
                }

                FloorplanElement.CeilingHeight -> {
                    WallInspectorCard(
                        letter = "Y",
                        title = "Altura del Techo",
                        subtitle = "Distancia libre piso a techo",
                        valueMm = room.clearHeightMm,
                        rangeMm = 2000f..3500f,
                        isLocked = false,
                        onLockToggle = {},
                        onValueChange = { onRoomChange(room.copy(clearHeightMm = it)) },
                        oppositeButtonLabel = null,
                        onMatchOpposite = {},
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

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
private fun ElementChip(
    label: String,
    selected: Boolean,
    isCorner: Boolean = false,
    onClick: () -> Unit,
) {
    val activeColor = if (isCorner) Color(0xFFD97706) else Color(0xFF2C55B5)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun WallInspectorCard(
    letter: String,
    title: String,
    subtitle: String,
    valueMm: Float,
    rangeMm: ClosedFloatingPointRange<Float> = 1500f..6000f,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onValueChange: (Float) -> Unit,
    oppositeButtonLabel: String?,
    onMatchOpposite: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (oppositeButtonLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLocked) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shadowElevation = 1.dp,
                            onClick = onLockToggle,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isLocked) "Bloqueado" else "Adaptable",
                                    tint = if (isLocked) {
                                        Color(0xFF2C55B5)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    WallBadge(letter = letter)
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isLocked) "Tamaño bloqueado (tocá el candado para editar)" else subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLocked) Color(0xFF2C55B5) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Direct Numeric Input + Fine-tuning step buttons
            DirectNumericInputRow(
                label = "Medida $letter (mm)",
                currentValueText = valueMm.roundToInt().toString(),
                unitSuffix = "mm",
                enabled = !isLocked,
                onCommitValue = { parsed ->
                    if (!isLocked) {
                        onValueChange(parsed.coerceIn(rangeMm))
                    }
                },
                trailingButtons = {
                    StepButton("-100", enabled = !isLocked) { onValueChange((valueMm - 100f).coerceIn(rangeMm)) }
                    StepButton("-25", enabled = !isLocked) { onValueChange((valueMm - 25f).coerceIn(rangeMm)) }
                    StepButton("+25", enabled = !isLocked) { onValueChange((valueMm + 25f).coerceIn(rangeMm)) }
                    StepButton("+100", enabled = !isLocked) { onValueChange((valueMm + 100f).coerceIn(rangeMm)) }
                },
            )

            DimensionSlider(
                label = "Deslizar medida ($letter)",
                valueMm = valueMm,
                rangeMm = rangeMm,
                stepMm = 25f,
                enabled = !isLocked,
                onValueChange = onValueChange,
            )

            if (oppositeButtonLabel != null) {
                OutlinedButton(
                    onClick = onMatchOpposite,
                    enabled = !isLocked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    Text(oppositeButtonLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CornerInspectorCard(
    cornerCode: String,
    title: String,
    subtitle: String,
    angleDeg: Float,
    isLocked: Boolean,
    allOthersLocked: Boolean,
    room: RoomSpec,
    onLockToggle: () -> Unit,
    onAngleChange: (Float) -> Unit,
) {
    val canEdit = !isLocked && !allOthersLocked
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLocked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shadowElevation = 1.dp,
                        onClick = onLockToggle,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isLocked) "Esquina bloqueada" else "Esquina libre",
                                tint = if (isLocked) {
                                    Color(0xFFD97706)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    WallBadge(letter = "∠ $cornerCode", bgColor = Color(0xFFD97706))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (allOthersLocked && !isLocked) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Desbloqueá al menos otra esquina para compensar Σ=360°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Direct Numeric Input for Angle + fine step buttons
            DirectNumericInputRow(
                label = "Ángulo $cornerCode (°)",
                currentValueText = String.format(Locale.US, "%.1f", angleDeg),
                unitSuffix = "°",
                enabled = canEdit,
                isDecimal = true,
                onCommitValue = { parsed ->
                    if (canEdit) {
                        onAngleChange(parsed.coerceIn(65f, 115f))
                    }
                },
                trailingButtons = {
                    StepButton("-2°", enabled = canEdit) {
                        onAngleChange((angleDeg - 2f).coerceIn(65f, 115f))
                    }
                    StepButton("90°", enabled = canEdit) {
                        onAngleChange(90f)
                    }
                    StepButton("+2°", enabled = canEdit) {
                        onAngleChange((angleDeg + 2f).coerceIn(65f, 115f))
                    }
                },
            )

            Slider(
                value = angleDeg.coerceIn(65f, 115f),
                enabled = canEdit,
                onValueChange = { raw ->
                    val snapped = (raw * 2f).roundToInt() / 2f
                    onAngleChange(snapped)
                },
                valueRange = 65f..115f,
            )

            val sumAngles = room.angleACDeg + room.angleABDeg + room.angleBEDeg + room.angleCDDeg
            Text(
                text = String.format(
                    Locale.US,
                    "Cierre automático activo (Σ = %.0f°) • E-D: %d mm",
                    sumAngles,
                    room.wallSouthMm.roundToInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DoorWallInspectorCard(
    room: RoomSpec,
    doorCentered: Boolean,
    onDoorCenteredChange: (Boolean) -> Unit,
    onRoomChange: (RoomSpec) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (room.lockWallSouth) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shadowElevation = 1.dp,
                        onClick = { onRoomChange(room.copy(lockWallSouth = !room.lockWallSouth)) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (room.lockWallSouth) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (room.lockWallSouth) "Pared E-D fija" else "Pared E-D adaptable",
                                tint = if (room.lockWallSouth) {
                                    Color(0xFF2C55B5)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    WallBadge(letter = "E")
                    Icon(
                        Icons.Default.DoorFront,
                        contentDescription = null,
                        tint = Color(0xFF8B5A2B),
                        modifier = Modifier.size(18.dp),
                    )
                    WallBadge(letter = "D")
                    Text(
                        text = "Pared E - Puerta - D",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = doorCentered,
                        onCheckedChange = onDoorCenteredChange,
                    )
                    Text(
                        text = "Centrada",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // Equation pill: [ E ] + [ Puerta ] + [ D ] = Total
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
                        text = "E: ${room.segmentDMm.roundToInt()} mm",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C55B5),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5A2B),
                        )
                        Icon(
                            imageVector = Icons.Default.DoorFront,
                            contentDescription = null,
                            tint = Color(0xFF8B5A2B),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${room.door.widthMm.roundToInt()} mm +",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5A2B),
                        )
                    }
                    Text(
                        text = "D: ${room.segmentEMm.roundToInt()} mm",
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

            // Door Width Lock + Direct Input section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (room.door.lockWidth) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shadowElevation = 1.dp,
                        onClick = {
                            onRoomChange(room.copy(door = room.door.copy(lockWidth = !room.door.lockWidth)))
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (room.door.lockWidth) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (room.door.lockWidth) "Puerta fija" else "Puerta escalable",
                                tint = if (room.door.lockWidth) {
                                    Color(0xFF8B5A2B)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Text(
                        text = "Ancho de Puerta",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            DirectNumericInputRow(
                label = "Puerta (mm)",
                currentValueText = room.door.widthMm.roundToInt().toString(),
                unitSuffix = "mm",
                enabled = !room.door.lockWidth,
                onCommitValue = { parsed ->
                    if (!room.door.lockWidth) {
                        onRoomChange(room.withDoorWidth(parsed.coerceIn(600f, 1400f), keepCentered = doorCentered))
                    }
                },
                trailingButtons = {
                    StepButton("-50", enabled = !room.door.lockWidth) {
                        onRoomChange(room.withDoorWidth((room.door.widthMm - 50f).coerceIn(600f, 1400f), keepCentered = doorCentered))
                    }
                    StepButton("+50", enabled = !room.door.lockWidth) {
                        onRoomChange(room.withDoorWidth((room.door.widthMm + 50f).coerceIn(600f, 1400f), keepCentered = doorCentered))
                    }
                },
            )

            DimensionSlider(
                label = "Deslizar Puerta",
                valueMm = room.door.widthMm,
                rangeMm = 600f..1400f,
                stepMm = 25f,
                enabled = !room.door.lockWidth,
                onValueChange = { newW ->
                    if (!room.door.lockWidth) {
                        onRoomChange(room.withDoorWidth(newW, keepCentered = doorCentered))
                    }
                },
            )

            val maxSide = room.maxDoorOffset().coerceAtLeast(100f)
            val segmentsEnabled = !room.lockWallSouth

            // Side-by-side Direct Numeric Inputs for Tramo E (left of door) and Tramo D (right of door)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DirectNumericInputRow(
                        label = "Tramo E (mm)",
                        currentValueText = room.segmentDMm.roundToInt().toString(),
                        unitSuffix = "mm",
                        enabled = segmentsEnabled,
                        onCommitValue = { parsed ->
                            if (segmentsEnabled) {
                                if (doorCentered) onDoorCenteredChange(false)
                                onRoomChange(room.withSegmentD(parsed.coerceIn(0f, maxSide)))
                            }
                        },
                        trailingButtons = {},
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DirectNumericInputRow(
                        label = "Tramo D (mm)",
                        currentValueText = room.segmentEMm.roundToInt().toString(),
                        unitSuffix = "mm",
                        enabled = segmentsEnabled,
                        onCommitValue = { parsed ->
                            if (segmentsEnabled) {
                                if (doorCentered) onDoorCenteredChange(false)
                                onRoomChange(room.withSegmentE(parsed.coerceIn(0f, maxSide)))
                            }
                        },
                        trailingButtons = {},
                    )
                }
            }

            DimensionSlider(
                label = "Deslizar Tramo E (D se adapta solo)",
                valueMm = room.segmentDMm,
                rangeMm = 0f..maxSide,
                stepMm = 25f,
                enabled = segmentsEnabled,
                onValueChange = { newE ->
                    if (segmentsEnabled) {
                        if (doorCentered) onDoorCenteredChange(false)
                        onRoomChange(room.withSegmentD(newE))
                    }
                },
            )

            DimensionSlider(
                label = "Deslizar Tramo D (E se adapta solo)",
                valueMm = room.segmentEMm,
                rangeMm = 0f..maxSide,
                stepMm = 25f,
                enabled = segmentsEnabled,
                onValueChange = { newD ->
                    if (segmentsEnabled) {
                        if (doorCentered) onDoorCenteredChange(false)
                        onRoomChange(room.withSegmentE(newD))
                    }
                },
            )
        }
    }
}

@Composable
private fun DirectNumericInputRow(
    label: String,
    currentValueText: String,
    unitSuffix: String,
    enabled: Boolean,
    isDecimal: Boolean = false,
    onCommitValue: (Float) -> Unit,
    trailingButtons: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var textValue by remember(currentValueText) { mutableStateOf(currentValueText) }

    LaunchedEffect(currentValueText) {
        textValue = currentValueText
    }

    fun applyInput() {
        val normalized = textValue.trim().replace(',', '.')
        val parsed = normalized.toFloatOrNull()
        if (parsed != null) {
            onCommitValue(parsed)
        } else {
            textValue = currentValueText
        }
        focusManager.clearFocus()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || (isDecimal && (it == '.' || it == ',')) }
                textValue = filtered
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            suffix = { Text(unitSuffix, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = {
                if (enabled && textValue != currentValueText) {
                    IconButton(
                        onClick = { applyInput() },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aplicar",
                            tint = Color(0xFF2C55B5),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { applyInput() },
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C55B5),
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailingButtons()
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shadowElevation = if (enabled) 1.dp else 0.dp,
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (enabled) {
                Color(0xFF2C55B5)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun WallBadge(letter: String, bgColor: Color = Color(0xFF2C55B5)) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
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
    enabled: Boolean = true,
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
            enabled = enabled,
            onValueChange = { raw ->
                val snapped = (raw / stepMm).roundToInt() * stepMm
                onValueChange(snapped.coerceIn(rangeMm.start, rangeMm.endInclusive))
            },
            valueRange = rangeMm,
            steps = steps.coerceAtLeast(0),
        )
    }
}
