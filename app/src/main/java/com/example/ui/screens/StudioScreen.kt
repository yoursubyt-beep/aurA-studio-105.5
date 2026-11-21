package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AspectRatioOption
import com.example.data.BodyArchetype
import com.example.data.ReferenceAttachment
import com.example.data.ReferenceRole
import com.example.data.StudioCatalog
import com.example.data.SynthesisUiState
import com.example.ui.components.AtelierSectionCard
import com.example.ui.components.DropdownSelectorField
import com.example.ui.components.FullscreenImageInspectorDialog
import com.example.ui.components.SplitCompareViewport
import com.example.ui.components.StudioRadarPad
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PureSilkWhite
import com.example.ui.theme.TerracottaRose
import com.example.viewmodel.StudioRigState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    rig: StudioRigState,
    synthesisState: SynthesisUiState,
    compiledPrompt: String,
    batchStatus: String?,
    isLooping: Boolean,
    onAttachUris: (List<android.net.Uri>) -> Unit,
    onSetPrimaryRef: (String) -> Unit,
    onUpdateRefRole: (String, ReferenceRole) -> Unit,
    onRemoveRef: (String) -> Unit,
    onClearRefs: () -> Unit,
    onUpdateIdentityNotes: (String) -> Unit,
    onSelectArchetype: (BodyArchetype) -> Unit,
    onUpdateFraming: (String) -> Unit,
    onUpdateYaw: (Int) -> Unit,
    onUpdateRadar: (Float, Float) -> Unit,
    onUpdateIntent: (String) -> Unit,
    onRefinePrompt: () -> Unit,
    onCleanPrompt: () -> Unit,
    onUpdateAspectRatio: (AspectRatioOption) -> Unit,
    onUpdateBatchCount: (Int) -> Unit,
    onToggleCycleCostumes: (Boolean) -> Unit,
    onToggleCompare: () -> Unit,
    onToggleAnatomyNodes: () -> Unit,
    onFocusAnatomyZone: (String) -> Unit,
    onPromoteActiveToRef: () -> Unit,
    onShareActive: () -> Unit,
    onSynthesizeSingle: () -> Unit,
    onRunBatch: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenAtelier: () -> Unit,
    onQueueStoryboard: () -> Unit,
    isWideLayout: Boolean
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (uris.isNotEmpty()) onAttachUris(uris)
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var inspectorTarget by remember { mutableStateOf<Pair<Any, String>?>(null) }
    var showCompiledPrompt by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    if (inspectorTarget != null) {
        FullscreenImageInspectorDialog(
            imageModel = inspectorTarget?.first,
            caption = inspectorTarget?.second.orEmpty(),
            onDismiss = { inspectorTarget = null }
        )
    }

    val viewportSection: @Composable (Modifier) -> Unit = { mod ->
        Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Viewport Toolbar
            Surface(
                color = ObsidianNoir,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, CharcoalCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "STUDIO VIEWPORT",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (batchStatus != null) {
                            Surface(
                                color = TerracottaRose.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, TerracottaRose)
                            ) {
                                Text(
                                    text = batchStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureSilkWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onToggleAnatomyNodes,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (rig.showAnatomyNodes) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle silhouette focus nodes",
                                tint = if (rig.showAnatomyNodes) AtelierGold else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleCompare,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Toggle split compare",
                                tint = if (rig.compareModeActive) AtelierGold else PureSilkWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = PureSilkWhite, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureSilkWhite
                        )
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = PureSilkWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Main Canvas Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 340.dp, max = 520.dp)
                    .aspectRatio(rig.aspectRatio.aspectFloat.coerceIn(0.55f, 1.8f), matchHeightConstraintsFirst = true)
                    .clip(MaterialTheme.shapes.large)
                    .background(ObsidianNoir)
                    .border(1.dp, CharcoalCard, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                when (synthesisState) {
                    is SynthesisUiState.Idle -> {
                        Image(
                            painter = painterResource(id = R.drawable.img_atelier_stage),
                            contentDescription = stringResource(id = R.string.cd_atelier_backdrop),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianNoir.copy(alpha = 0.55f),
                                            ObsidianNoir.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AtelierGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, AtelierGold),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = AtelierGold)
                                }
                            }
                            Text(
                                text = "Aura Costume & Silhouette Stage",
                                style = MaterialTheme.typography.headlineSmall,
                                color = PureSilkWhite,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Attach portrait or outfit references below, choose a look in Atelier, and tap Synthesize Frame.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is SynthesisUiState.Loading -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = AtelierGold)
                            Text(
                                text = synthesisState.statusMessage,
                                style = MaterialTheme.typography.labelLarge,
                                color = AtelierGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "High-resolution Gemini image generation typically takes 20–45 seconds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is SynthesisUiState.Error -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Generation error",
                                tint = TerracottaRose,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Synthesis Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureSilkWhite
                            )
                            Text(
                                text = synthesisState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onSynthesizeSingle,
                                colors = ButtonDefaults.buttonColors(containerColor = AtelierGold, contentColor = ObsidianNoir)
                            ) {
                                Text("Retry Synthesis")
                            }
                        }
                    }

                    is SynthesisUiState.Success -> {
                        val primaryRefUri = (rig.references.firstOrNull { it.isPrimary } ?: rig.references.firstOrNull())?.uriString
                        SplitCompareViewport(
                            outputBitmap = synthesisState.bitmap,
                            referenceUri = primaryRefUri,
                            zoomScale = zoomScale,
                            compareActive = rig.compareModeActive,
                            showAnatomyNodes = rig.showAnatomyNodes,
                            onFocusZone = onFocusAnatomyZone,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    inspectorTarget = synthesisState.bitmap to synthesisState.title
                                }
                        )

                        Surface(
                            color = ObsidianNoir.copy(alpha = 0.82f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = synthesisState.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PureSilkWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${rig.aspectRatio.ratioText} • Auto-Saved to Vault",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AtelierGold
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = onPromoteActiveToRef,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.7f))
                                    ) {
                                        Icon(Icons.Default.Anchor, contentDescription = "Lock as reference", tint = AtelierGold, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ref", color = PureSilkWhite, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = onShareActive,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AtelierGold, contentColor = ObsidianNoir)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share image", modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Costume Bar + Compiled Prompt Preview
            AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.35f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE COSTUME",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = rig.activeCostume.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = onOpenAtelier) {
                            Text("Change Look")
                        }
                        TextButton(onClick = onQueueStoryboard) {
                            Text("+ Story")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showCompiledPrompt) "Compiled Studio Prompt" else "View Compiled Prompt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { showCompiledPrompt = !showCompiledPrompt }
                    )
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(compiledPrompt)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy compiled prompt", modifier = Modifier.size(16.dp))
                    }
                }
                AnimatedVisibility(visible = showCompiledPrompt) {
                    Text(
                        text = compiledPrompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val controlsSection: @Composable (Modifier) -> Unit = { mod ->
        Column(modifier = mod, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // PANEL 1: Multi-Reference Attachment Lock
            AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.35f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("1:1 Reference Lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Attach up to 6 portrait, outfit, or pose references",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (rig.references.isNotEmpty()) {
                            TextButton(onClick = onClearRefs) {
                                Text("Clear", color = TerracottaRose)
                            }
                        }
                        Surface(
                            color = ObsidianNoir,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${rig.references.size}/6",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtelierGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attach_reference_button")
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(id = R.string.cd_add_reference), tint = AtelierGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Attach Reference Photos (Photo Picker)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (rig.references.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(rig.references, key = { _, item -> item.id }) { idx, ref ->
                            ReferenceAttachmentCard(
                                index = idx,
                                attachment = ref,
                                onInspect = { inspectorTarget = ref.uriString to "Reference #${idx + 1}" },
                                onSetPrimary = { onSetPrimaryRef(ref.id) },
                                onChangeRole = { onUpdateRefRole(ref.id, it) },
                                onRemove = { onRemoveRef(ref.id) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = rig.identityNotes,
                    onValueChange = onUpdateIdentityNotes,
                    label = { Text("Subject Identity Notes (Optional)") },
                    placeholder = { Text("e.g. wavy dark hair, warm olive skin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // PANEL 2: Body Archetype & Quick Framing
            AtelierSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Silhouette & Archetype", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = rig.archetype.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BodyArchetype.entries.forEach { archetype ->
                        val selected = rig.archetype == archetype
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectArchetype(archetype) },
                            label = { Text(archetype.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianNoir,
                                selectedLabelColor = AtelierGold
                            )
                        )
                    }
                }

                DropdownSelectorField(
                    label = "Camera Framing",
                    selectedValue = rig.shotFraming,
                    options = StudioCatalog.framingOptions,
                    onSelected = onUpdateFraming
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CAMERA ORBIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("${rig.cameraYaw}°", style = MaterialTheme.typography.labelSmall, color = AtelierGold, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = rig.cameraYaw.toFloat(),
                            onValueChange = { onUpdateYaw((it / 15f).roundToInt() * 15) },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(thumbColor = AtelierGold, activeTrackColor = AtelierGold)
                        )
                    }
                    StudioRadarPad(
                        radarX = rig.radarX,
                        radarY = rig.radarY,
                        onRadarChanged = onUpdateRadar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // PANEL 3: Scene Prompt, Aspect, Batch & Generate Actions
            AtelierSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scene & Pose Prompt", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onRefinePrompt) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Safe Refiner", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = onCleanPrompt) {
                            Text("Clean", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = rig.userIntent,
                    onValueChange = onUpdateIntent,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prompt_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AspectRatioOption.entries.forEach { ratio ->
                        val selected = rig.aspectRatio == ratio
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateAspectRatio(ratio) },
                            label = { Text(ratio.ratioText) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ObsidianNoir,
                                selectedLabelColor = AtelierGold
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Batch Takes (${rig.batchCount})", style = MaterialTheme.typography.labelLarge)
                        Text("Auto-Swap Costumes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4, 6).forEach { count ->
                            FilterChip(
                                selected = rig.batchCount == count,
                                onClick = { onUpdateBatchCount(count) },
                                label = { Text("${count}x") }
                            )
                        }
                        Switch(
                            checked = rig.cycleCostumesInBatch,
                            onCheckedChange = onToggleCycleCostumes
                        )
                    }
                }

                val isBusy = synthesisState is SynthesisUiState.Loading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSynthesizeSingle,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("generate_single_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianNoir, contentColor = PureSilkWhite)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AtelierGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Synthesize", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onRunBatch,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("generate_batch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AtelierGold, contentColor = ObsidianNoir)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Batch (${rig.batchCount}x)", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continuous_loop_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLooping) TerracottaRose else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isLooping) PureSilkWhite else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (isLooping) Icons.Default.Stop else Icons.Default.Loop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isLooping) "Stop Continuous Loop" else "Start Continuous Generation Loop",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (isWideLayout) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                controlsSection(Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                viewportSection(Modifier.fillMaxWidth())
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.widthIn(max = 600.dp)) {
                viewportSection(Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.widthIn(max = 600.dp)) {
                controlsSection(Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReferenceAttachmentCard(
    index: Int,
    attachment: ReferenceAttachment,
    onInspect: () -> Unit,
    onSetPrimary: () -> Unit,
    onChangeRole: (ReferenceRole) -> Unit,
    onRemove: () -> Unit
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (attachment.isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = if (attachment.isPrimary) 1.5.dp else 1.dp,
            color = if (attachment.isPrimary) AtelierGold else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.width(156.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onInspect)
            ) {
                AsyncImage(
                    model = attachment.uriString,
                    contentDescription = "Reference #${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(ObsidianNoir.copy(alpha = 0.75f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove reference", tint = PureSilkWhite, modifier = Modifier.size(14.dp))
                }
                Surface(
                    color = ObsidianNoir.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "#${attachment.seed % 10000}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSetPrimary),
                shape = RoundedCornerShape(8.dp),
                color = if (attachment.isPrimary) AtelierGold else Color.Transparent,
                border = BorderStroke(1.dp, AtelierGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (attachment.isPrimary) ObsidianNoir else AtelierGold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (attachment.isPrimary) "Primary Face" else "Set Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (attachment.isPrimary) ObsidianNoir else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { roleMenuExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = attachment.role.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                    ReferenceRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.label, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                onChangeRole(role)
                                roleMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
