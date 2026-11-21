package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.GeminiImageModelOption
import com.example.data.StudioPreferences
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PureSilkWhite
import com.example.ui.theme.TerracottaRose
import kotlin.math.roundToInt

@Composable
fun AtelierSectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun DropdownSelectorField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    displayTransform: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayTransform(selectedValue),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayTransform(option),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudioRadarPad(
    radarX: Float,
    radarY: Float,
    onRadarChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STYLE RADAR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[${"%.1f".format(radarX)}, ${"%.1f".format(radarY)}]",
                style = MaterialTheme.typography.labelSmall,
                color = AtelierGold,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(MaterialTheme.shapes.small)
                .background(ObsidianNoir)
                .border(1.dp, AtelierGold.copy(alpha = 0.4f), MaterialTheme.shapes.small)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val nx = ((offset.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                        val ny = (-((offset.y / size.height) * 2f - 1f)).coerceIn(-1f, 1f)
                        onRadarChanged(nx, ny)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val nx = ((change.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                        val ny = (-((change.position.y / size.height) * 2f - 1f)).coerceIn(-1f, 1f)
                        onRadarChanged(nx, ny)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = AtelierGold.copy(alpha = 0.22f)
                val cols = 6
                val rows = 3
                for (i in 1 until cols) {
                    val x = size.width * i / cols
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                }
                for (j in 1 until rows) {
                    val y = size.height * j / rows
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                val dotX = ((radarX + 1f) / 2f) * size.width
                val dotY = ((-radarY + 1f) / 2f) * size.height
                drawCircle(
                    color = AtelierGold.copy(alpha = 0.3f),
                    radius = 22f,
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = AtelierGold,
                    radius = 10f,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

@Composable
fun SplitCompareViewport(
    outputBitmap: Bitmap,
    referenceUri: String?,
    zoomScale: Float,
    compareActive: Boolean,
    showAnatomyNodes: Boolean,
    onFocusZone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        Image(
            bitmap = outputBitmap.asImageBitmap(),
            contentDescription = "Synthesized studio output",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
        )

        if (compareActive && referenceUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(splitFraction)
                    .clipToBounds()
                    .background(ObsidianNoir)
            ) {
                AsyncImage(
                    model = referenceUri,
                    contentDescription = "Primary reference portrait",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(this@BoxWithConstraints.maxWidth)
                )
                Surface(
                    color = ObsidianNoir.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "REFERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = ObsidianNoir.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = "OUTPUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureSilkWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            val handleX = (splitFraction * totalWidthPx).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(handleX - 24, 0) }
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(totalWidthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (totalWidthPx > 0f) {
                                splitFraction = (splitFraction + dragAmount.x / totalWidthPx)
                                    .coerceIn(0.08f, 0.92f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(AtelierGold)
                )
                Surface(
                    shape = CircleShape,
                    color = AtelierGold,
                    border = BorderStroke(2.dp, PureSilkWhite),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "<>",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianNoir,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (showAnatomyNodes) {
            AnatomyNodeChip("Neck", Alignment.TopCenter, Modifier.padding(top = 44.dp)) {
                onFocusZone("neck")
            }
            AnatomyNodeChip("Underarms", Alignment.CenterStart, Modifier.padding(start = 10.dp, bottom = 72.dp)) {
                onFocusZone("underarms")
            }
            AnatomyNodeChip("Neckline", Alignment.Center, Modifier.padding(bottom = 44.dp)) {
                onFocusZone("cleavage")
            }
            AnatomyNodeChip("Pierced Navel", Alignment.Center, Modifier.padding(top = 48.dp)) {
                onFocusZone("navel")
            }
            AnatomyNodeChip("Soft Arms", Alignment.CenterEnd, Modifier.padding(end = 10.dp, bottom = 28.dp)) {
                onFocusZone("arms")
            }
            AnatomyNodeChip("Thighs & Legs", Alignment.BottomCenter, Modifier.padding(bottom = 66.dp)) {
                onFocusZone("legs")
            }
        }
    }
}

@Composable
private fun AnatomyNodeChip(
    label: String,
    alignment: Alignment,
    offsetModifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(offsetModifier),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = ObsidianNoir.copy(alpha = 0.78f),
            shape = CircleShape,
            border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.85f))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PureSilkWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun FullscreenImageInspectorDialog(
    imageModel: Any?,
    caption: String,
    onDismiss: () -> Unit
) {
    if (imageModel == null) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss)
                .padding(16.dp)
        ) {
            if (imageModel is Bitmap) {
                Image(
                    bitmap = imageModel.asImageBitmap(),
                    contentDescription = caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = imageModel,
                    contentDescription = caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianNoir.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = AtelierGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.16f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close inspector",
                        tint = PureSilkWhite
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectorPresetSheet(
    onDismiss: () -> Unit,
    onSelectPresetIndex: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AtelierGold)
                Text("Clothing-Line & Silhouette Director", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "Instant wardrobe presets configured for specific anatomical framing & thin-fabric layering.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val presets = listOf(
                Triple(6, "Pierced Navel & Midriff Crop", "Cropped halter & low-rise cut framing pierced bellybutton and waist"),
                Triple(0, "Side-Cut & Underarms Halter", "Sleeveless cutaway halter framing neck, underarms & side chest"),
                Triple(3, "Sheer Organza & Satin Corset", "Translucent outer organza wrap over structured sweetheart bodice"),
                Triple(9, "High-Slit Satin & Soft Limbs", "Thigh-high slit gown showcasing soft upper arms, thighs & bare legs"),
                Triple(12, "Translucent Organza Saree", "Draped low on waist over plunge choli with bangles & navel view")
            )

            presets.forEach { (idx, title, subtitle) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onSelectPresetIndex(idx)
                            onDismiss()
                        },
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: StudioPreferences,
    onSelectModel: (GeminiImageModelOption) -> Unit,
    onToggleAutoRefine: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Gemini Synthesis Engine", style = MaterialTheme.typography.headlineSmall)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = EmeraldSoft,
                border = BorderStroke(1.dp, EmeraldStatus.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldStatus)
                    Column {
                        Text(
                            "Firebase AI Logic + App Check Active",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldStatus
                        )
                        Text(
                            "Secured via App Check attestation without client-side API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianNoir
                        )
                    }
                }
            }

            Text(
                "IMAGE SYNTHESIS MODEL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            GeminiImageModelOption.entries.forEach { option ->
                val selected = option == preferences.modelOption
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .testTag("model_${option.modelId}")
                        .clickable { onSelectModel(option) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) AtelierGold else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.displayName, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = AtelierGold
                            )
                        }
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Refine Prompts", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Translates terms into editorial couture phrasing automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.autoSafeRefine,
                        onCheckedChange = onToggleAutoRefine,
                        colors = SwitchDefaults.colors(checkedThumbColor = PureSilkWhite, checkedTrackColor = TerracottaRose)
                    )
                }
            }
        }
    }
}
