package com.closetstudio.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.closetstudio.core.model.DoorSpec
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.WallIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round

@Composable
fun RoomScreen() {
    // Habitación estándar limpia con su puerta en una pared
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
    val yawAnim = remember { Animatable(0f) }
    val pitchAnim = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDragX = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        coroutineScope.launch {
                            // Arrastre con el dedo: gira de a pocos
                            yawAnim.snapTo(yawAnim.value - dragAmount.x * 0.22f)
                            val newPitch = (pitchAnim.value + dragAmount.y * 0.12f).coerceIn(-18f, 18f)
                            pitchAnim.snapTo(newPitch)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            // Si el swipe fue marcado (> 80px), saltar 90° a la pared en esa dirección
                            val target = if (abs(totalDragX) > 80f) {
                                if (totalDragX < 0f) {
                                    // Deslizó hacia la izquierda -> gira hacia la derecha
                                    (round(yawAnim.value / 90f) + 1f) * 90f
                                } else {
                                    // Deslizó hacia la derecha -> gira hacia la izquierda
                                    (round(yawAnim.value / 90f) - 1f) * 90f
                                }
                            } else {
                                // Soltó despacio -> se imanta a la pared más cercana
                                round(yawAnim.value / 90f) * 90f
                            }
                            yawAnim.animateTo(target, tween(350, easing = FastOutSlowInEasing))
                            pitchAnim.animateTo(0f, tween(250))
                        }
                    },
                )
            },
    ) {
        RoomScene(
            room = room,
            yawAngleDegrees = yawAnim.value,
            pitchAngleDegrees = pitchAnim.value,
        )
    }
}
