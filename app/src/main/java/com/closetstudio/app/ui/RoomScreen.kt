package com.closetstudio.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.closetstudio.app.data.ProjectStorage
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.core.model.ClosetContainerSpec
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.PartitionHit
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex

import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class DraggedUnitType {
    CARCASS,
    VERTICAL_DIVIDER,
    HORIZONTAL_SHELF,
}

/**
 * Returns the base target position (center point on the wall) for a given wall index.
 * - Wall 0: South (Z = clearDepthMm, wall with door segments D and E)
 * - Wall 1: West (X = 0, Wall C)
 * - Wall 2: North (Z = 0, Wall A)
 * - Wall 3: East (X = clearWidthMm, Wall B)
 */
private fun getWallTarget(wallStep: Int, room: RoomSpec, split: Boolean = false): Float3 {
    val nw = room.vertexNW
    val ne = room.vertexNE
    val se = room.vertexSE
    val sw = room.vertexSW
    val midY = room.clearHeightMm * 0.5f * MM_TO_M

    if (!split) {
        return when (((wallStep % 4) + 4) % 4) {
            0 -> Float3((sw.x + se.x) * 0.5f * MM_TO_M, midY, (sw.z + se.z) * 0.5f * MM_TO_M)
            1 -> Float3((sw.x + nw.x) * 0.5f * MM_TO_M, midY, (sw.z + nw.z) * 0.5f * MM_TO_M)
            2 -> Float3((nw.x + ne.x) * 0.5f * MM_TO_M, midY, (nw.z + ne.z) * 0.5f * MM_TO_M)
            else -> Float3((ne.x + se.x) * 0.5f * MM_TO_M, midY, (ne.z + se.z) * 0.5f * MM_TO_M)
        }
    } else {
        val southDx = se.x - sw.x
        val southDz = se.z - sw.z
        val southLen = kotlin.math.sqrt(southDx * southDx + southDz * southDz).coerceAtLeast(100f)
        val dirX = southDx / southLen
        val dirZ = southDz / southLen

        return when (((wallStep % 5) + 5) % 5) {
            0 -> {
                // Segment E (Bottom-Left: 0 to segmentDMm)
                val offsetE = room.segmentDMm * 0.5f
                Float3((sw.x + dirX * offsetE) * MM_TO_M, midY, (sw.z + dirZ * offsetE) * MM_TO_M)
            }
            1 -> Float3((sw.x + nw.x) * 0.5f * MM_TO_M, midY, (sw.z + nw.z) * 0.5f * MM_TO_M)
            2 -> Float3((nw.x + ne.x) * 0.5f * MM_TO_M, midY, (nw.z + ne.z) * 0.5f * MM_TO_M)
            3 -> Float3((ne.x + se.x) * 0.5f * MM_TO_M, midY, (ne.z + se.z) * 0.5f * MM_TO_M)
            else -> {
                // Segment D (Bottom-Right: startD to southLen)
                val startD = (room.segmentDMm + room.door.widthMm).coerceAtMost(southLen)
                val offsetD = startD + room.segmentEMm * 0.5f
                Float3((sw.x + dirX * offsetD) * MM_TO_M, midY, (sw.z + dirZ * offsetD) * MM_TO_M)
            }
        }
    }
}

private fun shortestAngleTarget(currentDeg: Float, targetDeg: Float): Float {
    var diff = (targetDeg - currentDeg) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return currentDeg + diff
}

private fun getWallYaw(wallStep: Int, split: Boolean = false): Float {
    return if (!split) {
        val s = ((wallStep % 4) + 4) % 4
        180f + s * 90f
    } else {
        when (((wallStep % 5) + 5) % 5) {
            0 -> 180f // E (South wall looking North)
            1 -> 270f // A (West wall looking East)
            2 -> 360f // B (North wall looking South)
            3 -> 90f  // C (East wall looking West)
            else -> 180f // D (South wall looking North)
        }
    }
}

private fun wallIndexFromStep(wallStep: Int, split: Boolean = false): WallIndex {
    return if (!split) {
        when (((wallStep % 4) + 4) % 4) {
            0 -> WallIndex.SOUTH
            1 -> WallIndex.WEST
            2 -> WallIndex.NORTH
            else -> WallIndex.EAST
        }
    } else {
        when (((wallStep % 5) + 5) % 5) {
            0 -> WallIndex.SOUTH // Segment E
            1 -> WallIndex.WEST  // Wall A
            2 -> WallIndex.NORTH // Wall B
            3 -> WallIndex.EAST  // Wall C
            else -> WallIndex.SOUTH // Segment D
        }
    }
}

private fun stepFromWallIndex(wall: WallIndex, split: Boolean = false, isSegmentD: Boolean = false): Int = when (wall) {
    WallIndex.SOUTH -> if (split && isSegmentD) 4 else 0
    WallIndex.WEST -> 1
    WallIndex.NORTH -> 2
    WallIndex.EAST -> 3
}

private fun wallDisplayName(wall: WallIndex, lang: AppLanguage, split: Boolean = false, isSegmentD: Boolean = false): String {
    val s = stringsFor(lang)
    val prefix = s.wallALabel.substringBefore(" ")
    return when (wall) {
        WallIndex.WEST -> s.wallALabel
        WallIndex.NORTH -> s.wallBLabel
        WallIndex.EAST -> s.wallCLabel
        WallIndex.SOUTH -> {
            if (!split) "$prefix E-D"
            else if (isSegmentD) "$prefix D"
            else "$prefix E"
        }
    }
}

private fun getWallName(wallStep: Int, lang: AppLanguage, split: Boolean = false): String {
    val s = stringsFor(lang)
    val prefix = s.wallALabel.substringBefore(" ")
    return if (!split) {
        when (((wallStep % 4) + 4) % 4) {
            0 -> "$prefix E-D"
            1 -> s.wallALabel
            2 -> s.wallBLabel
            else -> s.wallCLabel
        }
    } else {
        when (((wallStep % 5) + 5) % 5) {
            0 -> "$prefix E"
            1 -> s.wallALabel
            2 -> s.wallBLabel
            3 -> s.wallCLabel
            else -> "$prefix D"
        }
    }
}

private fun getWallLengthMm(wall: WallIndex, room: RoomSpec): Float {
    val nw = room.vertexNW
    val ne = room.vertexNE
    val se = room.vertexSE
    val sw = room.vertexSW
    val (pStart, pEnd) = when (wall) {
        WallIndex.NORTH -> nw to ne
        WallIndex.EAST -> ne to se
        WallIndex.SOUTH -> se to sw
        WallIndex.WEST -> sw to nw
    }
    val dx = pEnd.x - pStart.x
    val dz = pEnd.z - pStart.z
    return kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(100f)
}

private fun getWallLengthMmForStep(wallStep: Int, room: RoomSpec, split: Boolean = false): Float {
    return if (!split) {
        val wallIdx = wallIndexFromStep(wallStep, false)
        getWallLengthMm(wallIdx, room)
    } else {
        when (((wallStep % 5) + 5) % 5) {
            0 -> room.segmentDMm.coerceAtLeast(100f) // Segment E (Bottom-Left)
            1 -> getWallLengthMm(WallIndex.WEST, room)
            2 -> getWallLengthMm(WallIndex.NORTH, room)
            3 -> getWallLengthMm(WallIndex.EAST, room)
            else -> room.segmentEMm.coerceAtLeast(100f) // Segment D (Bottom-Right)
        }
    }
}

private fun wallLetterCode(wall: WallIndex): String = when (wall) {
    WallIndex.WEST -> "A"
    WallIndex.NORTH -> "B"
    WallIndex.EAST -> "C"
    WallIndex.SOUTH -> "E-D"
}

private fun wallLetterCodeFromStep(wallStep: Int, split: Boolean = false): String {
    return if (!split) {
        when (((wallStep % 4) + 4) % 4) {
            0 -> "E-D"
            1 -> "A"
            2 -> "B"
            else -> "C"
        }
    } else {
        when (((wallStep % 5) + 5) % 5) {
            0 -> "E"
            1 -> "A"
            2 -> "B"
            3 -> "C"
            else -> "D"
        }
    }
}

private fun containerWallLetter(container: ClosetContainerSpec, room: RoomSpec, split: Boolean = false): String {
    return when (container.wall) {
        WallIndex.WEST -> "A"
        WallIndex.NORTH -> "B"
        WallIndex.EAST -> "C"
        WallIndex.SOUTH -> {
            if (!split) "E-D"
            else {
                if (container.offsetMm < room.segmentEMm) "D" else "E"
            }
        }
    }
}

private data class WallAnchor(
    val wall: WallIndex,
    val letter: String,
    val targetStep: Int,
    val lengthMm: Float,
    val topCenter3D: Float3,
    val normal: Float3,
)

private fun getWallAnchors(room: RoomSpec, split: Boolean = false): List<WallAnchor> {
    val nw = room.vertexNW
    val ne = room.vertexNE
    val se = room.vertexSE
    val sw = room.vertexSW
    val topY = (room.clearHeightMm + 40f) * MM_TO_M

    val wallA = WallAnchor(
        wall = WallIndex.WEST,
        letter = "A",
        targetStep = 1,
        lengthMm = getWallLengthMm(WallIndex.WEST, room),
        topCenter3D = Float3((sw.x + nw.x) * 0.5f * MM_TO_M, topY, (sw.z + nw.z) * 0.5f * MM_TO_M),
        normal = Float3(1f, 0f, 0f),
    )
    val wallB = WallAnchor(
        wall = WallIndex.NORTH,
        letter = "B",
        targetStep = 2,
        lengthMm = getWallLengthMm(WallIndex.NORTH, room),
        topCenter3D = Float3((nw.x + ne.x) * 0.5f * MM_TO_M, topY, (nw.z + ne.z) * 0.5f * MM_TO_M),
        normal = Float3(0f, 0f, 1f),
    )
    val wallC = WallAnchor(
        wall = WallIndex.EAST,
        letter = "C",
        targetStep = 3,
        lengthMm = getWallLengthMm(WallIndex.EAST, room),
        topCenter3D = Float3((ne.x + se.x) * 0.5f * MM_TO_M, topY, (ne.z + se.z) * 0.5f * MM_TO_M),
        normal = Float3(-1f, 0f, 0f),
    )

    if (!split) {
        val wallSouth = WallAnchor(
            wall = WallIndex.SOUTH,
            letter = "E-D",
            targetStep = 0,
            lengthMm = getWallLengthMm(WallIndex.SOUTH, room),
            topCenter3D = Float3((se.x + sw.x) * 0.5f * MM_TO_M, topY, (se.z + sw.z) * 0.5f * MM_TO_M),
            normal = Float3(0f, 0f, -1f),
        )
        return listOf(wallA, wallB, wallC, wallSouth)
    } else {
        val southDx = se.x - sw.x
        val southDz = se.z - sw.z
        val southLen = kotlin.math.sqrt(southDx * southDx + southDz * southDz).coerceAtLeast(100f)
        val dirX = southDx / southLen
        val dirZ = southDz / southLen

        val offsetE = room.segmentDMm * 0.5f
        val wallE = WallAnchor(
            wall = WallIndex.SOUTH,
            letter = "E",
            targetStep = 0,
            lengthMm = room.segmentDMm,
            topCenter3D = Float3((sw.x + dirX * offsetE) * MM_TO_M, topY, (sw.z + dirZ * offsetE) * MM_TO_M),
            normal = Float3(0f, 0f, -1f),
        )

        val startD = (room.segmentDMm + room.door.widthMm).coerceAtMost(southLen)
        val offsetD = startD + room.segmentEMm * 0.5f
        val wallD = WallAnchor(
            wall = WallIndex.SOUTH,
            letter = "D",
            targetStep = 4,
            lengthMm = room.segmentEMm,
            topCenter3D = Float3((sw.x + dirX * offsetD) * MM_TO_M, topY, (sw.z + dirZ * offsetD) * MM_TO_M),
            normal = Float3(0f, 0f, -1f),
        )

        return listOf(wallA, wallB, wallC, wallD, wallE)
    }
}

private fun project3DToScreen(
    point: Float3,
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    yawDeg: Float,
    pitchDeg: Float,
    distanceM: Float,
    isOrtho: Boolean,
    orthoSize: Float,
    viewportWidth: Float,
    viewportHeight: Float,
): Offset? {
    if (viewportWidth <= 10f || viewportHeight <= 10f) return null

    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

    val forwardX = sin(yawRad) * cos(pitchRad)
    val forwardY = sin(pitchRad)
    val forwardZ = -cos(yawRad) * cos(pitchRad)

    val eyeDist = if (isOrtho) maxOf(distanceM, 4.0f) else distanceM
    val eyeX = targetX - forwardX * eyeDist
    val eyeY = if (isOrtho && kotlin.math.abs(pitchDeg) < 5f) targetY else (targetY - forwardY * eyeDist).coerceAtLeast(0.10f)
    val eyeZ = targetZ - forwardZ * eyeDist

    val vx = point.x - eyeX
    val vy = point.y - eyeY
    val vz = point.z - eyeZ

    val rightLen = sqrt(forwardZ * forwardZ + forwardX * forwardX).coerceAtLeast(0.001f)
    val rightX = -forwardZ / rightLen
    val rightY = 0f
    val rightZ = forwardX / rightLen

    val upX = -rightZ * forwardY
    val upY = rightZ * forwardX - rightX * forwardZ
    val upZ = rightX * forwardY

    val camX = vx * rightX + vy * rightY + vz * rightZ
    val camY = vx * upX + vy * upY + vz * upZ
    val camZ = vx * forwardX + vy * forwardY + vz * forwardZ

    if (camZ <= 0.05f) return null

    val aspect = (viewportWidth / viewportHeight).toDouble()
    val ndcX: Float
    val ndcY: Float

    if (isOrtho) {
        val halfH = orthoSize * 0.5f
        val halfW = (halfH * aspect).toFloat()
        ndcX = camX / halfW
        ndcY = camY / halfH
    } else {
        val (fov, isVertical) = if (aspect >= 1.0) {
            48.0 to true
        } else {
            58.0 to false
        }
        val (tanHalfW, tanHalfH) = if (isVertical) {
            val tanV = kotlin.math.tan(Math.toRadians(fov * 0.5)).toFloat()
            (tanV * aspect.toFloat()) to tanV
        } else {
            val tanH = kotlin.math.tan(Math.toRadians(fov * 0.5)).toFloat()
            tanH to (tanH / aspect.toFloat())
        }
        ndcX = camX / (camZ * tanHalfW)
        ndcY = camY / (camZ * tanHalfH)
    }

    if (ndcX < -1.35f || ndcX > 1.35f || ndcY < -1.35f || ndcY > 1.35f) return null

    val screenX = (ndcX * 0.5f + 0.5f) * viewportWidth
    val screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * viewportHeight
    return Offset(screenX, screenY)
}

enum class RoomViewMode {
    PERSPECTIVE_3D, // Standard 3D perspective with free orbit
    TOP_DOWN,       // Vista de la habitación de arriba para abajo
    FRONT_2D,       // Vista de la pared actual en 2D (de frente)
}

/**
 * Multitouch gesture recognizer:
 * - Single tap: 3D ray selection (`onTap(pos, viewportWidth, viewportHeight)`)
 * - 1 Finger drag: Orbit (horizontal yaw and vertical pitch)
 * - 2 Fingers: Pan (translate focal target and camera) & Pinch (zoom distance)
 */
private suspend fun PointerInputScope.detectRoomInspectionGestures(
    onOrbit: (deltaX: Float, deltaY: Float) -> Unit,
    onPan: (panX: Float, panY: Float) -> Unit,
    onZoom: (zoomFactor: Float) -> Unit,
    onTap: (tapPos: Offset, viewportWidthPx: Float, viewportHeightPx: Float) -> Unit = { _, _, _ -> },
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        var lastSinglePos = downPos
        var totalMoveDistance = 0f
        var prevCentroid = Offset.Zero
        var prevDistance = 0f
        var wasMultiTouch = false

        do {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size == 1) {
                val pointer = activePointers[0]
                lastSinglePos = pointer.position
                if (wasMultiTouch) {
                    wasMultiTouch = false
                    prevDistance = 0f
                } else {
                    val delta = pointer.position - pointer.previousPosition
                    if (delta != Offset.Zero) {
                        totalMoveDistance += delta.getDistance()
                        if (totalMoveDistance > 8f) {
                            onOrbit(delta.x, delta.y)
                            pointer.consume()
                        }
                    }
                }
            } else if (activePointers.size >= 2) {
                wasMultiTouch = true
                totalMoveDistance += 100f
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
                    if ((zoomFactor in 0.5f..1.8f) && zoomFactor != 1f) {
                        onZoom(zoomFactor)
                    }
                }

                prevCentroid = centroid
                prevDistance = distance
                activePointers.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })

        if (!wasMultiTouch && totalMoveDistance <= 14f) {
            onTap(
                lastSinglePos,
                size.width.toFloat().coerceAtLeast(1f),
                size.height.toFloat().coerceAtLeast(1f),
            )
        }
    }
}

/**
 * Projects a 2D viewport tap coordinate into a 3D world-space ray (in millimeters)
 * matching [RoomScene]'s camera projection and tests intersection against placed containers.
 */
private fun computeTapRayMm(
    tapPos: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    yawDeg: Float,
    pitchDeg: Float,
    distanceM: Float,
    isOrtho: Boolean = false,
    orthoSize: Float = 3.0f,
): Pair<FloatArray, FloatArray> {
    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

    // Forward unit vector (from eye toward target)
    val forwardX = sin(yawRad) * cos(pitchRad)
    val forwardY = sin(pitchRad)
    val forwardZ = -cos(yawRad) * cos(pitchRad)

    // Eye position in meters
    val eyeDist = if (isOrtho) maxOf(distanceM, 4.0f) else distanceM
    val eyeXM = targetX - forwardX * eyeDist
    val eyeYM = if (isOrtho && kotlin.math.abs(pitchDeg) < 5f) targetY else (targetY - forwardY * eyeDist).coerceAtLeast(0.10f)
    val eyeZM = targetZ - forwardZ * eyeDist

    val fx = forwardX
    val fy = forwardY
    val fz = forwardZ

    // Right = normalize(cross(forward, worldUp(0,1,0)))
    val rLen = sqrt(fz * fz + fx * fx).coerceAtLeast(1e-5f)
    val rx = -fz / rLen
    val ry = 0f
    val rz = fx / rLen

    // Up = cross(right, forward)
    val ux = ry * fz - rz * fy
    val uy = rz * fx - rx * fz
    val uz = rx * fy - ry * fx

    val aspect = viewportWidthPx / viewportHeightPx.coerceAtLeast(1f)
    val normalizedX = (2f * tapPos.x / viewportWidthPx) - 1f
    val normalizedY = 1f - (2f * tapPos.y / viewportHeightPx)

    val originX: Float
    val originY: Float
    val originZ: Float
    val dirX: Float
    val dirY: Float
    val dirZ: Float

    if (isOrtho) {
        val halfH = orthoSize * 0.5f
        val halfW = halfH * aspect
        originX = eyeXM + normalizedX * halfW * rx + normalizedY * halfH * ux
        originY = eyeYM + normalizedX * halfW * ry + normalizedY * halfH * uy
        originZ = eyeZM + normalizedX * halfW * rz + normalizedY * halfH * uz
        dirX = fx
        dirY = fy
        dirZ = fz
    } else {
        val (halfTanX, halfTanY) = if (aspect >= 1f) {
            val tanV = kotlin.math.tan(Math.toRadians(24.0)).toFloat()
            (tanV * aspect) to tanV
        } else {
            val tanH = kotlin.math.tan(Math.toRadians(29.0)).toFloat()
            tanH to (tanH / aspect.coerceAtLeast(0.1f))
        }
        val dx = fx + normalizedX * halfTanX * rx + normalizedY * halfTanY * ux
        val dy = fy + normalizedX * halfTanX * ry + normalizedY * halfTanY * uy
        val dz = fz + normalizedX * halfTanX * rz + normalizedY * halfTanY * uz
        val dLen = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-5f)
        originX = eyeXM
        originY = eyeYM
        originZ = eyeZM
        dirX = dx / dLen
        dirY = dy / dLen
        dirZ = dz / dLen
    }

    return floatArrayOf(originX * 1000f, originY * 1000f, originZ * 1000f) to floatArrayOf(dirX, dirY, dirZ)
}

private fun pickContainerAtScreenTap(
    room: RoomSpec,
    tapPos: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    yawDeg: Float,
    pitchDeg: Float,
    distanceM: Float,
    isOrtho: Boolean = false,
    orthoSize: Float = 3.0f,
): String? {
    val (orig, dir) = computeTapRayMm(
        tapPos = tapPos,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        targetX = targetX,
        targetY = targetY,
        targetZ = targetZ,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        distanceM = distanceM,
        isOrtho = isOrtho,
        orthoSize = orthoSize,
    )
    return room.pickContainerByRayMm(
        rayOriginX = orig[0],
        rayOriginY = orig[1],
        rayOriginZ = orig[2],
        rayDirX = dir[0],
        rayDirY = dir[1],
        rayDirZ = dir[2],
    )
}

private fun pickPartitionAtScreenTap(
    room: RoomSpec,
    tapPos: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    yawDeg: Float,
    pitchDeg: Float,
    distanceM: Float,
    isOrtho: Boolean = false,
    orthoSize: Float = 3.0f,
): PartitionHit? {
    val (orig, dir) = computeTapRayMm(
        tapPos = tapPos,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        targetX = targetX,
        targetY = targetY,
        targetZ = targetZ,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        distanceM = distanceM,
        isOrtho = isOrtho,
        orthoSize = orthoSize,
    )
    return room.pickPartitionByRayMm(
        rayOriginX = orig[0],
        rayOriginY = orig[1],
        rayOriginZ = orig[2],
        rayDirX = dir[0],
        rayDirY = dir[1],
        rayDirZ = dir[2],
    )
}

@Composable
fun RoomScreen() {
    val context = LocalContext.current
    val projectStorage = remember(context) { ProjectStorage(context) }
    var appLanguage by remember {
        mutableStateOf(AppLanguage.fromCode(projectStorage.getLanguageCode()))
    }
    var savedProjects by remember { mutableStateOf(projectStorage.loadAllProjects()) }
    var currentProjectId by remember {
        mutableStateOf(projectStorage.getActiveProjectId() ?: savedProjects.firstOrNull()?.id)
    }
    var currentProjectName by remember {
        val active = savedProjects.firstOrNull { it.id == currentProjectId } ?: savedProjects.firstOrNull()
        mutableStateOf(active?.name ?: "${stringsFor(appLanguage).defaultProjectPrefix} 1")
    }

    var room by remember {
        val active = savedProjects.firstOrNull { it.id == currentProjectId } ?: savedProjects.firstOrNull()
        mutableStateOf(
            active?.room ?: RoomSpec(
                clearWidthMm = 3600f,
                clearHeightMm = 2600f,
                clearDepthMm = 3600f,
                door = DoorSpec(
                    wall = WallIndex.SOUTH,
                    offsetMm = 1350f,
                    widthMm = 900f,
                    heightMm = 2100f,
                ),
            ),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val messageHostState = remember { SnackbarHostState() }

    var showDimensionsSheet by remember { mutableStateOf(false) }
    var selectedPlanElement by remember { mutableStateOf<FloorplanElement>(FloorplanElement.WallA) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCreateNewDialog by remember { mutableStateOf(false) }
    var showMyProjectsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUnitSystemDialog by remember { mutableStateOf(false) }
    var appUnitSystem by remember {
        mutableStateOf(AppUnitSystem.fromCode(projectStorage.getUnitSystemCode()))
    }
    var showUnitsColumn by remember { mutableStateOf(false) }
    var isEditingContainerInPanel by remember { mutableStateOf(false) }
    var selectedContainerId by remember { mutableStateOf<String?>(null) }
    var selectedPartitionId by remember { mutableStateOf<String?>(null) }
    var selectedPartitionIsVertical by remember { mutableStateOf(true) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedCategoryKey by remember { mutableStateOf("CONTENT") }

    // View Mode (3D Perspective, Top-Down, Front 2D)
    var currentViewMode by remember { mutableStateOf(RoomViewMode.PERSPECTIVE_3D) }
    var showWallLabels by remember { mutableStateOf(true) }
    var splitDoorWall by remember { mutableStateOf(false) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }

    // Wall index counter (0: South/Door, 1: West, 2: North, 3: East)
    val wallStep = remember { mutableIntStateOf(0) }

    val initialTarget = remember(room) { getWallTarget(0, room, false) }
    val targetXAnim = remember { Animatable(initialTarget.x) }
    val targetYAnim = remember { Animatable(initialTarget.y) }
    val targetZAnim = remember { Animatable(initialTarget.z) }

    val yawAnim = remember { Animatable(180f) }
    val pitchAnim = remember { Animatable(0f) }
    val distanceAnim = remember { Animatable(3.40f) }
    val orthoSizeAnim = remember { Animatable(3.0f) }

    fun switchToTopDownView() {
        currentViewMode = RoomViewMode.TOP_DOWN
        showDimensionsSheet = false
        val centerX = room.clearWidthMm * 0.5f * MM_TO_M
        val centerZ = room.clearDepthMm * 0.5f * MM_TO_M
        val maxDim = maxOf(room.clearWidthMm, room.clearDepthMm) * MM_TO_M
        val estAspect = if (viewportWidthPx > 10f && viewportHeightPx > 10f) {
            viewportWidthPx / viewportHeightPx
        } else {
            16f / 9f
        }
        val targetTopDownOrtho = maxOf(
            room.clearDepthMm * MM_TO_M * 1.15f,
            (room.clearWidthMm * MM_TO_M * 1.15f) / estAspect
        ).coerceIn(2.5f, 15.0f)
        val topDownDist = (maxDim * 1.55f).coerceIn(4.2f, 9.5f)
        coroutineScope.launch {
            launch { yawAnim.animateTo(180f, tween(400, easing = FastOutSlowInEasing)) }
            // Looking straight down: -88° avoids gimbal lock and hides ceiling via backface culling
            launch { pitchAnim.animateTo(-88f, tween(400, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(topDownDist, tween(400, easing = FastOutSlowInEasing)) }
            launch { orthoSizeAnim.animateTo(targetTopDownOrtho, tween(400, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(centerX, tween(400, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(0.10f, tween(400, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(centerZ, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    fun switchToFront2DView(step: Int = wallStep.intValue) {
        currentViewMode = RoomViewMode.FRONT_2D
        showDimensionsSheet = false
        val numWalls = if (splitDoorWall) 5 else 4
        val normalizedStep = ((step % numWalls) + numWalls) % numWalls
        wallStep.intValue = normalizedStep
        val targetPos = getWallTarget(normalizedStep, room, splitDoorWall)
        val desiredYaw = getWallYaw(normalizedStep, splitDoorWall)
        val targetYaw = shortestAngleTarget(yawAnim.value, desiredYaw)
        val wallLenM = getWallLengthMmForStep(normalizedStep, room, splitDoorWall) * MM_TO_M
        val wallHeightM = room.clearHeightMm * MM_TO_M
        val tanV = kotlin.math.tan(Math.toRadians(24.0)).toFloat()
        val distH = (wallHeightM * 0.5f) / tanV
        val estAspect = if (viewportWidthPx > 10f && viewportHeightPx > 10f) {
            viewportWidthPx / viewportHeightPx
        } else {
            16f / 9f
        }
        val distW = (wallLenM * 0.5f) / (estAspect * tanV)
        val frontDist = (maxOf(distH, distW) * 1.08f).coerceIn(2.4f, 9.0f)
        val targetOrtho = maxOf(wallHeightM * 1.08f, (wallLenM * 1.08f) / estAspect).coerceIn(1.8f, 12.0f)

        coroutineScope.launch {
            launch { yawAnim.animateTo(targetYaw, tween(350, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(frontDist, tween(300, easing = FastOutSlowInEasing)) }
            launch { orthoSizeAnim.animateTo(targetOrtho, tween(350, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(targetPos.x, tween(350, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(targetPos.y, tween(350, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(targetPos.z, tween(350, easing = FastOutSlowInEasing)) }
        }
    }

    fun switchTo3DView() {
        currentViewMode = RoomViewMode.PERSPECTIVE_3D
        showDimensionsSheet = false
        val numWalls = if (splitDoorWall) 5 else 4
        val normalizedStep = ((wallStep.intValue % numWalls) + numWalls) % numWalls
        val targetPos = getWallTarget(normalizedStep, room, splitDoorWall)
        val desiredYaw = getWallYaw(normalizedStep, splitDoorWall) + 25f
        val targetYaw = shortestAngleTarget(yawAnim.value, desiredYaw)
        coroutineScope.launch {
            launch { yawAnim.animateTo(targetYaw, tween(350, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(-16f, tween(300, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(3.40f, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(targetPos.x, tween(350, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(targetPos.y, tween(350, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(targetPos.z, tween(350, easing = FastOutSlowInEasing)) }
        }
    }

    fun switchWall(newStep: Int) {
        val numWalls = if (splitDoorWall) 5 else 4
        val normalizedStep = ((newStep % numWalls) + numWalls) % numWalls
        wallStep.intValue = normalizedStep
        if (currentViewMode == RoomViewMode.FRONT_2D) {
            switchToFront2DView(normalizedStep)
        } else if (currentViewMode == RoomViewMode.TOP_DOWN) {
            // Stay in top-down view centered on the room
        } else {
            val targetPos = getWallTarget(normalizedStep, room, splitDoorWall)
            val desiredYaw = getWallYaw(normalizedStep, splitDoorWall)
            val targetYaw = shortestAngleTarget(yawAnim.value, desiredYaw)
            coroutineScope.launch {
                launch { yawAnim.animateTo(targetYaw, tween(400, easing = FastOutSlowInEasing)) }
                launch { pitchAnim.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                launch { distanceAnim.animateTo(3.40f, tween(300, easing = FastOutSlowInEasing)) }
                launch { targetXAnim.animateTo(targetPos.x, tween(400, easing = FastOutSlowInEasing)) }
                launch { targetYAnim.animateTo(targetPos.y, tween(400, easing = FastOutSlowInEasing)) }
                launch { targetZAnim.animateTo(targetPos.z, tween(400, easing = FastOutSlowInEasing)) }
            }
        }
    }

    fun recenterCurrentWall() {
        val numWalls = if (splitDoorWall) 5 else 4
        val normalizedStep = ((wallStep.intValue % numWalls) + numWalls) % numWalls
        if (currentViewMode == RoomViewMode.FRONT_2D) {
            switchToFront2DView(normalizedStep)
        } else if (currentViewMode == RoomViewMode.TOP_DOWN) {
            switchToTopDownView()
        } else {
            val targetPos = getWallTarget(normalizedStep, room, splitDoorWall)
            val desiredYaw = getWallYaw(normalizedStep, splitDoorWall)
            val targetYaw = shortestAngleTarget(yawAnim.value, desiredYaw)
            coroutineScope.launch {
                launch { yawAnim.animateTo(targetYaw, tween(350, easing = FastOutSlowInEasing)) }
                launch { pitchAnim.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
                launch { distanceAnim.animateTo(3.40f, tween(300, easing = FastOutSlowInEasing)) }
                launch { targetXAnim.animateTo(targetPos.x, tween(350, easing = FastOutSlowInEasing)) }
                launch { targetYAnim.animateTo(targetPos.y, tween(350, easing = FastOutSlowInEasing)) }
                launch { targetZAnim.animateTo(targetPos.z, tween(350, easing = FastOutSlowInEasing)) }
            }
        }
    }

    // Top-down / Floor plan Camera Transition for Dimensioning
    fun enterDimensionsMode() {
        showUnitsColumn = false
        showDimensionsSheet = true
        selectedCategoryKey = "ROOM"
        currentViewMode = RoomViewMode.TOP_DOWN
        val centerX = room.clearWidthMm * 0.5f * MM_TO_M
        val centerZ = room.clearDepthMm * 0.5f * MM_TO_M
        val maxDim = maxOf(room.clearWidthMm, room.clearDepthMm) * MM_TO_M
        val estAspect = if (viewportWidthPx > 10f && viewportHeightPx > 10f) {
            viewportWidthPx / viewportHeightPx
        } else {
            16f / 9f
        }
        val targetTopDownOrtho = maxOf(
            room.clearDepthMm * MM_TO_M * 1.15f,
            (room.clearWidthMm * MM_TO_M * 1.15f) / estAspect
        ).coerceIn(2.5f, 15.0f)
        val topDownDist = (maxDim * 1.55f).coerceIn(4.2f, 9.0f)
        coroutineScope.launch {
            launch { yawAnim.animateTo(180f, tween(450, easing = FastOutSlowInEasing)) }
            // Zenith pitch at -88°: ceiling disappears automatically via back-face culling
            launch { pitchAnim.animateTo(-88f, tween(450, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(topDownDist, tween(450, easing = FastOutSlowInEasing)) }
            launch { orthoSizeAnim.animateTo(targetTopDownOrtho, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(centerX, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(0.10f, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(centerZ, tween(450, easing = FastOutSlowInEasing)) }
        }
    }

    fun exitDimensionsMode() {
        showDimensionsSheet = false
        selectedCategoryKey = "CONTENT"
        currentViewMode = RoomViewMode.PERSPECTIVE_3D
        recenterCurrentWall()
    }

    fun updateRoom(newRoom: RoomSpec) {
        room = newRoom
        if (showDimensionsSheet) {
            val centerX = newRoom.clearWidthMm * 0.5f * MM_TO_M
            val centerZ = newRoom.clearDepthMm * 0.5f * MM_TO_M
            val maxDim = maxOf(newRoom.clearWidthMm, newRoom.clearDepthMm) * MM_TO_M
            val estAspect = if (viewportWidthPx > 10f && viewportHeightPx > 10f) {
                viewportWidthPx / viewportHeightPx
            } else {
                16f / 9f
            }
            val targetTopDownOrtho = maxOf(
                newRoom.clearDepthMm * MM_TO_M * 1.15f,
                (newRoom.clearWidthMm * MM_TO_M * 1.15f) / estAspect
            ).coerceIn(2.5f, 15.0f)
            val topDownDist = (maxDim * 1.55f).coerceIn(4.2f, 9.0f)
            coroutineScope.launch {
                targetXAnim.snapTo(centerX)
                targetYAnim.snapTo(0.10f)
                targetZAnim.snapTo(centerZ)
                distanceAnim.snapTo(topDownDist)
                orthoSizeAnim.snapTo(targetTopDownOrtho)
            }
        } else {
            val targetPos = getWallTarget(wallStep.intValue, newRoom)
            coroutineScope.launch {
                targetXAnim.snapTo(targetPos.x)
                targetYAnim.snapTo(targetPos.y)
                targetZAnim.snapTo(targetPos.z)
            }
        }
    }

    val s = stringsFor(appLanguage)
    val selectedCategoryLabel = when (selectedCategoryKey) {
        "ROOM" -> s.modeRoom
        "MODULES" -> s.modeModules
        else -> s.modeContent
    }

    fun selectLanguage(lang: AppLanguage) {
        appLanguage = lang
        projectStorage.setLanguageCode(lang.code)
        val newStrings = stringsFor(lang)
        coroutineScope.launch {
            messageHostState.showSnackbar(newStrings.languageChangedMessage)
        }
    }

    fun selectUnitSystem(unit: AppUnitSystem) {
        appUnitSystem = unit
        projectStorage.setUnitSystemCode(unit.code)
        coroutineScope.launch {
            messageHostState.showSnackbar("${s.unitsChangedToast} ${unit.displayName(appLanguage)}")
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalAppUnitSystem provides appUnitSystem,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Closet Studio 3D",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C55B5),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                    Text(
                        text = "${s.activeProjectPrefix}: $currentProjectName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()

                    // --- My Projects Section (matching reference screenshot) ---
                    Text(
                        text = s.myProjectsSection,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 6.dp),
                    )

                    NavigationDrawerItem(
                        label = { Text(s.saveProject) },
                        icon = { Icon(Icons.Default.Save, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showSaveDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = { Text(s.createNew) },
                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showCreateNewDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.myProjects)
                                if (savedProjects.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF2C55B5).copy(alpha = 0.14f),
                                    ) {
                                        Text(
                                            text = savedProjects.size.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2C55B5),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            savedProjects = projectStorage.loadAllProjects()
                            showMyProjectsDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = { Text(s.roomDimensions) },
                        icon = { Icon(Icons.Default.Straighten, null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            enterDimensionsMode()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = { Text(s.center3dView) },
                        icon = { Icon(Icons.Default.CenterFocusStrong, null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            recenterCurrentWall()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = { Text(s.roomInfo) },
                        icon = { Icon(Icons.Default.Info, null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showInfoDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = { Text(s.resetValues) },
                        icon = { Icon(Icons.Default.RestartAlt, null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            updateRoom(RoomSpec())
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.languageMenuLabel)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C55B5),
                                ) {
                                    Text(
                                        text = "${appLanguage.code} • ${appLanguage.nativeName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showLanguageDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.unitsMenuLabel)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C55B5),
                                ) {
                                    Text(
                                        text = appUnitSystem.symbol.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Straighten, contentDescription = null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showUnitSystemDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDEAE4)),
            ) {
                val isLandscape = maxWidth > maxHeight
                val screenWidth = maxWidth

                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. CAD TopAppBar (Royal Blue header matching reference)
                    Surface(
                        color = Color(0xFF2C55B5),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Hamburger Menu (≡)
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = s.myProjectsSection,
                                    tint = Color.White,
                                )
                            }

                            // Content / Mode Dropdown Selector
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showCategoryMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = selectedCategoryLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                }

                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(s.menuRoomPlanAndDimensions) },
                                        leadingIcon = { Icon(Icons.Default.Straighten, null) },
                                        onClick = {
                                            showCategoryMenu = false
                                            enterDimensionsMode()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.menuClosetModules) },
                                        leadingIcon = { Icon(Icons.Default.DoorFront, null) },
                                        onClick = {
                                            selectedCategoryKey = "MODULES"
                                            showDimensionsSheet = false
                                            showUnitsColumn = true
                                            showCategoryMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.viewTopDown) },
                                        leadingIcon = { Icon(Icons.Default.Layers, null) },
                                        onClick = {
                                            showCategoryMenu = false
                                            switchToTopDownView()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.viewFront2D) },
                                        leadingIcon = { Icon(Icons.Default.CropPortrait, null) },
                                        onClick = {
                                            showCategoryMenu = false
                                            switchToFront2DView()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.viewPerspective3D) },
                                        leadingIcon = { Icon(Icons.Default.ViewInAr, null) },
                                        onClick = {
                                            showCategoryMenu = false
                                            switchTo3DView()
                                        },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Undo (↶)
                            IconButton(
                                onClick = { recenterCurrentWall() },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }

                            // 3D View / Recenter Cube
                            IconButton(
                                onClick = {
                                    if (showDimensionsSheet) {
                                        exitDimensionsMode()
                                    } else if (currentViewMode != RoomViewMode.PERSPECTIVE_3D) {
                                        switchTo3DView()
                                    } else {
                                        recenterCurrentWall()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = s.center3dView,
                                    tint = if (currentViewMode == RoomViewMode.PERSPECTIVE_3D) Color.White else Color.White.copy(alpha = 0.65f),
                                )
                            }

                            // Toggle Wall Labels (A, B, C, D)
                            IconButton(
                                onClick = {
                                    showWallLabels = !showWallLabels
                                    coroutineScope.launch {
                                        messageHostState.showSnackbar(
                                            if (showWallLabels) s.wallLabelsShownToast else s.wallLabelsHiddenToast
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = if (showWallLabels) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Filled.LabelOff,
                                    contentDescription = s.toggleWallLabels,
                                    tint = if (showWallLabels) Color.White else Color.White.copy(alpha = 0.55f),
                                )
                            }

                            // Info (ⓘ)
                            IconButton(
                                onClick = { showInfoDialog = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = s.roomInfo,
                                    tint = Color.White,
                                )
                            }

                            // More Options (⋮)
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(s.overflowFloorplanAndDimensions) },
                                        leadingIcon = { Icon(Icons.Default.Straighten, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            enterDimensionsMode()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.overflowCenterWall) },
                                        leadingIcon = { Icon(Icons.Default.CenterFocusStrong, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            recenterCurrentWall()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.overflowReset36) },
                                        leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            updateRoom(RoomSpec())
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${s.languageMenuLabel}: ${appLanguage.nativeName}") },
                                        leadingIcon = { Icon(Icons.Default.Language, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showLanguageDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${s.unitsMenuLabel}: ${appUnitSystem.symbol.uppercase()}") },
                                        leadingIcon = { Icon(Icons.Default.Straighten, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showUnitSystemDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(s.toggleWallLabels)
                                                if (showWallLabels) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (showWallLabels) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Filled.LabelOff,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showWallLabels = !showWallLabels
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text(if (splitDoorWall) s.unifyDoorWallButton else s.splitDoorWallButton)
                                                if (splitDoorWall) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (splitDoorWall) Icons.AutoMirrored.Filled.MergeType else Icons.AutoMirrored.Filled.CallSplit,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            val newSplit = !splitDoorWall
                                            splitDoorWall = newSplit
                                            wallStep.intValue = if (newSplit) {
                                                when (((wallStep.intValue % 4) + 4) % 4) {
                                                    0 -> 0
                                                    1 -> 1
                                                    2 -> 2
                                                    else -> 3
                                                }
                                            } else {
                                                when (((wallStep.intValue % 5) + 5) % 5) {
                                                    0 -> 0
                                                    1 -> 1
                                                    2 -> 2
                                                    3 -> 3
                                                    else -> 0
                                                }
                                            }
                                            switchWall(wallStep.intValue)
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(
                                                    if (newSplit) s.doorWallSplitToast else s.doorWallUnifiedToast
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // 2. Interactive Body Area
                    if (!showDimensionsSheet) {
                        val activeContainer = room.containers.firstOrNull { it.id == selectedContainerId }
                        var isDraggingUnit by remember { mutableStateOf(false) }
                        var draggedUnitType by remember { mutableStateOf(DraggedUnitType.CARCASS) }
                        var dragScreenOffset by remember { mutableStateOf(Offset.Zero) }
                        var viewportOriginInRoot by remember { mutableStateOf(Offset.Zero) }
                        var panelOriginInRootX by remember { mutableFloatStateOf(Float.MAX_VALUE) }

                        LaunchedEffect(room.containers, selectedContainerId) {
                            if (selectedContainerId != null && room.containers.none { it.id == selectedContainerId }) {
                                selectedContainerId = null
                                selectedPartitionId = null
                                isEditingContainerInPanel = false
                            }
                        }

                        LaunchedEffect(room.containers, selectedPartitionId) {
                            if (selectedPartitionId != null && activeContainer != null) {
                                val exists = activeContainer.dividers.any { it.id == selectedPartitionId } ||
                                        activeContainer.shelves.any { it.id == selectedPartitionId }
                                if (!exists) {
                                    selectedPartitionId = null
                                }
                            }
                        }

                        // Normal 3D Mode: Fullscreen RoomScene + navigation arrows + recenter
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates ->
                                    viewportOriginInRoot = coordinates.positionInRoot()
                                    viewportWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                                    viewportHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                                },
                        ) {
                            val isOrthoMode = currentViewMode == RoomViewMode.FRONT_2D || currentViewMode == RoomViewMode.TOP_DOWN
                            RoomViewport(
                                room = room,
                                targetX = targetXAnim.value,
                                targetY = targetYAnim.value,
                                targetZ = targetZAnim.value,
                                yaw = yawAnim.value,
                                pitch = pitchAnim.value,
                                distance = distanceAnim.value,
                                isOrtho = isOrthoMode,
                                orthoSize = orthoSizeAnim.value,
                                selectedContainerId = activeContainer?.id,
                                selectedPartitionId = selectedPartitionId,
                                onOrbit = { deltaX, deltaY ->
                                    currentViewMode = RoomViewMode.PERSPECTIVE_3D
                                    coroutineScope.launch {
                                        yawAnim.snapTo(yawAnim.value - deltaX * 0.22f)
                                        val newPitch = (pitchAnim.value - deltaY * 0.18f).coerceIn(-88f, 25f)
                                        pitchAnim.snapTo(newPitch)
                                    }
                                },
                                onPan = { panX, panY ->
                                    val yawRad = Math.toRadians(yawAnim.value.toDouble()).toFloat()
                                    val pitchRad = Math.toRadians(pitchAnim.value.toDouble()).toFloat()
                                    val dirX = sin(yawRad) * cos(pitchRad)
                                    val dirY = sin(pitchRad)
                                    val dirZ = -cos(yawRad) * cos(pitchRad)
                                    val rightLen = sqrt(dirZ * dirZ + dirX * dirX).coerceAtLeast(0.001f)
                                    val rightX = -dirZ / rightLen
                                    val rightZ = dirX / rightLen
                                    val upX = -rightZ * dirY
                                    val upY = rightZ * dirX - rightX * dirZ
                                    val upZ = rightX * dirY
                                    val panScale = if (isOrthoMode) orthoSizeAnim.value * 0.0012f else distanceAnim.value * 0.0016f
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
                                        if (isOrthoMode) {
                                            val newOrtho = (orthoSizeAnim.value / zoomFactor).coerceIn(0.5f, 20.0f)
                                            orthoSizeAnim.snapTo(newOrtho)
                                        } else {
                                            val newDist = (distanceAnim.value / zoomFactor).coerceIn(0.8f, 10.0f)
                                            distanceAnim.snapTo(newDist)
                                        }
                                    }
                                },
                                onTap = { tapPos, vw, vh ->
                                    val partHit = pickPartitionAtScreenTap(
                                        room = room,
                                        tapPos = tapPos,
                                        viewportWidthPx = vw,
                                        viewportHeightPx = vh,
                                        targetX = targetXAnim.value,
                                        targetY = targetYAnim.value,
                                        targetZ = targetZAnim.value,
                                        yawDeg = yawAnim.value,
                                        pitchDeg = pitchAnim.value,
                                        distanceM = distanceAnim.value,
                                        isOrtho = isOrthoMode,
                                        orthoSize = orthoSizeAnim.value,
                                    )
                                    if (partHit != null) {
                                        selectedContainerId = partHit.containerId
                                        selectedPartitionId = partHit.partitionId
                                        selectedPartitionIsVertical = partHit.isVertical
                                        isEditingContainerInPanel = false
                                        showUnitsColumn = true
                                    } else {
                                        val hitId = pickContainerAtScreenTap(
                                            room = room,
                                            tapPos = tapPos,
                                            viewportWidthPx = vw,
                                            viewportHeightPx = vh,
                                            targetX = targetXAnim.value,
                                            targetY = targetYAnim.value,
                                            targetZ = targetZAnim.value,
                                            yawDeg = yawAnim.value,
                                            pitchDeg = pitchAnim.value,
                                            distanceM = distanceAnim.value,
                                            isOrtho = isOrthoMode,
                                            orthoSize = orthoSizeAnim.value,
                                        )
                                        selectedContainerId = hitId
                                        selectedPartitionId = null
                                        if (hitId == null && (isEditingContainerInPanel || showUnitsColumn)) {
                                            isEditingContainerInPanel = false
                                            showUnitsColumn = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )

                            // Floating Wall Labels (A, B, C, D)
                            if (showWallLabels && !showDimensionsSheet) {
                                if (currentViewMode == RoomViewMode.FRONT_2D) {
                                    val currentWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                    val wallLenMm = getWallLengthMmForStep(wallStep.intValue, room, splitDoorWall).roundToInt()
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 16.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Label,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = wallLetterCodeFromStep(wallStep.intValue, splitDoorWall),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(14.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant),
                                            )
                                            Text(
                                                text = appUnitSystem.format(wallLenMm.toFloat()),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else {
                                    val anchors = remember(room, splitDoorWall) { getWallAnchors(room, splitDoorWall) }
                                    val currentFocusedWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                    val isTopDown = currentViewMode == RoomViewMode.TOP_DOWN

                                    val yawRad = Math.toRadians(yawAnim.value.toDouble()).toFloat()
                                    val pitchRad = Math.toRadians(pitchAnim.value.toDouble()).toFloat()
                                    val dirX = sin(yawRad) * cos(pitchRad)
                                    val dirY = sin(pitchRad)
                                    val dirZ = -cos(yawRad) * cos(pitchRad)
                                    val eyeDist = if (isOrthoMode) maxOf(distanceAnim.value, 4.0f) else distanceAnim.value
                                    val eyeX = targetXAnim.value - dirX * eyeDist
                                    val eyeY = if (isOrthoMode && kotlin.math.abs(pitchAnim.value) < 5f) targetYAnim.value else (targetYAnim.value - dirY * eyeDist).coerceAtLeast(0.10f)
                                    val eyeZ = targetZAnim.value - dirZ * eyeDist

                                    anchors.forEach { anchor ->
                                        val toWallX = anchor.topCenter3D.x - eyeX
                                        val toWallY = anchor.topCenter3D.y - eyeY
                                        val toWallZ = anchor.topCenter3D.z - eyeZ
                                        val dotNormal = toWallX * anchor.normal.x + toWallY * anchor.normal.y + toWallZ * anchor.normal.z

                                        val isVisibleFacing = isTopDown || dotNormal < 0.25f

                                        if (isVisibleFacing) {
                                            val screenPos = project3DToScreen(
                                                point = anchor.topCenter3D,
                                                targetX = targetXAnim.value,
                                                targetY = targetYAnim.value,
                                                targetZ = targetZAnim.value,
                                                yawDeg = yawAnim.value,
                                                pitchDeg = pitchAnim.value,
                                                distanceM = distanceAnim.value,
                                                isOrtho = isOrthoMode,
                                                orthoSize = orthoSizeAnim.value,
                                                viewportWidth = viewportWidthPx,
                                                viewportHeight = viewportHeightPx,
                                            )

                                            if (screenPos != null) {
                                                val isWallActive = if (splitDoorWall && anchor.wall == WallIndex.SOUTH) {
                                                    anchor.targetStep == ((wallStep.intValue % 5) + 5) % 5
                                                } else {
                                                    anchor.wall == currentFocusedWall
                                                }
                                                val isContainerOnThisWall = if (activeContainer != null) {
                                                    if (splitDoorWall && activeContainer.wall == WallIndex.SOUTH) {
                                                        val contLetter = containerWallLetter(activeContainer, room, true)
                                                        contLetter == anchor.letter
                                                    } else {
                                                        activeContainer.wall == anchor.wall
                                                    }
                                                } else {
                                                    false
                                                }

                                                Box(
                                                    modifier = Modifier.layout { measurable, constraints ->
                                                        val placeable = measurable.measure(constraints)
                                                        layout(placeable.width, placeable.height) {
                                                            val minX = 16
                                                            val maxX = (viewportWidthPx.toInt() - placeable.width - 16).coerceAtLeast(minX)
                                                            val minY = 16
                                                            val maxY = (viewportHeightPx.toInt() - placeable.height - 80).coerceAtLeast(minY)
                                                            val x = (screenPos.x - placeable.width / 2f).roundToInt().coerceIn(minX, maxX)
                                                            val y = (screenPos.y - placeable.height / 2f).roundToInt().coerceIn(minY, maxY)
                                                            placeable.place(x, y)
                                                        }
                                                    }
                                                ) {
                                                    Surface(
                                                        onClick = {
                                                            switchWall(anchor.targetStep)
                                                        },
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = when {
                                                            isContainerOnThisWall -> Color(0xFF1E6B47)
                                                            isWallActive -> Color(0xFF2C55B5)
                                                            else -> Color(0xFF1E293B).copy(alpha = 0.88f)
                                                        },
                                                        border = BorderStroke(
                                                            1.5.dp,
                                                            if (isContainerOnThisWall || isWallActive) Color.White else Color.White.copy(alpha = 0.65f),
                                                        ),
                                                        shadowElevation = 5.dp,
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.Label,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(13.dp),
                                                            )
                                                            Text(
                                                                text = "${anchor.letter} (${appUnitSystem.formatCompact(anchor.lengthMm)})",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Left Navigation Button (<)
                            FilledIconButton(
                                onClick = { switchWall(wallStep.intValue - 1) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(y = (-36).dp)
                                    .padding(start = 16.dp)
                                    .size(48.dp)
                                    .shadow(4.dp, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = s.previousWall,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            // Floating Action Buttons for Selected Container (Edit ✏️, Duplicate ❐, Delete 🗑️)
                            if (activeContainer != null) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .navigationBarsPadding()
                                        .padding(start = 16.dp, bottom = if (isLandscape) 18.dp else 76.dp),
                                ) {
                                    // Wall Badge for active container
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF2C55B5),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                                        shadowElevation = 4.dp,
                                    ) {
                                        Text(
                                            text = containerWallLetter(activeContainer, room, splitDoorWall),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }

                                    // 1. Edit Button (✏️ opens right-side size inspector)
                                    Surface(
                                        onClick = {
                                            selectedContainerId = activeContainer.id
                                            isEditingContainerInPanel = true
                                            showUnitsColumn = true
                                        },
                                        shape = CircleShape,
                                        color = if (showUnitsColumn && isEditingContainerInPanel) {
                                            Color(0xFF2C55B5)
                                        } else {
                                            Color(0xFFE5DFDA).copy(alpha = 0.95f)
                                        },
                                        border = BorderStroke(2.dp, Color(0xFF4355B9)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(if (isLandscape) 46.dp else 52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = s.editContainerDesc,
                                                tint = if (showUnitsColumn && isEditingContainerInPanel) {
                                                    Color.White
                                                } else {
                                                    Color(0xFF3949AB)
                                                },
                                                modifier = Modifier.size(if (isLandscape) 22.dp else 24.dp),
                                            )
                                        }
                                    }

                                    // 2. Duplicate Button (❐)
                                    Surface(
                                        onClick = {
                                            val (updatedRoom, newId) = room.withDuplicatedContainer(activeContainer.id)
                                            updateRoom(updatedRoom)
                                            if (newId != null) {
                                                selectedContainerId = newId
                                                coroutineScope.launch {
                                                    messageHostState.showSnackbar(s.containerDuplicatedToast)
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFFE5DFDA).copy(alpha = 0.95f),
                                        border = BorderStroke(2.dp, Color(0xFF4355B9)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(if (isLandscape) 46.dp else 52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = s.duplicateContainerDesc,
                                                tint = Color(0xFF3949AB),
                                                modifier = Modifier.size(if (isLandscape) 21.dp else 23.dp),
                                            )
                                        }
                                    }

                                    // 3. Delete Button (🗑️)
                                    Surface(
                                        onClick = {
                                            val updatedRoom = room.withRemovedContainer(activeContainer.id)
                                            updateRoom(updatedRoom)
                                            selectedContainerId = updatedRoom.containers.lastOrNull()?.id
                                            if (updatedRoom.containers.isEmpty()) {
                                                isEditingContainerInPanel = false
                                            }
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.containerDeletedToast)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFFE5DFDA).copy(alpha = 0.95f),
                                        border = BorderStroke(2.dp, Color(0xFF4355B9)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(if (isLandscape) 46.dp else 52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = s.deleteContainerDesc,
                                                tint = Color(0xFF651FFF),
                                                modifier = Modifier.size(if (isLandscape) 23.dp else 25.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Navigation Button (>)
                            FilledIconButton(
                                onClick = { switchWall(wallStep.intValue + 1) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(y = (-36).dp)
                                    .padding(end = 16.dp)
                                    .size(48.dp)
                                    .shadow(4.dp, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = s.nextWall,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            // Bottom View Modes Toolbar (Arriba / Top-Down, Frente 2D, 3D, Centrar)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                ) {
                                    // 1. Botón "Arriba" (Vista de la habitación de arriba para abajo)
                                    ViewModePillButton(
                                        label = s.viewTopDown,
                                        icon = Icons.Default.Layers,
                                        selected = currentViewMode == RoomViewMode.TOP_DOWN,
                                        onClick = { switchToTopDownView() },
                                    )

                                    // 2. Botón "Frente 2D" (Pared actual en 2D, de frente)
                                    ViewModePillButton(
                                        label = s.viewFront2D,
                                        icon = Icons.Default.CropPortrait,
                                        selected = currentViewMode == RoomViewMode.FRONT_2D,
                                        onClick = { switchToFront2DView() },
                                    )

                                    // 3. Botón "3D" (Perspectiva 3D con órbita libre)
                                    ViewModePillButton(
                                        label = s.viewPerspective3D,
                                        icon = Icons.Default.ViewInAr,
                                        selected = currentViewMode == RoomViewMode.PERSPECTIVE_3D,
                                        onClick = { switchTo3DView() },
                                    )

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    )

                                    // Botón "Centrar"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { recenterCurrentWall() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = s.overflowCenterWall,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = s.centerButton,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }

                            // Bottom-Right Floating Actions: Corner Fusion/Split action + "+" Add Units FAB
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !showUnitsColumn,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(150)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .navigationBarsPadding()
                                    .padding(end = 16.dp, bottom = if (isLandscape) 18.dp else 76.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    // Floating Corner Fusion/Split Buttons stacked directly above the "+" FAB
                                    val cornerCandidates = remember(room) { room.detectCornerIntersections() }
                                    if (cornerCandidates.isNotEmpty()) {
                                        cornerCandidates.forEach { candidate ->
                                            val isFused = candidate.isFused
                                            Surface(
                                                onClick = {
                                                    val updated = room.withCornerFusion(
                                                        corner = candidate.corner,
                                                        fuse = !isFused,
                                                    )
                                                    updateRoom(updated)
                                                    coroutineScope.launch {
                                                        messageHostState.showSnackbar(
                                                            if (!isFused) {
                                                                "${s.cornerFusedToast} (${candidate.corner.codeLabel})"
                                                            } else {
                                                                "${s.cornerUnfusedToast} (${candidate.corner.codeLabel})"
                                                            },
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isFused) {
                                                    Color(0xFF1E6B47)
                                                } else {
                                                    Color(0xFF2C55B5)
                                                },
                                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                                                shadowElevation = 6.dp,
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                ) {
                                                    CornerActionIcon(
                                                        isFused = isFused,
                                                        color = Color.White,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        text = candidate.corner.codeLabel,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Floating Door Wall Split/Unify Toggle Button
                                    Surface(
                                        onClick = {
                                            val newSplit = !splitDoorWall
                                            splitDoorWall = newSplit
                                            wallStep.intValue = if (newSplit) {
                                                when (((wallStep.intValue % 4) + 4) % 4) {
                                                    0 -> 0
                                                    1 -> 1
                                                    2 -> 2
                                                    else -> 3
                                                }
                                            } else {
                                                when (((wallStep.intValue % 5) + 5) % 5) {
                                                    0 -> 0
                                                    1 -> 1
                                                    2 -> 2
                                                    3 -> 3
                                                    else -> 0
                                                }
                                            }
                                            switchWall(wallStep.intValue)
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(
                                                    if (newSplit) s.doorWallSplitToast else s.doorWallUnifiedToast
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (splitDoorWall) Color(0xFF1E6B47) else Color(0xFF2C55B5),
                                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                                        shadowElevation = 6.dp,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (splitDoorWall) Icons.AutoMirrored.Filled.MergeType else Icons.AutoMirrored.Filled.CallSplit,
                                                contentDescription = if (splitDoorWall) s.unifyDoorWallButton else s.splitDoorWallButton,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Text(
                                                text = if (splitDoorWall) "E-D" else "E / D",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                        }
                                    }

                                    FloatingActionButton(
                                        onClick = {
                                            isEditingContainerInPanel = false
                                            showUnitsColumn = true
                                        },
                                        shape = CircleShape,
                                        containerColor = Color(0xFF2C55B5),
                                        contentColor = Color.White,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = s.addUnitsFabDesc,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }

                            // Right-Side Units / Components Column (partial width in both Tablet and Phone modes)
                            val unitsColumnWidth = if (isLandscape) {
                                (screenWidth * 0.36f).coerceIn(290.dp, 380.dp)
                            } else {
                                (screenWidth * 0.76f).coerceIn(255.dp, 330.dp)
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showUnitsColumn,
                                enter = slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                                ) + fadeIn(tween(220)),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                                ) + fadeOut(tween(180)),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                            ) {
                                RightSideUnitsAndInspectorColumn(
                                    room = room,
                                    s = s,
                                    activeContainer = activeContainer,
                                    isEditingContainer = isEditingContainerInPanel && activeContainer != null,
                                    selectedPartitionId = selectedPartitionId,
                                    selectedPartitionIsVertical = selectedPartitionIsVertical,
                                    isDragging = isDraggingUnit,
                                    onToggleEditMode = { isEditingContainerInPanel = it },
                                    onSelectContainer = { container ->
                                        selectedContainerId = container.id
                                        selectedPartitionId = null
                                        val isD = container.wall == WallIndex.SOUTH && container.offsetMm < room.segmentEMm
                                        val targetStep = stepFromWallIndex(container.wall, splitDoorWall, isSegmentD = isD)
                                        if (targetStep != wallStep.intValue) {
                                            switchWall(targetStep)
                                        }
                                    },
                                    onAddContainerOnCurrentWall = { dropFractionX ->
                                        val targetWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                        val isD = splitDoorWall && targetWall == WallIndex.SOUTH && (((wallStep.intValue % 5) + 5) % 5 == 4)
                                        val newId = "container_${System.currentTimeMillis()}_${room.containers.size + 1}"
                                        val added = room.withAddedContainer(wall = targetWall, id = newId)
                                        val updatedRoom = if (splitDoorWall && targetWall == WallIndex.SOUTH) {
                                            val frac = dropFractionX ?: 0.5f
                                            if (isD) {
                                                added.withUpdatedContainer(newId) { item ->
                                                    val maxOff = (room.segmentEMm - item.widthMm).coerceAtLeast(0f)
                                                    item.copy(offsetMm = frac.coerceIn(0f, 1f) * maxOff)
                                                }
                                            } else {
                                                val baseOff = room.segmentEMm + room.door.widthMm
                                                added.withUpdatedContainer(newId) { item ->
                                                    val maxOff = (room.segmentDMm - item.widthMm).coerceAtLeast(0f)
                                                    item.copy(offsetMm = baseOff + frac.coerceIn(0f, 1f) * maxOff)
                                                }
                                            }
                                        } else if (dropFractionX != null) {
                                            val wallLen = added.wallLengthMm(targetWall).coerceAtLeast(0f)
                                            added.withUpdatedContainer(newId) { item ->
                                                val maxOff = (wallLen - item.widthMm).coerceAtLeast(0f)
                                                item.copy(offsetMm = (dropFractionX.coerceIn(0f, 1f) * maxOff))
                                            }
                                        } else {
                                            added
                                        }
                                        updateRoom(updatedRoom)
                                        selectedContainerId = newId
                                        selectedPartitionId = null
                                        showUnitsColumn = false
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(
                                                "${s.containerAddedToast} ${wallDisplayName(targetWall, appLanguage, splitDoorWall, isD)}",
                                            )
                                        }
                                    },
                                    onAddDivider = { dropFrac ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFrac != null) dropFrac * target.interiorWidthMm else null
                                            val updated = target.withAddedDivider(dropX)
                                            val newDiv = updated.dividers.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newDiv?.id
                                            selectedPartitionIsVertical = true
                                            showUnitsColumn = true
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        } else {
                                            val targetWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                            val newId = "container_${System.currentTimeMillis()}_1"
                                            val baseRoom = room.withAddedContainer(wall = targetWall, id = newId)
                                            val baseC = baseRoom.containers.first { it.id == newId }
                                            val updated = baseC.withAddedDivider()
                                            val newDiv = updated.dividers.lastOrNull()
                                            val updatedRoom = baseRoom.withUpdatedContainer(newId) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = newId
                                            selectedPartitionId = newDiv?.id
                                            selectedPartitionIsVertical = true
                                            showUnitsColumn = true
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddShelf = { dropFracX, dropFracY ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else target.interiorHeightMm * 0.5f
                                            val updated = target.withAddedShelf(bayIndex = bay.index, elevationYMm = dropY)
                                            val newShelf = updated.shelves.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newShelf?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = true
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        } else {
                                            val targetWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                            val newId = "container_${System.currentTimeMillis()}_1"
                                            val baseRoom = room.withAddedContainer(wall = targetWall, id = newId)
                                            val baseC = baseRoom.containers.first { it.id == newId }
                                            val updated = baseC.withAddedShelf()
                                            val newShelf = updated.shelves.lastOrNull()
                                            val updatedRoom = baseRoom.withUpdatedContainer(newId) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = newId
                                            selectedPartitionId = newShelf?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = true
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onDragUnitStart = { type, startRootPos ->
                                        draggedUnitType = type
                                        isDraggingUnit = true
                                        dragScreenOffset = startRootPos - viewportOriginInRoot
                                    },
                                    onDragUnitMove = { delta ->
                                        dragScreenOffset += delta
                                    },
                                    onDragUnitEnd = {
                                        if (isDraggingUnit) {
                                            val fractionX = (dragScreenOffset.x / viewportWidthPx).coerceIn(0.05f, 0.95f)
                                            val fractionY = (1f - (dragScreenOffset.y / viewportHeightPx)).coerceIn(0.1f, 0.9f)
                                            when (draggedUnitType) {
                                                DraggedUnitType.CARCASS -> {
                                                    val targetWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                                    val isD = splitDoorWall && targetWall == WallIndex.SOUTH && (((wallStep.intValue % 5) + 5) % 5 == 4)
                                                    val newId = "container_${System.currentTimeMillis()}_${room.containers.size + 1}"
                                                    val added = room.withAddedContainer(wall = targetWall, id = newId)
                                                    val updatedRoom = if (splitDoorWall && targetWall == WallIndex.SOUTH) {
                                                        if (isD) {
                                                            added.withUpdatedContainer(newId) { item ->
                                                                val maxOff = (room.segmentEMm - item.widthMm).coerceAtLeast(0f)
                                                                item.copy(offsetMm = fractionX * maxOff)
                                                            }
                                                        } else {
                                                            val baseOff = room.segmentEMm + room.door.widthMm
                                                            added.withUpdatedContainer(newId) { item ->
                                                                val maxOff = (room.segmentDMm - item.widthMm).coerceAtLeast(0f)
                                                                item.copy(offsetMm = baseOff + fractionX * maxOff)
                                                            }
                                                        }
                                                    } else {
                                                        val wallLen = added.wallLengthMm(targetWall).coerceAtLeast(0f)
                                                        added.withUpdatedContainer(newId) { item ->
                                                            val maxOff = (wallLen - item.widthMm).coerceAtLeast(0f)
                                                            item.copy(offsetMm = fractionX * maxOff)
                                                        }
                                                    }
                                                    updateRoom(updatedRoom)
                                                    selectedContainerId = newId
                                                    selectedPartitionId = null
                                                    showUnitsColumn = false
                                                    coroutineScope.launch {
                                                        messageHostState.showSnackbar(
                                                            "${s.containerAddedToast} ${wallDisplayName(targetWall, appLanguage, splitDoorWall, isD)}",
                                                        )
                                                    }
                                                }
                                                DraggedUnitType.VERTICAL_DIVIDER -> {
                                                    val target = activeContainer ?: room.containers.firstOrNull()
                                                    if (target != null) {
                                                        val dropOffsetMm = fractionX * target.interiorWidthMm
                                                        val updated = target.withAddedDivider(dropOffsetMm)
                                                        val newDiv = updated.dividers.lastOrNull()
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                                        updateRoom(updatedRoom)
                                                        selectedContainerId = target.id
                                                        selectedPartitionId = newDiv?.id
                                                        selectedPartitionIsVertical = true
                                                        showUnitsColumn = true
                                                        coroutineScope.launch {
                                                            messageHostState.showSnackbar(s.partitionAddedToast)
                                                        }
                                                    }
                                                }
                                                DraggedUnitType.HORIZONTAL_SHELF -> {
                                                    val target = activeContainer ?: room.containers.firstOrNull()
                                                    if (target != null) {
                                                        val dropXMm = fractionX * target.interiorWidthMm
                                                        val bay = target.findBayAtX(dropXMm)
                                                        val dropElevMm = fractionY * target.interiorHeightMm
                                                        val updated = target.withAddedShelf(bayIndex = bay.index, elevationYMm = dropElevMm)
                                                        val newShelf = updated.shelves.lastOrNull()
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                                        updateRoom(updatedRoom)
                                                        selectedContainerId = target.id
                                                        selectedPartitionId = newShelf?.id
                                                        selectedPartitionIsVertical = false
                                                        showUnitsColumn = true
                                                        coroutineScope.launch {
                                                            messageHostState.showSnackbar(s.partitionAddedToast)
                                                        }
                                                    }
                                                }
                                            }
                                            isDraggingUnit = false
                                        }
                                    },
                                    onDragUnitCancel = {
                                        isDraggingUnit = false
                                        showUnitsColumn = false
                                    },
                                    onUpdateContainer = { id, transform ->
                                        val updatedRoom = room.withUpdatedContainer(id, transform)
                                        updateRoom(updatedRoom)
                                    },
                                    onDuplicateContainer = { id ->
                                        val (updatedRoom, newId) = room.withDuplicatedContainer(id)
                                        updateRoom(updatedRoom)
                                        newId?.let { selectedContainerId = it }
                                    },
                                    onDeleteContainer = { id ->
                                        val updatedRoom = room.withRemovedContainer(id)
                                        updateRoom(updatedRoom)
                                        selectedContainerId = updatedRoom.containers.lastOrNull()?.id
                                        selectedPartitionId = null
                                        if (updatedRoom.containers.isEmpty()) {
                                            isEditingContainerInPanel = false
                                        }
                                    },
                                    onDuplicatePartition = { containerId, partitionId, isVertical ->
                                        updateRoom(room.withUpdatedContainer(containerId) { container ->
                                            if (isVertical) {
                                                val source = container.dividers.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val newOff = (source.offsetXMm + source.thicknessMm + 80f).coerceAtMost((container.interiorWidthMm - source.thicknessMm).coerceAtLeast(0f))
                                                val newDiv = source.copy(
                                                    id = "div_${System.currentTimeMillis()}_${container.dividers.size + 1}",
                                                    offsetXMm = newOff,
                                                )
                                                container.copy(dividers = container.dividers + newDiv)
                                            } else {
                                                val source = container.shelves.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val newElev = (source.elevationYMm + source.thicknessMm + 100f).coerceAtMost((container.interiorHeightMm - source.thicknessMm).coerceAtLeast(0f))
                                                val newShelf = source.copy(
                                                    id = "shelf_${System.currentTimeMillis()}_${container.shelves.size + 1}",
                                                    elevationYMm = newElev,
                                                )
                                                container.copy(shelves = container.shelves + newShelf)
                                            }
                                        })
                                    },
                                    onDeletePartition = { containerId, partitionId ->
                                        updateRoom(room.withUpdatedContainer(containerId) { it.withRemovedPartition(partitionId) })
                                        selectedPartitionId = null
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(s.partitionDeletedToast)
                                        }
                                    },
                                    onClosePartition = {
                                        selectedPartitionId = null
                                    },
                                    onClose = {
                                        selectedPartitionId = null
                                        showUnitsColumn = false
                                    },
                                    modifier = Modifier
                                        .width(unitsColumnWidth)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            alpha = if (isDraggingUnit) 0f else 1f
                                        }
                                        .onGloballyPositioned { coordinates ->
                                            panelOriginInRootX = coordinates.positionInRoot().x
                                        },
                                )
                            }

                            // Floating 3D Unit Preview while dragging from "Add Unit" onto the wall
                            if (isDraggingUnit) {
                                val previewSizeDp = 140.dp
                                val previewSizePx = with(LocalDensity.current) { previewSizeDp.toPx() }
                                val halfPreviewPx = previewSizePx * 0.5f
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (dragScreenOffset.x - halfPreviewPx).roundToInt(),
                                                y = (dragScreenOffset.y - halfPreviewPx).roundToInt(),
                                            )
                                        }
                                        .size(previewSizeDp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (draggedUnitType) {
                                        DraggedUnitType.CARCASS -> ClosetContainer3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.VERTICAL_DIVIDER -> VerticalDivider3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.HORIZONTAL_SHELF -> HorizontalShelf3DGraphic(modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    } else {
                        // Dimensioning Mode: Responsive Split-Screen with Interactive 2D CAD Floor Plan!
                        if (isLandscape) {
                            // Tablet / Landscape: Side by Side (Left: 2D CAD floor plan, Right: Contextual Inspector)
                            val panelWidth = (screenWidth * 0.42f).coerceIn(370.dp, 450.dp)
                            Row(modifier = Modifier.fillMaxSize()) {
                                RoomFloorplan2D(
                                    room = room,
                                    selectedElement = selectedPlanElement,
                                    onSelectElement = { selectedPlanElement = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )

                                RoomDimensionsPanel(
                                    room = room,
                                    selectedElement = selectedPlanElement,
                                    onSelectElement = { selectedPlanElement = it },
                                    onRoomChange = { updateRoom(it) },
                                    onDismiss = { exitDimensionsMode() },
                                    modifier = Modifier
                                        .width(panelWidth)
                                        .fillMaxHeight()
                                        .navigationBarsPadding(),
                                )
                            }
                        } else {
                            // Mobile / Portrait: Top / Bottom Split (Top: 2D CAD floor plan, Bottom: Contextual Inspector)
                            Column(modifier = Modifier.fillMaxSize()) {
                                RoomFloorplan2D(
                                    room = room,
                                    selectedElement = selectedPlanElement,
                                    onSelectElement = { selectedPlanElement = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.10f),
                                )

                                RoomDimensionsPanel(
                                    room = room,
                                    selectedElement = selectedPlanElement,
                                    onSelectElement = { selectedPlanElement = it },
                                    onRoomChange = { updateRoom(it) },
                                    onDismiss = { exitDimensionsMode() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.90f)
                                        .navigationBarsPadding(),
                                )
                            }
                        }
                    }
                }

                // Info Dialog (ⓘ)
                if (showInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showInfoDialog = false },
                        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text(s.roomInfoTitle) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• ${s.projectLabel}: $currentProjectName")
                                Text("• ${s.wallALabel}: ${appUnitSystem.format(room.wallCMm)}")
                                Text("• ${s.wallBLabel}: ${appUnitSystem.format(room.wallAMm)}")
                                Text("• ${s.wallCLabel}: ${appUnitSystem.format(room.wallBMm)}")
                                Text("• ${s.wallEDTotalLabel}: ${appUnitSystem.format(room.wallSouthMm)} (E:${appUnitSystem.formatCompact(room.segmentDMm)} + ${s.doorWord}:${appUnitSystem.formatCompact(room.door.widthMm)} + D:${appUnitSystem.formatCompact(room.segmentEMm)})")
                                Text("• ${s.anglesLabel}: A-B=${String.format(Locale.US, "%.1f°", room.angleACDeg)}, B-C=${String.format(Locale.US, "%.1f°", room.angleABDeg)}, C-D=${String.format(Locale.US, "%.1f°", room.angleBEDeg)}, A-E=${String.format(Locale.US, "%.1f°", room.angleCDDeg)}")
                                Text("• ${s.heightYLabel}: ${appUnitSystem.format(room.clearHeightMm)}")
                                Text("• ${s.currentViewLabel}: ${getWallName(wallStep.intValue, appLanguage, splitDoorWall)}")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showInfoDialog = false }) {
                                Text(s.understoodButton)
                            }
                        },
                    )
                }

                // Save Project Dialog (Save Project)
                if (showSaveDialog) {
                    var nameDraft by remember(currentProjectName) { mutableStateOf(currentProjectName) }
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text(s.saveProject) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = s.saveProjectDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = nameDraft,
                                    onValueChange = { nameDraft = it },
                                    singleLine = true,
                                    label = { Text(s.projectNameLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "A: ${appUnitSystem.formatCompact(room.wallCMm)} • B: ${appUnitSystem.formatCompact(room.wallAMm)} • C: ${appUnitSystem.formatCompact(room.wallBMm)} • E-D: ${appUnitSystem.formatCompact(room.wallSouthMm)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2C55B5),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            if (currentProjectId != null) {
                                TextButton(
                                    onClick = {
                                        val saved = projectStorage.saveProject(
                                            id = null,
                                            name = nameDraft.trim().ifEmpty { "${s.copyOfPrefix} $currentProjectName" },
                                            room = room,
                                        )
                                        savedProjects = projectStorage.loadAllProjects()
                                        currentProjectId = saved.id
                                        currentProjectName = saved.name
                                        showSaveDialog = false
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar("${s.savedAsNewToast}: ${saved.name}")
                                        }
                                    },
                                ) {
                                    Text(s.saveAsCopyButton)
                                }
                            } else {
                                TextButton(onClick = { showSaveDialog = false }) {
                                    Text(s.cancelButton)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val saved = projectStorage.saveProject(
                                        id = currentProjectId,
                                        name = nameDraft,
                                        room = room,
                                    )
                                    savedProjects = projectStorage.loadAllProjects()
                                    currentProjectId = saved.id
                                    currentProjectName = saved.name
                                    showSaveDialog = false
                                    coroutineScope.launch {
                                        messageHostState.showSnackbar("${s.projectSavedToast}: ${saved.name}")
                                    }
                                },
                            ) {
                                Text(s.saveButton, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }

                // Create New Project Dialog (Create New)
                if (showCreateNewDialog) {
                    val nextIndex = savedProjects.size + 1
                    var newNameDraft by remember(nextIndex, appLanguage) {
                        mutableStateOf("${s.defaultProjectPrefix} $nextIndex")
                    }
                    AlertDialog(
                        onDismissRequest = { showCreateNewDialog = false },
                        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text(s.createNewProjectTitle) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = s.createNewProjectDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = newNameDraft,
                                    onValueChange = { newNameDraft = it },
                                    singleLine = true,
                                    label = { Text(s.newProjectNameLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateNewDialog = false }) {
                                Text(s.cancelButton)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val freshRoom = RoomSpec()
                                    val created = projectStorage.saveProject(
                                        id = null,
                                        name = newNameDraft.trim().ifEmpty { "${s.defaultProjectPrefix} $nextIndex" },
                                        room = freshRoom,
                                    )
                                    savedProjects = projectStorage.loadAllProjects()
                                    currentProjectId = created.id
                                    currentProjectName = created.name
                                    updateRoom(freshRoom)
                                    showCreateNewDialog = false
                                    coroutineScope.launch {
                                        messageHostState.showSnackbar("${s.projectCreatedToast}: ${created.name}")
                                    }
                                },
                            ) {
                                Text(s.createButton, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }

                // My Projects Dialog (My Projects)
                if (showMyProjectsDialog) {
                    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                    AlertDialog(
                        onDismissRequest = { showMyProjectsDialog = false },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text("${s.myProjects} (${savedProjects.size})") },
                        text = {
                            if (savedProjects.isEmpty()) {
                                Text(
                                    text = s.emptyProjectsMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 360.dp),
                                ) {
                                    items(savedProjects, key = { it.id }) { project ->
                                        val isCurrent = project.id == currentProjectId
                                        val r = project.room
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCurrent) {
                                                    Color(0xFF2C55B5).copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                                },
                                            ),
                                            onClick = {
                                                currentProjectId = project.id
                                                currentProjectName = project.name
                                                projectStorage.setActiveProjectId(project.id)
                                                updateRoom(project.room)
                                                showMyProjectsDialog = false
                                                coroutineScope.launch {
                                                    messageHostState.showSnackbar("${s.projectOpenedToast}: ${project.name}")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        Text(
                                                            text = project.name,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCurrent) Color(0xFF2C55B5) else MaterialTheme.colorScheme.onSurface,
                                                        )
                                                        if (isCurrent) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFF2C55B5),
                                                            ) {
                                                                Text(
                                                                    text = s.activeBadge,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = "A:${r.wallCMm.roundToInt()} • B:${r.wallAMm.roundToInt()} • C:${r.wallBMm.roundToInt()} • E-D:${r.wallSouthMm.roundToInt()} mm",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Text(
                                                        text = "${s.doorWord}:${r.door.widthMm.roundToInt()} (E:${r.segmentDMm.roundToInt()}, D:${r.segmentEMm.roundToInt()}) • ${dateFormatter.format(Date(project.updatedAtEpochMs))}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        savedProjects = projectStorage.deleteProject(project.id)
                                                        if (currentProjectId == project.id) {
                                                            val fallback = savedProjects.firstOrNull()
                                                            currentProjectId = fallback?.id
                                                            currentProjectName = fallback?.name ?: "${s.defaultProjectPrefix} 1"
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = s.deleteProject,
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showMyProjectsDialog = false }) {
                                Text(s.closeButton)
                            }
                        },
                    )
                }

                // Scalable Language Selector Dialog (Language)
                if (showLanguageDialog) {
                    AlertDialog(
                        onDismissRequest = { showLanguageDialog = false },
                        icon = { Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text(s.selectLanguageTitle) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = s.selectLanguageSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                AppLanguage.entries.forEach { langOption ->
                                    val isSelected = langOption == appLanguage
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                Color(0xFF2C55B5).copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                        ),
                                        onClick = {
                                            selectLanguage(langOption)
                                            showLanguageDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0xFF2C55B5) else MaterialTheme.colorScheme.surface,
                                                ) {
                                                    Text(
                                                        text = langOption.code,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = langOption.nativeName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color(0xFF2C55B5) else MaterialTheme.colorScheme.onSurface,
                                                    )
                                                    Text(
                                                        text = langOption.subtitleName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF2C55B5),
                                                ) {
                                                    Text(
                                                        text = s.activeBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLanguageDialog = false }) {
                                Text(s.closeButton)
                            }
                        },
                    )
                }

                // Measurement Units Selector Dialog
                if (showUnitSystemDialog) {
                    AlertDialog(
                        onDismissRequest = { showUnitSystemDialog = false },
                        icon = { Icon(Icons.Default.Straighten, contentDescription = null, tint = Color(0xFF2C55B5)) },
                        title = { Text(s.selectUnitsTitle) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = s.selectUnitsSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                AppUnitSystem.entries.forEach { unitOption ->
                                    val isSelected = unitOption == appUnitSystem
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                Color(0xFF2C55B5).copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                        ),
                                        onClick = {
                                            selectUnitSystem(unitOption)
                                            showUnitSystemDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0xFF2C55B5) else MaterialTheme.colorScheme.surface,
                                                ) {
                                                    Text(
                                                        text = unitOption.symbol.uppercase(),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = unitOption.displayName(appLanguage),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color(0xFF2C55B5) else MaterialTheme.colorScheme.onSurface,
                                                    )
                                                    Text(
                                                        text = unitOption.subtitle(appLanguage),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF2C55B5),
                                                ) {
                                                    Text(
                                                        text = s.activeBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showUnitSystemDialog = false }) {
                                Text(s.closeButton)
                            }
                        },
                    )
                }

                SnackbarHost(
                    hostState = messageHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RoomViewport(
    room: RoomSpec,
    targetX: Float,
    targetY: Float,
    targetZ: Float,
    yaw: Float,
    pitch: Float,
    distance: Float,
    isOrtho: Boolean = false,
    orthoSize: Float = 3.0f,
    modifier: Modifier = Modifier,
    selectedContainerId: String? = null,
    selectedPartitionId: String? = null,
    onOrbit: (Float, Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onTap: (Offset, Float, Float) -> Unit = { _, _, _ -> },
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectRoomInspectionGestures(
                    onOrbit = onOrbit,
                    onPan = onPan,
                    onZoom = onZoom,
                    onTap = onTap,
                )
            },
    ) {
        RoomScene(
            room = room,
            target = Float3(targetX, targetY, targetZ),
            yawAngleDegrees = yaw,
            pitchAngleDegrees = pitch,
            distance = distance,
            isOrtho = isOrtho,
            orthoSize = orthoSize,
            selectedContainerId = selectedContainerId,
            selectedPartitionId = selectedPartitionId,
        )
    }
}

@Composable
private fun RightSideUnitsAndInspectorColumn(
    room: RoomSpec,
    s: UiStrings,
    activeContainer: ClosetContainerSpec?,
    isEditingContainer: Boolean,
    selectedPartitionId: String? = null,
    selectedPartitionIsVertical: Boolean = true,
    isDragging: Boolean = false,
    onToggleEditMode: (Boolean) -> Unit,
    onSelectContainer: (ClosetContainerSpec) -> Unit,
    onAddContainerOnCurrentWall: (dropFractionX: Float?) -> Unit,
    onAddDivider: (dropFractionX: Float?) -> Unit = {},
    onAddShelf: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
    onDragUnitStart: (DraggedUnitType, startRootPos: Offset) -> Unit,
    onDragUnitMove: (delta: Offset) -> Unit,
    onDragUnitEnd: () -> Unit,
    onDragUnitCancel: () -> Unit,
    onUpdateContainer: (String, (ClosetContainerSpec) -> ClosetContainerSpec) -> Unit,
    onDuplicateContainer: (String) -> Unit,
    onDeleteContainer: (String) -> Unit,
    onDuplicatePartition: (containerId: String, partitionId: String, isVertical: Boolean) -> Unit = { _, _, _ -> },
    onDeletePartition: (containerId: String, partitionId: String) -> Unit = { _, _ -> },
    onClosePartition: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = if (isDragging) 0f else 1f
        },
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = if (isDragging) 0.dp else 14.dp,
        shape = RoundedCornerShape(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            // Minimalist Header: "Module ✕" or "Add Unit ✕" or "Edit Container ✕"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        selectedPartitionId != null && activeContainer != null -> s.moduleTitle
                        isEditingContainer && activeContainer != null -> s.editContainerTitle
                        else -> s.unitsPanelTitle
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditingContainer && activeContainer != null && selectedPartitionId == null) {
                        TextButton(onClick = { onToggleEditMode(false) }) {
                            Text(
                                text = s.backToCatalogButton,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C55B5),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (selectedPartitionId != null) {
                                onClosePartition()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.closeButton,
                            tint = Color(0xFF1A1A1A),
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)

            if (selectedPartitionId != null && activeContainer != null) {
                // --- MODULE INSPECTOR PANEL (matching user screenshot media_1793474801389_65881bdb.png) ---
                ModuleInspectorContent(
                    activeContainer = activeContainer,
                    selectedPartitionId = selectedPartitionId,
                    selectedPartitionIsVertical = selectedPartitionIsVertical,
                    s = s,
                    onUpdateContainer = onUpdateContainer,
                    onDuplicatePartition = onDuplicatePartition,
                    onDeletePartition = onDeletePartition,
                )
            } else if (isEditingContainer && activeContainer != null) {
                // --- CONTAINER SIZE & POSITION EDITOR PANEL ---
                val clamped = room.clampContainer(activeContainer)
                val maxW = room.wallLengthMm(clamped.wall).coerceAtLeast(100f)
                val maxH = room.clearHeightMm.coerceAtLeast(100f)
                val maxD = room.maxContainerDepthForWall(clamped.wall).coerceAtLeast(100f)
                val maxOff = (maxW - clamped.widthMm).coerceAtLeast(0f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Wall Selector Chips (A, B, C, E-D)
                    Text(
                        text = s.containerWallLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val walls = listOf(
                            WallIndex.WEST to "A",
                            WallIndex.NORTH to "B",
                            WallIndex.EAST to "C",
                            WallIndex.SOUTH to "E-D",
                        )
                        walls.forEach { (wIdx, label) ->
                            val selected = clamped.wall == wIdx
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdateContainer(clamped.id) { current ->
                                        val newWallLen = room.wallLengthMm(wIdx).coerceAtLeast(0f)
                                        val newW = current.widthMm.coerceAtMost(newWallLen)
                                        current.copy(
                                            wall = wIdx,
                                            widthMm = newW,
                                            offsetMm = ((newWallLen - newW) * 0.5f).coerceAtLeast(0f),
                                        )
                                    }
                                    onSelectContainer(clamped.copy(wall = wIdx))
                                },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                            )
                        }
                    }

                    // 1. Width
                    ContainerDimensionControlCard(
                        label = s.containerWidthLabel,
                        valueMm = clamped.widthMm,
                        minMm = 200f.coerceAtMost(maxW),
                        maxMm = maxW,
                    ) { newW ->
                        onUpdateContainer(clamped.id) { it.copy(widthMm = newW) }
                    }

                    // 2. Height
                    ContainerDimensionControlCard(
                        label = s.containerHeightLabel,
                        valueMm = clamped.heightMm,
                        minMm = 300f.coerceAtMost(maxH),
                        maxMm = maxH,
                    ) { newH ->
                        onUpdateContainer(clamped.id) { it.copy(heightMm = newH) }
                    }

                    // 3. Depth (bounded by adjacent walls)
                    ContainerDimensionControlCard(
                        label = s.containerDepthLabel,
                        valueMm = clamped.depthMm,
                        minMm = 200f.coerceAtMost(maxD),
                        maxMm = maxD,
                    ) { newD ->
                        onUpdateContainer(clamped.id) { it.copy(depthMm = newD) }
                    }

                    // 4. Offset along Wall (Wall position + Center button)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = s.containerOffsetLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                OutlinedButton(
                                    onClick = {
                                        onUpdateContainer(clamped.id) {
                                            it.copy(offsetMm = ((maxW - it.widthMm) * 0.5f).coerceAtLeast(0f))
                                        }
                                    },
                                ) {
                                    Text(s.centerOnWallButton, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = "${clamped.offsetMm.roundToInt()} mm / max ${maxOff.roundToInt()} mm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C55B5),
                            )
                            Slider(
                                value = clamped.offsetMm.coerceIn(0f, maxOff.coerceAtLeast(1f)),
                                onValueChange = { newOff ->
                                    onUpdateContainer(clamped.id) { it.copy(offsetMm = newOff) }
                                },
                                valueRange = 0f..maxOff.coerceAtLeast(1f),
                                enabled = maxOff > 1f,
                            )
                        }
                    }

                    // Action row inside editor (Duplicate & Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onDuplicateContainer(clamped.id) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.duplicateButton, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { onDeleteContainer(clamped.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.deleteButton, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                // --- SIMPLE VISUAL CATALOG VIEW (Clean white column, pure 3D illustrations matching reference) ---
                var carcassRootOffset by remember { mutableStateOf(Offset.Zero) }
                var dividerRootOffset by remember { mutableStateOf(Offset.Zero) }
                var shelfRootOffset by remember { mutableStateOf(Offset.Zero) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 1st Unit: Closet Container (Carcass)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(145.dp)
                            .onGloballyPositioned { coordinates ->
                                carcassRootOffset = coordinates.positionInRoot()
                            }
                            .clickable {
                                onAddContainerOnCurrentWall(null)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.CARCASS, carcassRootOffset + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                )
                            },
                    ) {
                        ClosetContainer3DGraphic(
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .height(140.dp),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 2nd Unit: Vertical Divider (División Vertical matching screenshot media_1793474667205_def219d6.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(135.dp)
                            .onGloballyPositioned { coordinates ->
                                dividerRootOffset = coordinates.positionInRoot()
                            }
                            .clickable {
                                onAddDivider(null)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.VERTICAL_DIVIDER, dividerRootOffset + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                )
                            },
                    ) {
                        VerticalDivider3DGraphic(
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                                .height(130.dp),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 3rd Unit: Horizontal Shelf (División Horizontal matching screenshot media_1793474667205_def219d6.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(125.dp)
                            .onGloballyPositioned { coordinates ->
                                shelfRootOffset = coordinates.positionInRoot()
                            }
                            .clickable {
                                onAddShelf(null, null)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.HORIZONTAL_SHELF, shelfRootOffset + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                )
                            },
                    ) {
                        HorizontalShelf3DGraphic(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .height(115.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainerDimensionControlCard(
    label: String,
    valueMm: Float,
    minMm: Float,
    maxMm: Float,
    onValueChange: (Float) -> Unit,
) {
    val unitSystem = LocalAppUnitSystem.current
    val safeMax = maxMm.coerceAtLeast(minMm + 1f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${unitSystem.format(valueMm)} (max ${unitSystem.format(maxMm)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C55B5),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                unitSystem.stepValues().forEach { (stepLabel, delta) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Color(0xFF2C55B5).copy(alpha = 0.3f)),
                        onClick = {
                            onValueChange((valueMm + delta).coerceIn(minMm, maxMm))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            Text(
                                text = stepLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C55B5),
                            )
                        }
                    }
                }
            }

            Slider(
                value = valueMm.coerceIn(minMm, safeMax),
                onValueChange = { onValueChange(it.coerceIn(minMm, maxMm)) },
                valueRange = minMm..safeMax,
            )
        }
    }
}

@Composable
private fun ClosetContainer3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Perspective closet carcass matching the user's reference image:
        // Front outer rectangle + recessed inner back panel with 3D perspective depth + bottom plinth
        val outerLeft = w * 0.08f
        val outerRight = w * 0.92f
        val outerTop = h * 0.06f
        val outerBottom = h * 0.94f

        val frameT = (outerRight - outerLeft) * 0.045f
        val plinthH = (outerBottom - outerTop) * 0.075f
        val depthInsetX = (outerRight - outerLeft) * 0.095f
        val depthInsetTopY = (outerBottom - outerTop) * 0.065f
        val depthInsetBottomY = (outerBottom - outerTop) * 0.055f

        val innerFrontLeft = outerLeft + frameT
        val innerFrontRight = outerRight - frameT
        val innerFrontTop = outerTop + frameT
        val innerFrontBottom = outerBottom - plinthH - frameT * 0.7f

        val backLeft = innerFrontLeft + depthInsetX
        val backRight = innerFrontRight - depthInsetX
        val backTop = innerFrontTop + depthInsetTopY
        val backBottom = innerFrontBottom - depthInsetBottomY

        val faceEdgeWood = Color(0xFFD6B28A)
        val sideInnerWood = Color(0xFFBC9368)
        val topCeilingWood = Color(0xFFAA8157)
        val bottomShelfWood = Color(0xFFC59B70)
        val backCreamWood = Color(0xFFF3EAD8)
        val plinthWood = Color(0xFFCCA377)
        val lineStroke = Color(0xFF8C6742).copy(alpha = 0.45f)

        // 1. Cream Back Panel
        drawRect(
            color = backCreamWood,
            topLeft = Offset(backLeft, backTop),
            size = androidx.compose.ui.geometry.Size(backRight - backLeft, backBottom - backTop),
        )

        // 2. Left Interior Depth Wall (trapezoid from innerFrontLeft to backLeft)
        val leftWallPath = Path().apply {
            moveTo(innerFrontLeft, innerFrontTop)
            lineTo(backLeft, backTop)
            lineTo(backLeft, backBottom)
            lineTo(innerFrontLeft, innerFrontBottom)
            close()
        }
        drawPath(leftWallPath, color = sideInnerWood)

        // 3. Right Interior Depth Wall (trapezoid from innerFrontRight to backRight)
        val rightWallPath = Path().apply {
            moveTo(innerFrontRight, innerFrontTop)
            lineTo(backRight, backTop)
            lineTo(backRight, backBottom)
            lineTo(innerFrontRight, innerFrontBottom)
            close()
        }
        drawPath(rightWallPath, color = sideInnerWood)

        // 4. Top Interior Ceiling (trapezoid from innerFrontTop to backTop)
        val topCeilingPath = Path().apply {
            moveTo(innerFrontLeft, innerFrontTop)
            lineTo(innerFrontRight, innerFrontTop)
            lineTo(backRight, backTop)
            lineTo(backLeft, backTop)
            close()
        }
        drawPath(topCeilingPath, color = topCeilingWood)

        // 5. Bottom Interior Shelf Floor (trapezoid from innerFrontBottom to backBottom)
        val bottomShelfPath = Path().apply {
            moveTo(innerFrontLeft, innerFrontBottom)
            lineTo(innerFrontRight, innerFrontBottom)
            lineTo(backRight, backBottom)
            lineTo(backLeft, backBottom)
            close()
        }
        drawPath(bottomShelfPath, color = bottomShelfWood)

        // 6. Front Plinth / Toe Kick (bottom strip between left and right vertical panels)
        drawRect(
            color = plinthWood,
            topLeft = Offset(innerFrontLeft, innerFrontBottom),
            size = androidx.compose.ui.geometry.Size(
                innerFrontRight - innerFrontLeft,
                outerBottom - innerFrontBottom,
            ),
        )

        // 7. Front Thickness Edges (Left, Right, Top, Bottom shelf lip)
        drawRect(
            color = faceEdgeWood,
            topLeft = Offset(outerLeft, outerTop),
            size = androidx.compose.ui.geometry.Size(frameT, outerBottom - outerTop),
        )
        drawRect(
            color = faceEdgeWood,
            topLeft = Offset(innerFrontRight, outerTop),
            size = androidx.compose.ui.geometry.Size(frameT, outerBottom - outerTop),
        )
        drawRect(
            color = faceEdgeWood,
            topLeft = Offset(innerFrontLeft, outerTop),
            size = androidx.compose.ui.geometry.Size(innerFrontRight - innerFrontLeft, frameT),
        )
        drawRect(
            color = faceEdgeWood,
            topLeft = Offset(innerFrontLeft, innerFrontBottom),
            size = androidx.compose.ui.geometry.Size(innerFrontRight - innerFrontLeft, frameT * 0.7f),
        )

        // Subtle crisp architectural contour lines
        drawPath(leftWallPath, color = lineStroke, style = Stroke(width = 1.2f))
        drawPath(rightWallPath, color = lineStroke, style = Stroke(width = 1.2f))
        drawPath(topCeilingPath, color = lineStroke, style = Stroke(width = 1.2f))
        drawPath(bottomShelfPath, color = lineStroke, style = Stroke(width = 1.2f))
        drawRect(
            color = lineStroke,
            topLeft = Offset(outerLeft, outerTop),
            size = androidx.compose.ui.geometry.Size(outerRight - outerLeft, outerBottom - outerTop),
            style = Stroke(width = 1.4f),
        )
    }
}

@Composable
private fun VerticalDivider3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val faceEdgeWood = Color(0xFFE6C299)
        val sideWood = Color(0xFFD6B28A)
        val topWood = Color(0xFFAA8157)
        val lineStroke = Color(0xFF8C6742).copy(alpha = 0.5f)

        val thickness = (w * 0.12f).coerceAtLeast(10f)
        val boardH = h * 0.72f
        val depthX = w * 0.22f
        val depthY = h * 0.15f

        val x0 = (w - thickness - depthX) * 0.5f
        val y0 = (h - boardH + depthY) * 0.5f

        // Top face
        val topPath = Path().apply {
            moveTo(x0, y0)
            lineTo(x0 + thickness, y0)
            lineTo(x0 + thickness + depthX, y0 - depthY)
            lineTo(x0 + depthX, y0 - depthY)
            close()
        }
        drawPath(topPath, color = topWood)
        drawPath(topPath, color = lineStroke, style = Stroke(width = 1.2f))

        // Side depth face
        val sidePath = Path().apply {
            moveTo(x0 + thickness, y0)
            lineTo(x0 + thickness + depthX, y0 - depthY)
            lineTo(x0 + thickness + depthX, y0 - depthY + boardH)
            lineTo(x0 + thickness, y0 + boardH)
            close()
        }
        drawPath(sidePath, color = sideWood)
        drawPath(sidePath, color = lineStroke, style = Stroke(width = 1.2f))

        // Front edge face
        val frontPath = Path().apply {
            moveTo(x0, y0)
            lineTo(x0 + thickness, y0)
            lineTo(x0 + thickness, y0 + boardH)
            lineTo(x0, y0 + boardH)
            close()
        }
        drawPath(frontPath, color = faceEdgeWood)
        drawPath(frontPath, color = lineStroke, style = Stroke(width = 1.2f))
    }
}

@Composable
private fun HorizontalShelf3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val faceEdgeWood = Color(0xFFE6C299)
        val topWood = Color(0xFFD6B28A)
        val sideWood = Color(0xFFAA8157)
        val lineStroke = Color(0xFF8C6742).copy(alpha = 0.5f)

        val shelfW = w * 0.76f
        val thickness = (h * 0.11f).coerceAtLeast(8f)
        val depthX = w * 0.15f
        val depthY = h * 0.32f

        val x0 = (w - shelfW - depthX) * 0.5f
        val y0 = (h - thickness + depthY) * 0.5f

        // Top face
        val topPath = Path().apply {
            moveTo(x0, y0)
            lineTo(x0 + shelfW, y0)
            lineTo(x0 + shelfW + depthX, y0 - depthY)
            lineTo(x0 + depthX, y0 - depthY)
            close()
        }
        drawPath(topPath, color = topWood)
        drawPath(topPath, color = lineStroke, style = Stroke(width = 1.2f))

        // Right side edge
        val sidePath = Path().apply {
            moveTo(x0 + shelfW, y0)
            lineTo(x0 + shelfW + depthX, y0 - depthY)
            lineTo(x0 + shelfW + depthX, y0 - depthY + thickness)
            lineTo(x0 + shelfW, y0 + thickness)
            close()
        }
        drawPath(sidePath, color = sideWood)
        drawPath(sidePath, color = lineStroke, style = Stroke(width = 1.2f))

        // Front face edge
        drawRect(
            color = faceEdgeWood,
            topLeft = Offset(x0, y0),
            size = androidx.compose.ui.geometry.Size(shelfW, thickness),
        )
        drawRect(
            color = lineStroke,
            topLeft = Offset(x0, y0),
            size = androidx.compose.ui.geometry.Size(shelfW, thickness),
            style = Stroke(width = 1.2f),
        )
    }
}

@Composable
private fun ModuleDimensionRow(
    label: String,
    valueMm: Float,
    unitSystem: AppUnitSystem,
    stepMm: Float = 10f,
    minMm: Float = 0f,
    maxMm: Float = 10000f,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onValueChange((valueMm - stepMm).coerceIn(minMm, maxMm)) },
                enabled = enabled && valueMm > minMm,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (enabled && valueMm > minMm) Color.Black else Color.LightGray,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = unitSystem.format(valueMm),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
            )

            IconButton(
                onClick = { onValueChange((valueMm + stepMm).coerceIn(minMm, maxMm)) },
                enabled = enabled && valueMm < maxMm,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (enabled && valueMm < maxMm) Color.Black else Color.LightGray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(color = Color(0xFF757575).copy(alpha = 0.6f), thickness = 1.dp)
    }
}

@Composable
private fun ModuleInspectorContent(
    activeContainer: ClosetContainerSpec,
    selectedPartitionId: String,
    selectedPartitionIsVertical: Boolean,
    s: UiStrings,
    onUpdateContainer: (String, (ClosetContainerSpec) -> ClosetContainerSpec) -> Unit,
    onDuplicatePartition: (containerId: String, partitionId: String, isVertical: Boolean) -> Unit,
    onDeletePartition: (containerId: String, partitionId: String) -> Unit,
) {
    val unitSystem = LocalAppUnitSystem.current
    val step = if (unitSystem.code == "IN" || unitSystem.code == "FT_IN") 25.4f else 10f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Size:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
        )

        if (selectedPartitionIsVertical) {
            val div = activeContainer.dividers.firstOrNull { it.id == selectedPartitionId }
            if (div != null) {
                // Width (thickness)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = div.thicknessMm,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 12f,
                    maxMm = 50f,
                    enabled = true,
                ) { newThickness ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDivider(div.id) { it.copy(thicknessMm = newThickness) }
                    }
                }

                // Height (interior height of carcass)
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = activeContainer.interiorHeightMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Depth (carcass interior depth)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = activeContainer.depthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Position header with lock icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.partitionPositionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Position Lock",
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Relative position to neighbors:
                val sorted = activeContainer.dividers.sortedBy { it.offsetXMm }
                val idx = sorted.indexOfFirst { it.id == div.id }
                val leftBound = if (idx > 0) sorted[idx - 1].offsetXMm + sorted[idx - 1].thicknessMm else 0f
                val rightBound = if (idx in 0 until sorted.lastIndex) sorted[idx + 1].offsetXMm else activeContainer.interiorWidthMm

                val leftDist = (div.offsetXMm - leftBound).coerceAtLeast(0f)
                val rightDist = (rightBound - (div.offsetXMm + div.thicknessMm)).coerceAtLeast(0f)

                // Right position control
                ModuleDimensionRow(
                    label = s.partitionRightLabel,
                    valueMm = rightDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 10f,
                    maxMm = (rightBound - leftBound - div.thicknessMm - 10f).coerceAtLeast(10f),
                    enabled = true,
                ) { newRight ->
                    val newOffset = (rightBound - div.thicknessMm - newRight).coerceIn(
                        leftBound + 10f,
                        (rightBound - div.thicknessMm - 10f).coerceAtLeast(leftBound + 10f),
                    )
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDivider(div.id) { it.copy(offsetXMm = newOffset) }
                    }
                }

                // Left position control
                ModuleDimensionRow(
                    label = s.partitionLeftLabel,
                    valueMm = leftDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 10f,
                    maxMm = (rightBound - leftBound - div.thicknessMm - 10f).coerceAtLeast(10f),
                    enabled = true,
                ) { newLeft ->
                    val newOffset = (leftBound + newLeft).coerceIn(
                        leftBound + 10f,
                        (rightBound - div.thicknessMm - 10f).coerceAtLeast(leftBound + 10f),
                    )
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDivider(div.id) { it.copy(offsetXMm = newOffset) }
                    }
                }
            }
        } else {
            val shelf = activeContainer.shelves.firstOrNull { it.id == selectedPartitionId }
            if (shelf != null) {
                val bays = activeContainer.getBays()
                val bay = bays.getOrElse(shelf.bayIndex) { bays.last() }

                // Width (adapted from bay!)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = bay.widthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Height (thickness)
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = shelf.thicknessMm,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 12f,
                    maxMm = 50f,
                    enabled = true,
                ) { newThickness ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShelf(shelf.id) { it.copy(thicknessMm = newThickness) }
                    }
                }

                // Depth
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = activeContainer.depthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Position header with lock icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = s.partitionPositionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Position Lock",
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(20.dp),
                    )
                }

                val bottomDist = shelf.elevationYMm
                val topDist = (activeContainer.interiorHeightMm - (shelf.elevationYMm + shelf.thicknessMm)).coerceAtLeast(0f)
                val maxElev = (activeContainer.interiorHeightMm - shelf.thicknessMm - 10f).coerceAtLeast(10f)

                // Top distance
                ModuleDimensionRow(
                    label = s.partitionTopLabel,
                    valueMm = topDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 10f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newTop ->
                    val newElev = (activeContainer.interiorHeightMm - shelf.thicknessMm - newTop).coerceIn(10f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShelf(shelf.id) { it.copy(elevationYMm = newElev) }
                    }
                }

                // Bottom distance
                ModuleDimensionRow(
                    label = s.partitionBottomLabel,
                    valueMm = bottomDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 10f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newBottom ->
                    val newElev = newBottom.coerceIn(10f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShelf(shelf.id) { it.copy(elevationYMm = newElev) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Duplicate & Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    onDuplicatePartition(activeContainer.id, selectedPartitionId, selectedPartitionIsVertical)
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.duplicateButton, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = {
                    onDeletePartition(activeContainer.id, selectedPartitionId)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.deleteButton, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ViewModePillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

/**
 * Custom architectural icon matching the user's reference:
 * Two square modules meeting along a dashed diagonal miter seam.
 * - When `isFused == true` (Separar): Arrows point OUTWARD (↖ and ↘) away from the seam.
 * - When `isFused == false` (Fusionar): Arrows point INWARD (↘ and ↖) toward each other to fuse.
 */
@Composable
fun CornerActionIcon(
    isFused: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.088f).coerceIn(1.6f, 3.5f)
        val stroke = Stroke(
            width = strokeW,
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter,
        )

        val x0 = w * 0.08f
        val y0 = h * 0.08f
        val x1 = w * 0.40f
        val y1 = h * 0.40f
        val x2 = w * 0.60f
        val y2 = h * 0.60f
        val x3 = w * 0.92f
        val y3 = h * 0.92f

        // 1. Top-Left Square outline (open along diagonal seam from (x1, y2) to (x2, y1))
        val pathTopLeft = Path().apply {
            moveTo(x1, y2)
            lineTo(x0, y2)
            lineTo(x0, y0)
            lineTo(x2, y0)
            lineTo(x2, y1)
        }
        drawPath(pathTopLeft, color = color, style = stroke)

        // 2. Bottom-Right Square outline (open along diagonal seam from (x2, y1) to (x1, y2))
        val pathBottomRight = Path().apply {
            moveTo(x2, y1)
            lineTo(x3, y1)
            lineTo(x3, y3)
            lineTo(x1, y3)
            lineTo(x1, y2)
        }
        drawPath(pathBottomRight, color = color, style = stroke)

        // 3. Dashed diagonal seam connecting (x1, y2) and (x2, y1)
        val dashEffect = PathEffect.dashPathEffect(
            floatArrayOf(strokeW * 1.5f, strokeW * 1.3f),
            0f,
        )
        drawLine(
            color = color,
            start = Offset(x1, y2),
            end = Offset(x2, y1),
            strokeWidth = strokeW,
            cap = StrokeCap.Square,
            pathEffect = dashEffect,
        )

        // 4. Arrows
        val wingLen = w * 0.15f
        if (isFused) {
            // SEPARAR: Arrows point outward away from each other
            // Top-Left arrow points ↖
            val a1End = Offset(w * 0.17f, h * 0.17f)
            val a1Start = Offset(w * 0.41f, h * 0.41f)
            drawLine(color, a1Start, a1End, strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a1End, Offset(a1End.x + wingLen, a1End.y), strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a1End, Offset(a1End.x, a1End.y + wingLen), strokeWidth = strokeW, cap = StrokeCap.Square)

            // Bottom-Right arrow points ↘
            val a2End = Offset(w * 0.83f, h * 0.83f)
            val a2Start = Offset(w * 0.59f, h * 0.59f)
            drawLine(color, a2Start, a2End, strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a2End, Offset(a2End.x - wingLen, a2End.y), strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a2End, Offset(a2End.x, a2End.y - wingLen), strokeWidth = strokeW, cap = StrokeCap.Square)
        } else {
            // FUSIONAR: Arrows point inward toward each other
            // Top-Left arrow points ↘ toward seam
            val a1Start = Offset(w * 0.17f, h * 0.17f)
            val a1End = Offset(w * 0.41f, h * 0.41f)
            drawLine(color, a1Start, a1End, strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a1End, Offset(a1End.x - wingLen, a1End.y), strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a1End, Offset(a1End.x, a1End.y - wingLen), strokeWidth = strokeW, cap = StrokeCap.Square)

            // Bottom-Right arrow points ↖ toward seam
            val a2Start = Offset(w * 0.83f, h * 0.83f)
            val a2End = Offset(w * 0.59f, h * 0.59f)
            drawLine(color, a2Start, a2End, strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a2End, Offset(a2End.x + wingLen, a2End.y), strokeWidth = strokeW, cap = StrokeCap.Square)
            drawLine(color, a2End, Offset(a2End.x, a2End.y + wingLen), strokeWidth = strokeW, cap = StrokeCap.Square)
        }
    }
}
