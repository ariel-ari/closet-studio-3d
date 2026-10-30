package com.closetstudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.closetstudio.core.model.ClosetProject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    var project by remember { mutableStateOf(sampleProject()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Closet Studio 3D", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            MeasurementsPanel(
                project = project,
                onProjectChange = { project = it },
            )
        },
    ) { padding ->
        ClosetScene(
            project = project,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun MeasurementsPanel(
    project: ClosetProject,
    onProjectChange: (ClosetProject) -> Unit,
) {
    val room = project.room

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MeasurementSlider(
                label = "Width",
                valueMm = room.clearWidthMm,
                rangeMm = 1200f..4000f,
                onValueChange = {
                    onProjectChange(project.copy(room = room.copy(clearWidthMm = it)))
                },
            )
            MeasurementSlider(
                label = "Height",
                valueMm = room.clearHeightMm,
                rangeMm = 1800f..3000f,
                onValueChange = {
                    onProjectChange(project.copy(room = room.copy(clearHeightMm = it)))
                },
            )
            MeasurementSlider(
                label = "Depth",
                valueMm = room.clearDepthMm,
                rangeMm = 350f..750f,
                onValueChange = {
                    onProjectChange(project.copy(room = room.copy(clearDepthMm = it)))
                },
            )
            Text(
                text = "Approximate millimetres, converted to metres for the renderer.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MeasurementSlider(
    label: String,
    valueMm: Float,
    rangeMm: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 12.dp),
        )
        Slider(
            value = valueMm,
            onValueChange = { onValueChange((it / 10f).roundToInt() * 10f) },
            valueRange = rangeMm,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${valueMm.roundToInt()} mm",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
