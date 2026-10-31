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
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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

/**
 * Returns the base target position (center point on the wall) for a given wall index.
 * - Wall 0: South (Z = clearDepthMm, wall with door segments D and E)
 * - Wall 1: West (X = 0, Wall C)
 * - Wall 2: North (Z = 0, Wall A)
 * - Wall 3: East (X = clearWidthMm, Wall B)
 */
private fun getWallTarget(wallStep: Int, room: RoomSpec): Float3 {
    val nw = room.vertexNW
    val ne = room.vertexNE
    val se = room.vertexSE
    val sw = room.vertexSW
    val midY = room.clearHeightMm * 0.5f * MM_TO_M
    return when (((wallStep % 4) + 4) % 4) {
        0 -> Float3((sw.x + se.x) * 0.5f * MM_TO_M, midY, (sw.z + se.z) * 0.5f * MM_TO_M)
        1 -> Float3((sw.x + nw.x) * 0.5f * MM_TO_M, midY, (sw.z + nw.z) * 0.5f * MM_TO_M)
        2 -> Float3((nw.x + ne.x) * 0.5f * MM_TO_M, midY, (nw.z + ne.z) * 0.5f * MM_TO_M)
        else -> Float3((ne.x + se.x) * 0.5f * MM_TO_M, midY, (ne.z + se.z) * 0.5f * MM_TO_M)
    }
}

private fun getWallName(wallStep: Int, lang: AppLanguage): String {
    val s = stringsFor(lang)
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> "E - ${s.doorWord} - D"
        1 -> s.wallALabel
        2 -> s.wallBLabel
        else -> s.wallCLabel
    }
}

private fun wallIndexFromStep(wallStep: Int): WallIndex {
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> WallIndex.SOUTH
        1 -> WallIndex.WEST
        2 -> WallIndex.NORTH
        else -> WallIndex.EAST
    }
}

private fun stepFromWallIndex(wall: WallIndex): Int = when (wall) {
    WallIndex.SOUTH -> 0
    WallIndex.WEST -> 1
    WallIndex.NORTH -> 2
    WallIndex.EAST -> 3
}

private fun wallDisplayName(wall: WallIndex, lang: AppLanguage): String {
    val s = stringsFor(lang)
    return when (wall) {
        WallIndex.WEST -> s.wallALabel
        WallIndex.NORTH -> s.wallBLabel
        WallIndex.EAST -> s.wallCLabel
        WallIndex.SOUTH -> "${s.wallALabel.substringBefore(" ")} E-D"
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

    return room.pickContainerByRayMm(
        rayOriginX = originX * 1000f,
        rayOriginY = originY * 1000f,
        rayOriginZ = originZ * 1000f,
        rayDirX = dirX,
        rayDirY = dirY,
        rayDirZ = dirZ,
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
    var showUnitsColumn by remember { mutableStateOf(false) }
    var isEditingContainerInPanel by remember { mutableStateOf(false) }
    var selectedContainerId by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedCategoryKey by remember { mutableStateOf("CONTENT") }

    // View Mode (3D Perspective, Top-Down, Front 2D)
    var currentViewMode by remember { mutableStateOf(RoomViewMode.PERSPECTIVE_3D) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }

    // Wall index counter (0: South/Door, 1: West, 2: North, 3: East)
    val wallStep = remember { mutableIntStateOf(0) }

    val initialTarget = remember(room) { getWallTarget(0, room) }
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
        wallStep.intValue = step
        val targetPos = getWallTarget(step, room)
        val targetYaw = 180f + step * 90f
        val wallIndex = wallIndexFromStep(step)
        val wallLenM = getWallLengthMm(wallIndex, room) * MM_TO_M
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
        val targetPos = getWallTarget(wallStep.intValue, room)
        val targetYaw = 180f + wallStep.intValue * 90f + 25f
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
        wallStep.intValue = newStep
        if (currentViewMode == RoomViewMode.FRONT_2D) {
            switchToFront2DView(newStep)
        } else if (currentViewMode == RoomViewMode.TOP_DOWN) {
            // Stay in top-down view centered on the room
        } else {
            val targetPos = getWallTarget(newStep, room)
            val targetYaw = 180f + newStep * 90f
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
        if (currentViewMode == RoomViewMode.FRONT_2D) {
            switchToFront2DView(wallStep.intValue)
        } else if (currentViewMode == RoomViewMode.TOP_DOWN) {
            switchToTopDownView()
        } else {
            val targetPos = getWallTarget(wallStep.intValue, room)
            val targetYaw = 180f + wallStep.intValue * 90f
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

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
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
                                }
                            }
                        }
                    }

                    // 2. Interactive Body Area
                    if (!showDimensionsSheet) {
                        val activeContainer = room.containers.firstOrNull { it.id == selectedContainerId }
                        var isDraggingUnit by remember { mutableStateOf(false) }
                        var dragScreenOffset by remember { mutableStateOf(Offset.Zero) }
                        var viewportOriginInRoot by remember { mutableStateOf(Offset.Zero) }
                        var panelOriginInRootX by remember { mutableFloatStateOf(Float.MAX_VALUE) }

                        LaunchedEffect(room.containers, selectedContainerId) {
                            if (selectedContainerId != null && room.containers.none { it.id == selectedContainerId }) {
                                selectedContainerId = null
                                isEditingContainerInPanel = false
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
                                    if (hitId == null && isEditingContainerInPanel) {
                                        isEditingContainerInPanel = false
                                        showUnitsColumn = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )

                            // Left Navigation Button (<)
                            FilledIconButton(
                                onClick = { switchWall(wallStep.intValue - 1) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
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
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .navigationBarsPadding()
                                        .padding(start = 16.dp, bottom = 76.dp),
                                ) {
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
                                        modifier = Modifier.size(52.dp),
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
                                                modifier = Modifier.size(24.dp),
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
                                        modifier = Modifier.size(52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = s.duplicateContainerDesc,
                                                tint = Color(0xFF3949AB),
                                                modifier = Modifier.size(23.dp),
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
                                        modifier = Modifier.size(52.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = s.deleteContainerDesc,
                                                tint = Color(0xFF651FFF),
                                                modifier = Modifier.size(25.dp),
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
                                    .padding(end = 16.dp, bottom = 76.dp),
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
                                    isDragging = isDraggingUnit,
                                    onToggleEditMode = { isEditingContainerInPanel = it },
                                    onSelectContainer = { container ->
                                        selectedContainerId = container.id
                                        val targetStep = stepFromWallIndex(container.wall)
                                        if (targetStep != wallStep.intValue) {
                                            switchWall(targetStep)
                                        }
                                    },
                                    onAddContainerOnCurrentWall = { dropFractionX ->
                                        val targetWall = wallIndexFromStep(wallStep.intValue)
                                        val newId = "container_${System.currentTimeMillis()}_${room.containers.size + 1}"
                                        val added = room.withAddedContainer(wall = targetWall, id = newId)
                                        val updatedRoom = if (dropFractionX != null) {
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
                                        showUnitsColumn = false
                                        coroutineScope.launch {
                                            messageHostState.showSnackbar(
                                                "${s.containerAddedToast} ${wallDisplayName(targetWall, appLanguage)}",
                                            )
                                        }
                                    },
                                    onDragUnitStart = { startRootPos ->
                                        isDraggingUnit = true
                                        dragScreenOffset = startRootPos - viewportOriginInRoot
                                    },
                                    onDragUnitMove = { delta ->
                                        dragScreenOffset += delta
                                    },
                                    onDragUnitEnd = {
                                        if (isDraggingUnit) {
                                            val fractionX = (dragScreenOffset.x / viewportWidthPx).coerceIn(0.05f, 0.95f)
                                            val targetWall = wallIndexFromStep(wallStep.intValue)
                                            val newId = "container_${System.currentTimeMillis()}_${room.containers.size + 1}"
                                            val added = room.withAddedContainer(wall = targetWall, id = newId)
                                            val wallLen = added.wallLengthMm(targetWall).coerceAtLeast(0f)
                                            val updatedRoom = added.withUpdatedContainer(newId) { item ->
                                                val maxOff = (wallLen - item.widthMm).coerceAtLeast(0f)
                                                item.copy(offsetMm = fractionX * maxOff)
                                            }
                                            updateRoom(updatedRoom)
                                            selectedContainerId = newId
                                            isDraggingUnit = false
                                            showUnitsColumn = false
                                            coroutineScope.launch {
                                                messageHostState.showSnackbar(
                                                    "${s.containerAddedToast} ${wallDisplayName(targetWall, appLanguage)}",
                                                )
                                            }
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
                                        if (updatedRoom.containers.isEmpty()) {
                                            isEditingContainerInPanel = false
                                        }
                                    },
                                    onClose = { showUnitsColumn = false },
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
                                    ClosetContainer3DGraphic(
                                        modifier = Modifier.fillMaxSize(),
                                    )
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
                                Text("• ${s.wallALabel}: ${room.wallCMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.wallCMm / 1000f)} m)")
                                Text("• ${s.wallBLabel}: ${room.wallAMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.wallAMm / 1000f)} m)")
                                Text("• ${s.wallCLabel}: ${room.wallBMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.wallBMm / 1000f)} m)")
                                Text("• ${s.wallEDTotalLabel}: ${room.wallSouthMm.toInt()} mm (E:${room.segmentDMm.toInt()} + ${s.doorWord}:${room.door.widthMm.toInt()} + D:${room.segmentEMm.toInt()})")
                                Text("• ${s.anglesLabel}: A-B=${String.format(Locale.US, "%.1f°", room.angleACDeg)}, B-C=${String.format(Locale.US, "%.1f°", room.angleABDeg)}, C-D=${String.format(Locale.US, "%.1f°", room.angleBEDeg)}, A-E=${String.format(Locale.US, "%.1f°", room.angleCDDeg)}")
                                Text("• ${s.heightYLabel}: ${room.clearHeightMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.clearHeightMm / 1000f)} m)")
                                Text("• ${s.currentViewLabel}: ${getWallName(wallStep.intValue, appLanguage)}")
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
                                        text = "A: ${room.wallCMm.roundToInt()} • B: ${room.wallAMm.roundToInt()} • C: ${room.wallBMm.roundToInt()} • E-D: ${room.wallSouthMm.roundToInt()} mm",
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        )
    }
}

@Composable
private fun RightSideUnitsAndInspectorColumn(
    room: RoomSpec,
    s: UiStrings,
    activeContainer: ClosetContainerSpec?,
    isEditingContainer: Boolean,
    isDragging: Boolean = false,
    onToggleEditMode: (Boolean) -> Unit,
    onSelectContainer: (ClosetContainerSpec) -> Unit,
    onAddContainerOnCurrentWall: (dropFractionX: Float?) -> Unit,
    onDragUnitStart: (startRootPos: Offset) -> Unit,
    onDragUnitMove: (delta: Offset) -> Unit,
    onDragUnitEnd: () -> Unit,
    onDragUnitCancel: () -> Unit,
    onUpdateContainer: (String, (ClosetContainerSpec) -> ClosetContainerSpec) -> Unit,
    onDuplicateContainer: (String) -> Unit,
    onDeleteContainer: (String) -> Unit,
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
            // Minimalist "Add Unit    ✕" Header matching reference screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isEditingContainer && activeContainer != null) {
                        s.editContainerTitle
                    } else {
                        s.unitsPanelTitle
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditingContainer && activeContainer != null) {
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
                        onClick = onClose,
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

            if (isEditingContainer && activeContainer != null) {
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
                // --- SIMPLE VISUAL CATALOG VIEW (Clean white column, pure 3D illustration, drag or tap to place) ---
                var itemRootOffset by remember { mutableStateOf(Offset.Zero) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    // 1st Unit: Closet Container (Carcass) — pure 3D graphic without text clutter
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(175.dp)
                            .onGloballyPositioned { coordinates ->
                                itemRootOffset = coordinates.positionInRoot()
                            }
                            .clickable {
                                onAddContainerOnCurrentWall(null)
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onDragUnitStart(itemRootOffset + localOffset)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragUnitMove(dragAmount)
                                    },
                                    onDragEnd = {
                                        onDragUnitEnd()
                                    },
                                    onDragCancel = {
                                        onDragUnitCancel()
                                    },
                                )
                            },
                    ) {
                        ClosetContainer3DGraphic(
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .height(160.dp),
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
                    text = "${valueMm.roundToInt()} mm (max ${maxMm.roundToInt()})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C55B5),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(-100f, -25f, +25f, +100f).forEach { delta ->
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
                                text = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}",
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
