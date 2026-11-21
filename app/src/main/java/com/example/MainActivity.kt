package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.data.ReferenceRole
import com.example.ui.components.DirectorPresetSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.AtelierScreen
import com.example.ui.screens.StoryboardScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.AtelierGold
import com.example.ui.theme.AuraStudioTheme
import com.example.ui.theme.ObsidianNoir
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.TerracottaRose
import com.example.viewmodel.AuraStudioViewModel
import com.example.viewmodel.StudioEvent

object AuraRoutes {
    const val STUDIO = "studio"
    const val ATELIER = "atelier"
    const val STORYBOARD = "storyboard"
    const val VAULT = "vault"
}

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val destinations = listOf(
    NavDestination(AuraRoutes.STUDIO, R.string.nav_studio, Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    NavDestination(AuraRoutes.ATELIER, R.string.nav_atelier, Icons.Filled.Checkroom, Icons.Outlined.Checkroom),
    NavDestination(AuraRoutes.STORYBOARD, R.string.nav_storyboard, Icons.Filled.Movie, Icons.Outlined.Movie),
    NavDestination(AuraRoutes.VAULT, R.string.nav_vault, Icons.Filled.Collections, Icons.Outlined.Collections)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AuraStudioTheme {
                AuraStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraStudioApp(viewModel: AuraStudioViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AuraRoutes.STUDIO

    val rig by viewModel.rig.collectAsStateWithLifecycle()
    val synthesisState by viewModel.synthesisState.collectAsStateWithLifecycle()
    val batchStatus by viewModel.batchStatus.collectAsStateWithLifecycle()
    val isLooping by viewModel.isLooping.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val vaultTakes by viewModel.vaultTakes.collectAsStateWithLifecycle()
    val allCostumes by viewModel.allCostumes.collectAsStateWithLifecycle()
    val storyboardQueue by viewModel.storyboardQueue.collectAsStateWithLifecycle()
    val isStoryStreaming by viewModel.isStoryStreaming.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDirectorSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudioEvent.Toast -> snackbarHostState.showSnackbar(event.message)
                is StudioEvent.ShareUri -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, event.title))
                }
            }
        }
    }

    if (showDirectorSheet) {
        DirectorPresetSheet(
            onDismiss = { showDirectorSheet = false },
            onSelectPresetIndex = { idx ->
                viewModel.loadDirectorPreset(idx)
                navController.navigate(AuraRoutes.STUDIO) {
                    launchSingleTop = true
                    popUpTo(AuraRoutes.STUDIO)
                }
            }
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            preferences = preferences,
            onSelectModel = viewModel::setModelOption,
            onToggleAutoRefine = viewModel::setAutoSafeRefine,
            onDismiss = { showSettingsSheet = false }
        )
    }

    val compiledPrompt = remember(rig, preferences.autoSafeRefine) {
        viewModel.compiledPrompt()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ObsidianNoir,
                                border = BorderStroke(1.dp, AtelierGold),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "A",
                                        fontFamily = PlayfairDisplayFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = AtelierGold
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = preferences.modelOption.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            onClick = { showDirectorSheet = true },
                            shape = CircleShape,
                            color = TerracottaRose,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("director_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = stringResource(id = R.string.cd_director),
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Director",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(id = R.string.cd_settings)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(AuraRoutes.STUDIO) { saveState = true }
                                    }
                                },
                                icon = {
                                    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == AuraRoutes.VAULT && vaultTakes.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${vaultTakes.size}") } }) {
                                            Icon(icon, contentDescription = stringResource(id = dest.labelRes))
                                        }
                                    } else {
                                        Icon(icon, contentDescription = stringResource(id = dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(id = dest.labelRes)) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(AuraRoutes.STUDIO) { saveState = true }
                                    }
                                },
                                icon = {
                                    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == AuraRoutes.VAULT && vaultTakes.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${vaultTakes.size}") } }) {
                                            Icon(icon, contentDescription = stringResource(id = dest.labelRes))
                                        }
                                    } else {
                                        Icon(icon, contentDescription = stringResource(id = dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(id = dest.labelRes)) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AuraRoutes.STUDIO,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AuraRoutes.STUDIO) {
                        StudioScreen(
                            rig = rig,
                            synthesisState = synthesisState,
                            compiledPrompt = compiledPrompt,
                            batchStatus = batchStatus,
                            isLooping = isLooping,
                            onAttachUris = { uris -> viewModel.attachReferences(uris) },
                            onSetPrimaryRef = viewModel::setPrimaryReference,
                            onUpdateRefRole = viewModel::updateReferenceRole,
                            onRemoveRef = viewModel::removeReference,
                            onClearRefs = viewModel::clearReferences,
                            onUpdateIdentityNotes = viewModel::updateIdentityNotes,
                            onSelectArchetype = viewModel::selectBodyArchetype,
                            onUpdateFraming = viewModel::updateFraming,
                            onUpdateYaw = viewModel::updateCameraYaw,
                            onUpdateRadar = viewModel::updateRadar,
                            onUpdateIntent = viewModel::updateIntent,
                            onRefinePrompt = viewModel::refineIntentPrompt,
                            onCleanPrompt = viewModel::cleanDuplicatePromptClauses,
                            onUpdateAspectRatio = viewModel::updateAspectRatio,
                            onUpdateBatchCount = viewModel::updateBatchCount,
                            onToggleCycleCostumes = viewModel::updateCycleCostumesInBatch,
                            onToggleCompare = viewModel::toggleCompareMode,
                            onToggleAnatomyNodes = viewModel::toggleAnatomyNodes,
                            onFocusAnatomyZone = viewModel::focusAnatomicalZone,
                            onPromoteActiveToRef = viewModel::promoteActiveSuccessToReference,
                            onShareActive = viewModel::shareActiveBitmap,
                            onSynthesizeSingle = viewModel::synthesizeSingle,
                            onRunBatch = viewModel::runBatchTryOn,
                            onToggleLoop = viewModel::toggleContinuousLoop,
                            onOpenAtelier = {
                                navController.navigate(AuraRoutes.ATELIER) { launchSingleTop = true }
                            },
                            onQueueStoryboard = {
                                viewModel.queueCurrentRigToStoryboard()
                                navController.navigate(AuraRoutes.STORYBOARD) { launchSingleTop = true }
                            },
                            isWideLayout = isWide
                        )
                    }

                    composable(AuraRoutes.ATELIER) {
                        AtelierScreen(
                            rig = rig,
                            allCostumes = allCostumes,
                            onAttachOutfitRef = { uris ->
                                viewModel.attachReferences(uris, ReferenceRole.OUTFIT_DRAPE)
                            },
                            onSelectCategory = viewModel::selectCategory,
                            onSearchChanged = viewModel::updateCostumeSearch,
                            onEquipCostume = viewModel::equipCostume,
                            onDeleteCustomCostume = viewModel::deleteCustomCostume,
                            onUpdateMatrixOuter = viewModel::updateMatrixOuter,
                            onUpdateMatrixInner = viewModel::updateMatrixInner,
                            onUpdateMatrixFabric = viewModel::updateMatrixFabric,
                            onUpdateMatrixColor = viewModel::updateMatrixColor,
                            onRandomizeMatrix = viewModel::randomizeMatrixAndEquip,
                            onSaveAndEquipCustom = viewModel::saveAndEquipMatrixLook,
                            onUpdateUnderlayer = viewModel::updateUnderlayer,
                            onUpdateSheerness = viewModel::updateSheerness,
                            onUpdateNavel = viewModel::updateNavelStyle,
                            onUpdateBust = viewModel::updateBust,
                            onUpdateWaist = viewModel::updateWaist,
                            onUpdateHips = viewModel::updateHips,
                            onUpdateLimbs = viewModel::updateLimbSoftness,
                            onUpdateSkin = viewModel::updateSkinSheen,
                            onToggleChip = viewModel::toggleAttributeChip,
                            onNavigateToStudio = {
                                navController.navigate(AuraRoutes.STUDIO) {
                                    launchSingleTop = true
                                    popUpTo(AuraRoutes.STUDIO)
                                }
                            }
                        )
                    }

                    composable(AuraRoutes.STORYBOARD) {
                        StoryboardScreen(
                            scenes = storyboardQueue,
                            isStreaming = isStoryStreaming,
                            onAutoPopulateArc = viewModel::autoPopulateStoryArc,
                            onQueueCurrentRig = viewModel::queueCurrentRigToStoryboard,
                            onRunStream = viewModel::runStoryboardStream,
                            onStopStream = viewModel::stopStoryboardStream,
                            onRemoveScene = viewModel::removeStoryboardScene,
                            onClearStoryboard = viewModel::clearStoryboard
                        )
                    }

                    composable(AuraRoutes.VAULT) {
                        VaultScreen(
                            takes = vaultTakes,
                            onLoadIntoViewport = { take ->
                                viewModel.loadVaultTakeIntoViewport(take)
                                navController.navigate(AuraRoutes.STUDIO) {
                                    launchSingleTop = true
                                    popUpTo(AuraRoutes.STUDIO)
                                }
                            },
                            onPromoteToReference = viewModel::promoteTakeToReference,
                            onShareTake = viewModel::shareVaultTake,
                            onDeleteTake = viewModel::deleteTake,
                            onExportAllZip = viewModel::exportAllVaultAsZip,
                            onClearVault = viewModel::clearVault,
                            isWideLayout = isWide
                        )
                    }
                }
            }
        }
    }
}
