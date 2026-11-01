package com.closetstudio.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.closetstudio.app.material.MaterialCatalog
import com.closetstudio.app.material.TextureCategory
import com.closetstudio.app.material.TextureItem
import com.closetstudio.core.model.ClosetContainerSpec
import com.closetstudio.core.model.RoomSpec

@Composable
fun AssetTextureThumbnail(
    assetPath: String?,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color(0xFFD2A679),
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        if (assetPath == null) null
        else {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(fallbackColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun TextureCardItem(
    title: String,
    assetPath: String?,
    isSelected: Boolean,
    fallbackColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) Color(0xFF2C55B5) else Color(0xFFE2E8F0)
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2C55B5).copy(alpha = 0.08f) else Color.White
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                AssetTextureThumbnail(
                    assetPath = assetPath,
                    fallbackColor = fallbackColor,
                    modifier = Modifier.matchParentSize(),
                )
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        color = Color(0xFF2C55B5),
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(2.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF2C55B5) else Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Inspector card for the active container: visual melamine picker, grain orientation toggle,
 * and "apply to all furniture" action.
 */
@Composable
fun ContainerMelamineSection(
    container: ClosetContainerSpec,
    room: RoomSpec,
    s: UiStrings,
    onUpdateContainer: (ClosetContainerSpec) -> Unit,
    onApplyToAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textures = remember(context) { MaterialCatalog.loadTextures(context, TextureCategory.MELAMINE) }

    val currentTextureId = container.materialTextureId ?: room.defaultMelamineTextureId
    val currentItem = textures.firstOrNull { it.id == currentTextureId }
    val currentTitle = currentItem?.displayName ?: s.solidColorLabel

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatPaint,
                        contentDescription = null,
                        tint = Color(0xFF2C55B5),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s.tabMelamines,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2C55B5).copy(alpha = 0.12f),
                ) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C55B5),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal thumbnail carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Default Solid Wood Color item
                item {
                    TextureCardItem(
                        title = s.solidColorLabel,
                        assetPath = null,
                        isSelected = container.materialTextureId == null && room.defaultMelamineTextureId == null,
                        fallbackColor = Color(0xFFD2A679),
                        onClick = { onUpdateContainer(container.withMaterialTexture(null)) },
                        modifier = Modifier.width(96.dp),
                    )
                }

                items(textures, key = { it.id }) { item ->
                    TextureCardItem(
                        title = item.displayName,
                        assetPath = item.assetPath,
                        isSelected = (container.materialTextureId ?: room.defaultMelamineTextureId) == item.id,
                        fallbackColor = Color(0xFFD2A679),
                        onClick = { onUpdateContainer(container.withMaterialTexture(item.id)) },
                        modifier = Modifier.width(96.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wood Grain Orientation Selector
            Text(
                text = s.grainDirectionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = !container.isGrainHorizontal,
                    onClick = { onUpdateContainer(container.withGrainDirection(false)) },
                    label = { Text(s.grainVerticalLabel, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2C55B5).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF2C55B5),
                    ),
                    modifier = Modifier.weight(1f),
                )

                FilterChip(
                    selected = container.isGrainHorizontal,
                    onClick = { onUpdateContainer(container.withGrainDirection(true)) },
                    label = { Text(s.grainHorizontalLabel, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2C55B5).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF2C55B5),
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Apply to all furniture units in room
            OutlinedButton(
                onClick = onApplyToAll,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2C55B5)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(s.applyToAllContainersButton, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Global Materials & Textures Dialog accessible from Drawer or Overflow Menu.
 * Features 3 tabs: Melaminas (Furniture), Paredes (Walls), and Pisos (Floors).
 */
@Composable
fun MaterialsAndTexturesDialog(
    room: RoomSpec,
    s: UiStrings,
    onUpdateRoom: (RoomSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(s.tabMelamines, s.tabWalls, s.tabFloors)

    val melamines = remember(context) { MaterialCatalog.loadTextures(context, TextureCategory.MELAMINE) }
    val walls = remember(context) { MaterialCatalog.loadTextures(context, TextureCategory.WALL) }
    val floors = remember(context) { MaterialCatalog.loadTextures(context, TextureCategory.FLOOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = Color(0xFF2C55B5),
            )
        },
        title = {
            Text(
                text = s.materialsAndTexturesTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    // TAB 0: Melamines (Furniture)
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = s.grainDirectionLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = !room.defaultGrainHorizontal,
                                        onClick = { onUpdateRoom(room.withDefaultGrainHorizontal(false)) },
                                        label = { Text("Vertical", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(14.dp)) },
                                    )
                                    FilterChip(
                                        selected = room.defaultGrainHorizontal,
                                        onClick = { onUpdateRoom(room.withDefaultGrainHorizontal(true)) },
                                        label = { Text("Horizontal", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(14.dp)) },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 95.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) {
                                item {
                                    TextureCardItem(
                                        title = s.solidColorLabel,
                                        assetPath = null,
                                        isSelected = room.defaultMelamineTextureId == null,
                                        fallbackColor = Color(0xFFD2A679),
                                        onClick = {
                                            onUpdateRoom(
                                                room.withAppliedMelamineToAllContainers(
                                                    null,
                                                    room.defaultGrainHorizontal,
                                                )
                                            )
                                        },
                                    )
                                }

                                items(melamines, key = { it.id }) { item ->
                                    TextureCardItem(
                                        title = item.displayName,
                                        assetPath = item.assetPath,
                                        isSelected = room.defaultMelamineTextureId == item.id,
                                        fallbackColor = Color(0xFFD2A679),
                                        onClick = {
                                            onUpdateRoom(
                                                room.withAppliedMelamineToAllContainers(
                                                    item.id,
                                                    room.defaultGrainHorizontal,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // TAB 1: Walls (Paredes)
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (walls.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp),
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Color(0xFF2C55B5), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = s.copyTexturesHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1E3A8A),
                                        )
                                    }
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                item {
                                    TextureCardItem(
                                        title = s.solidColorLabel,
                                        assetPath = null,
                                        isSelected = room.wallTextureId == null,
                                        fallbackColor = Color(0xFFFAF7F0),
                                        onClick = { onUpdateRoom(room.withWallTexture(null)) },
                                    )
                                }

                                items(walls, key = { it.id }) { item ->
                                    TextureCardItem(
                                        title = item.displayName,
                                        assetPath = item.assetPath,
                                        isSelected = room.wallTextureId == item.id,
                                        fallbackColor = Color(0xFFFAF7F0),
                                        onClick = { onUpdateRoom(room.withWallTexture(item.id)) },
                                    )
                                }
                            }
                        }
                    }

                    // TAB 2: Floors (Pisos)
                    2 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (floors.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(10.dp),
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Color(0xFF2C55B5), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = s.copyTexturesHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1E3A8A),
                                        )
                                    }
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                item {
                                    TextureCardItem(
                                        title = s.solidColorLabel,
                                        assetPath = null,
                                        isSelected = room.floorTextureId == null,
                                        fallbackColor = Color(0xFFE0D2BE),
                                        onClick = { onUpdateRoom(room.withFloorTexture(null)) },
                                    )
                                }

                                items(floors, key = { it.id }) { item ->
                                    TextureCardItem(
                                        title = item.displayName,
                                        assetPath = item.assetPath,
                                        isSelected = room.floorTextureId == item.id,
                                        fallbackColor = Color(0xFFE0D2BE),
                                        onClick = { onUpdateRoom(room.withFloorTexture(item.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C55B5)),
            ) {
                Text(s.closeButton)
            }
        },
    )
}
