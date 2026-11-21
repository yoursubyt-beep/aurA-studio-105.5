package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CostumeCategory
import com.example.data.CostumePreset
import com.example.data.PromptTools
import com.example.data.StudioCatalog
import com.example.ui.components.AtelierSectionCard
import com.example.ui.components.DropdownSelectorField
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PureSilkWhite
import com.example.ui.theme.TerracottaRose
import com.example.viewmodel.StudioRigState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtelierScreen(
    rig: StudioRigState,
    allCostumes: List<CostumePreset>,
    onAttachOutfitRef: (List<android.net.Uri>) -> Unit,
    onSelectCategory: (CostumeCategory) -> Unit,
    onSearchChanged: (String) -> Unit,
    onEquipCostume: (CostumePreset) -> Unit,
    onDeleteCustomCostume: (String) -> Unit,
    onUpdateMatrixOuter: (String) -> Unit,
    onUpdateMatrixInner: (String) -> Unit,
    onUpdateMatrixFabric: (String) -> Unit,
    onUpdateMatrixColor: (String) -> Unit,
    onRandomizeMatrix: () -> Unit,
    onSaveAndEquipCustom: (String?, String?) -> Unit,
    onUpdateUnderlayer: (String) -> Unit,
    onUpdateSheerness: (String) -> Unit,
    onUpdateNavel: (String) -> Unit,
    onUpdateBust: (Int) -> Unit,
    onUpdateWaist: (Int) -> Unit,
    onUpdateHips: (Int) -> Unit,
    onUpdateLimbs: (Int) -> Unit,
    onUpdateSkin: (Int) -> Unit,
    onToggleChip: (String) -> Unit,
    onNavigateToStudio: () -> Unit
) {
    var customTitle by remember { mutableStateOf("") }
    var customPrompt by remember { mutableStateOf("") }

    val outfitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAttachOutfitRef(listOf(uri))
    }

    val filteredCostumes = remember(allCostumes, rig.selectedCategory, rig.costumeSearchQuery) {
        val q = rig.costumeSearchQuery.trim().lowercase()
        allCostumes.filter { item ->
            val matchesCat = rig.selectedCategory == CostumeCategory.ALL || item.category == rig.selectedCategory
            val matchesQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.prompt.lowercase().contains(q)
            matchesCat && matchesQuery
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
                // 4-Axis Combinatorial Wardrobe Matrix
                AtelierSectionCard(borderColor = AtelierGold.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlimited Costume Matrix", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Combine outer cuts, inner garments, fabric transfer & colorways",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                outfitPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Outfit Ref", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    DropdownSelectorField(
                        label = "1. Outer Layer / Silhouette Cut",
                        selectedValue = rig.matrixOuter,
                        options = StudioCatalog.outerLayers,
                        onSelected = onUpdateMatrixOuter
                    )
                    DropdownSelectorField(
                        label = "2. Inner / Base Garment",
                        selectedValue = rig.matrixInner,
                        options = StudioCatalog.innerGarments,
                        onSelected = onUpdateMatrixInner
                    )
                    DropdownSelectorField(
                        label = "3. Fabric Transfer & Weave",
                        selectedValue = rig.matrixFabric,
                        options = StudioCatalog.fabricWeaves,
                        onSelected = onUpdateMatrixFabric
                    )
                    DropdownSelectorField(
                        label = "4. Colorway & Tone",
                        selectedValue = rig.matrixColor,
                        options = StudioCatalog.colorways,
                        onSelected = onUpdateMatrixColor
                    )

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Custom Look Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("Or Type Custom Costume Description") },
                        placeholder = { Text("e.g. sheer ivory linen shirt over black lace bralette") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_costume_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRandomizeMatrix,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("randomize_matrix_button")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Randomize")
                        }
                        Button(
                            onClick = {
                                onSaveAndEquipCustom(customTitle, customPrompt)
                                customTitle = ""
                                customPrompt = ""
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("save_equip_costume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianNoir, contentColor = AtelierGold)
                        ) {
                            Text("Save & Equip Look", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Body Proportions & Anatomical Visibility Rig
                AtelierSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anatomical & Layering Rig", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = PromptTools.formatMeasurementsSummary(rig.bustCm, rig.waistCm, rig.hipsCm),
                            style = MaterialTheme.typography.labelSmall,
                            color = AtelierGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    MeasurementSlider(
                        label = "Bust / Neckline",
                        valueText = "${rig.bustCm} cm (${PromptTools.cupSizeFromBustCm(rig.bustCm)})",
                        value = rig.bustCm.toFloat(),
                        range = 76f..116f,
                        onValueChange = { onUpdateBust(it.roundToInt()) }
                    )
                    MeasurementSlider(
                        label = "Waist / Midriff",
                        valueText = "${rig.waistCm} cm (${(rig.waistCm / 2.54).roundToInt()}\")",
                        value = rig.waistCm.toFloat(),
                        range = 54f..94f,
                        onValueChange = { onUpdateWaist(it.roundToInt()) }
                    )
                    MeasurementSlider(
                        label = "Hips & Curves",
                        valueText = "${rig.hipsCm} cm (${(rig.hipsCm / 2.54).roundToInt()}\")",
                        value = rig.hipsCm.toFloat(),
                        range = 80f..122f,
                        onValueChange = { onUpdateHips(it.roundToInt()) }
                    )

                    val limbLabel = when {
                        rig.limbSoftness < 35 -> "Slender Toned"
                        rig.limbSoftness < 70 -> "Natural Balanced"
                        rig.limbSoftness < 85 -> "Natural Soft"
                        else -> "Lush Full Curves"
                    }
                    MeasurementSlider(
                        label = "Arms & Thighs Softness",
                        valueText = "$limbLabel (${rig.limbSoftness}%)",
                        value = rig.limbSoftness.toFloat(),
                        range = 0f..100f,
                        onValueChange = { onUpdateLimbs(it.roundToInt()) }
                    )

                    val skinLabel = when {
                        rig.skinSheen < 35 -> "Natural Matte"
                        rig.skinSheen < 68 -> "Dewy Glow"
                        else -> "Glistening Sheen"
                    }
                    MeasurementSlider(
                        label = "Skin Sheen / Finish",
                        valueText = skinLabel,
                        value = rig.skinSheen.toFloat(),
                        range = 0f..100f,
                        onValueChange = { onUpdateSkin(it.roundToInt()) }
                    )

                    DropdownSelectorField(
                        label = "Navel / Bellybutton Style",
                        selectedValue = rig.navelStyle,
                        options = StudioCatalog.navelOptions,
                        onSelected = onUpdateNavel
                    )
                    DropdownSelectorField(
                        label = "Inner Layer / Lingerie Line",
                        selectedValue = rig.underlayer,
                        options = StudioCatalog.underlayerOptions,
                        onSelected = onUpdateUnderlayer
                    )
                    DropdownSelectorField(
                        label = "Thin-Fabric Transfer & Fit",
                        selectedValue = rig.sheerness,
                        options = StudioCatalog.sheernessOptions,
                        onSelected = onUpdateSheerness
                    )

                    Text(
                        text = "SILHOUETTE FOCUS CHIPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rig.attributeChips.forEach { chip ->
                            FilterChip(
                                selected = chip.isActive,
                                onClick = { onToggleChip(chip.id) },
                                label = { Text(chip.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ObsidianNoir,
                                    selectedLabelColor = AtelierGold
                                )
                            )
                        }
                    }
                }

                // Curated & Saved Costumes Catalog
                AtelierSectionCard {
                    Text("Curated & Saved Costume Catalog", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = rig.costumeSearchQuery,
                        onValueChange = onSearchChanged,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search halter, slit, navel, sheer...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CostumeCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = rig.selectedCategory == cat,
                                onClick = { onSelectCategory(cat) },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ObsidianNoir,
                                    selectedLabelColor = AtelierGold
                                )
                            )
                        }
                    }

                    if (filteredCostumes.isEmpty()) {
                        Text(
                            text = "No costumes match your filter. Use the Matrix above to create custom silhouettes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredCostumes.forEach { costume ->
                                val isActive = rig.activeCostume.id == costume.id ||
                                    rig.activeCostume.prompt == costume.prompt
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            onEquipCostume(costume)
                                            onNavigateToStudio()
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) AtelierGold else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(costume.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = costume.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (costume.isCustom) {
                                            IconButton(
                                                onClick = { onDeleteCustomCostume(costume.id) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete custom costume",
                                                    tint = TerracottaRose
                                                )
                                            }
                                        }
                                        if (isActive) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Equipped",
                                                tint = AtelierGold
                                            )
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

@Composable
private fun MeasurementSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = AtelierGold, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = AtelierGold, activeTrackColor = AtelierGold)
        )
    }
}
