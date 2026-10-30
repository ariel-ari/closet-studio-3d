package com.closetstudio.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex
import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Returns the base target position (center point on the wall) for a given wall index.
 * - Wall 0: South (Z = clearDepthMm, wall with door)
 * - Wall 1: West (X = 0)
 * - Wall 2: North (Z = 0)
 * - Wall 3: East (X = clearWidthMm)
 */
private fun getWallTarget(wallStep: Int, room: RoomSpec): Float3 {
    val w = room.clearWidthMm * MM_TO_M
    val d = room.clearDepthMm * MM_TO_M
    val midY = 1.40f // Eye height / wall center height
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> Float3(w * 0.5f, midY, d)
        1 -> Float3(0f, midY, d * 0.5f)
        2 -> Float3(w * 0.5f, midY, 0f)
        else -> Float3(w, midY, d * 0.5f)
    }
}

/**
 * Multi-touch gesture recognizer:
 * - 1 Finger: Orbit (horizontal yaw and vertical pitch)
 * - 2 Fingers: Pan (translate focal target and camera) & Pinch (zoom distance)
 */
private suspend fun PointerInputScope.detectRoomInspectionGestures(
    onOrbit: (deltaX: Float, deltaY: Float) -> Unit,
    onPan: (panX: Float, panY: Float) -> Unit,
    onZoom: (zoomFactor: Float) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var prevCentroid = Offset.Zero
        var prevDistance = 0f
        var wasMultiTouch = false

        do {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size == 1) {
                val pointer = activePointers[0]
                if (wasMultiTouch) {
                    // Reset multi-touch state so lifting finger does not cause jump
                    wasMultiTouch = false
                    prevDistance = 0f
                } else {
                    val delta = pointer.position - pointer.previousPosition
                    if (delta != Offset.Zero) {
                        onOrbit(delta.x, delta.y)
                        pointer.consume()
                    }
                }
            } else if (activePointers.size >= 2) {
                wasMultiTouch = true
                val p1 = activePointers[0].position
                val p2 = activePointers[1].position
                val centroid = (p1 + p2) / 2f
                val distance = (p1 - p2).getDistance()

                if (prevDistance > 0f) {
                    val panDelta = centroid - prevCentroid
                    val zoomFactor = distance / prevDistance

                    if (panDelta != Offset.Zero) {
                        onPan(panDelta.x, panDelta.y)
                    }
                    if (zoomFactor in 0.5f..1.8f && zoomFactor != 1f) {
                        onZoom(zoomFactor)
                    }
                }

                prevCentroid = centroid
                prevDistance = distance
                activePointers.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
fun RoomScreen() {
    val room = remember {
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
    }

    val coroutineScope = rememberCoroutineScope()

    // Wall index counter (0: South/Door, 1: West, 2: North, 3: East)
    val wallStep = remember { mutableIntStateOf(0) }

    val initialTarget = remember(room) { getWallTarget(0, room) }
    val targetXAnim = remember { Animatable(initialTarget.x) }
    val targetYAnim = remember { Animatable(initialTarget.y) }
    val targetZAnim = remember { Animatable(initialTarget.z) }

    val yawAnim = remember { Animatable(180f) }
    val pitchAnim = remember { Animatable(0f) }
    val distanceAnim = remember { Animatable(2.45f) }

    fun switchWall(newStep: Int) {
        wallStep.intValue = newStep
        val targetPos = getWallTarget(newStep, room)
        val targetYaw = 180f + newStep * 90f
        coroutineScope.launch {
            launch { yawAnim.animateTo(targetYaw, tween(400, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(2.45f, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(targetPos.x, tween(400, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(targetPos.y, tween(400, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(targetPos.z, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    fun recenterCurrentWall() {
        val targetPos = getWallTarget(wallStep.intValue, room)
        val targetYaw = 180f + wallStep.intValue * 90f
        coroutineScope.launch {
            launch { yawAnim.animateTo(targetYaw, tween(300, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(2.45f, tween(250, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(targetPos.x, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(targetPos.y, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(targetPos.z, tween(300, easing = FastOutSlowInEasing)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEAE4)),
    ) {
        // 3D Scene with 1-finger Orbit, 2-finger Pan & 2-finger Zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectRoomInspectionGestures(
                        onOrbit = { deltaX, deltaY ->
                            coroutineScope.launch {
                                yawAnim.snapTo(yawAnim.value - deltaX * 0.22f)
                                // Pitch acotado: desde -55° (vista maqueta cenital/superior) hasta +25° (vista rasante desde el suelo)
                                // Mantiene la cámara siempre por encima del piso sin deformación ni compresión
                                val newPitch = (pitchAnim.value - deltaY * 0.18f).coerceIn(-55f, 25f)
                                pitchAnim.snapTo(newPitch)
                            }
                        },
                        onPan = { panX, panY ->
                            val yawRad = Math.toRadians(yawAnim.value.toDouble()).toFloat()
                            val pitchRad = Math.toRadians(pitchAnim.value.toDouble()).toFloat()

                            val dirX = sin(yawRad) * cos(pitchRad)
                            val dirY = sin(pitchRad)
                            val dirZ = -cos(yawRad) * cos(pitchRad)

                            // Local camera right vector in world coordinates
                            val rightLen = sqrt(dirZ * dirZ + dirX * dirX).coerceAtLeast(0.001f)
                            val rightX = -dirZ / rightLen
                            val rightZ = dirX / rightLen

                            // Local camera up vector
                            val upX = -rightZ * dirY
                            val upY = rightZ * dirX - rightX * dirZ
                            val upZ = rightX * dirY

                            val panScale = distanceAnim.value * 0.0016f

                            // Inverted 2-finger pan: camera translates directly with finger direction
                            val deltaX = -rightX * (panX * panScale) + upX * (panY * panScale)
                            val deltaY = upY * (panY * panScale)
                            val deltaZ = -rightZ * (panX * panScale) + upZ * (panY * panScale)

                            coroutineScope.launch {
                                targetXAnim.snapTo(targetXAnim.value + deltaX)
                                targetYAnim.snapTo(targetYAnim.value + deltaY)
                                targetZAnim.snapTo(targetZAnim.value + deltaZ)
                            }
                        },
                        onZoom = { zoomFactor ->
                            coroutineScope.launch {
                                val newDist = (distanceAnim.value / zoomFactor).coerceIn(0.6f, 6.0f)
                                distanceAnim.snapTo(newDist)
                            }
                        },
                    )
                },
        ) {
            RoomScene(
                room = room,
                target = Float3(targetXAnim.value, targetYAnim.value, targetZAnim.value),
                yawAngleDegrees = yawAnim.value,
                pitchAngleDegrees = pitchAnim.value,
                distance = distanceAnim.value,
            )
        }

        // Left Navigation Button (< Previous Wall)
        IconButton(
            onClick = { switchWall(wallStep.intValue - 1) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Pared anterior",
                modifier = Modifier.size(32.dp),
            )
        }

        // Right Navigation Button (> Next Wall)
        IconButton(
            onClick = { switchWall(wallStep.intValue + 1) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Pared siguiente",
                modifier = Modifier.size(32.dp),
            )
        }

        // Bottom Recenter / Reset Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { recenterCurrentWall() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Centrar pared",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "  Centrar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
