package com.closetstudio.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.app.data.ProjectStorage
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.SavedProject
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
    val midY = 1.40f
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> Float3((sw.x + se.x) * 0.5f * MM_TO_M, midY, (sw.z + se.z) * 0.5f * MM_TO_M)
        1 -> Float3((sw.x + nw.x) * 0.5f * MM_TO_M, midY, (sw.z + nw.z) * 0.5f * MM_TO_M)
        2 -> Float3((nw.x + ne.x) * 0.5f * MM_TO_M, midY, (nw.z + ne.z) * 0.5f * MM_TO_M)
        else -> Float3((ne.x + se.x) * 0.5f * MM_TO_M, midY, (ne.z + se.z) * 0.5f * MM_TO_M)
    }
}

private fun getWallName(wallStep: Int, lang: AppLanguage): String {
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> tr(lang, "Pared E - Puerta - D", "Wall E - Door - D")
        1 -> tr(lang, "Pared A", "Wall A")
        2 -> tr(lang, "Pared B", "Wall B")
        else -> tr(lang, "Pared C", "Wall C")
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
        mutableStateOf(active?.name ?: tr(appLanguage, "Proyecto 1", "Project 1"))
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
    val snackbarHostState = remember { SnackbarHostState() }

    var showDimensionsSheet by remember { mutableStateOf(false) }
    var selectedFloorplanElement by remember { mutableStateOf<FloorplanElement>(FloorplanElement.WallA) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCreateNewDialog by remember { mutableStateOf(false) }
    var showMyProjectsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedCategoryKey by remember { mutableStateOf("CONTENT") }

    // Wall index counter (0: South/Door, 1: West, 2: North, 3: East)
    val wallStep = remember { mutableIntStateOf(0) }

    val initialTarget = remember(room) { getWallTarget(0, room) }
    val targetXAnim = remember { Animatable(initialTarget.x) }
    val targetYAnim = remember { Animatable(initialTarget.y) }
    val targetZAnim = remember { Animatable(initialTarget.z) }

    val yawAnim = remember { Animatable(180f) }
    val pitchAnim = remember { Animatable(0f) }
    val distanceAnim = remember { Animatable(3.40f) }

    fun switchWall(newStep: Int) {
        wallStep.intValue = newStep
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

    fun recenterCurrentWall() {
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

    // Top-down / Floorplan Camera Transition for Dimensioning
    fun enterDimensionsMode() {
        showDimensionsSheet = true
        selectedCategoryKey = "ROOM"
        val centerX = room.clearWidthMm * 0.5f * MM_TO_M
        val centerZ = room.clearDepthMm * 0.5f * MM_TO_M
        val maxDim = maxOf(room.clearWidthMm, room.clearDepthMm) * MM_TO_M
        val topDownDist = (maxDim * 1.55f).coerceIn(4.2f, 9.0f)
        coroutineScope.launch {
            launch { yawAnim.animateTo(180f, tween(450, easing = FastOutSlowInEasing)) }
            // Pitch cenital a -86°: el techo desaparece solo por backface culling
            launch { pitchAnim.animateTo(-86f, tween(450, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(topDownDist, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(centerX, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(0.10f, tween(450, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(centerZ, tween(450, easing = FastOutSlowInEasing)) }
        }
    }

    fun exitDimensionsMode() {
        showDimensionsSheet = false
        selectedCategoryKey = "CONTENT"
        recenterCurrentWall()
    }

    fun updateRoom(newRoom: RoomSpec) {
        room = newRoom
        if (showDimensionsSheet) {
            val centerX = newRoom.clearWidthMm * 0.5f * MM_TO_M
            val centerZ = newRoom.clearDepthMm * 0.5f * MM_TO_M
            val maxDim = maxOf(newRoom.clearWidthMm, newRoom.clearDepthMm) * MM_TO_M
            val topDownDist = (maxDim * 1.55f).coerceIn(4.2f, 9.0f)
            coroutineScope.launch {
                targetXAnim.snapTo(centerX)
                targetYAnim.snapTo(0.10f)
                targetZAnim.snapTo(centerZ)
                distanceAnim.snapTo(topDownDist)
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
            snackbarHostState.showSnackbar(newStrings.languageChangedMessage)
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

                    // --- My Projects / Mis Proyectos Section (matching reference screenshot) ---
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
                                            text = "${savedProjects.size}",
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
                                            showCategoryMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(s.menuGeneral3dView) },
                                        leadingIcon = { Icon(Icons.Default.ViewInAr, null) },
                                        onClick = {
                                            showCategoryMenu = false
                                            exitDimensionsMode()
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
                                    } else {
                                        recenterCurrentWall()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = s.center3dView,
                                    tint = Color.White,
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
                        // Normal 3D Mode: Fullscreen RoomScene + navigation arrows + centrar
                        Box(modifier = Modifier.fillMaxSize()) {
                            RoomViewport(
                                room = room,
                                targetX = targetXAnim.value,
                                targetY = targetYAnim.value,
                                targetZ = targetZAnim.value,
                                yaw = yawAnim.value,
                                pitch = pitchAnim.value,
                                distance = distanceAnim.value,
                                onOrbit = { deltaX, deltaY ->
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
                                    val panScale = distanceAnim.value * 0.0016f
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
                                        val newDist = (distanceAnim.value / zoomFactor).coerceIn(0.8f, 10.0f)
                                        distanceAnim.snapTo(newDist)
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

                            // Bottom Recenter Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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
                                        contentDescription = s.overflowCenterWall,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = s.centerButton,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    } else {
                        // Dimensioning Mode: Responsive Split-Screen with Interactive 2D CAD Floorplan!
                        if (isLandscape) {
                            // Tablet / Landscape: Side by Side (Left: 2D CAD floorplan, Right: Contextual Inspector)
                            val panelWidth = (screenWidth * 0.42f).coerceIn(370.dp, 450.dp)
                            Row(modifier = Modifier.fillMaxSize()) {
                                RoomFloorplan2D(
                                    room = room,
                                    selectedElement = selectedFloorplanElement,
                                    onSelectElement = { selectedFloorplanElement = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )

                                RoomDimensionsPanel(
                                    room = room,
                                    selectedElement = selectedFloorplanElement,
                                    onSelectElement = { selectedFloorplanElement = it },
                                    onRoomChange = { updateRoom(it) },
                                    onDismiss = { exitDimensionsMode() },
                                    modifier = Modifier
                                        .width(panelWidth)
                                        .fillMaxHeight()
                                        .navigationBarsPadding(),
                                )
                            }
                        } else {
                            // Mobile / Portrait: Top / Bottom Split (Top: 2D CAD floorplan, Bottom: Contextual Inspector)
                            Column(modifier = Modifier.fillMaxSize()) {
                                RoomFloorplan2D(
                                    room = room,
                                    selectedElement = selectedFloorplanElement,
                                    onSelectElement = { selectedFloorplanElement = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.10f),
                                )

                                RoomDimensionsPanel(
                                    room = room,
                                    selectedElement = selectedFloorplanElement,
                                    onSelectElement = { selectedFloorplanElement = it },
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

                // Save Project Dialog (💾 Guardar Proyecto)
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
                                            snackbarHostState.showSnackbar("${s.savedAsNewToast}: ${saved.name}")
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
                                        snackbarHostState.showSnackbar("${s.projectSavedToast}: ${saved.name}")
                                    }
                                },
                            ) {
                                Text(s.saveButton, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }

                // Create New Project Dialog (📁+ Crear nuevo)
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
                                        snackbarHostState.showSnackbar("${s.projectCreatedToast}: ${created.name}")
                                    }
                                },
                            ) {
                                Text(s.createButton, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }

                // My Projects Dialog (📂 Mis Proyectos)
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
                                    items(savedProjects, key = { it.id }) { proj ->
                                        val isCurrent = proj.id == currentProjectId
                                        val r = proj.room
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
                                                currentProjectId = proj.id
                                                currentProjectName = proj.name
                                                projectStorage.setActiveProjectId(proj.id)
                                                updateRoom(proj.room)
                                                showMyProjectsDialog = false
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("${s.projectOpenedToast}: ${proj.name}")
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
                                                            text = proj.name,
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
                                                        text = "${s.doorWord}:${r.door.widthMm.roundToInt()} (E:${r.segmentDMm.roundToInt()}, D:${r.segmentEMm.roundToInt()}) • ${dateFormatter.format(Date(proj.updatedAtEpochMs))}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        savedProjects = projectStorage.deleteProject(proj.id)
                                                        if (currentProjectId == proj.id) {
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

                // Scalable Language Selector Dialog (🌐 Idioma / Language)
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
                    hostState = snackbarHostState,
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
    onOrbit: (Float, Float) -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectRoomInspectionGestures(
                    onOrbit = onOrbit,
                    onPan = onPan,
                    onZoom = onZoom,
                )
            },
    ) {
        RoomScene(
            room = room,
            target = Float3(targetX, targetY, targetZ),
            yawAngleDegrees = yaw,
            pitchAngleDegrees = pitch,
            distance = distance,
        )
    }
}

@Composable
private fun FloorplanBadgesOverlay(
    room: RoomSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        // Top Wall (Pared A / Norte)
        FloorplanWallBadge(
            letter = "A",
            dimensionText = "${room.clearWidthMm.roundToInt()} mm",
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Right Wall (Pared B / Este)
        FloorplanWallBadge(
            letter = "B",
            dimensionText = "${room.clearDepthMm.roundToInt()} mm",
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        // Left Wall (Pared C / Oeste)
        FloorplanWallBadge(
            letter = "C",
            dimensionText = "${room.clearDepthMm.roundToInt()} mm",
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Bottom Wall with Door (Tramos D y E)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                FloorplanSegmentBadge("D", "${room.segmentDMm.roundToInt()} mm")
                Text("•", color = Color.Gray)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.DoorFront,
                        contentDescription = null,
                        tint = Color(0xFF8B5A2B),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${room.door.widthMm.roundToInt()} mm",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5A2B),
                    )
                }
                Text("•", color = Color.Gray)
                FloorplanSegmentBadge("E", "${room.segmentEMm.roundToInt()} mm")
            }
        }

        // 4 Corner 90° Indicators
        CornerAngleBadge(text = "90°", modifier = Modifier.align(Alignment.TopStart))
        CornerAngleBadge(text = "90°", modifier = Modifier.align(Alignment.TopEnd))
        CornerAngleBadge(text = "90°", modifier = Modifier.align(Alignment.BottomStart))
        CornerAngleBadge(text = "90°", modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun FloorplanWallBadge(letter: String, dimensionText: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 5.dp,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
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
            Text(
                text = dimensionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FloorplanSegmentBadge(letter: String, dimensionText: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF2C55B5),
        ) {
            Text(
                text = " $letter ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }
        Text(
            text = dimensionText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C55B5),
        )
    }
}

@Composable
private fun CornerAngleBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}
