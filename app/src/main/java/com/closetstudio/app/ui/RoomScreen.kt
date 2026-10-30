package com.closetstudio.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.app.render.MM_TO_M
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex
import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.launch
import java.util.Locale
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

private fun getWallName(wallStep: Int): String {
    val wall = ((wallStep % 4) + 4) % 4
    return when (wall) {
        0 -> "Pared Sur (Fondo / Puerta)"
        1 -> "Pared Oeste (Lateral Izquierda)"
        2 -> "Pared Norte (Frente)"
        else -> "Pared Este (Lateral Derecha)"
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var showDimensionsSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Contenido") }

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
            launch { yawAnim.animateTo(targetYaw, tween(300, easing = FastOutSlowInEasing)) }
            launch { pitchAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
            launch { distanceAnim.animateTo(3.40f, tween(250, easing = FastOutSlowInEasing)) }
            launch { targetXAnim.animateTo(targetPos.x, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetYAnim.animateTo(targetPos.y, tween(300, easing = FastOutSlowInEasing)) }
            launch { targetZAnim.animateTo(targetPos.z, tween(300, easing = FastOutSlowInEasing)) }
        }
    }

    fun updateRoom(newRoom: RoomSpec) {
        room = newRoom
        val targetPos = getWallTarget(wallStep.intValue, newRoom)
        coroutineScope.launch {
            targetXAnim.snapTo(targetPos.x)
            targetYAnim.snapTo(targetPos.y)
            targetZAnim.snapTo(targetPos.z)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Closet Studio 3D",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C55B5),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Text(
                    text = "Arquitectura y Modulación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    label = { Text("Dimensiones de la Habitación") },
                    icon = { Icon(Icons.Default.Straighten, null) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showDimensionsSheet = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                NavigationDrawerItem(
                    label = { Text("Centrar Vista 3D") },
                    icon = { Icon(Icons.Default.CenterFocusStrong, null) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        recenterCurrentWall()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                NavigationDrawerItem(
                    label = { Text("Información del Ambiente") },
                    icon = { Icon(Icons.Default.Info, null) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showInfoDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                NavigationDrawerItem(
                    label = { Text("Restablecer Valores") },
                    icon = { Icon(Icons.Default.RestartAlt, null) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        updateRoom(RoomSpec())
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEAE4)),
        ) {
            // 1. 3D Scene with 1-finger Orbit, 2-finger Pan & 2-finger Zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectRoomInspectionGestures(
                            onOrbit = { deltaX, deltaY ->
                                coroutineScope.launch {
                                    yawAnim.snapTo(yawAnim.value - deltaX * 0.22f)
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
                                    val newDist = (distanceAnim.value / zoomFactor).coerceIn(0.8f, 7.0f)
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

            // 2. CAD TopAppBar (Royal Blue header matching reference)
            Surface(
                color = Color(0xFF2C55B5),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
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
                            contentDescription = "Menú principal",
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
                                text = selectedCategory,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Cambiar modo",
                                tint = Color.White,
                            )
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Habitación (Dimensiones)") },
                                leadingIcon = { Icon(Icons.Default.Straighten, null) },
                                onClick = {
                                    selectedCategory = "Habitación"
                                    showCategoryMenu = false
                                    showDimensionsSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Módulos de Placard") },
                                leadingIcon = { Icon(Icons.Default.DoorFront, null) },
                                onClick = {
                                    selectedCategory = "Módulos"
                                    showCategoryMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Vista 3D General") },
                                leadingIcon = { Icon(Icons.Default.ViewInAr, null) },
                                onClick = {
                                    selectedCategory = "Contenido"
                                    showCategoryMenu = false
                                    recenterCurrentWall()
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Undo (↶)
                    IconButton(
                        onClick = {
                            // Reset current wall target
                            recenterCurrentWall()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = Color.White,
                        )
                    }

                    // 3D View / Recenter Cube
                    IconButton(
                        onClick = { recenterCurrentWall() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Centrar vista 3D",
                            tint = Color.White,
                        )
                    }

                    // Info (ⓘ)
                    IconButton(
                        onClick = { showInfoDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información del ambiente",
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
                                contentDescription = "Más opciones",
                                tint = Color.White,
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dimensiones del cuarto...") },
                                leadingIcon = { Icon(Icons.Default.Straighten, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDimensionsSheet = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Centrar pared") },
                                leadingIcon = { Icon(Icons.Default.CenterFocusStrong, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    recenterCurrentWall()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Restablecer a 3.6 x 3.6 m") },
                                leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                                onClick = {
                                    showOverflowMenu = false
                                    updateRoom(RoomSpec())
                                },
                            )
                        }
                    }
                }
            }

            // 3. Left Navigation Button (< Previous Wall)
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
                    contentDescription = "Pared anterior",
                    modifier = Modifier.size(32.dp),
                )
            }

            // 4. Right Navigation Button (> Next Wall)
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
                    contentDescription = "Pared siguiente",
                    modifier = Modifier.size(32.dp),
                )
            }

            // 5. Bottom Recenter / Reset Button
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

            // 6. Room Dimensions ModalBottomSheet
            if (showDimensionsSheet) {
                RoomDimensionsSheet(
                    room = room,
                    onRoomChange = { updateRoom(it) },
                    onDismiss = { showDimensionsSheet = false },
                )
            }

            // 7. Info Dialog (ⓘ)
            if (showInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2C55B5)) },
                    title = { Text("Datos del Ambiente") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("• Ancho (X): ${room.clearWidthMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.clearWidthMm / 1000f)} m)")
                            Text("• Profundidad (Z): ${room.clearDepthMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.clearDepthMm / 1000f)} m)")
                            Text("• Altura (Y): ${room.clearHeightMm.toInt()} mm (${String.format(Locale.US, "%.2f", room.clearHeightMm / 1000f)} m)")
                            Text("• Superficie Piso: ${String.format(Locale.US, "%.2f", (room.clearWidthMm * room.clearDepthMm) / 1_000_000f)} m²")
                            Text("• Pared Activa: ${getWallName(wallStep.intValue)}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("Entendido")
                        }
                    },
                )
            }
        }
    }
}
