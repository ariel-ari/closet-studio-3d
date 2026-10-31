package com.closetstudio.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.RoomSpec
import com.closetstudio.core.model.Vec2Mm
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents the element currently selected on the interactive 2D Floorplan.
 */
sealed interface FloorplanElement {
    data object WallA : FloorplanElement
    data object WallB : FloorplanElement
    data object WallC : FloorplanElement
    data object WallSouthDoor : FloorplanElement
    data object CornerAC : FloorplanElement
    data object CornerAB : FloorplanElement
    data object CornerCD : FloorplanElement
    data object CornerBE : FloorplanElement
    data object CeilingHeight : FloorplanElement
}

private data class FloorplanHitTargets(
    val pNW: Offset,
    val pNE: Offset,
    val pSE: Offset,
    val pSW: Offset,
    val angleBadgeNW: Offset,
    val angleBadgeNE: Offset,
    val angleBadgeSE: Offset,
    val angleBadgeSW: Offset,
    val wallBadgeA: Offset,
    val wallBadgeB: Offset,
    val wallBadgeC: Offset,
    val wallBadgeD: Offset,
    val wallBadgeDoor: Offset,
    val wallBadgeE: Offset,
)

/**
 * Interactive 2D Parametric CAD Floorplan view:
 * - Tapping any wall (A, B, C, or South Door D/E) or its dimension badge selects that wall.
 * - Tapping any corner vertex (A-C, A-B, C-D, B-E) or its angle badge selects that corner.
 * - All labels, dimension lines, and angle arcs are mathematically anchored to the 2D polygon.
 */
@Composable
fun RoomFloorplan2D(
    room: RoomSpec,
    selectedElement: FloorplanElement,
    onSelectElement: (FloorplanElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val padX = with(density) { 102.dp.toPx() }
    val padTop = with(density) { 62.dp.toPx() }
    val padBottom = with(density) { 76.dp.toPx() }
    val wallOffsetA = with(density) { 28.dp.toPx() }
    val wallOffsetBC = with(density) { 34.dp.toPx() }
    val southOffset = with(density) { 26.dp.toPx() }
    val southDoorExtra = with(density) { 22.dp.toPx() }
    val cornerRadiusMax = with(density) { 26.dp.toPx() }
    val cornerRadiusMin = with(density) { 14.dp.toPx() }
    val cornerLabelOffset = with(density) { 16.dp.toPx() }

    val currentRoom by androidx.compose.runtime.rememberUpdatedState(room)
    val currentOnSelect by androidx.compose.runtime.rememberUpdatedState(onSelectElement)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F1EC))
            .pointerInput(density) {
                detectTapGestures { tap ->
                    if ((size.width <= 0) || (size.height <= 0)) return@detectTapGestures
                    val targets = computeHitTargets(
                        room = currentRoom,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        padX = padX,
                        padTop = padTop,
                        padBottom = padBottom,
                        wallOffsetA = wallOffsetA,
                        wallOffsetBC = wallOffsetBC,
                        southOffset = southOffset,
                        southDoorExtra = southDoorExtra,
                        cornerRadiusMax = cornerRadiusMax,
                        cornerRadiusMin = cornerRadiusMin,
                        cornerLabelOffset = cornerLabelOffset,
                    )
                    val cornerHitRadius = with(density) { 48.dp.toPx() }
                    val badgeHitRadius = with(density) { 64.dp.toPx() }
                    val wallSegmentRadius = with(density) { 48.dp.toPx() }

                    // 1. Check Corner Vertices and Corner Angle Badges first
                    val cornerCandidates = listOf(
                        FloorplanElement.CornerAC to min(
                            dist(tap, targets.pNW),
                            dist(tap, targets.angleBadgeNW),
                        ),
                        FloorplanElement.CornerAB to min(
                            dist(tap, targets.pNE),
                            dist(tap, targets.angleBadgeNE),
                        ),
                        FloorplanElement.CornerBE to min(
                            dist(tap, targets.pSE),
                            dist(tap, targets.angleBadgeSE),
                        ),
                        FloorplanElement.CornerCD to min(
                            dist(tap, targets.pSW),
                            dist(tap, targets.angleBadgeSW),
                        ),
                    )
                    val bestCorner = cornerCandidates.minByOrNull { it.second }
                    if ((bestCorner != null) && (bestCorner.second <= cornerHitRadius)) {
                        currentOnSelect(bestCorner.first)
                        return@detectTapGestures
                    }

                    // 2. Check Wall Dimension Badges
                    // Left wall = A (targets.wallBadgeC), Top wall = B (targets.wallBadgeA), Right wall = C (targets.wallBadgeB)
                    val badgeCandidates = listOf(
                        FloorplanElement.WallA to dist(tap, targets.wallBadgeC),
                        FloorplanElement.WallB to dist(tap, targets.wallBadgeA),
                        FloorplanElement.WallC to dist(tap, targets.wallBadgeB),
                        FloorplanElement.WallSouthDoor to minOf(
                            dist(tap, targets.wallBadgeD),
                            dist(tap, targets.wallBadgeDoor),
                            dist(tap, targets.wallBadgeE),
                        ),
                    )
                    val bestBadge = badgeCandidates.minByOrNull { it.second }
                    if ((bestBadge != null) && (bestBadge.second <= badgeHitRadius)) {
                        currentOnSelect(bestBadge.first)
                        return@detectTapGestures
                    }

                    // 3. Check Wall Line Segments
                    val segmentCandidates = listOf(
                        FloorplanElement.WallA to pointToSegmentDist(tap, targets.pSW, targets.pNW),
                        FloorplanElement.WallB to pointToSegmentDist(tap, targets.pNW, targets.pNE),
                        FloorplanElement.WallC to pointToSegmentDist(tap, targets.pNE, targets.pSE),
                        FloorplanElement.WallSouthDoor to pointToSegmentDist(tap, targets.pSW, targets.pSE),
                    )
                    val bestSegment = segmentCandidates.minByOrNull { it.second }
                    if ((bestSegment != null) && (bestSegment.second <= wallSegmentRadius)) {
                        currentOnSelect(bestSegment.first)
                    }
                }
            },
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

            val availW = (size.width - (padX * 2f)).coerceAtLeast(100f)
            val availH = (size.height - padTop - padBottom).coerceAtLeast(100f)

            val scale = min(availW / roomSpanX, availH / roomSpanZ)

            val centerWorldX = (minX + maxX) * 0.5f
            val centerWorldZ = (minZ + maxZ) * 0.5f
            val centerScreenX = size.width * 0.5f
            val centerScreenY = padTop + (availH * 0.5f)

            fun toScreen(pt: Vec2Mm): Offset {
                return Offset(
                    x = centerScreenX + (pt.x - centerWorldX) * scale,
                    y = centerScreenY + (pt.z - centerWorldZ) * scale,
                )
            }

            // 1. Background 500 mm CAD Grid
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

            // 3. Corner Angle Arcs & Interactive Corner Selection
            drawCornerAngleArc(
                vertex = pNW,
                arm1End = pNE,
                arm2End = pSW,
                angleDeg = room.angleACDeg,
                isLocked = room.lockAngleAC,
                isSelected = selectedElement == FloorplanElement.CornerAC,
            )
            drawCornerAngleArc(
                vertex = pNE,
                arm1End = pSE,
                arm2End = pNW,
                angleDeg = room.angleABDeg,
                isLocked = room.lockAngleAB,
                isSelected = selectedElement == FloorplanElement.CornerAB,
            )
            drawCornerAngleArc(
                vertex = pSE,
                arm1End = pSW,
                arm2End = pNE,
                angleDeg = room.angleBEDeg,
                isLocked = room.lockAngleBE,
                isSelected = selectedElement == FloorplanElement.CornerBE,
            )
            drawCornerAngleArc(
                vertex = pSW,
                arm1End = pNW,
                arm2End = pSE,
                angleDeg = room.angleCDDeg,
                isLocked = room.lockAngleCD,
                isSelected = selectedElement == FloorplanElement.CornerCD,
            )

            // 4. Structural Walls with Selection Glow
            val wallStrokePx = 8.dp.toPx()
            val glowStrokePx = 20.dp.toPx()
            val baseWallColor = Color(0xFF334155)
            val selectedWallColor = Color(0xFF1D4ED8)
            val selectedGlowColor = Color(0xFF3B82F6).copy(alpha = 0.28f)

            // Wall B (Top: NW -> NE)
            if (selectedElement == FloorplanElement.WallB) {
                drawLine(selectedGlowColor, pNW, pNE, glowStrokePx, StrokeCap.Round)
            }
            drawLine(
                color = if (selectedElement == FloorplanElement.WallB) selectedWallColor else baseWallColor,
                start = pNW,
                end = pNE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Round,
            )

            // Wall C (Right: NE -> SE)
            if (selectedElement == FloorplanElement.WallC) {
                drawLine(selectedGlowColor, pNE, pSE, glowStrokePx, StrokeCap.Round)
            }
            drawLine(
                color = if (selectedElement == FloorplanElement.WallC) selectedWallColor else baseWallColor,
                start = pNE,
                end = pSE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Round,
            )

            // Wall A (Left: SW -> NW)
            if (selectedElement == FloorplanElement.WallA) {
                drawLine(selectedGlowColor, pSW, pNW, glowStrokePx, StrokeCap.Round)
            }
            drawLine(
                color = if (selectedElement == FloorplanElement.WallA) selectedWallColor else baseWallColor,
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

            val isSouthSelected = selectedElement == FloorplanElement.WallSouthDoor
            if (isSouthSelected) {
                drawLine(selectedGlowColor, pSW, pSE, glowStrokePx, StrokeCap.Round)
            }

            // Segment D line
            drawLine(
                color = if (isSouthSelected) selectedWallColor else baseWallColor,
                start = pSW,
                end = pDoorStart,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Butt,
            )

            // Segment E line
            drawLine(
                color = if (isSouthSelected) selectedWallColor else baseWallColor,
                start = pDoorEnd,
                end = pSE,
                strokeWidth = wallStrokePx,
                cap = StrokeCap.Butt,
            )

            // Door threshold line
            drawLine(
                color = Color(0xFFC29B61),
                start = pDoorStart,
                end = pDoorEnd,
                strokeWidth = 2.dp.toPx(),
            )

            // Architectural Door Swing
            val doorVecX = pDoorEnd.x - pDoorStart.x
            val doorVecY = pDoorEnd.y - pDoorStart.y
            val doorRadiusPx = sqrt(doorVecX * doorVecX + doorVecY * doorVecY)
            if (doorRadiusPx > 6f) {
                val ux = doorVecX / doorRadiusPx
                val uz = doorVecY / doorRadiusPx
                val inNy = -ux
                val pDoorOpenTip = Offset(
                    x = pDoorStart.x + uz * doorRadiusPx,
                    y = pDoorStart.y + inNy * doorRadiusPx,
                )

                drawLine(
                    color = Color(0xFF8B5A2B),
                    start = pDoorStart,
                    end = pDoorOpenTip,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                val startAngleDeg = Math.toDegrees(atan2(inNy.toDouble(), uz.toDouble())).toFloat()
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

            // 5. Corner Vertex Selection Rings (visual tap targets on vertices)
            listOf(
                pNW to (selectedElement == FloorplanElement.CornerAC),
                pNE to (selectedElement == FloorplanElement.CornerAB),
                pSE to (selectedElement == FloorplanElement.CornerBE),
                pSW to (selectedElement == FloorplanElement.CornerCD),
            ).forEach { (pt, sel) ->
                if (sel) {
                    drawCircle(
                        color = Color(0xFFD97706).copy(alpha = 0.28f),
                        radius = 15.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = Color(0xFFD97706),
                        radius = 6.dp.toPx(),
                        center = pt,
                    )
                } else {
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = baseWallColor,
                        radius = 4.5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                }
            }

            // 6. Glued CAD Dimension Badges along each wall (Clockwise: Left=A, Top=B, Right=C, BottomRight=D, BottomLeft=E)
            drawWallDimensionCallout(
                start = pNW,
                end = pNE,
                outwardSign = -1f,
                offsetPx = wallOffsetA,
                letter = "B",
                valueText = "${room.wallAMm.roundToInt()} mm",
                isLocked = room.lockWallA,
                isSelected = selectedElement == FloorplanElement.WallB,
                badgeColor = Color(0xFF2C55B5),
            )

            drawWallDimensionCallout(
                start = pNE,
                end = pSE,
                outwardSign = -1f,
                offsetPx = wallOffsetBC,
                letter = "C",
                valueText = "${room.wallBMm.roundToInt()} mm",
                isLocked = room.lockWallB,
                isSelected = selectedElement == FloorplanElement.WallC,
                badgeColor = Color(0xFF2C55B5),
            )

            drawWallDimensionCallout(
                start = pSW,
                end = pNW,
                outwardSign = -1f,
                offsetPx = wallOffsetBC,
                letter = "A",
                valueText = "${room.wallCMm.roundToInt()} mm",
                isLocked = room.lockWallC,
                isSelected = selectedElement == FloorplanElement.WallA,
                badgeColor = Color(0xFF2C55B5),
            )

            val southDx = pSE.x - pSW.x
            val southDy = pSE.y - pSW.y
            val southPxLen = sqrt(southDx * southDx + southDy * southDy).coerceAtLeast(1f)
            val outSouthX = -southDy / southPxLen
            val outSouthY = southDx / southPxLen

            val midLeft = Offset(
                x = (pSW.x + pDoorStart.x) * 0.5f + outSouthX * southOffset,
                y = (pSW.y + pDoorStart.y) * 0.5f + outSouthY * southOffset,
            )
            val midDoor = Offset(
                x = (pDoorStart.x + pDoorEnd.x) * 0.5f + outSouthX * (southOffset + southDoorExtra),
                y = (pDoorStart.y + pDoorEnd.y) * 0.5f + outSouthY * (southOffset + southDoorExtra),
            )
            val midRight = Offset(
                x = (pDoorEnd.x + pSE.x) * 0.5f + outSouthX * southOffset,
                y = (pDoorEnd.y + pSE.y) * 0.5f + outSouthY * southOffset,
            )

            // Left of door is E, Right of door is D (continuous clockwise order: A -> B -> C -> D -> Door -> E)
            drawBadgePill(
                center = midLeft,
                letter = "E",
                text = room.segmentDMm.roundToInt().toString(),
                badgeColor = Color(0xFF2C55B5),
                isSelected = isSouthSelected,
                isLocked = room.lockSegmentD || room.lockWallSouth,
            )
            drawBadgePill(
                center = midDoor,
                letter = "",
                text = "${room.door.widthMm.roundToInt()} (Total: ${room.wallSouthMm.roundToInt()})",
                badgeColor = Color(0xFF8B5A2B),
                isSelected = isSouthSelected,
                isLocked = room.door.lockWidth || room.lockWallSouth,
                isDoorIcon = true,
            )
            drawBadgePill(
                center = midRight,
                letter = "D",
                text = room.segmentEMm.roundToInt().toString(),
                badgeColor = Color(0xFF2C55B5),
                isSelected = isSouthSelected,
                isLocked = room.lockSegmentE || room.lockWallSouth,
            )
        }

        val lang = LocalAppLanguage.current

        // Top-left interactive hint chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Text(
                text = tr(
                    lang,
                    "Tocá cualquier pared o esquina en el plano 2D",
                    "Tap any wall or corner on the 2D floorplan",
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C55B5),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun computeHitTargets(
    room: RoomSpec,
    width: Float,
    height: Float,
    padX: Float,
    padTop: Float,
    padBottom: Float,
    wallOffsetA: Float,
    wallOffsetBC: Float,
    southOffset: Float,
    southDoorExtra: Float,
    cornerRadiusMax: Float,
    cornerRadiusMin: Float,
    cornerLabelOffset: Float,
): FloorplanHitTargets {
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

    val availW = (width - padX * 2f).coerceAtLeast(100f)
    val availH = (height - padTop - padBottom).coerceAtLeast(100f)
    val scale = min(availW / roomSpanX, availH / roomSpanZ)

    val centerWorldX = (minX + maxX) * 0.5f
    val centerWorldZ = (minZ + maxZ) * 0.5f
    val centerScreenX = width * 0.5f
    val centerScreenY = padTop + availH * 0.5f

    fun toScreen(pt: Vec2Mm): Offset {
        return Offset(
            x = centerScreenX + (pt.x - centerWorldX) * scale,
            y = centerScreenY + (pt.z - centerWorldZ) * scale,
        )
    }

    val pNW = toScreen(nw)
    val pNE = toScreen(ne)
    val pSE = toScreen(se)
    val pSW = toScreen(sw)

    fun cornerBadgeCenter(vertex: Offset, arm1End: Offset, arm2End: Offset): Offset {
        val v1x = arm1End.x - vertex.x
        val v1y = arm1End.y - vertex.y
        val v2x = arm2End.x - vertex.x
        val v2y = arm2End.y - vertex.y
        val len1 = sqrt(v1x * v1x + v1y * v1y).coerceAtLeast(1f)
        val len2 = sqrt(v2x * v2x + v2y * v2y).coerceAtLeast(1f)
        val radius = minOf(len1 * 0.25f, len2 * 0.25f, cornerRadiusMax).coerceAtLeast(cornerRadiusMin)
        val startDeg = Math.toDegrees(atan2(v1y.toDouble(), v1x.toDouble())).toFloat()
        val endDeg = Math.toDegrees(atan2(v2y.toDouble(), v2x.toDouble())).toFloat()
        var sweep = (endDeg - startDeg) % 360f
        if (sweep < 0f) sweep += 360f
        if (sweep > 180f) sweep -= 360f
        val midAngleRad = Math.toRadians((startDeg + sweep * 0.5f).toDouble()).toFloat()
        val labelDist = radius + cornerLabelOffset
        return Offset(
            x = vertex.x + cos(midAngleRad) * labelDist,
            y = vertex.y + sin(midAngleRad) * labelDist,
        )
    }

    fun wallBadgeCenter(start: Offset, end: Offset, outwardSign: Float, offsetPx: Float): Offset {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val nx = (-dy / len) * outwardSign
        val ny = (dx / len) * outwardSign
        return Offset(
            x = (start.x + end.x) * 0.5f + nx * offsetPx,
            y = (start.y + end.y) * 0.5f + ny * offsetPx,
        )
    }

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

    val southDx = pSE.x - pSW.x
    val southDy = pSE.y - pSW.y
    val southPxLen = sqrt(southDx * southDx + southDy * southDy).coerceAtLeast(1f)
    val outSouthX = -southDy / southPxLen
    val outSouthY = southDx / southPxLen

    return FloorplanHitTargets(
        pNW = pNW,
        pNE = pNE,
        pSE = pSE,
        pSW = pSW,
        angleBadgeNW = cornerBadgeCenter(pNW, pNE, pSW),
        angleBadgeNE = cornerBadgeCenter(pNE, pSE, pNW),
        angleBadgeSE = cornerBadgeCenter(pSE, pSW, pNE),
        angleBadgeSW = cornerBadgeCenter(pSW, pNW, pSE),
        wallBadgeA = wallBadgeCenter(pNW, pNE, -1f, wallOffsetA),
        wallBadgeB = wallBadgeCenter(pNE, pSE, -1f, wallOffsetBC),
        wallBadgeC = wallBadgeCenter(pSW, pNW, -1f, wallOffsetBC),
        wallBadgeD = Offset(
            x = (pSW.x + pDoorStart.x) * 0.5f + outSouthX * southOffset,
            y = (pSW.y + pDoorStart.y) * 0.5f + outSouthY * southOffset,
        ),
        wallBadgeDoor = Offset(
            x = (pDoorStart.x + pDoorEnd.x) * 0.5f + outSouthX * (southOffset + southDoorExtra),
            y = (pDoorStart.y + pDoorEnd.y) * 0.5f + outSouthY * (southOffset + southDoorExtra),
        ),
        wallBadgeE = Offset(
            x = (pDoorEnd.x + pSE.x) * 0.5f + outSouthX * southOffset,
            y = (pDoorEnd.y + pSE.y) * 0.5f + outSouthY * southOffset,
        ),
    )
}

private fun dist(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private fun pointToSegmentDist(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1e-4f) return dist(p, a)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
    val proj = Offset(a.x + t * dx, a.y + t * dy)
    return dist(p, proj)
}

private fun DrawScope.drawCornerAngleArc(
    vertex: Offset,
    arm1End: Offset,
    arm2End: Offset,
    angleDeg: Float,
    isLocked: Boolean,
    isSelected: Boolean,
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
    val arcColor = when {
        isSelected -> Color(0xFFD97706)
        isOrtho -> Color(0xFF2C55B5)
        else -> Color(0xFFD97706)
    }

    drawArc(
        color = arcColor.copy(alpha = if (isSelected) 0.32f else 0.16f),
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
        style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx()),
    )

    val midAngleRad = Math.toRadians((startDeg + sweep * 0.5f).toDouble()).toFloat()
    val labelDist = radius + 16.dp.toPx()
    val lx = vertex.x + cos(midAngleRad) * labelDist
    val ly = vertex.y + sin(midAngleRad) * labelDist

    val angleStr = if (isOrtho) {
        "90°"
    } else {
        String.format(Locale.US, "%.1f°", angleDeg)
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10.5.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }
    val textW = textPaint.measureText(angleStr)
    val lockExtraW = if (isLocked) 13.dp.toPx() else 0f
    val pillW = textW + 10.dp.toPx() + lockExtraW
    val pillH = 17.dp.toPx()

    if (isSelected) {
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(lx - pillW * 0.5f - 3.dp.toPx(), ly - pillH * 0.5f - 3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(pillW + 6.dp.toPx(), pillH + 6.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFFD97706),
            topLeft = Offset(lx - pillW * 0.5f - 3.dp.toPx(), ly - pillH * 0.5f - 3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(pillW + 6.dp.toPx(), pillH + 6.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    drawRoundRect(
        color = arcColor,
        topLeft = Offset(lx - pillW * 0.5f, ly - pillH * 0.5f),
        size = androidx.compose.ui.geometry.Size(pillW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    )

    val textStartX = lx - pillW * 0.5f + 5.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        angleStr,
        textStartX,
        ly + 3.8.dp.toPx(),
        textPaint,
    )

    if (isLocked) {
        drawVectorPadlock(
            center = Offset(textStartX + textW + 6.5.dp.toPx(), ly),
            sizePx = 9.5.dp.toPx(),
            color = Color.White,
        )
    }
}

private fun DrawScope.drawWallDimensionCallout(
    start: Offset,
    end: Offset,
    outwardSign: Float,
    offsetPx: Float,
    letter: String,
    valueText: String,
    isLocked: Boolean,
    isSelected: Boolean,
    badgeColor: Color,
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = (-dy / len) * outwardSign
    val ny = (dx / len) * outwardSign

    val dimStart = Offset(start.x + nx * (offsetPx * 0.55f), start.y + ny * (offsetPx * 0.55f))
    val dimEnd = Offset(end.x + nx * (offsetPx * 0.55f), end.y + ny * (offsetPx * 0.55f))

    drawLine(
        color = badgeColor.copy(alpha = if (isSelected) 0.85f else 0.45f),
        start = start,
        end = dimStart,
        strokeWidth = if (isSelected) 1.5f else 1f,
    )
    drawLine(
        color = badgeColor.copy(alpha = if (isSelected) 0.85f else 0.45f),
        start = end,
        end = dimEnd,
        strokeWidth = if (isSelected) 1.5f else 1f,
    )
    drawLine(
        color = badgeColor.copy(alpha = if (isSelected) 0.95f else 0.65f),
        start = dimStart,
        end = dimEnd,
        strokeWidth = if (isSelected) 2.2f else 1.5f,
    )

    val mid = Offset(
        x = (start.x + end.x) * 0.5f + nx * offsetPx,
        y = (start.y + end.y) * 0.5f + ny * offsetPx,
    )
    drawBadgePill(
        center = mid,
        letter = letter,
        text = valueText,
        badgeColor = badgeColor,
        isSelected = isSelected,
        isLocked = isLocked,
    )
}

private fun DrawScope.drawBadgePill(
    center: Offset,
    letter: String,
    text: String,
    badgeColor: Color,
    isSelected: Boolean,
    isLocked: Boolean = false,
    isDoorIcon: Boolean = false,
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

    val letterW = if (isDoorIcon) {
        20.dp.toPx()
    } else {
        (letterPaint.measureText(letter) + 10.dp.toPx()).coerceAtLeast(20.dp.toPx())
    }
    val valW = textPaint.measureText(text)
    val lockExtraW = if (isLocked) 15.dp.toPx() else 0f
    val padH = 8.dp.toPx()
    val totalW = letterW + valW + padH * 2.2f + lockExtraW
    val pillH = 22.dp.toPx()

    val left = center.x - totalW * 0.5f
    val top = center.y - pillH * 0.5f

    if (isSelected) {
        drawRoundRect(
            color = badgeColor.copy(alpha = 0.22f),
            topLeft = Offset(left - 3.dp.toPx(), top - 3.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(totalW + 6.dp.toPx(), pillH + 6.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        )
    }

    drawRoundRect(
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(totalW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx()),
    )
    drawRoundRect(
        color = if (isSelected) badgeColor else badgeColor.copy(alpha = 0.35f),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(totalW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx()),
        style = Stroke(width = if (isSelected) 2.2.dp.toPx() else 1.dp.toPx()),
    )

    drawRoundRect(
        color = badgeColor,
        topLeft = Offset(left + 3.dp.toPx(), top + 3.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(letterW, pillH - 6.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
    )

    if (isDoorIcon) {
        drawVectorDoorIcon(
            center = Offset(left + 3.dp.toPx() + letterW * 0.5f, center.y),
            sizePx = 11.dp.toPx(),
            color = Color.White,
        )
    } else {
        drawContext.canvas.nativeCanvas.drawText(
            letter,
            left + 3.dp.toPx() + letterW * 0.5f,
            center.y + 4.dp.toPx(),
            letterPaint,
        )
    }

    val textStartX = left + 6.dp.toPx() + letterW + 4.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        text,
        textStartX,
        center.y + 4.dp.toPx(),
        textPaint,
    )

    if (isLocked) {
        drawVectorPadlock(
            center = Offset(textStartX + valW + 8.dp.toPx(), center.y),
            sizePx = 11.dp.toPx(),
            color = badgeColor,
        )
    }
}

/**
 * Draws a crisp vector padlock icon (body + shackle arc) without using emojis.
 */
private fun DrawScope.drawVectorPadlock(
    center: Offset,
    sizePx: Float,
    color: Color,
) {
    val bodyW = sizePx * 0.78f
    val bodyH = sizePx * 0.56f
    val bodyLeft = center.x - bodyW * 0.5f
    val bodyTop = center.y - sizePx * 0.04f

    val shackleW = sizePx * 0.46f
    val shackleH = sizePx * 0.46f
    val shackleStroke = (sizePx * 0.16f).coerceAtLeast(1.4f)

    // Shackle arch
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - shackleW * 0.5f, bodyTop - shackleH * 0.55f),
        size = androidx.compose.ui.geometry.Size(shackleW, shackleH),
        style = Stroke(width = shackleStroke, cap = StrokeCap.Round),
    )

    // Padlock body
    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft, bodyTop),
        size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(sizePx * 0.16f, sizePx * 0.16f),
    )
}

/**
 * Draws a clean vector door icon inside the brown Door badge pill without using emojis.
 */
private fun DrawScope.drawVectorDoorIcon(
    center: Offset,
    sizePx: Float,
    color: Color,
) {
    val w = sizePx * 0.68f
    val h = sizePx * 0.88f
    val left = center.x - w * 0.5f
    val top = center.y - h * 0.5f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(sizePx * 0.1f, sizePx * 0.1f),
        style = Stroke(width = (sizePx * 0.15f).coerceAtLeast(1.4f)),
    )
    // Door knob dot
    drawCircle(
        color = color,
        radius = (sizePx * 0.09f).coerceAtLeast(1f),
        center = Offset(left + w * 0.72f, center.y),
    )
}
