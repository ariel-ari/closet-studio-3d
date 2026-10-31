package com.closetstudio.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.Vec2Mm
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure 2D Parametric CAD Floorplan view for room dimensioning.
 *
 * All wall segments, dimension lines, letter badges (A, B, C, D, E), door swing arc,
 * and corner angle indicators are mathematically anchored to the 2D polygon vertices
 * so they move and deform in real time as any wall or corner angle changes.
 */
@Composable
fun RoomFloorplan2D(
    room: RoomSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F1EC)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nw = room.vertexNW
            val ne = room.vertexNE
            val se = room.vertexSE
            val sw = room.vertexSW

            val minX = minOf(nw.x, ne.x, se.x, sw.x)
            val maxX = maxOf(nw.x, ne.x, se.x, sw.x)
            val minZ = minOf(nw.z, ne.z, se.z, sw.z)
            val maxZ = maxOf(nw.z, ne.z, se.z, sw.z)

            val roomSpanX = (maxX - minX).coerceAtLeast(1000f)
            val roomSpanZ = (maxZ - minZ).coerceAtLeast(1000f)

            val padX = 102.dp.toPx()
            val padTop = 62.dp.toPx()
            val padBottom = 76.dp.toPx()

            val availW = (size.width - padX * 2f).coerceAtLeast(100f)
            val availH = (size.height - padTop - padBottom).coerceAtLeast(100f)

            val scale = min(availW / roomSpanX, availH / roomSpanZ)

            val centerWorldX = (minX + maxX) * 0.5f
            val centerWorldZ = (minZ + maxZ) * 0.5f
            val centerScreenX = size.width * 0.5f
            val centerScreenY = padTop + availH * 0.5f

            fun toScreen(pt: Vec2Mm): Offset {
                return Offset(
                    x = centerScreenX + (pt.x - centerWorldX) * scale,
                    y = centerScreenY + (pt.z - centerWorldZ) * scale,
                )
            }

            // 1. Background 500 mm CAD Grid (scaled in world coordinates)
            val gridStepMm = 500f
            val gridStepPx = gridStepMm * scale
            if (gridStepPx >= 12f) {
                val startGridX = ((minX - 2000f) / gridStepMm).toInt() * gridStepMm
                val endGridX = ((maxX + 2000f) / gridStepMm).toInt() * gridStepMm
                var gx = startGridX
                while (gx <= endGridX) {
                    val sx = centerScreenX + (gx - centerWorldX) * scale
                    if (sx in 0f..size.width) {
                        val isMajor = (gx.roundToInt() % 1000 == 0)
                        drawLine(
                            color = if (isMajor) Color(0xFFE0DDD5) else Color(0xFFEAE7E0),
                            start = Offset(sx, 0f),
                            end = Offset(sx, size.height),
                            strokeWidth = if (isMajor) 1.5f else 1f,
                        )
                    }
                    gx += gridStepMm
                }

                val startGridZ = ((minZ - 2000f) / gridStepMm).toInt() * gridStepMm
                val endGridZ = ((maxZ + 2000f) / gridStepMm).toInt() * gridStepMm
                var gz = startGridZ
                while (gz <= endGridZ) {
                    val sy = centerScreenY + (gz - centerWorldZ) * scale
                    if (sy in 0f..size.height) {
                        val isMajor = (gz.roundToInt() % 1000 == 0)
                        drawLine(
                            color = if (isMajor) Color(0xFFE0DDD5) else Color(0xFFEAE7E0),
                            start = Offset(0f, sy),
                            end = Offset(size.width, sy),
                            strokeWidth = if (isMajor) 1.5f else 1f,
                        )
                    }
                    gz += gridStepMm
                }
            }

            val pNW = toScreen(nw)
            val pNE = toScreen(ne)
            val pSE = toScreen(se)
            val pSW = toScreen(sw)

            // 2. Floor Polygon Fill
            val floorPath = Path().apply {
                moveTo(pNW.x, pNW.y)
                lineTo(pNE.x, pNE.y)
                lineTo(pSE.x, pSE.y)
                lineTo(pSW.x, pSW.y)
                close()
            }
            drawPath(
                path = floorPath,
                color = Color(0xFFFDFBF6),
            )

            // 3. Corner Angle Arcs & Labels inside the 4 vertices
            drawCornerAngleArc(
                vertex = pNW,
                arm1End = pNE,
                arm2End = pSW,
                angleDeg = room.angleACDeg,
                isLocked = room.lockAngleAC,
            )
            drawCornerAngleArc(
                vertex = pNE,
                arm1End = pSE,
                arm2End = pNW,
                angleDeg = room.angleABDeg,
                isLocked = room.lockAngleAB,
            )
            drawCornerAngleArc(
                vertex = pSE,
                arm1End = pSW,
                arm2End = pNE,
                angleDeg = room.angleBEDeg,
                isLocked = room.lockAngleBE,
            )
            drawCornerAngleArc(
                vertex = pSW,
                arm1End = pNW,
                arm2End = pSE,
                angleDeg = room.angleCDDeg,
                isLocked = room.lockAngleCD,
            )

            // 4. Structural Walls & Door Opening on South Wall
            val wallStrokePx = 8.dp.toPx()
            val wallColor = Color(0xFF334155)

            // Wall A (North: NW -> NE)
            drawLine(
                color = wallColor,
                start = pNW,
                end = pNE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Round,
            )

            // Wall B (East: NE -> SE)
            drawLine(
                color = wallColor,
                start = pNE,
                end = pSE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Round,
            )

            // Wall C (West: SW -> NW)
            drawLine(
                color = wallColor,
                start = pSW,
                end = pNW,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Round,
            )

            // South Wall (SW -> SE): split into Segment D, Door Opening, Segment E
            val southLenMm = room.wallSouthMm.coerceAtLeast(1f)
            val dRatio = (room.segmentDMm / southLenMm).coerceIn(0f, 1f)
            val doorEndRatio = ((room.segmentDMm + room.door.widthMm) / southLenMm).coerceIn(0f, 1f)

            val pDoorStart = Offset(
                x = pSW.x + (pSE.x - pSW.x) * dRatio,
                y = pSW.y + (pSE.y - pSW.y) * dRatio,
            )
            val pDoorEnd = Offset(
                x = pSW.x + (pSE.x - pSW.x) * doorEndRatio,
                y = pSW.y + (pSE.y - pSW.y) * doorEndRatio,
            )

            // Segment D line
            drawLine(
                color = wallColor,
                start = pSW,
                end = pDoorStart,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Butt,
            )

            // Segment E line
            drawLine(
                color = wallColor,
                start = pDoorEnd,
                end = pSE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Butt,
            )

            // Door threshold line (thin light line across opening)
            drawLine(
                color = Color(0xFFC29B61),
                start = pDoorStart,
                end = pDoorEnd,
                strokeWidth = 2.dp.toPx(),
            )

            // Architectural Door Swing (into the room from pDoorStart)
            val doorVecX = pDoorEnd.x - pDoorStart.x
            val doorVecY = pDoorEnd.y - pDoorStart.y
            val doorRadiusPx = sqrt(doorVecX * doorVecX + doorVecY * doorVecY)
            if (doorRadiusPx > 6f) {
                val ux = doorVecX / doorRadiusPx
                val uz = doorVecY / doorRadiusPx
                // Inward normal pointing into the room
                val inNx = uz
                val inNy = -ux
                val pDoorOpenTip = Offset(
                    x = pDoorStart.x + inNx * doorRadiusPx,
                    y = pDoorStart.y + inNy * doorRadiusPx,
                )

                // Door leaf panel in open position
                drawLine(
                    color = Color(0xFF8B5A2B),
                    start = pDoorStart,
                    end = pDoorOpenTip,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Dashed 90° swing arc from pDoorOpenTip to pDoorEnd
                val startAngleDeg = Math.toDegrees(atan2(inNy.toDouble(), inNx.toDouble())).toFloat()
                drawArc(
                    color = Color(0xFF8B5A2B).copy(alpha = 0.65f),
                    startAngle = startAngleDeg,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(pDoorStart.x - doorRadiusPx, pDoorStart.y - doorRadiusPx),
                    size = androidx.compose.ui.geometry.Size(doorRadiusPx * 2f, doorRadiusPx * 2f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    ),
                )
            }

            // 5. Glued CAD Dimension Badges along each wall
            // Wall A (North: outward normal is -Y / up)
            drawWallDimensionCallout(
                start = pNW,
                end = pNE,
                outwardSign = -1f,
                offsetPx = 28.dp.toPx(),
                letter = "A",
                valueText = "${room.wallAMm.roundToInt()} mm",
                isLocked = room.lockWallA,
                badgeColor = Color(0xFF2C55B5),
            )

            // Wall B (East: NE -> SE, outward normal is to the right)
            drawWallDimensionCallout(
                start = pNE,
                end = pSE,
                outwardSign = -1f,
                offsetPx = 34.dp.toPx(),
                letter = "B",
                valueText = "${room.wallBMm.roundToInt()} mm",
                isLocked = room.lockWallB,
                badgeColor = Color(0xFF2C55B5),
            )

            // Wall C (West: SW -> NW, outward normal is to the left)
            drawWallDimensionCallout(
                start = pSW,
                end = pNW,
                outwardSign = -1f,
                offsetPx = 34.dp.toPx(),
                letter = "C",
                valueText = "${room.wallCMm.roundToInt()} mm",
                isLocked = room.lockWallC,
                badgeColor = Color(0xFF2C55B5),
            )

            // South Wall Segments D, Door, and E (glued along South wall)
            val southDx = pSE.x - pSW.x
            val southDy = pSE.y - pSW.y
            val southPxLen = sqrt(southDx * southDx + southDy * southDy).coerceAtLeast(1f)
            val outSouthX = -southDy / southPxLen
            val outSouthY = southDx / southPxLen
            val southOffset = 26.dp.toPx()

            val midD = Offset(
                x = (pSW.x + pDoorStart.x) * 0.5f + outSouthX * southOffset,
                y = (pSW.y + pDoorStart.y) * 0.5f + outSouthY * southOffset,
            )
            val midDoor = Offset(
                x = (pDoorStart.x + pDoorEnd.x) * 0.5f + outSouthX * (southOffset + 22.dp.toPx()),
                y = (pDoorStart.y + pDoorEnd.y) * 0.5f + outSouthY * (southOffset + 22.dp.toPx()),
            )
            val midE = Offset(
                x = (pDoorEnd.x + pSE.x) * 0.5f + outSouthX * southOffset,
                y = (pDoorEnd.y + pSE.y) * 0.5f + outSouthY * southOffset,
            )

            drawBadgePill(
                center = midD,
                letter = "D",
                text = "${room.segmentDMm.roundToInt()}",
                badgeColor = Color(0xFF2C55B5),
            )
            drawBadgePill(
                center = midDoor,
                letter = "🚪",
                text = "${room.door.widthMm.roundToInt()} (${if (room.lockWallSouth) "🔒" else "🔓"} ${room.wallSouthMm.roundToInt()})",
                badgeColor = Color(0xFF8B5A2B),
            )
            drawBadgePill(
                center = midE,
                letter = "E",
                text = "${room.segmentEMm.roundToInt()}",
                badgeColor = Color(0xFF2C55B5),
            )
        }

        // Top-left subtle 2D CAD indicator chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Text(
                text = "PLANO 2D • Escala automática",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C55B5),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun DrawScope.drawCornerAngleArc(
    vertex: Offset,
    arm1End: Offset,
    arm2End: Offset,
    angleDeg: Float,
    isLocked: Boolean,
) {
    val v1x = arm1End.x - vertex.x
    val v1y = arm1End.y - vertex.y
    val v2x = arm2End.x - vertex.x
    val v2y = arm2End.y - vertex.y
    val len1 = sqrt(v1x * v1x + v1y * v1y).coerceAtLeast(1f)
    val len2 = sqrt(v2x * v2x + v2y * v2y).coerceAtLeast(1f)

    val radius = minOf(len1 * 0.25f, len2 * 0.25f, 26.dp.toPx()).coerceAtLeast(14.dp.toPx())
    val startDeg = Math.toDegrees(atan2(v1y.toDouble(), v1x.toDouble())).toFloat()
    val endDeg = Math.toDegrees(atan2(v2y.toDouble(), v2x.toDouble())).toFloat()
    var sweep = (endDeg - startDeg) % 360f
    if (sweep < 0f) sweep += 360f
    if (sweep > 180f) sweep -= 360f

    val isOrtho = kotlin.math.abs(angleDeg - 90f) < 0.3f
    val arcColor = if (isOrtho) Color(0xFF2C55B5) else Color(0xFFD97706)

    drawArc(
        color = arcColor.copy(alpha = 0.18f),
        startAngle = startDeg,
        sweepAngle = sweep,
        useCenter = true,
        topLeft = Offset(vertex.x - radius, vertex.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
    )
    drawArc(
        color = arcColor,
        startAngle = startDeg,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(vertex.x - radius, vertex.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
        style = Stroke(width = 2.dp.toPx()),
    )

    // Angle text along the bisector inside the room
    val midAngleRad = Math.toRadians((startDeg + sweep * 0.5f).toDouble()).toFloat()
    val labelDist = radius + 16.dp.toPx()
    val lx = vertex.x + cos(midAngleRad) * labelDist
    val ly = vertex.y + sin(midAngleRad) * labelDist

    val angleStr = if (isOrtho) {
        "90°"
    } else {
        String.format(Locale.US, "%.1f°", angleDeg)
    }
    val lockIcon = if (isLocked) "🔒" else "🔓"

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10.5.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val label = "$angleStr $lockIcon"
    val textW = textPaint.measureText(label)
    val pillW = textW + 10.dp.toPx()
    val pillH = 17.dp.toPx()

    drawRoundRect(
        color = arcColor,
        topLeft = Offset(lx - pillW * 0.5f, ly - pillH * 0.5f),
        size = androidx.compose.ui.geometry.Size(pillW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    )
    drawContext.canvas.nativeCanvas.drawText(
        label,
        lx,
        ly + 3.8.dp.toPx(),
        textPaint,
    )
}

private fun DrawScope.drawWallDimensionCallout(
    start: Offset,
    end: Offset,
    outwardSign: Float,
    offsetPx: Float,
    letter: String,
    valueText: String,
    isLocked: Boolean,
    badgeColor: Color,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    // Normal vector
    val nx = (-dy / len) * outwardSign
    val ny = (dx / len) * outwardSign

    val dimStart = Offset(start.x + nx * (offsetPx * 0.55f), start.y + ny * (offsetPx * 0.55f))
    val dimEnd = Offset(end.x + nx * (offsetPx * 0.55f), end.y + ny * (offsetPx * 0.55f))

    // Extension lines
    drawLine(
        color = badgeColor.copy(alpha = 0.45f),
        start = start,
        end = dimStart,
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = badgeColor.copy(alpha = 0.45f),
        start = end,
        end = dimEnd,
        strokeWidth = 1.dp.toPx(),
    )
    // Dimension line parallel to wall
    drawLine(
        color = badgeColor.copy(alpha = 0.65f),
        start = dimStart,
        end = dimEnd,
        strokeWidth = 1.5.dp.toPx(),
    )

    val mid = Offset(
        x = (start.x + end.x) * 0.5f + nx * offsetPx,
        y = (start.y + end.y) * 0.5f + ny * offsetPx,
    )
    val lockStr = if (isLocked) " 🔒" else " 🔓"
    drawBadgePill(
        center = mid,
        letter = letter,
        text = "$valueText$lockStr",
        badgeColor = badgeColor,
    )
}

private fun DrawScope.drawBadgePill(
    center: Offset,
    letter: String,
    text: String,
    badgeColor: Color,
) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 41, 59)
        textSize = 11.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 11.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val letterW = (letterPaint.measureText(letter) + 10.dp.toPx()).coerceAtLeast(20.dp.toPx())
    val valW = textPaint.measureText(text)
    val padH = 8.dp.toPx()
    val totalW = letterW + valW + padH * 2.2f
    val pillH = 22.dp.toPx()

    val left = center.x - totalW * 0.5f
    val top = center.y - pillH * 0.5f

    // Outer white pill
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(totalW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx()),
    )
    drawRoundRect(
        color = badgeColor.copy(alpha = 0.35f),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(totalW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )

    // Colored letter box inside left of pill
    drawRoundRect(
        color = badgeColor,
        topLeft = Offset(left + 3.dp.toPx(), top + 3.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(letterW, pillH - 6.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
    )

    drawContext.canvas.nativeCanvas.apply {
        drawText(
            letter,
            left + 3.dp.toPx() + letterW * 0.5f,
            center.y + 4.dp.toPx(),
            letterPaint,
        )
        drawText(
            text,
            left + 6.dp.toPx() + letterW + 4.dp.toPx(),
            center.y + 4.dp.toPx(),
            textPaint,
        )
    }
}
