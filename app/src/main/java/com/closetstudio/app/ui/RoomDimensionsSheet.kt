package com.closetstudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.RoomSpec
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDimensionsSheet(
    room: RoomSpec,
    onRoomChange: (RoomSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Dimensiones del Ambiente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                    )
                }
            }

            // Ancho (X)
            DimensionSlider(
                label = "Ancho Habitación (X)",
                valueMm = room.clearWidthMm,
                rangeMm = 2000f..6000f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearWidthMm = it)) },
            )

            // Profundidad (Z)
            DimensionSlider(
                label = "Profundidad Habitación (Z)",
                valueMm = room.clearDepthMm,
                rangeMm = 2000f..6000f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearDepthMm = it)) },
            )

            // Altura (Y)
            DimensionSlider(
                label = "Altura de Paredes (Y)",
                valueMm = room.clearHeightMm,
                rangeMm = 2000f..3500f,
                stepMm = 50f,
                onValueChange = { onRoomChange(room.copy(clearHeightMm = it)) },
            )

            // Puerta
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

            DimensionSlider(
                label = "Ancho de la Puerta",
                valueMm = room.door.widthMm,
                rangeMm = 600f..1200f,
                stepMm = 50f,
                onValueChange = {
                    val safeOffset = room.door.offsetMm.coerceAtMost((room.clearWidthMm - it).coerceAtLeast(0f))
                    onRoomChange(room.copy(door = room.door.copy(widthMm = it, offsetMm = safeOffset)))
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Listo")
            }
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
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${valueMm.roundToInt()} mm (${String.format(Locale.US, "%.2f", valueMm / 1000f)} m)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = valueMm.coerceIn(rangeMm.start, rangeMm.endInclusive),
            onValueChange = { raw ->
                val stepped = ((raw - rangeMm.start) / stepMm).roundToInt() * stepMm + rangeMm.start
                onValueChange(stepped.coerceIn(rangeMm.start, rangeMm.endInclusive))
            },
            valueRange = rangeMm,
        )
    }
}
