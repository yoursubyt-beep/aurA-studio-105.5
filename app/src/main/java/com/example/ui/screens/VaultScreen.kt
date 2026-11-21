package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.VaultTakeEntity
import com.example.ui.components.AtelierSectionCard
import com.example.ui.components.FullscreenImageInspectorDialog
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PureSilkWhite
import com.example.ui.theme.TerracottaRose
import java.io.File

@Composable
fun VaultScreen(
    takes: List<VaultTakeEntity>,
    onLoadIntoViewport: (VaultTakeEntity) -> Unit,
    onPromoteToReference: (VaultTakeEntity) -> Unit,
    onShareTake: (VaultTakeEntity) -> Unit,
    onDeleteTake: (VaultTakeEntity) -> Unit,
    onExportAllZip: () -> Unit,
    onClearVault: () -> Unit,
    isWideLayout: Boolean
) {
    var inspectedTake by remember { mutableStateOf<VaultTakeEntity?>(null) }

    if (inspectedTake != null) {
        FullscreenImageInspectorDialog(
            imageModel = File(inspectedTake!!.filePath),
            caption = inspectedTake!!.title,
            onDismiss = { inspectedTake = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.4f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Generation Vault (${takes.size})", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Saved locally on device. Promote any frame to Reference Lock or export all as ZIP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onExportAllZip,
                        enabled = takes.isNotEmpty(),
                        modifier = Modifier.testTag("export_zip_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianNoir, contentColor = AtelierGold),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ZIP All", style = MaterialTheme.typography.labelMedium)
                    }
                    if (takes.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearVault,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear vault", tint = TerracottaRose, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (takes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = AtelierGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Your Vault Is Empty", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Generated frames from Studio and Storyboard appear here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isWideLayout) 4 else 2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(takes, key = { _, item -> item.id }) { index, take ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = ObsidianNoir,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onLoadIntoViewport(take) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(take.filePath),
                                contentDescription = take.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { inspectedTake = take }
                            )

                            Surface(
                                color = ObsidianNoir.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "#${takes.size - index}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AtelierGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteTake(take) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(ObsidianNoir.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete take", tint = PureSilkWhite, modifier = Modifier.size(16.dp))
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, ObsidianNoir.copy(alpha = 0.92f))
                                        )
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = take.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PureSilkWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onPromoteToReference(take) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Anchor, contentDescription = "Lock as ref", tint = AtelierGold, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Ref", style = MaterialTheme.typography.labelSmall, color = PureSilkWhite, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onShareTake(take) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = AtelierGold
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = ObsidianNoir, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Share", style = MaterialTheme.typography.labelSmall, color = ObsidianNoir, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
