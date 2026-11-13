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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import com.closetstudio.core.model.DrawerFrontType
import com.closetstudio.core.model.PartitionHit
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex
import com.closetstudio.core.model.containerLocalToWorldMm
import com.closetstudio.core.model.getDimensionCallouts

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
    DRAWER,
    DRAWER_NOTCH,
    CLOTHES_RAIL,
    VERTICAL_POLE,
    SHOE_RACK,
    CABINET_DOORS,
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
    canDragPartitionAt: (downPos: Offset, viewportWidthPx: Float, viewportHeightPx: Float) -> Boolean = { _, _, _ -> false },
    onDragPartition: (pointerPos: Offset, viewportWidthPx: Float, viewportHeightPx: Float) -> Unit = { _, _, _ -> },
    canDragContainerAt: (downPos: Offset, viewportWidthPx: Float, viewportHeightPx: Float) -> Boolean = { _, _, _ -> false },
    onDragContainer: (pointerPos: Offset, viewportWidthPx: Float, viewportHeightPx: Float) -> Unit = { _, _, _ -> },
    isContainerMovementLocked: Boolean = false,
    isPanelOpen: Boolean = false,
    panelWidthPx: Float = 0f,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        val vw = size.width.toFloat().coerceAtLeast(1f)
        val vh = size.height.toFloat().coerceAtLeast(1f)
        if (isPanelOpen && downPos.x >= (vw - panelWidthPx - 10f)) {
            // Touch started inside the open catalog / inspector panel: ignore in 3D viewport
            return@awaitEachGesture
        }
        val dragTarget = resolveRoomDragTarget(
            isContainerMovementLocked = isContainerMovementLocked,
            canDragPartitionAt = { canDragPartitionAt(downPos, vw, vh) },
            canDragContainerAt = { canDragContainerAt(downPos, vw, vh) },
        )
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
                            when (dragTarget) {
                                RoomDragTarget.PARTITION -> onDragPartition(pointer.position, vw, vh)
                                RoomDragTarget.CONTAINER -> onDragContainer(pointer.position, vw, vh)
                                RoomDragTarget.CAMERA -> onOrbit(delta.x, delta.y)
                            }
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

internal enum class RoomDragTarget {
    CAMERA,
    PARTITION,
    CONTAINER,
}

internal fun resolveRoomDragTarget(
    isContainerMovementLocked: Boolean,
    canDragPartitionAt: () -> Boolean,
    canDragContainerAt: () -> Boolean,
): RoomDragTarget {
    if (canDragPartitionAt()) return RoomDragTarget.PARTITION
    if (!isContainerMovementLocked && canDragContainerAt()) return RoomDragTarget.CONTAINER
    return RoomDragTarget.CAMERA
}

/**
 * Direction-aware gesture recognizer for units in the catalog:
 * - Vertical slide: Treated as a catalog list scroll (allows [verticalScroll] in parent without consuming).
 * - Horizontal drag to the left: Pulls unit out of the catalog drawer into the 3D closet.
 * - Quick stationary tap (< 14px): Invokes [onTap] to place the item at the default location.
 */
private suspend fun PointerInputScope.detectCatalogUnitDragGestures(
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onTap: () -> Unit = {},
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPos = down.position
        var totalDeltaX = 0f
        var totalDeltaY = 0f
        var isHorizontalDrag = false
        var isVerticalScroll = false
        var prevPos = downPos

        do {
            val event = awaitPointerEvent()
            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

            if (!pointer.pressed) {
                if (isHorizontalDrag) {
                    pointer.consume()
                    onDragEnd()
                } else if (!isVerticalScroll) {
                    val dist = (pointer.position - downPos).getDistance()
                    if (dist < 14f) {
                        pointer.consume()
                        onTap()
                    }
                }
                break
            }

            val currentPos = pointer.position
            val delta = currentPos - prevPos
            prevPos = currentPos

            if (!isHorizontalDrag) {
                totalDeltaX += delta.x
                totalDeltaY += delta.y
                val absX = kotlin.math.abs(totalDeltaX)
                val absY = kotlin.math.abs(totalDeltaY)

                val slopX = 14f
                val slopY = 22f

                if (totalDeltaX < -slopX && !isVerticalScroll) {
                    // Dragged towards the left (into the 3D room)
                    isHorizontalDrag = true
                    pointer.consume()
                    onDragStart(downPos)
                    onDrag(Offset(totalDeltaX, totalDeltaY))
                } else if (totalDeltaX < -32f) {
                    // Clearly pulling left out of the drawer even if vertical scroll started
                    isHorizontalDrag = true
                    isVerticalScroll = false
                    pointer.consume()
                    onDragStart(downPos)
                    onDrag(Offset(totalDeltaX, totalDeltaY))
                } else if (!isVerticalScroll && absY > slopY && absY > absX * 1.8f && totalDeltaX > -10f) {
                    // Scrolling vertically through catalog units
                    isVerticalScroll = true
                }
            } else {
                pointer.consume()
                onDrag(delta)
            }
        } while (event.changes.any { it.pressed })
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
            val tanV = kotlin.math.tan(Math.toRadians(16.0)).toFloat()
            (tanV * aspect) to tanV
        } else {
            val tanH = kotlin.math.tan(Math.toRadians(21.0)).toFloat()
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
    val hit = room.pickContainerByRayMm(
        rayOriginX = orig[0],
        rayOriginY = orig[1],
        rayOriginZ = orig[2],
        rayDirX = dir[0],
        rayDirY = dir[1],
        rayDirZ = dir[2],
    )
    return hit
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
    includeDoors: Boolean = true,
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
    val hit = room.pickPartitionByRayMm(
        rayOriginX = orig[0],
        rayOriginY = orig[1],
        rayOriginZ = orig[2],
        rayDirX = dir[0],
        rayDirY = dir[1],
        rayDirZ = dir[2],
        includeDoors = includeDoors,
    )
    return hit
}

private fun projectScreenToContainerInterior(
    room: RoomSpec,
    container: ClosetContainerSpec,
    screenPos: Offset,
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
): Pair<Float, Float>? {
    val (rayOrigin, rayDir) = computeTapRayMm(
        tapPos = screenPos,
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

    val (pStart, pEnd) = when (container.wall) {
        WallIndex.NORTH -> room.vertexNW to room.vertexNE
        WallIndex.EAST -> room.vertexNE to room.vertexSE
        WallIndex.SOUTH -> room.vertexSE to room.vertexSW
        WallIndex.WEST -> room.vertexSW to room.vertexNW
    }

    val dx = pEnd.x - pStart.x
    val dz = pEnd.z - pStart.z
    val wallLen = kotlin.math.sqrt(dx * dx + dz * dz)
    if (wallLen < 10f) return null

    val ux = dx / wallLen
    val uz = dz / wallLen
    val nx = -uz
    val nz = ux

    val wallGap = 4f
    val totalD = container.depthMm.coerceAtLeast(350f)
    val planeD = wallGap + totalD

    val denom = rayDir[0] * nx + rayDir[2] * nz
    if (kotlin.math.abs(denom) < 1e-4f) return null

    val p0x = pStart.x + planeD * nx
    val p0z = pStart.z + planeD * nz
    val numer = (p0x - rayOrigin[0]) * nx + (p0z - rayOrigin[2]) * nz
    val t = numer / denom
    if (t < 0f) return null

    val hitX = rayOrigin[0] + t * rayDir[0]
    val hitY = rayOrigin[1] + t * rayDir[1]
    val hitZ = rayOrigin[2] + t * rayDir[2]

    val relX = hitX - pStart.x
    val relZ = hitZ - pStart.z
    val s = relX * ux + relZ * uz

    val s0Inner = container.offsetMm + container.effectivePanelThicknessMm
    val innerX = s - s0Inner
    val innerY = hitY - (container.effectivePlinthHeightMm + container.effectivePanelThicknessMm)

    return innerX to innerY
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
    var showMaterialsDialog by remember { mutableStateOf(false) }
    var appUnitSystem by remember {
        mutableStateOf(AppUnitSystem.fromCode(projectStorage.getUnitSystemCode()))
    }
    var showUnitsColumn by remember { mutableStateOf(false) }
    var isEditingContainerInPanel by remember { mutableStateOf(false) }
    var isEditingPartitionInPanel by remember { mutableStateOf(false) }
    var selectedContainerId by remember { mutableStateOf<String?>(null) }
    var selectedPartitionId by remember { mutableStateOf<String?>(null) }
    var containerDragGrabOffsetMm by remember { mutableFloatStateOf(0f) }
    var selectedPartitionIsVertical by remember { mutableStateOf(true) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedCategoryKey by remember { mutableStateOf("CONTENT") }

    // View Mode (3D Perspective, Top-Down, Front 2D)
    var currentViewMode by remember { mutableStateOf(RoomViewMode.PERSPECTIVE_3D) }
    var showWallLabels by remember { mutableStateOf(true) }
    var showClosetDoors by remember { mutableStateOf(true) }
    var showClosetDimensions by remember { mutableStateOf(false) }
    var isContainerMovementLocked by remember { mutableStateOf(false) }
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
    val distanceAnim = remember { Animatable(4.20f) }
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
        val desiredYaw = getWallYaw(normalizedStep, splitDoorWall) + 18f
        val targetYaw = shortestAngleTarget(yawAnim.value, desiredYaw)
        coroutineScope.launch {
            launch { yawAnim.animateTo(targetYaw, tween(350, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(-5f, tween(300, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(4.20f, tween(300, easing = FastOutSlowInEasing)) }
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
                launch { distanceAnim.animateTo(4.20f, tween(300, easing = FastOutSlowInEasing)) }
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
                launch { distanceAnim.animateTo(4.20f, tween(300, easing = FastOutSlowInEasing)) }
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
        Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
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
                        label = { Text(s.materialsAndTexturesTitle) },
                        icon = { Icon(Icons.Default.Palette, null) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showMaterialsDialog = true
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
                val isCompactWidth = maxWidth < 600.dp
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

                            if (!isCompactWidth) {
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

                                // Toggle Closet Doors Visibility
                                IconButton(
                                    onClick = {
                                        showClosetDoors = !showClosetDoors
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(
                                                if (showClosetDoors) s.doorsShownToast else s.doorsHiddenToast
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoorFront,
                                        contentDescription = s.toggleDoorsVisibility,
                                        tint = if (showClosetDoors) Color.White else Color.White.copy(alpha = 0.55f),
                                    )
                            }

                                // Toggle Closet Dimensions Visibility
                                IconButton(
                                    onClick = {
                                        showClosetDimensions = !showClosetDimensions
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(
                                                if (showClosetDimensions) s.closetDimensionsShownToast else s.closetDimensionsHiddenToast
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = s.toggleClosetDimensions,
                                        tint = if (showClosetDimensions) Color(0xFFB388FF) else Color.White.copy(alpha = 0.55f),
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
                                        text = { Text(s.materialsAndTexturesTitle) },
                                        leadingIcon = { Icon(Icons.Default.Palette, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showMaterialsDialog = true
                                        },
                                    )
                                    if (isCompactWidth) {
                                        DropdownMenuItem(
                                            text = { Text(s.roomInfo) },
                                            leadingIcon = { Icon(Icons.Default.Info, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showInfoDialog = true
                                            },
                                        )
                                    }
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
                                                Text(s.toggleDoorsVisibility)
                                                if (showClosetDoors) {
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
                                                imageVector = Icons.Default.DoorFront,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showClosetDoors = !showClosetDoors
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(s.toggleClosetDimensions)
                                                if (showClosetDimensions) {
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
                                                imageVector = Icons.Default.Straighten,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showClosetDimensions = !showClosetDimensions
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
                        val unitsColumnWidth = if (isLandscape) {
                            (screenWidth * 0.36f).coerceIn(290.dp, 380.dp)
                        } else {
                            (screenWidth * 0.76f).coerceIn(255.dp, 330.dp)
                        }
                        val unitsColumnWidthPx = with(LocalDensity.current) { unitsColumnWidth.toPx() }
                        var isDraggingUnit by remember { mutableStateOf(false) }
                        var activeDragPartitionId by remember { mutableStateOf<String?>(null) }
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
                                        activeContainer.shelves.any { it.id == selectedPartitionId } ||
                                        activeContainer.drawers.any { it.id == selectedPartitionId } ||
                                        activeContainer.clothesRails.any { it.id == selectedPartitionId } ||
                                        activeContainer.verticalPoles.any { it.id == selectedPartitionId } ||
                                        activeContainer.shoeRacks.any { it.id == selectedPartitionId } ||
                                        activeContainer.cabinetDoors.any { it.id == selectedPartitionId }
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
                                showDoors = showClosetDoors,
                                isPanelOpen = showUnitsColumn,
                                panelWidthPx = unitsColumnWidthPx,
                                isContainerMovementLocked = isContainerMovementLocked,
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
                                        includeDoors = showClosetDoors,
                                    )
                                    if (partHit != null) {
                                        selectedContainerId = partHit.containerId
                                        selectedPartitionId = partHit.partitionId
                                        selectedPartitionIsVertical = partHit.isVertical
                                        isEditingContainerInPanel = false
                                        showUnitsColumn = false
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
                                        if (hitId == null && (isEditingContainerInPanel || isEditingPartitionInPanel || showUnitsColumn)) {
                                            isEditingContainerInPanel = false
                                            isEditingPartitionInPanel = false
                                            showUnitsColumn = false
                                        }
                                    }
                            },
                                canDragPartitionAt = { downPos, vw, vh ->
                                    val hit = pickPartitionAtScreenTap(
                                        room = room,
                                        tapPos = downPos,
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
                                        includeDoors = showClosetDoors,
                                    )
                                    if (hit != null) {
                                        selectedContainerId = hit.containerId
                                        selectedPartitionId = hit.partitionId
                                        selectedPartitionIsVertical = hit.isVertical
                                        true
                                    } else if (selectedPartitionId != null && activeContainer != null) {
                                        val interiorPt = projectScreenToContainerInterior(
                                            room = room,
                                            container = activeContainer,
                                            screenPos = downPos,
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
                                        if (interiorPt != null) {
                                            val (tapX, tapY) = interiorPt
                                            val rail = activeContainer.clothesRails.firstOrNull { it.id == selectedPartitionId }
                                            val pole = if (rail == null) activeContainer.verticalPoles.firstOrNull { it.id == selectedPartitionId } else null
                                            val rack = if (rail == null && pole == null) activeContainer.shoeRacks.firstOrNull { it.id == selectedPartitionId } else null
                                            val doors = if (rail == null && pole == null && rack == null) activeContainer.cabinetDoors.firstOrNull { it.id == selectedPartitionId } else null
                                            if (rail != null) {
                                                val bay = activeContainer.getBays().getOrElse(rail.bayIndex) { activeContainer.getBays().last() }
                                                tapX in (bay.startXMm - 60f)..(bay.endXMm + 60f) &&
                                                    kotlin.math.abs(tapY - rail.elevationYMm) <= 220f
                                            } else if (pole != null) {
                                                val bay = activeContainer.getBays().getOrElse(pole.bayIndex) { activeContainer.getBays().last() }
                                                val (compY0, compY1) = pole.effectiveYRange(activeContainer)
                                                val poleX = pole.effectiveX(activeContainer)
                                                tapY in (compY0 - 60f)..(compY1 + 60f) &&
                                                    kotlin.math.abs(tapX - poleX) <= 120f
                                            } else if (rack != null) {
                                                val bay = activeContainer.getBays().getOrElse(rack.bayIndex) { activeContainer.getBays().last() }
                                                tapX in (bay.startXMm - 60f)..(bay.endXMm + 60f) &&
                                                    tapY in (rack.elevationYMm - 60f)..(rack.elevationYMm + rack.totalVerticalSpanMm(activeContainer.interiorDepthMm) + 60f)
                                            } else if (doors != null) {
                                                val bay = activeContainer.getBays().getOrElse(doors.bayIndex) { activeContainer.getBays().last() }
                                                tapX in (bay.startXMm - 60f)..(bay.endXMm + 60f) &&
                                                    tapY in (doors.elevationYMm - 60f)..(doors.elevationYMm + doors.heightMm + 60f)
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    } else {
                                        false
                                    }
                            },
                                onDragPartition = { pointerPos, vw, vh ->
                                    val partId = selectedPartitionId
                                    val c = activeContainer
                                    if (partId != null && c != null) {
                                        val interiorPt = projectScreenToContainerInterior(
                                            room = room,
                                            container = c,
                                            screenPos = pointerPos,
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
                                        if (interiorPt != null) {
                                            val (dragXMm, dragYMm) = interiorPt
                                            val updatedRoom = room.withUpdatedContainer(c.id) { currentC ->
                                                if (currentC.verticalPoles.any { it.id == partId }) {
                                                    currentC.withMovedVerticalPole(partId, newOffsetXMm = dragXMm, targetYMm = dragYMm)
                                                } else if (selectedPartitionIsVertical) {
                                                    currentC.withMovedDivider(partId, newOffsetXMm = dragXMm, targetYMm = dragYMm)
                                                } else if (currentC.drawers.any { it.id == partId }) {
                                                    currentC.withMovedDrawer(partId, newElevationYMm = dragYMm, targetXMm = dragXMm)
                                                } else if (currentC.clothesRails.any { it.id == partId }) {
                                                    currentC.withMovedClothesRail(partId, newElevationYMm = dragYMm, targetXMm = dragXMm)
                                                } else if (currentC.shoeRacks.any { it.id == partId }) {
                                                    currentC.withMovedShoeRack(partId, newElevationYMm = dragYMm, targetXMm = dragXMm)
                                                } else if (currentC.cabinetDoors.any { it.id == partId }) {
                                                    currentC.withMovedCabinetDoors(partId, newElevationYMm = dragYMm, targetXMm = dragXMm)
                                                } else {
                                                    currentC.withMovedShelf(partId, newElevationYMm = dragYMm, targetXMm = dragXMm)
                                                }
                                            }
                                            updateRoom(updatedRoom)
                                        }
                                    }
                            },
                                canDragContainerAt = { downPos, vw, vh ->
                                    val id = pickContainerAtScreenTap(
                                        room = room, tapPos = downPos, viewportWidthPx = vw, viewportHeightPx = vh,
                                        targetX = targetXAnim.value, targetY = targetYAnim.value, targetZ = targetZAnim.value,
                                        yawDeg = yawAnim.value, pitchDeg = pitchAnim.value, distanceM = distanceAnim.value,
                                        isOrtho = isOrthoMode, orthoSize = orthoSizeAnim.value,
                                    )
                                    val container = room.containers.firstOrNull { it.id == id } ?: return@RoomViewport false
                                    selectedContainerId = id
                                    selectedPartitionId = null
                                    val local = projectScreenToContainerInterior(
                                        room, container, downPos, vw, vh, targetXAnim.value, targetYAnim.value,
                                        targetZAnim.value, yawAnim.value, pitchAnim.value, distanceAnim.value,
                                        isOrthoMode, orthoSizeAnim.value,
                                    ) ?: return@RoomViewport false
                                    containerDragGrabOffsetMm = local.first
                                    true
                            },
                                onDragContainer = { pointerPos, vw, vh ->
                                    val container = room.containers.firstOrNull { it.id == selectedContainerId }
                                    if (container != null) {
                                        val local = projectScreenToContainerInterior(
                                            room, container, pointerPos, vw, vh, targetXAnim.value, targetYAnim.value,
                                            targetZAnim.value, yawAnim.value, pitchAnim.value, distanceAnim.value,
                                            isOrthoMode, orthoSizeAnim.value,
                                        )
                                        if (local != null) {
                                            val wallPosition = container.offsetMm + container.effectivePanelThicknessMm + local.first
                                            updateRoom(room.withMovedContainer(container.id, wallPosition - containerDragGrabOffsetMm))
                                        }
                                    }
                            },
                                modifier = Modifier.fillMaxSize(),
                            )

                            // Closet Dimension Callouts Overlay (CAD measurements in purple with ticks & white-outlined text)
                            if (showClosetDimensions && !showDimensionsSheet) {
                                val currentFocusedWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                val currentWallContainers = room.containers.filter { it.wall == currentFocusedWall }
                                val containersToDimension = if (activeContainer != null) {
                                    listOf(activeContainer)
                            } else if (currentWallContainers.isNotEmpty()) {
                                    currentWallContainers
                            } else {
                                    room.containers
                            }

                                val purpleColor = Color(0xFF7038FF)
                                val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }
                                val tickLengthPx = with(LocalDensity.current) { 6.dp.toPx() }
                                val strokeOutlinePx = with(LocalDensity.current) { 3.5.dp.toPx() }
                                val textOffsetHorizPx = with(LocalDensity.current) { 13.dp.toPx() }
                                val textOffsetVertPx = with(LocalDensity.current) { 17.dp.toPx() }

                                val strokePaint = remember {
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        textSize = 34f
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = strokeOutlinePx
                                        color = android.graphics.Color.WHITE
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                            }
                                val fillPaint = remember {
                                    android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        textSize = 34f
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        style = android.graphics.Paint.Style.FILL
                                        color = android.graphics.Color.BLACK
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                            }

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val yawDeg = yawAnim.value
                                    val pitchDeg = pitchAnim.value
                                    val distM = distanceAnim.value
                                    val orthoSz = orthoSizeAnim.value
                                    val targetX = targetXAnim.value
                                    val targetY = targetYAnim.value
                                    val targetZ = targetZAnim.value
                                    val vw = size.width
                                    val vh = size.height
                                    val isOrtho = currentViewMode == RoomViewMode.FRONT_2D || currentViewMode == RoomViewMode.TOP_DOWN

                                    for (c in containersToDimension) {
                                        val callouts = c.getDimensionCallouts()
                                        for (callout in callouts) {
                                            val startPt = room.containerLocalToWorldMm(
                                                container = c,
                                                xLocalMm = callout.startXLocalMm,
                                                yLocalMm = callout.startYLocalMm,
                                                zLocalMm = callout.startZLocalMm,
                                            )
                                            val endPt = room.containerLocalToWorldMm(
                                                container = c,
                                                xLocalMm = callout.endXLocalMm,
                                                yLocalMm = callout.endYLocalMm,
                                                zLocalMm = callout.endZLocalMm,
                                            )

                                            val pStart = project3DToScreen(
                                                point = Float3(startPt.x * MM_TO_M, startPt.y * MM_TO_M, startPt.z * MM_TO_M),
                                                targetX = targetX,
                                                targetY = targetY,
                                                targetZ = targetZ,
                                                yawDeg = yawDeg,
                                                pitchDeg = pitchDeg,
                                                distanceM = distM,
                                                isOrtho = isOrtho,
                                                orthoSize = orthoSz,
                                                viewportWidth = vw,
                                                viewportHeight = vh,
                                            )
                                            val pEnd = project3DToScreen(
                                                point = Float3(endPt.x * MM_TO_M, endPt.y * MM_TO_M, endPt.z * MM_TO_M),
                                                targetX = targetX,
                                                targetY = targetY,
                                                targetZ = targetZ,
                                                yawDeg = yawDeg,
                                                pitchDeg = pitchDeg,
                                                distanceM = distM,
                                                isOrtho = isOrtho,
                                                orthoSize = orthoSz,
                                                viewportWidth = vw,
                                                viewportHeight = vh,
                                            )

                                            if (pStart != null && pEnd != null) {
                                                val dx = pEnd.x - pStart.x
                                                val dy = pEnd.y - pStart.y
                                                val len = sqrt(dx * dx + dy * dy)
                                                if (len > 12f) {
                                                    // Main purple line
                                                    drawLine(
                                                        color = purpleColor,
                                                        start = pStart,
                                                        end = pEnd,
                                                        strokeWidth = strokeWidthPx,
                                                        cap = StrokeCap.Round,
                                                    )

                                                    // Perpendicular tick marks
                                                    val ux = dx / len
                                                    val uy = dy / len
                                                    val nx = -uy
                                                    val ny = ux

                                                    drawLine(
                                                        color = purpleColor,
                                                        start = Offset(pStart.x - nx * tickLengthPx, pStart.y - ny * tickLengthPx),
                                                        end = Offset(pStart.x + nx * tickLengthPx, pStart.y + ny * tickLengthPx),
                                                        strokeWidth = strokeWidthPx,
                                                        cap = StrokeCap.Round,
                                                    )
                                                    drawLine(
                                                        color = purpleColor,
                                                        start = Offset(pEnd.x - nx * tickLengthPx, pEnd.y - ny * tickLengthPx),
                                                        end = Offset(pEnd.x + nx * tickLengthPx, pEnd.y + ny * tickLengthPx),
                                                        strokeWidth = strokeWidthPx,
                                                        cap = StrokeCap.Round,
                                                    )

                                                    // Midpoint and text label
                                                    val midX = (pStart.x + pEnd.x) * 0.5f
                                                    val midY = (pStart.y + pEnd.y) * 0.5f

                                                    val isHorizontal = kotlin.math.abs(dx) >= kotlin.math.abs(dy)
                                                    val textPos = if (isHorizontal) {
                                                        var normX = nx
                                                        var normY = ny
                                                        if (normY > 0) {
                                                            normX = -normX
                                                            normY = -normY
                                                        }
                                                        Offset(midX + normX * textOffsetHorizPx, midY + normY * textOffsetHorizPx)
                                                    } else {
                                                        var normX = nx
                                                        var normY = ny
                                                        if (normX < 0) {
                                                            normX = -normX
                                                            normY = -normY
                                                        }
                                                        Offset(midX + normX * textOffsetVertPx, midY + normY * textOffsetVertPx)
                                                    }

                                                    val displayText = when (appUnitSystem) {
                                                        AppUnitSystem.MM -> "${callout.valueMm.roundToInt()}"
                                                        else -> appUnitSystem.formatCompact(callout.valueMm)
                                                    }

                                                    val fontMetrics = fillPaint.fontMetrics
                                                    val textCenterY = textPos.y - (fontMetrics.descent + fontMetrics.ascent) * 0.5f

                                                    drawContext.canvas.nativeCanvas.drawText(displayText, textPos.x, textCenterY, strokePaint)
                                                    drawContext.canvas.nativeCanvas.drawText(displayText, textPos.x, textCenterY, fillPaint)
                                                }
                                            }
                                        }
                                    }
                            }
                            }

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

                            FilledIconButton(
                                onClick = {
                                    isContainerMovementLocked = !isContainerMovementLocked
                                    val statusMessage = if (isContainerMovementLocked) {
                                        tr(appLanguage, "El arrastre mueve la cámara", "Drag to move the camera")
                                    } else {
                                        tr(appLanguage, "El arrastre mueve el closet", "Drag to move the closet")
                                    }
                                    coroutineScope.launch { messageHostState.showSnackbar(statusMessage) }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                            ) {
                                Icon(
                                    imageVector = if (isContainerMovementLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isContainerMovementLocked) {
                                        tr(appLanguage, "Desbloquear movimiento del closet", "Unlock closet movement")
                                    } else {
                                        tr(appLanguage, "Bloquear movimiento del closet", "Lock closet movement")
                                    },
                                )
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
                                    val hasSelectedPartition = selectedPartitionId != null
                                    val isEditingActive = if (hasSelectedPartition) {
                                        showUnitsColumn && isEditingPartitionInPanel
                                    } else {
                                        showUnitsColumn && isEditingContainerInPanel
                                    }

                                    // Wall Badge for active container / partition
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (hasSelectedPartition) Color(0xFF651FFF) else Color(0xFF2C55B5),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                                        shadowElevation = 4.dp,
                                    ) {
                                        val baseLetter = containerWallLetter(activeContainer, room, splitDoorWall)
                                        val badgeText = if (hasSelectedPartition) {
                                            if (activeContainer.verticalPoles.any { it.id == selectedPartitionId }) "$baseLetter • POS"
                                            else if (selectedPartitionIsVertical) "$baseLetter • DIV"
                                            else if (activeContainer.drawers.any { it.id == selectedPartitionId }) "$baseLetter • CAJ"
                                            else if (activeContainer.clothesRails.any { it.id == selectedPartitionId }) "$baseLetter • TUB"
                                            else if (activeContainer.shoeRacks.any { it.id == selectedPartitionId }) "$baseLetter • ZAP"
                                            else if (activeContainer.cabinetDoors.any { it.id == selectedPartitionId }) "$baseLetter • PUE"
                                            else "$baseLetter • EST"
                                        } else {
                                            baseLetter
                                        }
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }

                                    // 1. Edit Button (✏️ opens right-side inspector)
                                    Surface(
                                        onClick = {
                                            if (hasSelectedPartition) {
                                                isEditingPartitionInPanel = true
                                                isEditingContainerInPanel = false
                                                showUnitsColumn = true
                                            } else {
                                                selectedContainerId = activeContainer.id
                                                isEditingContainerInPanel = true
                                                isEditingPartitionInPanel = false
                                                showUnitsColumn = true
                                            }
                                        },
                                        shape = CircleShape,
                                        color = if (isEditingActive) {
                                            if (hasSelectedPartition) Color(0xFF651FFF) else Color(0xFF2C55B5)
                                        } else {
                                            Color(0xFFE5DFDA).copy(alpha = 0.95f)
                                        },
                                        border = BorderStroke(2.dp, if (hasSelectedPartition) Color(0xFF7C4DFF) else Color(0xFF4355B9)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(if (isLandscape) 46.dp else 52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = s.editContainerDesc,
                                                tint = if (isEditingActive) {
                                                    Color.White
                                                } else {
                                                    if (hasSelectedPartition) Color(0xFF651FFF) else Color(0xFF3949AB)
                                                },
                                                modifier = Modifier.size(if (isLandscape) 22.dp else 24.dp),
                                            )
                                        }
                                    }

                                    // 2. Duplicate Button (❐)
                                    Surface(
                                        onClick = {
                                            if (hasSelectedPartition && selectedPartitionId != null) {
                                                var createdId: String? = null
                                                val updatedRoom = room.withUpdatedContainer(activeContainer.id) { container ->
                                                    if (container.verticalPoles.any { it.id == selectedPartitionId }) {
                                                        val source = container.verticalPoles.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val bay = container.getBays().getOrElse(source.bayIndex) { container.getBays().last() }
                                                        val curX = source.effectiveX(container)
                                                        val newX = (curX + 120f).coerceAtMost(bay.endXMm - source.diameterMm * 0.5f)
                                                        val newPole = source.copy(
                                                            id = "pole_${System.currentTimeMillis()}_${container.verticalPoles.size + 1}",
                                                            offsetXMm = newX,
                                                        )
                                                        createdId = newPole.id
                                                        container.copy(verticalPoles = container.verticalPoles + newPole)
                                                    } else if (selectedPartitionIsVertical) {
                                                        val source = container.dividers.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val newOff = (source.offsetXMm + source.thicknessMm + 80f).coerceAtMost((container.interiorWidthMm - source.thicknessMm).coerceAtLeast(0f))
                                                        val newDiv = source.copy(
                                                            id = "div_${System.currentTimeMillis()}_${container.dividers.size + 1}",
                                                            offsetXMm = newOff,
                                                        )
                                                        createdId = newDiv.id
                                                        container.copy(dividers = container.dividers + newDiv)
                                                    } else if (container.drawers.any { it.id == selectedPartitionId }) {
                                                        val source = container.drawers.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val newElev = (source.elevationYMm + source.heightMm).coerceAtMost((container.interiorHeightMm - source.heightMm).coerceAtLeast(0f))
                                                        val newDrawer = source.copy(
                                                            id = "drawer_${System.currentTimeMillis()}_${container.drawers.size + 1}",
                                                            elevationYMm = newElev,
                                                        )
                                                        createdId = newDrawer.id
                                                        container.copy(drawers = container.drawers + newDrawer)
                                                    } else if (container.clothesRails.any { it.id == selectedPartitionId }) {
                                                        val source = container.clothesRails.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val newElev = (source.elevationYMm + 200f).coerceAtMost((container.interiorHeightMm - source.diameterMm).coerceAtLeast(0f))
                                                        val newRail = source.copy(
                                                            id = "rail_${System.currentTimeMillis()}_${container.clothesRails.size + 1}",
                                                            elevationYMm = newElev,
                                                        )
                                                        createdId = newRail.id
                                                        container.copy(clothesRails = container.clothesRails + newRail)
                                                    } else if (container.shoeRacks.any { it.id == selectedPartitionId }) {
                                                        val source = container.shoeRacks.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val targetY = source.elevationYMm + source.totalVerticalSpanMm(container.interiorDepthMm) + 30f
                                                        val newRack = source.copy(
                                                            id = "shoerack_${System.currentTimeMillis()}_${container.shoeRacks.size + 1}",
                                                            elevationYMm = targetY.coerceIn(0f, source.maxElevation(container.interiorHeightMm, container.interiorDepthMm)),
                                                        )
                                                        createdId = newRack.id
                                                        container.copy(shoeRacks = container.shoeRacks + newRack)
                                                    } else if (container.cabinetDoors.any { it.id == selectedPartitionId }) {
                                                        val source = container.cabinetDoors.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val targetY = source.elevationYMm + source.heightMm + 10f
                                                        val newDoors = source.copy(
                                                            id = "doors_${System.currentTimeMillis()}_${container.cabinetDoors.size + 1}",
                                                            elevationYMm = targetY.coerceIn(0f, (container.interiorHeightMm - source.heightMm).coerceAtLeast(0f)),
                                                        )
                                                        createdId = newDoors.id
                                                        container.copy(cabinetDoors = container.cabinetDoors + newDoors)
                                                    } else {
                                                        val source = container.shelves.firstOrNull { it.id == selectedPartitionId } ?: return@withUpdatedContainer container
                                                        val newElev = (source.elevationYMm + source.thicknessMm + 100f).coerceAtMost((container.interiorHeightMm - source.thicknessMm).coerceAtLeast(0f))
                                                        val newShelf = source.copy(
                                                            id = "shelf_${System.currentTimeMillis()}_${container.shelves.size + 1}",
                                                            elevationYMm = newElev,
                                                        )
                                                        createdId = newShelf.id
                                                        container.copy(shelves = container.shelves + newShelf)
                                                    }
                                                }
                                                updateRoom(updatedRoom)
                                                createdId?.let { selectedPartitionId = it }
                                                coroutineScope.launch {
                                                    messageHostState.showSnackbar(s.duplicateButton)
                                                }
                                            } else {
                                                val (updatedRoom, newId) = room.withDuplicatedContainer(activeContainer.id)
                                                updateRoom(updatedRoom)
                                                if (newId != null) {
                                                    selectedContainerId = newId
                                                    coroutineScope.launch {
                                                        messageHostState.showSnackbar(s.containerDuplicatedToast)
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFFE5DFDA).copy(alpha = 0.95f),
                                        border = BorderStroke(2.dp, if (hasSelectedPartition) Color(0xFF7C4DFF) else Color(0xFF4355B9)),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(if (isLandscape) 46.dp else 52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = s.duplicateContainerDesc,
                                                tint = if (hasSelectedPartition) Color(0xFF651FFF) else Color(0xFF3949AB),
                                                modifier = Modifier.size(if (isLandscape) 21.dp else 23.dp),
                                            )
                                        }
                                    }

                                    // 3. Delete Button (🗑️)
                                    Surface(
                                        onClick = {
                                            if (hasSelectedPartition && selectedPartitionId != null) {
                                                val partId = selectedPartitionId!!
                                                val updatedRoom = room.withUpdatedContainer(activeContainer.id) { it.withRemovedPartition(partId) }
                                                updateRoom(updatedRoom)
                                                selectedPartitionId = null
                                                showUnitsColumn = false
                                                coroutineScope.launch {
                                                    messageHostState.showSnackbar(s.partitionDeletedToast)
                                                }
                                            } else {
                                                val updatedRoom = room.withRemovedContainer(activeContainer.id)
                                                updateRoom(updatedRoom)
                                                selectedContainerId = updatedRoom.containers.lastOrNull()?.id
                                                if (updatedRoom.containers.isEmpty()) {
                                                    isEditingContainerInPanel = false
                                                }
                                                coroutineScope.launch {
                                                    messageHostState.showSnackbar(s.containerDeletedToast)
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFFE5DFDA).copy(alpha = 0.95f),
                                        border = BorderStroke(2.dp, if (hasSelectedPartition) Color(0xFF7C4DFF) else Color(0xFF4355B9)),
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
                                            isEditingPartitionInPanel = false
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
                                    isEditingPartition = isEditingPartitionInPanel && activeContainer != null,
                                    selectedPartitionId = selectedPartitionId,
                                    selectedPartitionIsVertical = selectedPartitionIsVertical,
                                    isDragging = isDraggingUnit,
                                    onToggleEditMode = { isEditingContainerInPanel = it },
                                    onTogglePartitionEditMode = { isEditingPartitionInPanel = it },
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
                                            showUnitsColumn = false
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
                                            showUnitsColumn = false
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
                                            showUnitsColumn = false
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
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddDrawer = { dropFracX, dropFracY, frontType ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else null
                                            val updated = target.withAddedDrawer(bayIndex = bay.index, elevationYMm = dropY, frontType = frontType)
                                            val newDrawer = updated.drawers.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newDrawer?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddClothesRail = { dropFracX, dropFracY ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else null
                                            val updated = target.withAddedClothesRail(bayIndex = bay.index, elevationYMm = dropY)
                                            val newRail = updated.clothesRails.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newRail?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddVerticalPole = { dropFracX, dropFracY ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else target.interiorHeightMm * 0.5f
                                            val updated = target.withAddedVerticalPole(bayIndex = bay.index, offsetXMm = dropX, targetYMm = dropY)
                                            val newPole = updated.verticalPoles.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newPole?.id
                                            selectedPartitionIsVertical = true
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddShoeRack = { dropFracX, dropFracY ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else null
                                            val updated = target.withAddedShoeRack(bayIndex = bay.index, elevationYMm = dropY)
                                            val newRack = updated.shoeRacks.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newRack?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onAddCabinetDoors = { dropFracX, dropFracY ->
                                        val target = activeContainer ?: room.containers.firstOrNull()
                                        if (target != null) {
                                            val dropX = if (dropFracX != null) dropFracX * target.interiorWidthMm else target.interiorWidthMm * 0.5f
                                            val bay = target.findBayAtX(dropX)
                                            val dropY = if (dropFracY != null) dropFracY * target.interiorHeightMm else null
                                            val updated = target.withAddedCabinetDoors(bayIndex = bay.index, targetYMm = dropY)
                                            val newDoors = updated.cabinetDoors.lastOrNull()
                                            val updatedRoom = room.withUpdatedContainer(target.id) { updated }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = target.id
                                            selectedPartitionId = newDoors?.id
                                            selectedPartitionIsVertical = false
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(s.partitionAddedToast)
                                            }
                                        }
                                    },
                                    onDragUnitStart = { type, startRootPos ->
                                        draggedUnitType = type
                                        isDraggingUnit = true
                                        dragScreenOffset = startRootPos - viewportOriginInRoot
                                        activeDragPartitionId = null
                                    },
                                    onDragUnitMove = { delta ->
                                        dragScreenOffset += delta

                                        val currentWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                        val target = activeContainer
                                            ?: room.containers.firstOrNull { it.wall == currentWall }
                                            ?: room.containers.firstOrNull()

                                        if (target != null && (draggedUnitType == DraggedUnitType.VERTICAL_DIVIDER || draggedUnitType == DraggedUnitType.HORIZONTAL_SHELF || draggedUnitType == DraggedUnitType.DRAWER || draggedUnitType == DraggedUnitType.DRAWER_NOTCH || draggedUnitType == DraggedUnitType.CLOTHES_RAIL || draggedUnitType == DraggedUnitType.VERTICAL_POLE || draggedUnitType == DraggedUnitType.SHOE_RACK || draggedUnitType == DraggedUnitType.CABINET_DOORS)) {
                                            val interiorPt = projectScreenToContainerInterior(
                                                room = room,
                                                container = target,
                                                screenPos = dragScreenOffset,
                                                viewportWidthPx = viewportWidthPx,
                                                viewportHeightPx = viewportHeightPx,
                                                targetX = targetXAnim.value,
                                                targetY = targetYAnim.value,
                                                targetZ = targetZAnim.value,
                                                yawDeg = yawAnim.value,
                                                pitchDeg = pitchAnim.value,
                                                distanceM = distanceAnim.value,
                                                isOrtho = isOrthoMode,
                                                orthoSize = orthoSizeAnim.value,
                                            )
                                            if (interiorPt != null) {
                                                val (rawX, rawY) = interiorPt
                                                val isOverPanel = showUnitsColumn && !isDraggingUnit && (dragScreenOffset.x + viewportOriginInRoot.x >= (panelOriginInRootX - 15f))
                                                val isInsideCloset = !isOverPanel &&
                                                    rawX >= -20f && rawX <= (target.interiorWidthMm + 20f) &&
                                                    rawY >= -20f && rawY <= (target.interiorHeightMm + 20f)

                                                val clampedX = rawX.coerceIn(10f, (target.interiorWidthMm - 28f).coerceAtLeast(10f))
                                                val clampedY = rawY.coerceIn(10f, (target.interiorHeightMm - 28f).coerceAtLeast(10f))

                                                if (draggedUnitType == DraggedUnitType.VERTICAL_DIVIDER) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val updated = target.withAddedDivider(offsetXMm = clampedX, targetYMm = clampedY)
                                                            val newDiv = updated.dividers.lastOrNull()
                                                            if (newDiv != null) {
                                                                activeDragPartitionId = newDiv.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newDiv.id
                                                                selectedPartitionIsVertical = true
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedDivider(partId, newOffsetXMm = clampedX, targetYMm = clampedY)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.HORIZONTAL_SHELF) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedShelf(bayIndex = bay.index, elevationYMm = clampedY)
                                                            val newShelf = updated.shelves.lastOrNull()
                                                            if (newShelf != null) {
                                                                activeDragPartitionId = newShelf.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newShelf.id
                                                                selectedPartitionIsVertical = false
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedShelf(partId, newElevationYMm = clampedY, targetXMm = clampedX)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.DRAWER || draggedUnitType == DraggedUnitType.DRAWER_NOTCH) {
                                                    val partId = activeDragPartitionId
                                                    val frontType = if (draggedUnitType == DraggedUnitType.DRAWER_NOTCH) DrawerFrontType.NOTCH else DrawerFrontType.HANDLE
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedDrawer(bayIndex = bay.index, elevationYMm = clampedY, frontType = frontType)
                                                            val newDrawer = updated.drawers.lastOrNull()
                                                            if (newDrawer != null) {
                                                                activeDragPartitionId = newDrawer.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newDrawer.id
                                                                selectedPartitionIsVertical = false
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedDrawer(partId, newElevationYMm = clampedY, targetXMm = clampedX)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.CLOTHES_RAIL) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedClothesRail(bayIndex = bay.index, elevationYMm = clampedY)
                                                            val newRail = updated.clothesRails.lastOrNull()
                                                            if (newRail != null) {
                                                                activeDragPartitionId = newRail.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newRail.id
                                                                selectedPartitionIsVertical = false
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedClothesRail(partId, newElevationYMm = clampedY, targetXMm = clampedX)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.VERTICAL_POLE) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedVerticalPole(bayIndex = bay.index, offsetXMm = clampedX, targetYMm = clampedY)
                                                            val newPole = updated.verticalPoles.lastOrNull()
                                                            if (newPole != null) {
                                                                activeDragPartitionId = newPole.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newPole.id
                                                                selectedPartitionIsVertical = true
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedVerticalPole(partId, newOffsetXMm = clampedX, targetYMm = clampedY)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.SHOE_RACK) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedShoeRack(bayIndex = bay.index, elevationYMm = clampedY)
                                                            val newRack = updated.shoeRacks.lastOrNull()
                                                            if (newRack != null) {
                                                                activeDragPartitionId = newRack.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newRack.id
                                                                selectedPartitionIsVertical = false
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedShoeRack(partId, newElevationYMm = clampedY, targetXMm = clampedX)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                } else if (draggedUnitType == DraggedUnitType.CABINET_DOORS) {
                                                    val partId = activeDragPartitionId
                                                    if (partId == null) {
                                                        if (isInsideCloset) {
                                                            val bay = target.findBayAtX(clampedX)
                                                            val updated = target.withAddedCabinetDoors(bayIndex = bay.index, targetYMm = clampedY)
                                                            val newDoors = updated.cabinetDoors.lastOrNull()
                                                            if (newDoors != null) {
                                                                activeDragPartitionId = newDoors.id
                                                                selectedContainerId = target.id
                                                                selectedPartitionId = newDoors.id
                                                                selectedPartitionIsVertical = false
                                                                updateRoom(room.withUpdatedContainer(target.id) { updated })
                                                            }
                                                        }
                                                    } else {
                                                        val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                            currentC.withMovedCabinetDoors(partId, newElevationYMm = clampedY, targetXMm = clampedX)
                                                        }
                                                        updateRoom(updatedRoom)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDragUnitEnd = {
                                        if (isDraggingUnit) {
                                            val currentWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                            val target = activeContainer
                                                ?: room.containers.firstOrNull { it.wall == currentWall }
                                                ?: room.containers.firstOrNull()

                                            when (draggedUnitType) {
                                                DraggedUnitType.CARCASS -> {
                                                    val fractionX = (dragScreenOffset.x / viewportWidthPx).coerceIn(0.05f, 0.95f)
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
                                                    isEditingContainerInPanel = false
                                                    isEditingPartitionInPanel = false
                                                    coroutineScope.launch {
                                                        messageHostState.showSnackbar(
                                                            "${s.containerAddedToast} ${wallDisplayName(targetWall, appLanguage, splitDoorWall, isD)}",
                                                        )
                                                    }
                                                }
                                                DraggedUnitType.VERTICAL_DIVIDER, DraggedUnitType.HORIZONTAL_SHELF, DraggedUnitType.DRAWER, DraggedUnitType.DRAWER_NOTCH, DraggedUnitType.CLOTHES_RAIL, DraggedUnitType.VERTICAL_POLE, DraggedUnitType.SHOE_RACK, DraggedUnitType.CABINET_DOORS -> {
                                                    val partId = activeDragPartitionId
                                                    if (partId != null && target != null) {
                                                        selectedContainerId = target.id
                                                        selectedPartitionId = partId
                                                        selectedPartitionIsVertical = (draggedUnitType == DraggedUnitType.VERTICAL_DIVIDER || draggedUnitType == DraggedUnitType.VERTICAL_POLE)
                                                        coroutineScope.launch {
                                                            messageHostState.showSnackbar(s.partitionAddedToast)
                                                        }
                                                    }
                                                    showUnitsColumn = false
                                                    isEditingContainerInPanel = false
                                                    isEditingPartitionInPanel = false
                                                }
                                            }
                                            activeDragPartitionId = null
                                            isDraggingUnit = false
                                        }
                                    },
                                    onDragUnitCancel = {
                                        val partId = activeDragPartitionId
                                        if (partId != null) {
                                            val currentWall = wallIndexFromStep(wallStep.intValue, splitDoorWall)
                                            val target = activeContainer
                                                ?: room.containers.firstOrNull { it.wall == currentWall }
                                                ?: room.containers.firstOrNull()
                                            if (target != null) {
                                                val updatedRoom = room.withUpdatedContainer(target.id) { currentC ->
                                                    currentC.withRemovedPartition(partId)
                                                }
                                                updateRoom(updatedRoom)
                                            }
                                        }
                                        activeDragPartitionId = null
                                        selectedPartitionId = null
                                        isDraggingUnit = false
                                        showUnitsColumn = false
                                        isEditingContainerInPanel = false
                                        isEditingPartitionInPanel = false
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
                                            if (container.verticalPoles.any { it.id == partitionId }) {
                                                val source = container.verticalPoles.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val bay = container.getBays().getOrElse(source.bayIndex) { container.getBays().last() }
                                                val curX = source.effectiveX(container)
                                                val newX = (curX + 120f).coerceAtMost(bay.endXMm - source.diameterMm * 0.5f)
                                                val newPole = source.copy(
                                                    id = "pole_${System.currentTimeMillis()}_${container.verticalPoles.size + 1}",
                                                    offsetXMm = newX,
                                                )
                                                container.copy(verticalPoles = container.verticalPoles + newPole)
                                            } else if (isVertical) {
                                                val source = container.dividers.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val newOff = (source.offsetXMm + source.thicknessMm + 80f).coerceAtMost((container.interiorWidthMm - source.thicknessMm).coerceAtLeast(0f))
                                                val newDiv = source.copy(
                                                    id = "div_${System.currentTimeMillis()}_${container.dividers.size + 1}",
                                                    offsetXMm = newOff,
                                                )
                                                container.copy(dividers = container.dividers + newDiv)
                                            } else if (container.drawers.any { it.id == partitionId }) {
                                                val source = container.drawers.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val newElev = (source.elevationYMm + source.heightMm).coerceAtMost((container.interiorHeightMm - source.heightMm).coerceAtLeast(0f))
                                                val newDrawer = source.copy(
                                                    id = "drawer_${System.currentTimeMillis()}_${container.drawers.size + 1}",
                                                    elevationYMm = newElev,
                                                )
                                                container.copy(drawers = container.drawers + newDrawer)
                                            } else if (container.clothesRails.any { it.id == partitionId }) {
                                                val source = container.clothesRails.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val newElev = (source.elevationYMm + 120f).coerceAtMost((container.interiorHeightMm - source.diameterMm).coerceAtLeast(0f))
                                                val newRail = source.copy(
                                                    id = "rail_${System.currentTimeMillis()}_${container.clothesRails.size + 1}",
                                                    elevationYMm = newElev,
                                                )
                                                container.copy(clothesRails = container.clothesRails + newRail)
                                            } else if (container.shoeRacks.any { it.id == partitionId }) {
                                                val source = container.shoeRacks.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val targetY = source.elevationYMm + source.totalVerticalSpanMm(container.interiorDepthMm) + 30f
                                                val newElev = targetY.coerceIn(0f, source.maxElevation(container.interiorHeightMm, container.interiorDepthMm))
                                                val newRack = source.copy(
                                                    id = "shoerack_${System.currentTimeMillis()}_${container.shoeRacks.size + 1}",
                                                    elevationYMm = newElev,
                                                )
                                                container.copy(shoeRacks = container.shoeRacks + newRack)
                                            } else if (container.cabinetDoors.any { it.id == partitionId }) {
                                                val source = container.cabinetDoors.firstOrNull { it.id == partitionId } ?: return@withUpdatedContainer container
                                                val targetY = source.elevationYMm + source.heightMm + 10f
                                                val newElev = targetY.coerceIn(0f, (container.interiorHeightMm - source.heightMm).coerceAtLeast(0f))
                                                val newDoors = source.copy(
                                                    id = "doors_${System.currentTimeMillis()}_${container.cabinetDoors.size + 1}",
                                                    elevationYMm = newElev,
                                                )
                                                container.copy(cabinetDoors = container.cabinetDoors + newDoors)
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
                                    onApplyMelamineToAll = { textureId, horizontal ->
                                        updateRoom(room.withAppliedMelamineToAllContainers(textureId, horizontal))
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(s.finishAppliedToAllToast)
                                        }
                                    },
                                    onClose = {
                                        showUnitsColumn = false
                                        isEditingContainerInPanel = false
                                        isEditingPartitionInPanel = false
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
                            if (isDraggingUnit && activeDragPartitionId == null) {
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
                                        .size(previewSizeDp)
                                        .graphicsLayer {
                                            alpha = 0.92f
                                            scaleX = 0.95f
                                            scaleY = 0.95f
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (draggedUnitType) {
                                        DraggedUnitType.CARCASS -> ClosetContainer3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.VERTICAL_DIVIDER -> VerticalDivider3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.HORIZONTAL_SHELF -> HorizontalShelf3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.DRAWER -> Drawer3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.DRAWER_NOTCH -> DrawerNotch3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.CLOTHES_RAIL -> ClothesRail3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.VERTICAL_POLE -> VerticalPole3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.SHOE_RACK -> ShoeRack3DGraphic(modifier = Modifier.fillMaxSize())
                                        DraggedUnitType.CABINET_DOORS -> CabinetDoors3DGraphic(modifier = Modifier.fillMaxSize())
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
                                        runCatching {
                                            projectStorage.saveProject(
                                                id = null,
                                                name = nameDraft.trim().ifEmpty { "${s.copyOfPrefix} $currentProjectName" },
                                                room = room,
                                            )
                                        }.onSuccess { saved ->
                                            savedProjects = projectStorage.loadAllProjects()
                                            currentProjectId = saved.id
                                            currentProjectName = saved.name
                                            showSaveDialog = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar("${s.savedAsNewToast}: ${saved.name}")
                                            }
                                        }.onFailure {
                                            coroutineScope.launch { messageHostState.showSnackbar(s.projectStorageError) }
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
                                    runCatching {
                                        projectStorage.saveProject(
                                            id = currentProjectId,
                                            name = nameDraft,
                                            room = room,
                                        )
                                    }.onSuccess { saved ->
                                        savedProjects = projectStorage.loadAllProjects()
                                        currentProjectId = saved.id
                                        currentProjectName = saved.name
                                        showSaveDialog = false
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar("${s.projectSavedToast}: ${saved.name}")
                                        }
                                    }.onFailure {
                                        coroutineScope.launch { messageHostState.showSnackbar(s.projectStorageError) }
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
                                    runCatching {
                                        projectStorage.saveProject(
                                            id = null,
                                            name = newNameDraft.trim().ifEmpty { "${s.defaultProjectPrefix} $nextIndex" },
                                            room = freshRoom,
                                        )
                                    }.onSuccess { created ->
                                        savedProjects = projectStorage.loadAllProjects()
                                        currentProjectId = created.id
                                        currentProjectName = created.name
                                        updateRoom(freshRoom)
                                        showCreateNewDialog = false
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar("${s.projectCreatedToast}: ${created.name}")
                                        }
                                    }.onFailure {
                                        coroutineScope.launch { messageHostState.showSnackbar(s.projectStorageError) }
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

                if (showMaterialsDialog) {
                    MaterialsAndTexturesDialog(
                        room = room,
                        s = s,
                        onUpdateRoom = { updateRoom(it) },
                        onDismiss = { showMaterialsDialog = false },
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
            if (drawerState.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 300.dp)
                        .clickable { coroutineScope.launch { drawerState.close() } },
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
    showDoors: Boolean = true,
    isPanelOpen: Boolean = false,
    panelWidthPx: Float = 0f,
    isContainerMovementLocked: Boolean = false,
    onOrbit: (Float, Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onTap: (Offset, Float, Float) -> Unit = { _, _, _ -> },
    canDragPartitionAt: (Offset, Float, Float) -> Boolean = { _, _, _ -> false },
    onDragPartition: (Offset, Float, Float) -> Unit = { _, _, _ -> },
    canDragContainerAt: (Offset, Float, Float) -> Boolean = { _, _, _ -> false },
    onDragContainer: (Offset, Float, Float) -> Unit = { _, _, _ -> },
) {
    Box(
        modifier = modifier
            .pointerInput(selectedPartitionId, selectedContainerId, isPanelOpen, panelWidthPx, isContainerMovementLocked) {
                detectRoomInspectionGestures(
                    onOrbit = onOrbit,
                    onPan = onPan,
                    onZoom = onZoom,
                    onTap = onTap,
                    canDragPartitionAt = canDragPartitionAt,
                    onDragPartition = onDragPartition,
                    canDragContainerAt = canDragContainerAt,
                    onDragContainer = onDragContainer,
                    isContainerMovementLocked = isContainerMovementLocked,
                    isPanelOpen = isPanelOpen,
                    panelWidthPx = panelWidthPx,
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
            showDoors = showDoors,
        )
    }
}

@Composable
private fun RightSideUnitsAndInspectorColumn(
    room: RoomSpec,
    s: UiStrings,
    activeContainer: ClosetContainerSpec?,
    isEditingContainer: Boolean,
    isEditingPartition: Boolean = false,
    selectedPartitionId: String? = null,
    selectedPartitionIsVertical: Boolean = true,
    isDragging: Boolean = false,
    onToggleEditMode: (Boolean) -> Unit,
    onTogglePartitionEditMode: (Boolean) -> Unit = {},
    onSelectContainer: (ClosetContainerSpec) -> Unit,
    onAddContainerOnCurrentWall: (dropFractionX: Float?) -> Unit,
    onAddDivider: (dropFractionX: Float?) -> Unit = {},
    onAddShelf: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
    onAddDrawer: (dropFractionX: Float?, dropFractionY: Float?, frontType: DrawerFrontType) -> Unit = { _, _, _ -> },
    onAddClothesRail: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
    onAddVerticalPole: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
    onAddShoeRack: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
    onAddCabinetDoors: (dropFractionX: Float?, dropFractionY: Float?) -> Unit = { _, _ -> },
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
    onApplyMelamineToAll: (String?, Boolean) -> Unit = { _, _ -> },
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
            val showModuleInspector = isEditingPartition && selectedPartitionId != null && activeContainer != null
            val showContainerEditor = isEditingContainer && activeContainer != null

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
                        showModuleInspector -> s.moduleTitle
                        showContainerEditor -> s.editContainerTitle
                        else -> s.unitsPanelTitle
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showContainerEditor || showModuleInspector) {
                        TextButton(onClick = {
                            onToggleEditMode(false)
                            onTogglePartitionEditMode(false)
                        }) {
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
                            onTogglePartitionEditMode(false)
                            onToggleEditMode(false)
                            onClosePartition()
                            onClose()
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

            if (showModuleInspector) {
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

                    // Melamine Material & Grain Section
                    ContainerMelamineSection(
                        container = clamped,
                        room = room,
                        s = s,
                        onUpdateContainer = { updated ->
                            onUpdateContainer(clamped.id) { updated }
                        },
                        onApplyToAll = {
                            val activeTex = clamped.materialTextureId ?: room.defaultMelamineTextureId
                            onApplyMelamineToAll(activeTex, clamped.isGrainHorizontal)
                        },
                    )

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
                var drawerRootOffset by remember { mutableStateOf(Offset.Zero) }
                var drawerNotchRootOffset by remember { mutableStateOf(Offset.Zero) }
                var clothesRailRootOffset by remember { mutableStateOf(Offset.Zero) }
                var verticalPoleRootOffset by remember { mutableStateOf(Offset.Zero) }
                var shoeRackRootOffset by remember { mutableStateOf(Offset.Zero) }
                var cabinetDoorsRootOffset by remember { mutableStateOf(Offset.Zero) }
                val thumbnailSlotHeight = 96.dp
                val thumbnailHeight = 84.dp
                val thumbnailShape = RoundedCornerShape(10.dp)

                fun thumbnailModifier(widthFraction: Float): Modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(thumbnailHeight)
                    .clip(thumbnailShape)
                    .background(Color(0xFFFAF8F5), thumbnailShape)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // 1st Unit: Closet Container (Carcass)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                carcassRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.CARCASS, carcassRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddContainerOnCurrentWall(null) },
                                )
                            },
                    ) {
                        ClosetContainer3DGraphic(
                            modifier = thumbnailModifier(0.78f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 2nd Unit: Vertical Divider (División Vertical matching screenshot media_1793474667205_def219d6.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                dividerRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.VERTICAL_DIVIDER, dividerRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddDivider(null) },
                                )
                            },
                    ) {
                        VerticalDivider3DGraphic(
                            modifier = thumbnailModifier(0.68f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 3rd Unit: Horizontal Shelf (División Horizontal matching screenshot media_1793474667205_def219d6.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                shelfRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.HORIZONTAL_SHELF, shelfRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddShelf(null, null) },
                                )
                            },
                    ) {
                        HorizontalShelf3DGraphic(
                            modifier = thumbnailModifier(0.72f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 4th Unit: Drawer with handle (Cajón con tirador matching screenshot media_1793498475563_a2bc7a9c.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                drawerRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.DRAWER, drawerRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddDrawer(null, null, DrawerFrontType.HANDLE) },
                                )
                            },
                    ) {
                        Drawer3DGraphic(
                            modifier = thumbnailModifier(0.72f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 5th Unit: Drawer with finger pull / notch cutout (Cajón con uñero matching screenshot media_1793504581483_389611b8.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                drawerNotchRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.DRAWER_NOTCH, drawerNotchRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddDrawer(null, null, DrawerFrontType.NOTCH) },
                                )
                            },
                    ) {
                        DrawerNotch3DGraphic(
                            modifier = thumbnailModifier(0.72f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 6th Unit: Clothes Rail / Hanger Rod ("Tubo colgador", matching screenshot media_1793508080702_50ab7e38.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                clothesRailRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.CLOTHES_RAIL, clothesRailRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddClothesRail(null, null) },
                                )
                            },
                    ) {
                        ClothesRail3DGraphic(
                            modifier = thumbnailModifier(0.74f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 7th Unit: Vertical Pole / Post ("Tubo vertical", matching screenshot media_1793543136114_789256b4.png)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                verticalPoleRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.VERTICAL_POLE, verticalPoleRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddVerticalPole(null, null) },
                                )
                            },
                    ) {
                        VerticalPole3DGraphic(
                            modifier = thumbnailModifier(0.42f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 8th Unit: Inclined Shoe Rack with Solid Wood Retaining Lip ("Zapatera inclinada")
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                shoeRackRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.SHOE_RACK, shoeRackRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddShoeRack(null, null) },
                                )
                            },
                    ) {
                        ShoeRack3DGraphic(
                            modifier = thumbnailModifier(0.74f),
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

                    // 9th Unit: Cabinet Doors ("Puertas de armario")
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(thumbnailSlotHeight)
                            .onGloballyPositioned { coordinates ->
                                cabinetDoorsRootOffset = coordinates.positionInRoot()
                            }
                            .pointerInput(Unit) {
                                detectCatalogUnitDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(DraggedUnitType.CABINET_DOORS, cabinetDoorsRootOffset + localOffset)
                                    },
                                    onDrag = { dragAmount ->
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = { onDragUnitEnd() },
                                    onDragCancel = { onDragUnitCancel() },
                                    onTap = { onAddCabinetDoors(null, null) },
                                )
                            },
                    ) {
                        CabinetDoors3DGraphic(
                            modifier = thumbnailModifier(0.74f),
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
private fun Drawer3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val frontWood = Color(0xFFE6C299)
        val sideWood = Color(0xFFAA8157)
        val innerBoxWood = Color(0xFFD6B28A)
        val bottomFloor = Color(0xFF7E7266)
        val handleChrome = Color(0xFFE8EEF5)
        val handleStroke = Color(0xFF6B7280)
        val lineStroke = Color(0xFF8C6742).copy(alpha = 0.5f)

        val drawerW = w * 0.80f
        val frontH = (h * 0.38f).coerceAtLeast(22f)
        val depthX = w * 0.14f
        val depthY = h * 0.28f

        val x0 = (w - drawerW - depthX) * 0.5f
        val y0 = (h - frontH + depthY) * 0.5f + 8f

        // 1. Box Bottom Floor
        val floorPath = Path().apply {
            moveTo(x0 + 10f, y0)
            lineTo(x0 + drawerW - 10f, y0)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + 10f + depthX, y0 - depthY + 6f)
            close()
        }
        drawPath(floorPath, color = bottomFloor)

        // 2. Box Back Panel
        val backPath = Path().apply {
            moveTo(x0 + 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + 10f + depthX, y0 - depthY - frontH * 0.40f)
            close()
        }
        drawPath(backPath, color = innerBoxWood)
        drawPath(backPath, color = lineStroke, style = Stroke(width = 1f))

        // 3. Box Left Inner Side
        val leftPath = Path().apply {
            moveTo(x0 + 10f, y0)
            lineTo(x0 + 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + 10f, y0 - frontH * 0.40f)
            close()
        }
        drawPath(leftPath, color = sideWood)
        drawPath(leftPath, color = lineStroke, style = Stroke(width = 1f))

        // 4. Box Right Inner Side
        val rightPath = Path().apply {
            moveTo(x0 + drawerW - 10f, y0)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + drawerW - 10f, y0 - frontH * 0.40f)
            close()
        }
        drawPath(rightPath, color = sideWood)
        drawPath(rightPath, color = lineStroke, style = Stroke(width = 1f))

        // 5. Drawer Front Face
        drawRect(
            color = frontWood,
            topLeft = Offset(x0, y0),
            size = androidx.compose.ui.geometry.Size(drawerW, frontH),
        )
        drawRect(
            color = lineStroke,
            topLeft = Offset(x0, y0),
            size = androidx.compose.ui.geometry.Size(drawerW, frontH),
            style = Stroke(width = 1.2f),
        )

        // 6. Right edge of front face
        val frontRightPath = Path().apply {
            moveTo(x0 + drawerW, y0)
            lineTo(x0 + drawerW + 6f, y0 - 5f)
            lineTo(x0 + drawerW + 6f, y0 - 5f + frontH)
            lineTo(x0 + drawerW, y0 + frontH)
            close()
        }
        drawPath(frontRightPath, color = sideWood)
        drawPath(frontRightPath, color = lineStroke, style = Stroke(width = 1f))

        // 7. Chrome Handle on Center of Front Face
        val handleW = drawerW * 0.30f
        val handleH = 5f
        val hX = x0 + (drawerW - handleW) * 0.5f
        val hY = y0 + (frontH - handleH) * 0.5f
        drawRoundRect(
            color = handleChrome,
            topLeft = Offset(hX, hY),
            size = androidx.compose.ui.geometry.Size(handleW, handleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
        drawRoundRect(
            color = handleStroke,
            topLeft = Offset(hX, hY),
            size = androidx.compose.ui.geometry.Size(handleW, handleH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            style = Stroke(width = 1f),
        )
    }
}

@Composable
private fun DrawerNotch3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val frontWood = Color(0xFFE6C299)
        val sideWood = Color(0xFFAA8157)
        val innerBoxWood = Color(0xFFD6B28A)
        val bottomFloor = Color(0xFF7E7266)
        val lineStroke = Color(0xFF8C6742).copy(alpha = 0.5f)

        val drawerW = w * 0.80f
        val frontH = (h * 0.38f).coerceAtLeast(22f)
        val depthX = w * 0.14f
        val depthY = h * 0.28f

        val x0 = (w - drawerW - depthX) * 0.5f
        val y0 = (h - frontH + depthY) * 0.5f + 8f

        // 1. Box Bottom Floor
        val floorPath = Path().apply {
            moveTo(x0 + 10f, y0)
            lineTo(x0 + drawerW - 10f, y0)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + 10f + depthX, y0 - depthY + 6f)
            close()
        }
        drawPath(floorPath, color = bottomFloor)

        // 2. Box Back Panel
        val backPath = Path().apply {
            moveTo(x0 + 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + 10f + depthX, y0 - depthY - frontH * 0.40f)
            close()
        }
        drawPath(backPath, color = innerBoxWood)
        drawPath(backPath, color = lineStroke, style = Stroke(width = 1f))

        // 3. Box Left Inner Side
        val leftPath = Path().apply {
            moveTo(x0 + 10f, y0)
            lineTo(x0 + 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + 10f, y0 - frontH * 0.40f)
            close()
        }
        drawPath(leftPath, color = sideWood)
        drawPath(leftPath, color = lineStroke, style = Stroke(width = 1f))

        // 4. Box Right Inner Side
        val rightPath = Path().apply {
            moveTo(x0 + drawerW - 10f, y0)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY + 6f)
            lineTo(x0 + drawerW - 10f + depthX, y0 - depthY - frontH * 0.40f)
            lineTo(x0 + drawerW - 10f, y0 - frontH * 0.40f)
            close()
        }
        drawPath(rightPath, color = sideWood)
        drawPath(rightPath, color = lineStroke, style = Stroke(width = 1f))

        // 5. Drawer Front Face with Central Finger Pull Notch (matching user screenshot media_1793504581483_389611b8.png)
        val notchW = drawerW * 0.36f
        val notchH = frontH * 0.35f
        val notchX0 = x0 + (drawerW - notchW) * 0.5f
        val notchX1 = notchX0 + notchW

        val frontPath = Path().apply {
            moveTo(x0, y0 + frontH)
            lineTo(x0 + drawerW, y0 + frontH)
            lineTo(x0 + drawerW, y0)
            lineTo(notchX1, y0)
            lineTo(notchX1, y0 + notchH)
            lineTo(notchX0, y0 + notchH)
            lineTo(notchX0, y0)
            lineTo(x0, y0)
            close()
        }
        drawPath(frontPath, color = frontWood)
        drawPath(frontPath, color = lineStroke, style = Stroke(width = 1.2f))

        // 6. Right edge of front face
        val frontRightPath = Path().apply {
            moveTo(x0 + drawerW, y0)
            lineTo(x0 + drawerW + 6f, y0 - 5f)
            lineTo(x0 + drawerW + 6f, y0 - 5f + frontH)
            lineTo(x0 + drawerW, y0 + frontH)
            close()
        }
        drawPath(frontRightPath, color = sideWood)
        drawPath(frontRightPath, color = lineStroke, style = Stroke(width = 1f))
    }
}

@Composable
private fun ClothesRail3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val chromeBase = Color(0xFFC4C4C4)
        val chromeHighlight = Color(0xFFFFFFFF)
        val chromeShadow = Color(0xFF757575)
        val bracketColor = Color(0xFF8E8E8E)
        val lineStroke = Color(0xFF555555)

        val railW = w * 0.85f
        val x0 = (w - railW) * 0.5f
        val x1 = x0 + railW
        val yCenter = h * 0.5f

        val rodRadiusY = (h * 0.12f).coerceIn(6f, 12f)
        val bracketRadiusY = rodRadiusY * 1.8f
        val bracketW = 12f

        // 1. Left Mounting Flange Bracket
        val leftBracketPath = Path().apply {
            addOval(
                Rect(
                    left = x0,
                    top = yCenter - bracketRadiusY,
                    right = x0 + bracketW,
                    bottom = yCenter + bracketRadiusY,
                )
            )
        }
        drawPath(leftBracketPath, color = bracketColor)
        drawPath(leftBracketPath, color = lineStroke, style = Stroke(width = 1f))

        // 2. Right Mounting Flange Bracket
        val rightBracketPath = Path().apply {
            addOval(
                Rect(
                    left = x1 - bracketW,
                    top = yCenter - bracketRadiusY,
                    right = x1,
                    bottom = yCenter + bracketRadiusY,
                )
            )
        }
        drawPath(rightBracketPath, color = bracketColor)
        drawPath(rightBracketPath, color = lineStroke, style = Stroke(width = 1f))

        // 3. Central Chrome Rod Body (gradient shading from top highlight to bottom shadow)
        val rodRect = Rect(
            left = x0 + bracketW * 0.6f,
            top = yCenter - rodRadiusY,
            right = x1 - bracketW * 0.6f,
            bottom = yCenter + rodRadiusY,
        )
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    chromeShadow,
                    chromeHighlight,
                    chromeBase,
                    chromeShadow,
                ),
                startY = rodRect.top,
                endY = rodRect.bottom,
            ),
            topLeft = Offset(rodRect.left, rodRect.top),
            size = Size(rodRect.width, rodRect.height),
        )
        // Outer stroke of rod
        drawRect(
            color = lineStroke.copy(alpha = 0.7f),
            topLeft = Offset(rodRect.left, rodRect.top),
            size = Size(rodRect.width, rodRect.height),
            style = Stroke(width = 1f),
        )
        // Specular highlight line along top edge
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(rodRect.left + 2f, yCenter - rodRadiusY * 0.35f),
            end = Offset(rodRect.right - 2f, yCenter - rodRadiusY * 0.35f),
            strokeWidth = 1.8f,
        )
    }
}

@Composable
private fun VerticalPole3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val chromeBase = Color(0xFFC4C4C4)
        val chromeHighlight = Color(0xFFFFFFFF)
        val chromeShadow = Color(0xFF757575)
        val bracketColor = Color(0xFF8E8E8E)
        val lineStroke = Color(0xFF555555)

        val poleH = h * 0.88f
        val y0 = (h - poleH) * 0.5f
        val y1 = y0 + poleH
        val xCenter = w * 0.5f

        val poleRadiusX = (w * 0.045f).coerceIn(4f, 8f)
        val bracketRadiusX = poleRadiusX * 2.2f
        val bracketH = 8f

        // 1. Top Mounting Flange Bracket
        val topBracketPath = Path().apply {
            addOval(
                Rect(
                    left = xCenter - bracketRadiusX,
                    top = y0,
                    right = xCenter + bracketRadiusX,
                    bottom = y0 + bracketH,
                )
            )
        }
        drawPath(topBracketPath, color = bracketColor)
        drawPath(topBracketPath, color = lineStroke, style = Stroke(width = 1f))

        // 2. Bottom Mounting Flange Bracket
        val bottomBracketPath = Path().apply {
            addOval(
                Rect(
                    left = xCenter - bracketRadiusX,
                    top = y1 - bracketH,
                    right = xCenter + bracketRadiusX,
                    bottom = y1,
                )
            )
        }
        drawPath(bottomBracketPath, color = bracketColor)
        drawPath(bottomBracketPath, color = lineStroke, style = Stroke(width = 1f))

        // 3. Central Chrome Pole Body (horizontal gradient shading from shadow to highlight to shadow)
        val poleRect = Rect(
            left = xCenter - poleRadiusX,
            top = y0 + bracketH * 0.6f,
            right = xCenter + poleRadiusX,
            bottom = y1 - bracketH * 0.6f,
        )
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(
                    chromeShadow,
                    chromeHighlight,
                    chromeBase,
                    chromeShadow,
                ),
                startX = poleRect.left,
                endX = poleRect.right,
            ),
            topLeft = Offset(poleRect.left, poleRect.top),
            size = Size(poleRect.width, poleRect.height),
        )
        // Outer stroke
        drawRect(
            color = lineStroke.copy(alpha = 0.7f),
            topLeft = Offset(poleRect.left, poleRect.top),
            size = Size(poleRect.width, poleRect.height),
            style = Stroke(width = 1f),
        )
        // Specular highlight vertical line along left-center
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(xCenter - poleRadiusX * 0.35f, poleRect.top + 2f),
            end = Offset(xCenter - poleRadiusX * 0.35f, poleRect.bottom - 2f),
            strokeWidth = 1.8f,
        )
    }
}

@Composable
private fun ShoeRack3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val woodTop = Color(0xFFE8C8A3)
        val woodFront = Color(0xFFD6AD80)
        val woodSide = Color(0xFFAA8157)
        val lipFront = Color(0xFFE0BB90)
        val lipTop = Color(0xFFF4D8B8)
        val lipSide = Color(0xFFAA8157)
        val lineStroke = Color(0xFF8B6B4C)

        // 3D Perspective of an inclined shelf board:
        val pBackLeft = Offset(w * 0.22f, h * 0.28f)
        val pBackRight = Offset(w * 0.88f, h * 0.38f)
        val pFrontRight = Offset(w * 0.76f, h * 0.72f)
        val pFrontLeft = Offset(w * 0.10f, h * 0.62f)

        val shelfThickY = 12f

        // Shelf Bottom & Side thickness
        val shelfLeftEdgePath = Path().apply {
            moveTo(pBackLeft.x, pBackLeft.y)
            lineTo(pFrontLeft.x, pFrontLeft.y)
            lineTo(pFrontLeft.x, pFrontLeft.y + shelfThickY)
            lineTo(pBackLeft.x, pBackLeft.y + shelfThickY)
            close()
        }
        drawPath(shelfLeftEdgePath, color = woodSide)
        drawPath(shelfLeftEdgePath, color = lineStroke, style = Stroke(width = 1f))

        // Shelf Slanted Top Surface
        val shelfTopPath = Path().apply {
            moveTo(pBackLeft.x, pBackLeft.y)
            lineTo(pBackRight.x, pBackRight.y)
            lineTo(pFrontRight.x, pFrontRight.y)
            lineTo(pFrontLeft.x, pFrontLeft.y)
            close()
        }
        drawPath(shelfTopPath, color = woodTop)
        drawPath(shelfTopPath, color = lineStroke, style = Stroke(width = 1f))

        // Solid Wood Front Retaining Lip (Zócalo de madera sólida que retiene los zapatos)
        val lipHeight = 24f
        val lipThickX = 6f
        val lipThickY = 4f

        // Lip Front Face
        val lipFrontPath = Path().apply {
            moveTo(pFrontLeft.x - lipThickX, pFrontLeft.y + shelfThickY + lipThickY)
            lineTo(pFrontRight.x + lipThickX, pFrontRight.y + shelfThickY + lipThickY)
            lineTo(pFrontRight.x + lipThickX, pFrontRight.y - lipHeight + lipThickY)
            lineTo(pFrontLeft.x - lipThickX, pFrontLeft.y - lipHeight + lipThickY)
            close()
        }
        drawPath(lipFrontPath, color = lipFront)
        drawPath(lipFrontPath, color = lineStroke, style = Stroke(width = 1.2f))

        // Lip Top Rim
        val lipTopPath = Path().apply {
            moveTo(pFrontLeft.x - lipThickX, pFrontLeft.y - lipHeight + lipThickY)
            lineTo(pFrontRight.x + lipThickX, pFrontRight.y - lipHeight + lipThickY)
            lineTo(pFrontRight.x, pFrontRight.y - lipHeight)
            lineTo(pFrontLeft.x, pFrontLeft.y - lipHeight)
            close()
        }
        drawPath(lipTopPath, color = lipTop)
        drawPath(lipTopPath, color = lineStroke, style = Stroke(width = 1f))

        // Lip Left Edge
        val lipLeftPath = Path().apply {
            moveTo(pFrontLeft.x - lipThickX, pFrontLeft.y + shelfThickY + lipThickY)
            lineTo(pFrontLeft.x, pFrontLeft.y + shelfThickY)
            lineTo(pFrontLeft.x, pFrontLeft.y - lipHeight)
            lineTo(pFrontLeft.x - lipThickX, pFrontLeft.y - lipHeight + lipThickY)
            close()
        }
        drawPath(lipLeftPath, color = lipSide)
        drawPath(lipLeftPath, color = lineStroke, style = Stroke(width = 1f))
    }
}

@Composable
private fun CabinetDoors3DGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val woodTop = Color(0xFFE8C8A3)
        val woodFront = Color(0xFFD6AD80)
        val woodFrontRight = Color(0xFFCE9F70)
        val woodSide = Color(0xFFAA8157)
        val lineStroke = Color(0xFF8B6B4C)
        val chromeColor = Color(0xFFE0E0E0)
        val chromeHighlight = Color(0xFFFFFFFF)
        val chromeShadow = Color(0xFF9E9E9E)

        val fLeft = w * 0.18f
        val fRight = w * 0.88f
        val fTop = h * 0.15f
        val fBottom = h * 0.88f
        val depthX = w * 0.12f
        val depthY = h * 0.08f

        // Top depth face
        val topPath = Path().apply {
            moveTo(fLeft, fTop)
            lineTo(fLeft - depthX, fTop - depthY)
            lineTo(fRight - depthX, fTop - depthY)
            lineTo(fRight, fTop)
            close()
        }
        drawPath(topPath, color = woodTop)
        drawPath(topPath, color = lineStroke, style = Stroke(width = 1f))

        // Left depth face
        val leftPath = Path().apply {
            moveTo(fLeft, fTop)
            lineTo(fLeft - depthX, fTop - depthY)
            lineTo(fLeft - depthX, fBottom - depthY)
            lineTo(fLeft, fBottom)
            close()
        }
        drawPath(leftPath, color = woodSide)
        drawPath(leftPath, color = lineStroke, style = Stroke(width = 1f))

        // Center seam gap
        val fWidth = fRight - fLeft
        val seamX = fLeft + fWidth * 0.5f
        val gap = 2.5f

        // Left door leaf
        val leftDoorPath = Path().apply {
            moveTo(fLeft, fTop)
            lineTo(seamX - gap, fTop)
            lineTo(seamX - gap, fBottom)
            lineTo(fLeft, fBottom)
            close()
        }
        drawPath(leftDoorPath, color = woodFront)
        drawPath(leftDoorPath, color = lineStroke, style = Stroke(width = 1.2f))

        // Right door leaf
        val rightDoorPath = Path().apply {
            moveTo(seamX + gap, fTop)
            lineTo(fRight, fTop)
            lineTo(fRight, fBottom)
            lineTo(seamX + gap, fBottom)
            close()
        }
        drawPath(rightDoorPath, color = woodFrontRight)
        drawPath(rightDoorPath, color = lineStroke, style = Stroke(width = 1.2f))

        // Center shadow line between doors
        drawLine(
            color = Color(0x66000000),
            start = Offset(seamX, fTop),
            end = Offset(seamX, fBottom),
            strokeWidth = gap * 1.5f,
        )

        // Chrome Vertical Handles in the center
        val handleH = (fBottom - fTop) * 0.28f
        val handleMidY = (fTop + fBottom) * 0.5f
        val handleY0 = handleMidY - handleH * 0.5f
        val handleY1 = handleMidY + handleH * 0.5f
        val handleW = 4f

        // Left handle (near right edge of left door)
        val hLeftX = seamX - gap - 10f
        drawRect(
            color = chromeShadow,
            topLeft = Offset(hLeftX - handleW * 0.5f + 1f, handleY0 + 1f),
            size = Size(handleW, handleH),
        )
        drawRect(
            color = chromeColor,
            topLeft = Offset(hLeftX - handleW * 0.5f, handleY0),
            size = Size(handleW, handleH),
        )
        drawLine(
            color = chromeHighlight,
            start = Offset(hLeftX, handleY0 + 2f),
            end = Offset(hLeftX, handleY1 - 2f),
            strokeWidth = 1.2f,
        )

        // Right handle (near left edge of right door)
        val hRightX = seamX + gap + 10f
        drawRect(
            color = chromeShadow,
            topLeft = Offset(hRightX - handleW * 0.5f + 1f, handleY0 + 1f),
            size = Size(handleW, handleH),
        )
        drawRect(
            color = chromeColor,
            topLeft = Offset(hRightX - handleW * 0.5f, handleY0),
            size = Size(handleW, handleH),
        )
        drawLine(
            color = chromeHighlight,
            start = Offset(hRightX, handleY0 + 2f),
            end = Offset(hRightX, handleY1 - 2f),
            strokeWidth = 1.2f,
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

        val pole = activeContainer.verticalPoles.firstOrNull { it.id == selectedPartitionId }
        val div = if (pole == null && selectedPartitionIsVertical) activeContainer.dividers.firstOrNull { it.id == selectedPartitionId } else null

        if (pole != null) {
            val bays = activeContainer.getBays()
            val bay = bays.getOrElse(pole.bayIndex) { bays.last() }
            val (compY0, compY1) = pole.effectiveYRange(activeContainer)
            val compHeight = (compY1 - compY0).coerceAtLeast(0f)

            // Width (bay span, read-only)
            ModuleDimensionRow(
                label = s.partitionWidthLabel,
                valueMm = bay.widthMm,
                unitSystem = unitSystem,
                enabled = false,
            ) {}

            // Height / Length (read-only)
            ModuleDimensionRow(
                label = s.partitionHeightLabel,
                valueMm = compHeight,
                unitSystem = unitSystem,
                enabled = false,
            ) {}

            // Diameter (editable: 15-50mm)
            ModuleDimensionRow(
                label = s.partitionDiameterLabel,
                valueMm = pole.diameterMm,
                unitSystem = unitSystem,
                stepMm = 1f,
                minMm = 15f,
                maxMm = 50f,
                enabled = true,
            ) { newDiameter ->
                onUpdateContainer(activeContainer.id) { c ->
                    c.withUpdatedVerticalPole(pole.id) { it.copy(diameterMm = newDiameter) }
                }
            }

            // Depth (editable, distance from back panel)
            val currentDepth = pole.effectiveDepth(activeContainer.interiorDepthMm)
            ModuleDimensionRow(
                label = s.partitionDepthLabel,
                valueMm = currentDepth,
                unitSystem = unitSystem,
                stepMm = step,
                minMm = 50f,
                maxMm = (activeContainer.interiorDepthMm - 50f).coerceAtLeast(50f),
                enabled = true,
            ) { newDepth ->
                onUpdateContainer(activeContainer.id) { c ->
                    c.withUpdatedVerticalPole(pole.id) { it.copy(depthOffsetMm = newDepth) }
                }
            }

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
            }

            val currentX = pole.effectiveX(activeContainer)
            val leftBound = bay.startXMm
            val rightBound = bay.endXMm
            val radius = pole.diameterMm * 0.5f

            val leftDist = (currentX - leftBound - radius).coerceAtLeast(0f)
            val rightDist = (rightBound - currentX - radius).coerceAtLeast(0f)
            val maxClearance = (bay.widthMm - pole.diameterMm).coerceAtLeast(0f)

            // Right distance
            ModuleDimensionRow(
                label = s.partitionRightLabel,
                valueMm = rightDist,
                unitSystem = unitSystem,
                stepMm = step,
                minMm = 0f,
                maxMm = maxClearance,
                enabled = true,
            ) { newRight ->
                val targetX = (rightBound - radius - newRight).coerceIn(leftBound + radius, rightBound - radius)
                onUpdateContainer(activeContainer.id) { c ->
                    c.withUpdatedVerticalPole(pole.id) { it.copy(offsetXMm = targetX) }
                }
            }

            // Left distance
            ModuleDimensionRow(
                label = s.partitionLeftLabel,
                valueMm = leftDist,
                unitSystem = unitSystem,
                stepMm = step,
                minMm = 0f,
                maxMm = maxClearance,
                enabled = true,
            ) { newLeft ->
                val targetX = (leftBound + radius + newLeft).coerceIn(leftBound + radius, rightBound - radius)
                onUpdateContainer(activeContainer.id) { c ->
                    c.withUpdatedVerticalPole(pole.id) { it.copy(offsetXMm = targetX) }
                }
            }
        } else if (div != null) {
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

                // Height (effective height of divider)
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = div.effectiveHeight(activeContainer.interiorHeightMm),
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Depth (editable, anchored to back panel)
                val currentDepth = div.effectiveDepth(activeContainer.interiorDepthMm)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = currentDepth,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 50f,
                    maxMm = 2000f,
                    enabled = true,
                ) { newDepth ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDivider(div.id) { it.copy(depthMm = newDepth) }
                    }
                }

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
            } else {
            val shelf = activeContainer.shelves.firstOrNull { it.id == selectedPartitionId }
            val drawer = if (shelf == null) activeContainer.drawers.firstOrNull { it.id == selectedPartitionId } else null
            val rail = if (shelf == null && drawer == null) activeContainer.clothesRails.firstOrNull { it.id == selectedPartitionId } else null
            val rack = if (shelf == null && drawer == null && rail == null) activeContainer.shoeRacks.firstOrNull { it.id == selectedPartitionId } else null
            val doors = if (shelf == null && drawer == null && rail == null && rack == null) activeContainer.cabinetDoors.firstOrNull { it.id == selectedPartitionId } else null
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

                // Depth (editable, anchored to back panel)
                val currentDepth = shelf.effectiveDepth(activeContainer.interiorDepthMm)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = currentDepth,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 50f,
                    maxMm = 2000f,
                    enabled = true,
                ) { newDepth ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShelf(shelf.id) { it.copy(depthMm = newDepth) }
                    }
                }

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
            } else if (drawer != null) {
                val bays = activeContainer.getBays()
                val bay = bays.getOrElse(drawer.bayIndex) { bays.last() }

                // Width (adapted from bay!)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = bay.widthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Height (box height: min 80mm, max 600mm)
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = drawer.heightMm,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 80f,
                    maxMm = 600f,
                    enabled = true,
                ) { newHeight ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDrawer(drawer.id) { it.copy(heightMm = newHeight) }
                    }
                }

                // Depth (editable, anchored to back panel)
                val currentDepth = drawer.effectiveDepth(activeContainer.interiorDepthMm)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = currentDepth,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 100f,
                    maxMm = 2000f,
                    enabled = true,
                ) { newDepth ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDrawer(drawer.id) { it.copy(depthMm = newDepth) }
                    }
                }

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

                val bottomDist = drawer.elevationYMm
                val topDist = (activeContainer.interiorHeightMm - (drawer.elevationYMm + drawer.heightMm)).coerceAtLeast(0f)
                val maxElev = (activeContainer.interiorHeightMm - drawer.heightMm).coerceAtLeast(0f)

                // Top distance
                ModuleDimensionRow(
                    label = s.partitionTopLabel,
                    valueMm = topDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newTop ->
                    val newElev = (activeContainer.interiorHeightMm - drawer.heightMm - newTop).coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDrawer(drawer.id) { it.copy(elevationYMm = newElev) }
                    }
                }

                // Bottom distance
                ModuleDimensionRow(
                    label = s.partitionBottomLabel,
                    valueMm = bottomDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newBottom ->
                    val newElev = newBottom.coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedDrawer(drawer.id) { it.copy(elevationYMm = newElev) }
                    }
                }
            } else if (rail != null) {
                val bays = activeContainer.getBays()
                val bay = bays.getOrElse(rail.bayIndex) { bays.last() }

                // Width (adapted from bay!)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = bay.widthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Diameter
                ModuleDimensionRow(
                    label = s.partitionDiameterLabel,
                    valueMm = rail.diameterMm,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 15f,
                    maxMm = 50f,
                    enabled = true,
                ) { newDiameter ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedClothesRail(rail.id) { it.copy(diameterMm = newDiameter) }
                    }
                }

                // Depth
                val currentDepth = rail.effectiveDepth(activeContainer.interiorDepthMm)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = currentDepth,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 50f,
                    maxMm = (activeContainer.interiorDepthMm - 50f).coerceAtLeast(50f),
                    enabled = true,
                ) { newDepth ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedClothesRail(rail.id) { it.copy(depthOffsetMm = newDepth) }
                    }
                }

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
                }

                val maxElev = (activeContainer.interiorHeightMm - rail.diameterMm).coerceAtLeast(0f)
                val bottomDist = rail.elevationYMm
                val topDist = (activeContainer.interiorHeightMm - rail.elevationYMm).coerceAtLeast(0f)

                // Top distance (clearance to roof)
                ModuleDimensionRow(
                    label = s.partitionTopLabel,
                    valueMm = topDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newTop ->
                    val newElev = (activeContainer.interiorHeightMm - newTop).coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedClothesRail(rail.id) { it.copy(elevationYMm = newElev) }
                    }
                }

                // Bottom distance
                ModuleDimensionRow(
                    label = s.partitionBottomLabel,
                    valueMm = bottomDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newBottom ->
                    val newElev = newBottom.coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedClothesRail(rail.id) { it.copy(elevationYMm = newElev) }
                    }
                }
            } else if (rack != null) {
                val bays = activeContainer.getBays()
                val bay = bays.getOrElse(rack.bayIndex) { bays.last() }

                // Width (adapted from bay!)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = bay.widthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Thickness
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = rack.thicknessMm,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 12f,
                    maxMm = 30f,
                    enabled = true,
                ) { newThick ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(thicknessMm = newThick) }
                    }
                }

                // Depth
                val currentDepth = rack.effectiveDepth(activeContainer.interiorDepthMm)
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = currentDepth,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 200f,
                    maxMm = activeContainer.interiorDepthMm,
                    enabled = true,
                ) { newDepth ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(depthMm = newDepth) }
                    }
                }

                // Tilt Angle
                ModuleDimensionRow(
                    label = s.shoeRackAngleLabel,
                    valueMm = rack.inclinationAngleDeg,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 5f,
                    maxMm = 35f,
                    enabled = true,
                ) { newAngle ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(inclinationAngleDeg = newAngle) }
                    }
                }

                // Solid Wood Lip Height
                ModuleDimensionRow(
                    label = s.shoeRackLipHeightLabel,
                    valueMm = rack.lipHeightMm,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 20f,
                    maxMm = 80f,
                    enabled = true,
                ) { newLipH ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(lipHeightMm = newLipH) }
                    }
                }

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
                }

                val maxElev = rack.maxElevation(activeContainer.interiorHeightMm, activeContainer.interiorDepthMm)
                val bottomDist = rack.elevationYMm
                val topDist = (activeContainer.interiorHeightMm - (rack.elevationYMm + rack.totalVerticalSpanMm(activeContainer.interiorDepthMm))).coerceAtLeast(0f)

                // Top distance (clearance to roof)
                ModuleDimensionRow(
                    label = s.partitionTopLabel,
                    valueMm = topDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newTop ->
                    val newElev = (activeContainer.interiorHeightMm - rack.totalVerticalSpanMm(activeContainer.interiorDepthMm) - newTop).coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(elevationYMm = newElev) }
                    }
                }

                // Bottom distance
                ModuleDimensionRow(
                    label = s.partitionBottomLabel,
                    valueMm = bottomDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newBottom ->
                    val newElev = newBottom.coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedShoeRack(rack.id) { it.copy(elevationYMm = newElev) }
                    }
                }
            } else if (doors != null) {
                val bays = activeContainer.getBays()
                val bay = bays.getOrElse(doors.bayIndex) { bays.last() }

                // Door Count Selection (1 or 2 doors)
                Text(
                    text = s.doorsCountLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = doors.doorCount == 1,
                        onClick = {
                            onUpdateContainer(activeContainer.id) { c ->
                                c.withUpdatedCabinetDoors(doors.id) { it.copy(doorCount = 1) }
                            }
                        },
                        label = { Text(s.oneDoorLabel) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = doors.doorCount == 2,
                        onClick = {
                            onUpdateContainer(activeContainer.id) { c ->
                                c.withUpdatedCabinetDoors(doors.id) { it.copy(doorCount = 2) }
                            }
                        },
                        label = { Text(s.twoDoorsLabel) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Compartment Coverage Selection
                val bayComps = activeContainer.getBayCompartments(doors.bayIndex)
                if (bayComps.size > 1) {
                    Text(
                        text = s.doorsCoverageLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )

                    val fullBottom = bayComps.first().bottomYMm
                    val fullTop = bayComps.last().topYMm
                    val fullH = (fullTop - fullBottom).coerceAtLeast(100f)
                    val isFullBaySelected = kotlin.math.abs(doors.elevationYMm - fullBottom) < 5f &&
                        kotlin.math.abs(doors.heightMm - fullH) < 5f

                    FilterChip(
                        selected = isFullBaySelected,
                        onClick = {
                            onUpdateContainer(activeContainer.id) { c ->
                                c.withUpdatedCabinetDoors(doors.id) {
                                    it.copy(elevationYMm = fullBottom, heightMm = fullH)
                            }
                            }
                        },
                        label = { Text(s.doorsFullBayLabel) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (bayComps.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val isLowerSelected = kotlin.math.abs(doors.elevationYMm - bayComps[0].bottomYMm) < 5f &&
                                kotlin.math.abs(doors.heightMm - bayComps[0].heightMm) < 5f
                            val isUpperSelected = kotlin.math.abs(doors.elevationYMm - bayComps[1].bottomYMm) < 5f &&
                                kotlin.math.abs(doors.heightMm - bayComps[1].heightMm) < 5f

                            FilterChip(
                                selected = isLowerSelected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = bayComps[0].bottomYMm, heightMm = bayComps[0].heightMm)
                                        }
                                    }
                            },
                                label = { Text(s.doorsLowerLabel) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = isUpperSelected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = bayComps[1].bottomYMm, heightMm = bayComps[1].heightMm)
                                        }
                                    }
                            },
                                label = { Text(s.doorsUpperLabel) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else if (bayComps.size == 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val isLowerSelected = kotlin.math.abs(doors.elevationYMm - bayComps[0].bottomYMm) < 5f &&
                                kotlin.math.abs(doors.heightMm - bayComps[0].heightMm) < 5f
                            val isMidSelected = kotlin.math.abs(doors.elevationYMm - bayComps[1].bottomYMm) < 5f &&
                                kotlin.math.abs(doors.heightMm - bayComps[1].heightMm) < 5f
                            val isUpperSelected = kotlin.math.abs(doors.elevationYMm - bayComps[2].bottomYMm) < 5f &&
                                kotlin.math.abs(doors.heightMm - bayComps[2].heightMm) < 5f

                            FilterChip(
                                selected = isLowerSelected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = bayComps[0].bottomYMm, heightMm = bayComps[0].heightMm)
                                        }
                                    }
                            },
                                label = { Text(s.doorsLowerLabel) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = isMidSelected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = bayComps[1].bottomYMm, heightMm = bayComps[1].heightMm)
                                        }
                                    }
                            },
                                label = { Text(s.doorsMiddleLabel) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = isUpperSelected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = bayComps[2].bottomYMm, heightMm = bayComps[2].heightMm)
                                        }
                                    }
                            },
                                label = { Text(s.doorsUpperLabel) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val span12Elev = bayComps[0].bottomYMm
                            val span12H = (bayComps[1].topYMm - bayComps[0].bottomYMm).coerceAtLeast(100f)
                            val is12Selected = kotlin.math.abs(doors.elevationYMm - span12Elev) < 5f &&
                                kotlin.math.abs(doors.heightMm - span12H) < 5f

                            val span23Elev = bayComps[1].bottomYMm
                            val span23H = (bayComps[2].topYMm - bayComps[1].bottomYMm).coerceAtLeast(100f)
                            val is23Selected = kotlin.math.abs(doors.elevationYMm - span23Elev) < 5f &&
                                kotlin.math.abs(doors.heightMm - span23H) < 5f

                            FilterChip(
                                selected = is12Selected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = span12Elev, heightMm = span12H)
                                        }
                                    }
                            },
                                label = { Text("${s.doorsLowerLabel} + ${s.doorsMiddleLabel}") },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = is23Selected,
                                onClick = {
                                    onUpdateContainer(activeContainer.id) { c ->
                                        c.withUpdatedCabinetDoors(doors.id) {
                                            it.copy(elevationYMm = span23Elev, heightMm = span23H)
                                        }
                                    }
                            },
                                label = { Text("${s.doorsMiddleLabel} + ${s.doorsUpperLabel}") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            bayComps.forEachIndexed { idx, comp ->
                                val isCompSelected = kotlin.math.abs(doors.elevationYMm - comp.bottomYMm) < 5f &&
                                    kotlin.math.abs(doors.heightMm - comp.heightMm) < 5f
                                FilterChip(
                                    selected = isCompSelected,
                                    onClick = {
                                        onUpdateContainer(activeContainer.id) { c ->
                                            c.withUpdatedCabinetDoors(doors.id) {
                                                it.copy(elevationYMm = comp.bottomYMm, heightMm = comp.heightMm)
                                            }
                                        }
                                    },
                                    label = { Text("${idx + 1}") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                // Width (bay width, read-only)
                ModuleDimensionRow(
                    label = s.partitionWidthLabel,
                    valueMm = bay.widthMm,
                    unitSystem = unitSystem,
                    enabled = false,
                ) {}

                // Height
                ModuleDimensionRow(
                    label = s.partitionHeightLabel,
                    valueMm = doors.heightMm,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 150f,
                    maxMm = activeContainer.interiorHeightMm,
                    enabled = true,
                ) { newHeight ->
                    val clampedH = newHeight.coerceIn(150f, activeContainer.interiorHeightMm)
                    val maxElev = (activeContainer.interiorHeightMm - clampedH).coerceAtLeast(0f)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedCabinetDoors(doors.id) {
                            it.copy(
                                heightMm = clampedH,
                                elevationYMm = it.elevationYMm.coerceIn(0f, maxElev),
                            )
                        }
                    }
                }

                // Thickness
                ModuleDimensionRow(
                    label = s.partitionDepthLabel,
                    valueMm = doors.thicknessMm,
                    unitSystem = unitSystem,
                    stepMm = 1f,
                    minMm = 12f,
                    maxMm = 30f,
                    enabled = true,
                ) { newThick ->
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedCabinetDoors(doors.id) { it.copy(thicknessMm = newThick) }
                    }
                }

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
                }

                val maxElev = (activeContainer.interiorHeightMm - doors.heightMm).coerceAtLeast(0f)
                val bottomDist = doors.elevationYMm
                val topDist = (activeContainer.interiorHeightMm - (doors.elevationYMm + doors.heightMm)).coerceAtLeast(0f)

                // Top distance (clearance to roof)
                ModuleDimensionRow(
                    label = s.partitionTopLabel,
                    valueMm = topDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newTop ->
                    val newElev = (activeContainer.interiorHeightMm - doors.heightMm - newTop).coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedCabinetDoors(doors.id) { it.copy(elevationYMm = newElev) }
                    }
                }

                // Bottom distance
                ModuleDimensionRow(
                    label = s.partitionBottomLabel,
                    valueMm = bottomDist,
                    unitSystem = unitSystem,
                    stepMm = step,
                    minMm = 0f,
                    maxMm = maxElev,
                    enabled = true,
                ) { newBottom ->
                    val newElev = newBottom.coerceIn(0f, maxElev)
                    onUpdateContainer(activeContainer.id) { c ->
                        c.withUpdatedCabinetDoors(doors.id) { it.copy(elevationYMm = newElev) }
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
