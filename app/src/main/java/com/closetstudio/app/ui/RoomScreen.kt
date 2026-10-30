package com.closetstudio.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen() {
    var room by remember {
        mutableStateOf(
            RoomSpec(
                clearWidthMm = 3600f,
                clearHeightMm = 2600f,
                clearDepthMm = 3600f,
                door = DoorSpec(
                    wall = WallIndex.SOUTH,
                    offsetMm = 1350f,
                    widthMm = 900f,
                    heightMm = 2100f,
                ),
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val yawAnim = remember { Animatable(0f) }
    val pitchAnim = remember { Animatable(0f) }

    // Normalize angle for wall detection in [0, 360)
    val normalizedAngle = ((yawAnim.value % 360f) + 360f) % 360f
    val activeWallIndex = when {
        normalizedAngle in 315f..360f || normalizedAngle in 0f..45f -> WallIndex.NORTH
        normalizedAngle in 45f..135f -> WallIndex.EAST
        normalizedAngle in 135f..225f -> WallIndex.SOUTH
        else -> WallIndex.WEST
    }

    fun rotateByDegrees(delta: Float) {
        coroutineScope.launch {
            val target = round((yawAnim.value + delta) / 90f) * 90f
            yawAnim.animateTo(target, tween(350, easing = FastOutSlowInEasing))
        }
    }

    fun snapToWall(wall: WallIndex) {
        coroutineScope.launch {
            // Find shortest angular direction to the target wall
            var diff = (wall.degrees - (yawAnim.value % 360f)) % 360f
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            yawAnim.animateTo(yawAnim.value + diff, tween(400, easing = FastOutSlowInEasing))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Habitación 360°",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = activeWallIndex.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
        bottomBar = {
            RoomDimensionsPanel(
                room = room,
                onRoomChange = { room = it },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // 1-finger drag: fine smooth rotation ("de a pocos")
                                yawAnim.snapTo(yawAnim.value - dragAmount.x * 0.22f)
                                val newPitch = (pitchAnim.value + dragAmount.y * 0.15f).coerceIn(-22f, 22f)
                                pitchAnim.snapTo(newPitch)
                            }
                        },
                        onDragEnd = {
                            // Magnetic snap: if near a cardinal 90° angle, smoothly lock in
                            val current = ((yawAnim.value % 360f) + 360f) % 360f
                            val nearest = (round(current / 90f) * 90f) % 360f
                            val diff = abs(current - nearest)
                            if (diff < 18f || diff > 342f) {
                                coroutineScope.launch {
                                    val target = round(yawAnim.value / 90f) * 90f
                                    yawAnim.animateTo(target, tween(250, easing = FastOutSlowInEasing))
                                }
                            }
                        },
                    )
                },
        ) {
            // 3D Scene Viewport
            RoomScene(
                room = room,
                yawAngleDegrees = yawAnim.value,
                pitchAngleDegrees = pitchAnim.value,
            )

            // Top Quick Wall Selector (Tabs N / E / S / O)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WallIndex.values().forEach { wall ->
                    FilterChip(
                        selected = activeWallIndex == wall,
                        onClick = { snapToWall(wall) },
                        label = {
                            Text(
                                text = when (wall) {
                                    WallIndex.NORTH -> "Norte"
                                    WallIndex.EAST -> "Este"
                                    WallIndex.SOUTH -> "Sur (Puerta)"
                                    WallIndex.WEST -> "Oeste"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }

            // Quick 90° Turn Buttons: Left Arrow
            FilledIconButton(
                onClick = { rotateByDegrees(-90f) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Girar 90° Izquierda",
                )
            }

            // Quick 90° Turn Buttons: Right Arrow
            FilledIconButton(
                onClick = { rotateByDegrees(90f) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(46.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Girar 90° Derecha",
                )
            }

            // Hint pill in center bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.65f),
            ) {
                Text(
                    text = "Arrastrá con 1 dedo para girar de a poco • Tocá las flechas para 90°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RoomDimensionsPanel(
    room: RoomSpec,
    onRoomChange: (RoomSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                Text(
                    text = "Dimensiones y Puerta (mm)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            DimensionSlider(
                label = "Ancho Habitación (X)",
                valueMm = room.clearWidthMm,
                rangeMm = 2000f..5000f,
                stepMm = 100f,
                onValueChange = { onRoomChange(room.copy(clearWidthMm = it)) },
            )

            DimensionSlider(
                label = "Profundidad Habitación (Z)",
                valueMm = room.clearDepthMm,
                rangeMm = 2000f..5000f,
                stepMm = 100f,
                onValueChange = { onRoomChange(room.copy(clearDepthMm = it)) },
            )

            // Parametric Door Position Slider!
            val maxOffset = room.maxDoorOffset()
            DimensionSlider(
                label = "Posición de la Puerta (offset)",
                valueMm = room.door.offsetMm.coerceIn(0f, maxOffset),
                rangeMm = 0f..maxOffset.coerceAtLeast(100f),
                stepMm = 50f,
                onValueChange = {
                    onRoomChange(room.copy(door = room.door.copy(offsetMm = it)))
                },
            )
        }
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
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${valueMm.roundToInt()} mm",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = valueMm.coerceIn(rangeMm.start, rangeMm.endInclusive),
            onValueChange = { raw ->
                val stepped = (raw / stepMm).roundToInt() * stepMm
                onValueChange(stepped.coerceIn(rangeMm.start, rangeMm.endInclusive))
            },
            valueRange = rangeMm,
        )
    }
}
