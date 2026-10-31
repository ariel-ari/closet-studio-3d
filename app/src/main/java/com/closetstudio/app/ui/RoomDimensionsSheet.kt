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
    val lang = LocalAppLanguage.current
    val unitSystem = LocalAppUnitSystem.current
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
                            text = tr(lang, "Inspector del Plano 2D", "2D Floorplan Inspector"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = tr(
                                lang,
                                "Tocá una pared o esquina en el dibujo para editarla",
                                "Tap a wall or corner on the drawing to edit it",
                            ),
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
                    Text(tr(lang, " Escuadra 90°", " Square 90°"), style = MaterialTheme.typography.labelSmall)
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = tr(lang, "Cerrar", "Close"), modifier = Modifier.size(20.dp))
                }
            }

            // 2. Quick Selector Strip (synced with 2D floorplan tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ElementChip(tr(lang, "Pared A", "Wall A"), selectedElement == FloorplanElement.WallA) {
                    onSelectElement(FloorplanElement.WallA)
                }
                ElementChip(tr(lang, "Pared B", "Wall B"), selectedElement == FloorplanElement.WallB) {
                    onSelectElement(FloorplanElement.WallB)
                }
                ElementChip(tr(lang, "Pared C", "Wall C"), selectedElement == FloorplanElement.WallC) {
                    onSelectElement(FloorplanElement.WallC)
                }
                ElementChip(tr(lang, "Puerta E-D", "Door E-D"), selectedElement == FloorplanElement.WallSouthDoor) {
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
                ElementChip(tr(lang, "Altura Y", "Height Y"), selectedElement == FloorplanElement.CeilingHeight) {
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
                        title = tr(lang, "Pared A", "Wall A"),
                        subtitle = tr(lang, "Se redimensiona sola sin obligar a Pared C", "Resizes independently without forcing Wall C"),
                        valueMm = room.wallCMm,
                        isLocked = room.lockWallC,
                        onLockToggle = { onRoomChange(room.copy(lockWallC = !room.lockWallC)) },
                        onValueChange = { onRoomChange(room.withWallC(it, doorCentered)) },
                        oppositeButtonLabel = tr(
                            lang,
                            "Igualar A con Pared C (${room.wallBMm.roundToInt()} mm)",
                            "Match A with Wall C (${room.wallBMm.roundToInt()} mm)",
                        ),
                    ) {
                        onRoomChange(room.withWallC(room.wallBMm, doorCentered))
                    }
                }

                FloorplanElement.WallB -> {
                    WallInspectorCard(
                        letter = "B",
                        title = tr(lang, "Pared B", "Wall B"),
                        subtitle = tr(lang, "Pared principal frente a la puerta", "Main wall opposite the door"),
                        valueMm = room.wallAMm,
                        isLocked = room.lockWallA,
                        onLockToggle = { onRoomChange(room.copy(lockWallA = !room.lockWallA)) },
                        onValueChange = { onRoomChange(room.withWallA(it, doorCentered)) },
                        oppositeButtonLabel = tr(
                            lang,
                            "Igualar B con Pared E-D (${room.wallSouthMm.roundToInt()} mm)",
                            "Match B with Wall E-D (${room.wallSouthMm.roundToInt()} mm)",
                        ),
                    ) {
                        onRoomChange(room.withWallA(room.wallSouthMm, doorCentered))
                    }
                }

                FloorplanElement.WallC -> {
                    WallInspectorCard(
                        letter = "C",
                        title = tr(lang, "Pared C", "Wall C"),
                        subtitle = tr(lang, "Se redimensiona sola sin obligar a Pared A", "Resizes independently without forcing Wall A"),
                        valueMm = room.wallBMm,
                        isLocked = room.lockWallB,
                        onLockToggle = { onRoomChange(room.copy(lockWallB = !room.lockWallB)) },
                        onValueChange = { onRoomChange(room.withWallB(it, doorCentered)) },
                        oppositeButtonLabel = tr(
                            lang,
                            "Igualar C con Pared A (${room.wallCMm.roundToInt()} mm)",
                            "Match C with Wall A (${room.wallCMm.roundToInt()} mm)",
                        ),
                    ) {
                        onRoomChange(room.withWallB(room.wallCMm, doorCentered))
                    }
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
                        title = tr(lang, "Esquina A-B", "Corner A-B"),
                        subtitle = tr(lang, "Ángulo interior entre Pared A y Pared B", "Interior angle between Wall A and Wall B"),
                        angleDeg = room.angleACDeg,
                        isLocked = room.lockAngleAC,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAC = !room.lockAngleAC)) },
                    ) {
                        onRoomChange(room.withAngleAC(it, doorCentered))
                    }
                }

                FloorplanElement.CornerAB -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleCD, room.lockAngleBE).count { it }
                    CornerInspectorCard(
                        cornerCode = "B-C",
                        title = tr(lang, "Esquina B-C", "Corner B-C"),
                        subtitle = tr(lang, "Ángulo interior entre Pared B y Pared C", "Interior angle between Wall B and Wall C"),
                        angleDeg = room.angleABDeg,
                        isLocked = room.lockAngleAB,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleAB = !room.lockAngleAB)) },
                    ) {
                        onRoomChange(room.withAngleAB(it, doorCentered))
                    }
                }

                FloorplanElement.CornerBE -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleAB, room.lockAngleCD).count { it }
                    CornerInspectorCard(
                        cornerCode = "C-D",
                        title = tr(lang, "Esquina C-D", "Corner C-D"),
                        subtitle = tr(lang, "Ángulo interior entre Pared C y Tramo D", "Interior angle between Wall C and Segment D"),
                        angleDeg = room.angleBEDeg,
                        isLocked = room.lockAngleBE,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleBE = !room.lockAngleBE)) },
                    ) {
                        onRoomChange(room.withAngleBE(it, doorCentered))
                    }
                }

                FloorplanElement.CornerCD -> {
                    val otherLockedCount = listOf(room.lockAngleAC, room.lockAngleAB, room.lockAngleBE).count { it }
                    CornerInspectorCard(
                        cornerCode = "A-E",
                        title = tr(lang, "Esquina A-E", "Corner A-E"),
                        subtitle = tr(lang, "Ángulo interior entre Pared A y Tramo E", "Interior angle between Wall A and Segment E"),
                        angleDeg = room.angleCDDeg,
                        isLocked = room.lockAngleCD,
                        allOthersLocked = otherLockedCount == 3,
                        room = room,
                        onLockToggle = { onRoomChange(room.copy(lockAngleCD = !room.lockAngleCD)) },
                    ) {
                        onRoomChange(room.withAngleCD(it, doorCentered))
                    }
                }

                FloorplanElement.CeilingHeight -> {
                    WallInspectorCard(
                        letter = "Y",
                        title = tr(lang, "Altura del Techo", "Ceiling Height"),
                        subtitle = tr(lang, "Distancia libre piso a techo", "Clear floor-to-ceiling height"),
                        valueMm = room.clearHeightMm,
                        rangeMm = 2000f..3500f,
                        isLocked = false,
                        onLockToggle = {},
                        onValueChange = { onRoomChange(room.copy(clearHeightMm = it)) },
                        oppositeButtonLabel = null,
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(tr(lang, "Listo • Ver en 3D", "Done • View in 3D"), fontWeight = FontWeight.Bold)
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
    rangeMm: ClosedFloatingPointRange<Float> = 0f..maxOf(10000f, valueMm * 1.5f),
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onValueChange: (Float) -> Unit,
    oppositeButtonLabel: String?,
    onMatchOpposite: () -> Unit,
) {
    val lang = LocalAppLanguage.current
    val unitSystem = LocalAppUnitSystem.current
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
                                    contentDescription = if (isLocked) tr(lang, "Bloqueado", "Locked") else tr(lang, "Adaptable", "Unlocked"),
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
                            text = if (isLocked) tr(lang, "Tamaño bloqueado (tocá el candado para editar)", "Size locked (tap padlock to edit)") else subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLocked) Color(0xFF2C55B5) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Direct Numeric Input + Fine-tuning step buttons
            DirectNumericInputRow(
                label = tr(lang, "Medida $letter (${unitSystem.symbol})", "Dimension $letter (${unitSystem.symbol})"),
                currentValueText = unitSystem.formatInput(valueMm),
                unitSuffix = unitSystem.symbol,
                enabled = !isLocked,
                isDecimal = unitSystem != AppUnitSystem.MM,
                isImperial = unitSystem == AppUnitSystem.INCH || unitSystem == AppUnitSystem.FEET_INCHES,
                onCommitText = { rawText ->
                    if (!isLocked) {
                        unitSystem.parseInput(rawText)?.let { parsed ->
                            onValueChange(parsed.coerceAtLeast(0f))
                        }
                    }
                },
                trailingButtons = {
                    unitSystem.stepValues().forEach { (stepLabel, stepDeltaMm) ->
                        StepButton(stepLabel, enabled = !isLocked) {
                            onValueChange((valueMm + stepDeltaMm).coerceAtLeast(0f))
                        }
                    }
                },
            )

            DimensionSlider(
                label = tr(lang, "Deslizar medida ($letter)", "Slide dimension ($letter)"),
                valueMm = valueMm,
                rangeMm = rangeMm,
                stepMm = unitSystem.sliderStepMm(),
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
    val lang = LocalAppLanguage.current
    val unitSystem = LocalAppUnitSystem.current
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
                                contentDescription = if (isLocked) tr(lang, "Esquina bloqueada", "Corner locked") else tr(lang, "Esquina libre", "Corner unlocked"),
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
                        text = tr(lang, "Desbloqueá al menos otra esquina para compensar Σ=360°", "Unlock at least one other corner to compensate Σ=360°"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Direct Numeric Input for Angle + fine step buttons
            DirectNumericInputRow(
                label = tr(lang, "Ángulo $cornerCode (°)", "Angle $cornerCode (°)"),
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
                    tr(
                        lang,
                        "Cierre automático activo (Σ = %.0f°) • E-D: %d mm",
                        "Automatic closure active (Σ = %.0f°) • E-D: %d mm",
                    ),
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
    val lang = LocalAppLanguage.current
    val unitSystem = LocalAppUnitSystem.current
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
            val totalLocked = room.isSouthWallTotalLocked
            val allThreeSegmentsLocked = room.lockSegmentD && room.door.lockWidth && room.lockSegmentE

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
                        color = if (totalLocked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shadowElevation = 1.dp,
                        onClick = {
                            if (allThreeSegmentsLocked && !room.lockWallSouth) {
                                // Unlocking when all 3 segments were locked: unlock E & D so the total can be adjusted
                                onRoomChange(
                                    room.copy(
                                        lockWallSouth = false,
                                        lockSegmentD = false,
                                        lockSegmentE = false,
                                    ),
                                )
                            } else {
                                onRoomChange(room.copy(lockWallSouth = !room.lockWallSouth))
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (totalLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (totalLocked) tr(lang, "Pared E-D fija", "Wall E-D locked") else tr(lang, "Pared E-D adaptable", "Wall E-D unlocked"),
                                tint = if (totalLocked) {
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
                        text = tr(lang, "Pared E - Puerta - D", "Wall E - Door - D"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = doorCentered,
                        enabled = !totalLocked && !room.lockSegmentD && !room.lockSegmentE,
                        onCheckedChange = onDoorCenteredChange,
                    )
                    Text(
                        text = tr(lang, "Centrada", "Centered"),
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
                        text = "E: ${unitSystem.formatCompact(room.segmentDMm)}",
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
                            text = "${unitSystem.formatCompact(room.door.widthMm)} +",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5A2B),
                        )
                    }
                    Text(
                        text = "D: ${unitSystem.formatCompact(room.segmentEMm)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C55B5),
                    )
                    Text(
                        text = "= ${unitSystem.formatCompact(room.doorWallLengthMm)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Total Wall E-D Direct Input & Slider
            DirectNumericInputRow(
                label = tr(lang, "Total Pared E-D (${unitSystem.symbol})", "Total Wall E-D (${unitSystem.symbol})"),
                currentValueText = unitSystem.formatInput(room.doorWallLengthMm),
                unitSuffix = unitSystem.symbol,
                enabled = !totalLocked,
                isDecimal = unitSystem != AppUnitSystem.MM,
                isImperial = unitSystem == AppUnitSystem.INCH || unitSystem == AppUnitSystem.FEET_INCHES,
                onCommitText = { rawText ->
                    if (!totalLocked) {
                        unitSystem.parseInput(rawText)?.let { parsed ->
                            onRoomChange(room.withWallSouth(parsed.coerceAtLeast(0f), keepDoorCentered = doorCentered))
                        }
                    }
                },
                trailingButtons = {
                    unitSystem.stepValues().forEach { (stepLabel, stepDeltaMm) ->
                        StepButton(stepLabel, enabled = !totalLocked) {
                            onRoomChange(room.withWallSouth((room.doorWallLengthMm + stepDeltaMm).coerceAtLeast(0f), keepDoorCentered = doorCentered))
                        }
                    }
                },
            )

            DimensionSlider(
                label = if (allThreeSegmentsLocked) {
                    tr(lang, "Total bloqueado (E, Puerta y D bloqueados)", "Total locked (E, Door and D locked)")
                } else {
                    tr(lang, "Deslizar Total Pared E-D", "Slide Total Wall E-D")
                },
                valueMm = room.doorWallLengthMm,
                rangeMm = 0f..maxOf(10000f, room.doorWallLengthMm * 1.5f),
                stepMm = unitSystem.sliderStepMm(),
                enabled = !totalLocked,
            ) { newTotal ->
                if (!totalLocked) {
                    onRoomChange(room.withWallSouth(newTotal, keepDoorCentered = doorCentered))
                }
            }

            HorizontalDivider()

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
                                contentDescription = if (room.door.lockWidth) tr(lang, "Puerta fija", "Door locked") else tr(lang, "Puerta escalable", "Door unlocked"),
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
                        text = tr(lang, "Ancho de Puerta", "Door Width"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            DirectNumericInputRow(
                label = tr(lang, "Puerta (${unitSystem.symbol})", "Door (${unitSystem.symbol})"),
                currentValueText = unitSystem.formatInput(room.door.widthMm),
                unitSuffix = unitSystem.symbol,
                enabled = !room.door.lockWidth,
                isDecimal = unitSystem != AppUnitSystem.MM,
                isImperial = unitSystem == AppUnitSystem.INCH || unitSystem == AppUnitSystem.FEET_INCHES,
                onCommitText = { rawText ->
                    if (!room.door.lockWidth) {
                        unitSystem.parseInput(rawText)?.let { parsed ->
                            onRoomChange(room.withDoorWidth(parsed.coerceAtLeast(0f), keepCentered = doorCentered))
                        }
                    }
                },
                trailingButtons = {
                    unitSystem.stepValues().forEach { (stepLabel, stepDeltaMm) ->
                        StepButton(stepLabel, enabled = !room.door.lockWidth) {
                            onRoomChange(room.withDoorWidth((room.door.widthMm + stepDeltaMm).coerceAtLeast(0f), keepCentered = doorCentered))
                        }
                    }
                },
            )

            DimensionSlider(
                label = tr(lang, "Deslizar Puerta", "Slide Door Width"),
                valueMm = room.door.widthMm,
                rangeMm = 0f..maxOf(3000f, room.doorWallLengthMm, room.door.widthMm * 1.5f),
                stepMm = unitSystem.sliderStepMm(),
                enabled = !room.door.lockWidth,
            ) { newW ->
                if (!room.door.lockWidth) {
                    onRoomChange(room.withDoorWidth(newW, keepCentered = doorCentered))
                }
            }

            HorizontalDivider()

            val maxSliderSide = if (room.lockSegmentE || room.lockSegmentD) {
                maxOf(10000f, room.doorWallLengthMm, room.segmentDMm * 1.5f, room.segmentEMm * 1.5f)
            } else {
                room.maxDoorOffset().coerceAtLeast(100f)
            }
            val canEditE = !room.lockSegmentD && !(room.lockSegmentE && room.lockWallSouth)
            val canEditD = !room.lockSegmentE && !(room.lockSegmentD && room.lockWallSouth)

            // Tramo E Lock + Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (room.lockSegmentD) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shadowElevation = 1.dp,
                    onClick = { onRoomChange(room.copy(lockSegmentD = !room.lockSegmentD)) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (room.lockSegmentD) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (room.lockSegmentD) tr(lang, "Tramo E bloqueado", "Segment E locked") else tr(lang, "Tramo E adaptable", "Segment E unlocked"),
                            tint = if (room.lockSegmentD) {
                                Color(0xFF2C55B5)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                WallBadge(letter = "E")
                Text(
                    text = tr(lang, "Tramo E", "Segment E"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            DirectNumericInputRow(
                label = tr(lang, "Tramo E (${unitSystem.symbol})", "Segment E (${unitSystem.symbol})"),
                currentValueText = unitSystem.formatInput(room.segmentDMm),
                unitSuffix = unitSystem.symbol,
                enabled = canEditE,
                isDecimal = unitSystem != AppUnitSystem.MM,
                isImperial = unitSystem == AppUnitSystem.INCH || unitSystem == AppUnitSystem.FEET_INCHES,
                onCommitText = { rawText ->
                    if (canEditE) {
                        unitSystem.parseInput(rawText)?.let { parsed ->
                            if (doorCentered) onDoorCenteredChange(false)
                            onRoomChange(room.withSegmentD(parsed.coerceAtLeast(0f)))
                        }
                    }
                },
                trailingButtons = {
                    unitSystem.stepValues().forEach { (stepLabel, stepDeltaMm) ->
                        StepButton(stepLabel, enabled = canEditE) {
                            if (doorCentered) onDoorCenteredChange(false)
                            onRoomChange(room.withSegmentD((room.segmentDMm + stepDeltaMm).coerceAtLeast(0f)))
                        }
                    }
                },
            )

            DimensionSlider(
                label = if (room.lockSegmentE) {
                    tr(lang, "Deslizar Tramo E (agranda el total)", "Slide Segment E (resizes total wall)")
                } else {
                    tr(lang, "Deslizar Tramo E (D se adapta solo)", "Slide Segment E (D adapts automatically)")
                },
                valueMm = room.segmentDMm,
                rangeMm = 0f..maxSliderSide,
                stepMm = unitSystem.sliderStepMm(),
                enabled = canEditE,
            ) { newE ->
                if (canEditE) {
                    if (doorCentered) onDoorCenteredChange(false)
                    onRoomChange(room.withSegmentD(newE))
                }
            }

            HorizontalDivider()

            // Tramo D Lock + Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (room.lockSegmentE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shadowElevation = 1.dp,
                    onClick = { onRoomChange(room.copy(lockSegmentE = !room.lockSegmentE)) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (room.lockSegmentE) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (room.lockSegmentE) tr(lang, "Tramo D bloqueado", "Segment D locked") else tr(lang, "Tramo D adaptable", "Segment D unlocked"),
                            tint = if (room.lockSegmentE) {
                                Color(0xFF2C55B5)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                WallBadge(letter = "D")
                Text(
                    text = tr(lang, "Tramo D", "Segment D"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            DirectNumericInputRow(
                label = tr(lang, "Tramo D (${unitSystem.symbol})", "Segment D (${unitSystem.symbol})"),
                currentValueText = unitSystem.formatInput(room.segmentEMm),
                unitSuffix = unitSystem.symbol,
                enabled = canEditD,
                isDecimal = unitSystem != AppUnitSystem.MM,
                isImperial = unitSystem == AppUnitSystem.INCH || unitSystem == AppUnitSystem.FEET_INCHES,
                onCommitText = { rawText ->
                    if (canEditD) {
                        unitSystem.parseInput(rawText)?.let { parsed ->
                            if (doorCentered) onDoorCenteredChange(false)
                            onRoomChange(room.withSegmentE(parsed.coerceAtLeast(0f)))
                        }
                    }
                },
                trailingButtons = {
                    unitSystem.stepValues().forEach { (stepLabel, stepDeltaMm) ->
                        StepButton(stepLabel, enabled = canEditD) {
                            if (doorCentered) onDoorCenteredChange(false)
                            onRoomChange(room.withSegmentE((room.segmentEMm + stepDeltaMm).coerceAtLeast(0f)))
                        }
                    }
                },
            )

            DimensionSlider(
                label = if (room.lockSegmentD) {
                    tr(lang, "Deslizar Tramo D (agranda el total)", "Slide Segment D (resizes total wall)")
                } else {
                    tr(lang, "Deslizar Tramo D (E se adapta solo)", "Slide Segment D (E adapts automatically)")
                },
                valueMm = room.segmentEMm,
                rangeMm = 0f..maxSliderSide,
                stepMm = unitSystem.sliderStepMm(),
                enabled = canEditD,
            ) { newD ->
                if (canEditD) {
                    if (doorCentered) onDoorCenteredChange(false)
                    onRoomChange(room.withSegmentE(newD))
                }
            }
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
    isImperial: Boolean = false,
    onCommitValue: ((Float) -> Unit)? = null,
    onCommitText: ((String) -> Unit)? = null,
    trailingButtons: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var textValue by remember(currentValueText) { mutableStateOf(currentValueText) }

    LaunchedEffect(currentValueText) {
        textValue = currentValueText
    }

    fun applyInput() {
        if (onCommitText != null) {
            onCommitText(textValue)
        } else if (onCommitValue != null) {
            val normalized = textValue.trim().replace(',', '.')
            val parsed = normalized.toFloatOrNull()
            if (parsed != null) {
                onCommitValue(parsed)
            } else {
                textValue = currentValueText
            }
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
                val filtered = if (isImperial) {
                    raw.filter { it.isDigit() || it == '.' || it == ',' || it == '\'' || it == '"' || it == '/' || it == ' ' }
                } else {
                    raw.filter { (it.isDigit()) || (isDecimal && ((it == '.') || (it == ','))) }
                }
                textValue = filtered
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            suffix = { Text(unitSuffix, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = {
                if (enabled && (textValue != currentValueText)) {
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
                keyboardType = if (isImperial) KeyboardType.Text else if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
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
    enabled: Boolean = true,
    stepMm: Float = 25f,
    onValueChange: (Float) -> Unit,
) {
    val unitSystem = LocalAppUnitSystem.current
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
                text = unitSystem.format(valueMm),
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
