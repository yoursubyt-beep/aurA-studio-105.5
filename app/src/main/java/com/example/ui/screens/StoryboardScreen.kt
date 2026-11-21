package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.StoryboardScene
import com.example.data.StoryboardStatus
import com.example.ui.components.AtelierSectionCard
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.EmeraldStatus
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PureSilkWhite
import com.example.ui.theme.TerracottaRose
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun StoryboardScreen(
    scenes: List<StoryboardScene>,
    isStreaming: Boolean,
    onAutoPopulateArc: () -> Unit,
    onQueueCurrentRig: () -> Unit,
    onRunStream: () -> Unit,
    onStopStream: () -> Unit,
    onRemoveScene: (Long) -> Unit,
    onClearStoryboard: () -> Unit
) {
    val completedScenes = remember(scenes) {
        scenes.filter { it.status == StoryboardStatus.COMPLETED && it.imagePath != null }
    }
    var isPlayingReel by remember { mutableStateOf(false) }
    var reelIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlayingReel, completedScenes.size) {
        if (isPlayingReel && completedScenes.isNotEmpty()) {
            while (isPlayingReel) {
                delay(1100L)
                reelIndex = (reelIndex + 1) % completedScenes.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.widthIn(max = 760.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header & Actions
                AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sequential Storyboard", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Queue continuous angles & outfits of the locked subject into a sequential reel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (scenes.isNotEmpty()) {
                            IconButton(onClick = onClearStoryboard) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear storyboard", tint = TerracottaRose)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAutoPopulateArc,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("auto_story_arc_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AtelierGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Auto 4-Scene Arc")
                        }
                        Button(
                            onClick = onQueueCurrentRig,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("queue_rig_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianNoir, contentColor = PureSilkWhite)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Queue Rig")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = if (isStreaming) onStopStream else onRunStream,
                            enabled = scenes.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("run_story_stream_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreaming) TerracottaRose else AtelierGold,
                                contentColor = if (isStreaming) PureSilkWhite else ObsidianNoir
                            )
                        ) {
                            Icon(
                                imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isStreaming) "Pause Stream" else "Run Story Stream", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isPlayingReel = !isPlayingReel },
                            enabled = completedScenes.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingReel) Icons.Default.Pause else Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPlayingReel) "Pause Reel" else "Play Reel (${completedScenes.size})")
                        }
                    }
                }

                // Animated Reel Previewer when scenes are completed
                if (completedScenes.isNotEmpty()) {
                    val activeScene = completedScenes[reelIndex % completedScenes.size]
                    AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Storyboard Reel Preview", style = MaterialTheme.typography.titleMedium)
                            Surface(color = ObsidianNoir, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "SCENE ${(reelIndex % completedScenes.size) + 1} / ${completedScenes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AtelierGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .aspectRatio(0.75f, matchHeightConstraintsFirst = true)
                                .clip(MaterialTheme.shapes.medium)
                                .background(ObsidianNoir),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = activeScene.imagePath?.let { File(it) },
                                contentDescription = "Storyboard scene ${activeScene.orderIndex}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Scene Queue List
                AtelierSectionCard {
                    Text("Scene Timeline (${scenes.size})", style = MaterialTheme.typography.titleMedium)

                    if (scenes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Queue is empty. Tap Auto 4-Scene Arc or Queue Rig above to start.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            scenes.forEach { scene ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 56.dp, height = 74.dp)
                                                .clip(MaterialTheme.shapes.small)
                                                .background(ObsidianNoir)
                                                .border(1.dp, AtelierGold.copy(alpha = 0.4f), MaterialTheme.shapes.small),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                scene.imagePath != null -> {
                                                    AsyncImage(
                                                        model = File(scene.imagePath),
                                                        contentDescription = "Scene ${scene.orderIndex}",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                scene.status == StoryboardStatus.GENERATING -> {
                                                    CircularProgressIndicator(color = AtelierGold, modifier = Modifier.size(22.dp))
                                                }
                                                else -> {
                                                    Text(
                                                        text = "#${scene.orderIndex}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = AtelierGold
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Scene #${scene.orderIndex}",
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                val statusColor = when (scene.status) {
                                                    StoryboardStatus.COMPLETED -> EmeraldStatus
                                                    StoryboardStatus.GENERATING -> AtelierGold
                                                    StoryboardStatus.ERROR -> TerracottaRose
                                                    StoryboardStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                Text(
                                                    text = scene.status.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = statusColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = scene.wardrobeTitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = scene.errorMessage ?: scene.prompt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (scene.errorMessage != null) TerracottaRose else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(onClick = { onRemoveScene(scene.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove scene")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
