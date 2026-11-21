package com.example.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.ai.AuraAiEngine
import com.example.data.AspectRatioOption
import com.example.data.AttributeChip
import com.example.data.AuraRepository
import com.example.data.BodyArchetype
import com.example.data.CostumeCategory
import com.example.data.CostumePreset
import com.example.data.GeminiImageModelOption
import com.example.data.PromptTools
import com.example.data.ReferenceAttachment
import com.example.data.ReferenceRole
import com.example.data.StoryboardScene
import com.example.data.StoryboardStatus
import com.example.data.StudioCatalog
import com.example.data.StudioPreferences
import com.example.data.SynthesisUiState
import com.example.data.local.VaultTakeEntity
import com.example.data.remote.CloudCustomCostume
import com.example.data.remote.CloudStoryboardScene
import com.example.data.remote.CloudUserProfile
import com.example.data.remote.FirestoreRepository
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AuraStudioViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

data class StudioRigState(
    val references: List<ReferenceAttachment> = emptyList(),
    val identityNotes: String = "",
    val archetype: BodyArchetype = BodyArchetype.SOFT_CURVE,
    val bustCm: Int = BodyArchetype.SOFT_CURVE.bustCm,
    val waistCm: Int = BodyArchetype.SOFT_CURVE.waistCm,
    val hipsCm: Int = BodyArchetype.SOFT_CURVE.hipsCm,
    val limbSoftness: Int = BodyArchetype.SOFT_CURVE.limbSoftness,
    val skinSheen: Int = BodyArchetype.SOFT_CURVE.skinSheen,
    val navelStyle: String = StudioCatalog.navelOptions.first(),
    val activeCostume: CostumePreset = StudioCatalog.builtinCostumes.first(),
    val underlayer: String = StudioCatalog.underlayerOptions.first(),
    val sheerness: String = StudioCatalog.sheernessOptions.first(),
    val attributeChips: List<AttributeChip> = StudioCatalog.defaultChips,
    val shotFraming: String = StudioCatalog.framingOptions.first(),
    val cameraYaw: Int = 0,
    val radarX: Float = 0f,
    val radarY: Float = 0f,
    val userIntent: String = "Keep the exact same person from the locked reference images, editorial fashion & silhouette study with one arm raised gently overhead framing the neck and natural underarm, open side-cut neckline showcasing cleavage, bare midriff with pierced bellybutton, and seated stance highlighting natural soft upper arms, thighs, and bare legs, warm golden studio light.",
    val aspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_3_4,
    val batchCount: Int = 4,
    val cycleCostumesInBatch: Boolean = false,
    val matrixOuter: String = StudioCatalog.outerLayers.first(),
    val matrixInner: String = StudioCatalog.innerGarments.first(),
    val matrixFabric: String = StudioCatalog.fabricWeaves.first(),
    val matrixColor: String = StudioCatalog.colorways.first(),
    val selectedCategory: CostumeCategory = CostumeCategory.ALL,
    val costumeSearchQuery: String = "",
    val compareModeActive: Boolean = false,
    val showAnatomyNodes: Boolean = true
)

sealed interface StudioEvent {
    data class Toast(val message: String, val isError: Boolean = false) : StudioEvent
    data class ShareUri(val uri: Uri, val mimeType: String, val title: String) : StudioEvent
}

private data class SceneRuntime(
    val status: StoryboardStatus = StoryboardStatus.QUEUED,
    val imagePath: String? = null,
    val errorMessage: String? = null
)

class AuraStudioViewModel(
    private val repository: AuraRepository,
    private val aiEngine: AuraAiEngine,
    private val firestoreRepository: FirestoreRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String
) : ViewModel() {

    private val _rig = MutableStateFlow(StudioRigState())
    val rig: StateFlow<StudioRigState> = _rig.asStateFlow()

    private val _synthesisState = MutableStateFlow<SynthesisUiState>(SynthesisUiState.Idle)
    val synthesisState: StateFlow<SynthesisUiState> = _synthesisState.asStateFlow()

    private val _batchStatus = MutableStateFlow<String?>(null)
    val batchStatus: StateFlow<String?> = _batchStatus.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _sceneRuntime = MutableStateFlow<Map<String, SceneRuntime>>(emptyMap())

    private val _isStoryStreaming = MutableStateFlow(false)
    val isStoryStreaming: StateFlow<Boolean> = _isStoryStreaming.asStateFlow()

    private val _dismissedCloudError = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<StudioEvent>()
    val events: SharedFlow<StudioEvent> = _events.asSharedFlow()

    private val cloudProfileState: StateFlow<CloudUiState<CloudUserProfile?>> =
        firestoreRepository.observeUserProfile(currentUserId)
            .map<CloudUserProfile?, CloudUiState<CloudUserProfile?>> { CloudUiState.Success(it) }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing cloud profile", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to sync profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val cloudCostumesState: StateFlow<CloudUiState<List<CostumePreset>>> =
        firestoreRepository.observeCustomCostumes(currentUserId)
            .map<List<CloudCustomCostume>, CloudUiState<List<CostumePreset>>> { items ->
                CloudUiState.Success(items.map { it.toDomain() })
            }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing cloud costumes", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to sync custom costumes"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val cloudStoryboardState: StateFlow<CloudUiState<List<CloudStoryboardScene>>> =
        firestoreRepository.observeStoryboardScenes(currentUserId)
            .map<List<CloudStoryboardScene>, CloudUiState<List<CloudStoryboardScene>>> {
                CloudUiState.Success(it)
            }
            .catch { error ->
                val rootError = unwrapFirestoreError(error)
                Log.w(TAG, "Error observing cloud storyboard", rootError)
                emit(CloudUiState.Error(rootError.localizedMessage ?: "Failed to sync storyboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val cloudSyncError: StateFlow<String?> = combine(
        cloudProfileState,
        cloudCostumesState,
        cloudStoryboardState,
        _dismissedCloudError
    ) { profileState, costumesState, storyState, dismissed ->
        val err = (profileState as? CloudUiState.Error)?.message
            ?: (costumesState as? CloudUiState.Error)?.message
            ?: (storyState as? CloudUiState.Error)?.message
        if (err != null && err != dismissed) err else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    val preferences: StateFlow<StudioPreferences> = combine(
        repository.preferences,
        cloudProfileState
    ) { localPrefs, cloudState ->
        val cloudProfile = (cloudState as? CloudUiState.Success)?.data
        if (cloudProfile != null) {
            StudioPreferences(
                modelOption = cloudProfile.resolveModelOption(),
                autoSafeRefine = cloudProfile.autoSafeRefine
            )
        } else {
            localPrefs
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StudioPreferences()
    )

    val vaultTakes: StateFlow<List<VaultTakeEntity>> = repository.vaultTakes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    private val localCustomCostumes: StateFlow<List<CostumePreset>> = repository.customCostumes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val allCostumes: StateFlow<List<CostumePreset>> = combine(
        cloudCostumesState,
        localCustomCostumes
    ) { cloudState, local ->
        val cloudItems = (cloudState as? CloudUiState.Success)?.data.orEmpty()
        val cloudKeys = cloudItems.map { it.title to it.prompt }.toSet()
        val localExtras = local.filterNot { (it.title to it.prompt) in cloudKeys }
        cloudItems + localExtras + StudioCatalog.builtinCostumes
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StudioCatalog.builtinCostumes
    )

    val storyboardQueue: StateFlow<List<StoryboardScene>> = combine(
        cloudStoryboardState,
        _sceneRuntime
    ) { cloudState, runtime ->
        val cloudScenes = (cloudState as? CloudUiState.Success)?.data.orEmpty()
        cloudScenes.map { cloudScene ->
            val rt = runtime[cloudScene.id] ?: SceneRuntime()
            cloudScene.toDomain(
                status = rt.status,
                imagePath = rt.imagePath,
                errorMessage = rt.errorMessage
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    private var activeGenerationJob: Job? = null
    private var storyJob: Job? = null

    init {
        viewModelScope.launch {
            val initialPrefs = repository.preferences.first()
            firestoreRepository.ensureUserProfile(
                userId = currentUserId,
                displayName = currentUserDisplayName,
                modelId = initialPrefs.modelOption.modelId,
                autoSafeRefine = initialPrefs.autoSafeRefine
            )
        }
    }

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    fun dismissCloudSyncError() {
        _dismissedCloudError.value = cloudSyncError.value
    }

    fun compiledPrompt(customOutfitPrompt: String? = null, angleOffset: Int = 0): String {
        val state = _rig.value
        val prefs = preferences.value
        return PromptTools.compileStudioPrompt(
            intent = state.userIntent,
            autoRefine = prefs.autoSafeRefine,
            outfitPrompt = customOutfitPrompt ?: state.activeCostume.prompt,
            underlayer = state.underlayer,
            sheerness = state.sheerness,
            navelStyle = state.navelStyle,
            framing = state.shotFraming,
            identityNotes = state.identityNotes,
            archetype = state.archetype,
            bustCm = state.bustCm,
            waistCm = state.waistCm,
            hipsCm = state.hipsCm,
            limbSoftness = state.limbSoftness,
            skinSheen = state.skinSheen,
            activeChips = state.attributeChips,
            references = state.references,
            cameraYaw = state.cameraYaw,
            radarX = state.radarX,
            radarY = state.radarY,
            angleOffset = angleOffset
        )
    }

    // --- Reference Management ---
    fun attachReferences(uris: List<Uri>, defaultRole: ReferenceRole = ReferenceRole.IDENTITY_FACE) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val current = _rig.value.references.toMutableList()
            uris.take(6 - current.size).forEach { uri ->
                val (cachedUri, name) = aiEngine.copyUriToInternalReferenceCache(uri)
                val isFirst = current.isEmpty()
                current.add(
                    ReferenceAttachment(
                        id = "ref_${System.currentTimeMillis()}_${(100..999).random()}",
                        uriString = cachedUri,
                        displayName = name,
                        role = if (isFirst) ReferenceRole.IDENTITY_FACE else defaultRole,
                        isPrimary = isFirst,
                        seed = PromptTools.computeDeterministicSeed(cachedUri + name)
                    )
                )
            }
            _rig.update { it.copy(references = current) }
            emitToast("Locked ${current.size} reference attachment(s)")
        }
    }

    fun setPrimaryReference(id: String) {
        _rig.update { state ->
            state.copy(references = state.references.map { it.copy(isPrimary = it.id == id) })
        }
    }

    fun updateReferenceRole(id: String, role: ReferenceRole) {
        _rig.update { state ->
            state.copy(references = state.references.map { if (it.id == id) it.copy(role = role) else it })
        }
    }

    fun removeReference(id: String) {
        _rig.update { state ->
            val filtered = state.references.filterNot { it.id == id }
            val normalized = if (filtered.isNotEmpty() && filtered.none { it.isPrimary }) {
                filtered.mapIndexed { idx, item -> item.copy(isPrimary = idx == 0) }
            } else filtered
            state.copy(
                references = normalized,
                compareModeActive = if (normalized.isEmpty()) false else state.compareModeActive
            )
        }
    }

    fun clearReferences() {
        _rig.update { it.copy(references = emptyList(), compareModeActive = false) }
        emitToast("Cleared reference attachments")
    }

    fun promoteTakeToReference(take: VaultTakeEntity) {
        val current = _rig.value.references
        if (current.size >= 6) {
            emitToast("Maximum 6 reference attachments reached", isError = true)
            return
        }
        val uriString = Uri.fromFile(java.io.File(take.filePath)).toString()
        val isFirst = current.isEmpty()
        val newRef = ReferenceAttachment(
            id = "ref_${System.currentTimeMillis()}",
            uriString = uriString,
            displayName = take.title,
            role = ReferenceRole.IDENTITY_FACE,
            isPrimary = isFirst,
            seed = PromptTools.computeDeterministicSeed(take.filePath)
        )
        _rig.update { it.copy(references = current + newRef) }
        emitToast("Locked take as reference #${current.size + 1}")
    }

    fun promoteActiveSuccessToReference() {
        val success = _synthesisState.value as? SynthesisUiState.Success ?: return
        val take = vaultTakes.value.firstOrNull { it.id == success.takeId }
        if (take != null) {
            promoteTakeToReference(take)
        }
    }

    // --- Rig & Body Controls ---
    fun selectBodyArchetype(archetype: BodyArchetype) {
        _rig.update {
            it.copy(
                archetype = archetype,
                bustCm = archetype.bustCm,
                waistCm = archetype.waistCm,
                hipsCm = archetype.hipsCm,
                limbSoftness = archetype.limbSoftness,
                skinSheen = archetype.skinSheen
            )
        }
    }

    fun updateBust(v: Int) = _rig.update { it.copy(bustCm = v) }
    fun updateWaist(v: Int) = _rig.update { it.copy(waistCm = v) }
    fun updateHips(v: Int) = _rig.update { it.copy(hipsCm = v) }
    fun updateLimbSoftness(v: Int) = _rig.update { it.copy(limbSoftness = v) }
    fun updateSkinSheen(v: Int) = _rig.update { it.copy(skinSheen = v) }
    fun updateNavelStyle(v: String) = _rig.update { it.copy(navelStyle = v) }
    fun updateUnderlayer(v: String) = _rig.update { it.copy(underlayer = v) }
    fun updateSheerness(v: String) = _rig.update { it.copy(sheerness = v) }
    fun updateFraming(v: String) = _rig.update { it.copy(shotFraming = v) }
    fun updateCameraYaw(v: Int) = _rig.update { it.copy(cameraYaw = v) }
    fun updateRadar(x: Float, y: Float) = _rig.update { it.copy(radarX = x, radarY = y) }
    fun updateIntent(v: String) = _rig.update { it.copy(userIntent = v) }
    fun updateIdentityNotes(v: String) = _rig.update { it.copy(identityNotes = v) }
    fun updateAspectRatio(v: AspectRatioOption) = _rig.update { it.copy(aspectRatio = v) }
    fun updateBatchCount(v: Int) = _rig.update { it.copy(batchCount = v) }
    fun updateCycleCostumesInBatch(v: Boolean) = _rig.update { it.copy(cycleCostumesInBatch = v) }
    fun toggleCompareMode() = _rig.update {
        if (it.references.isEmpty()) {
            emitToast("Attach a reference photo to compare", isError = true)
            it
        } else it.copy(compareModeActive = !it.compareModeActive)
    }
    fun toggleAnatomyNodes() = _rig.update { it.copy(showAnatomyNodes = !it.showAnatomyNodes) }

    fun toggleAttributeChip(chipId: String) {
        _rig.update { state ->
            state.copy(
                attributeChips = state.attributeChips.map {
                    if (it.id == chipId) it.copy(isActive = !it.isActive) else it
                }
            )
        }
    }

    fun refineIntentPrompt() {
        val current = _rig.value.userIntent
        val refined = PromptTools.refinePromptEditorial(current)
        _rig.update { it.copy(userIntent = refined) }
        emitToast(if (refined != current) "Refined prompt for editorial compatibility" else "Prompt is already clean")
    }

    fun cleanDuplicatePromptClauses() {
        val cleaned = PromptTools.cleanDuplicateClauses(_rig.value.userIntent)
        _rig.update { it.copy(userIntent = cleaned) }
        emitToast("Cleaned duplicate phrases")
    }

    fun focusAnatomicalZone(zoneKey: String) {
        val addition = when (zoneKey) {
            "neck" -> "focus on graceful neck, collarbone, and open shoulders"
            "underarms" -> "arm raised gently framing smooth natural underarms and side-chest silhouette"
            "cleavage" -> "flattering low-cut neckline framing natural cleavage line"
            "navel" -> "exposed bare midriff highlighting pierced bellybutton crystal navel ring"
            "arms" -> "natural unretouched soft upper-arm contours clearly visible"
            "legs" -> "seated pose highlighting natural soft thighs and bare legs"
            else -> return
        }
        val current = _rig.value.userIntent.trim().removeSuffix(".")
        _rig.update { it.copy(userIntent = "$current, $addition.") }
        synthesizeSingle()
    }

    // --- Atelier & Costume Management (Synced with Firestore) ---
    fun selectCategory(cat: CostumeCategory) = _rig.update { it.copy(selectedCategory = cat) }
    fun updateCostumeSearch(q: String) = _rig.update { it.copy(costumeSearchQuery = q) }
    fun equipCostume(preset: CostumePreset) {
        _rig.update { it.copy(activeCostume = preset) }
        emitToast("Equipped: ${preset.title}")
    }

    fun updateMatrixOuter(v: String) = _rig.update { it.copy(matrixOuter = v) }
    fun updateMatrixInner(v: String) = _rig.update { it.copy(matrixInner = v) }
    fun updateMatrixFabric(v: String) = _rig.update { it.copy(matrixFabric = v) }
    fun updateMatrixColor(v: String) = _rig.update { it.copy(matrixColor = v) }

    fun randomizeMatrixAndEquip() {
        val outer = StudioCatalog.outerLayers.random()
        val inner = StudioCatalog.innerGarments.random()
        val fabric = StudioCatalog.fabricWeaves.random()
        val color = StudioCatalog.colorways.random()
        _rig.update {
            it.copy(
                matrixOuter = outer,
                matrixInner = inner,
                matrixFabric = fabric,
                matrixColor = color
            )
        }
        saveAndEquipMatrixLook(
            customTitle = "Matrix: ${color.substringBefore(" ").replaceFirstChar { c -> c.uppercase() }} Silhouette",
            customPrompt = null
        )
    }

    fun saveAndEquipMatrixLook(customTitle: String?, customPrompt: String?) {
        val state = _rig.value
        val prompt = (customPrompt?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${state.matrixColor} ${state.matrixOuter} layered over ${state.matrixInner}, crafted in ${state.matrixFabric}")
            .take(1200)
        val title = (customTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Custom: ${prompt.take(28)}...").take(120)
        viewModelScope.launch {
            val cloudResult = firestoreRepository.addCustomCostume(
                userId = currentUserId,
                title = title,
                prompt = prompt,
                category = CostumeCategory.CUSTOM.name
            )
            val createdId = cloudResult.getOrNull() ?: "cust_${System.currentTimeMillis()}"
            if (cloudResult.isFailure) {
                repository.saveCustomCostume(title, prompt)
            }
            val created = CostumePreset(
                id = createdId,
                category = CostumeCategory.CUSTOM,
                title = title,
                prompt = prompt,
                isCustom = true
            )
            _rig.update { it.copy(activeCostume = created) }
            emitToast(
                if (cloudResult.isSuccess) "Saved to Cloud Atelier & equipped \"$title\""
                else "Saved locally & equipped \"$title\""
            )
        }
    }

    fun deleteCustomCostume(id: String) {
        viewModelScope.launch {
            firestoreRepository.deleteCustomCostume(currentUserId, id)
            repository.deleteCustomCostume(id)
        }
    }

    fun loadDirectorPreset(index: Int) {
        val preset = StudioCatalog.builtinCostumes.getOrNull(index) ?: return
        _rig.update {
            it.copy(
                activeCostume = preset,
                userIntent = "Keep the exact same person from the locked reference images, wearing ${preset.prompt}, showcasing neck, natural underarms, side-torso cut, neckline, pierced bellybutton, and natural soft arms, thighs, and bare legs in warm editorial light."
            )
        }
        emitToast("Loaded ${preset.title}")
        synthesizeSingle()
    }

    // --- Settings (Synced with Firestore + Local DataStore) ---
    fun setModelOption(option: GeminiImageModelOption) {
        viewModelScope.launch {
            repository.setModelOption(option)
            val autoRefine = preferences.value.autoSafeRefine
            firestoreRepository.updateUserPreferences(
                userId = currentUserId,
                displayName = currentUserDisplayName,
                modelId = option.modelId,
                autoSafeRefine = autoRefine
            )
        }
    }

    fun setAutoSafeRefine(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSafeRefine(enabled)
            val modelId = preferences.value.modelOption.modelId
            firestoreRepository.updateUserPreferences(
                userId = currentUserId,
                displayName = currentUserDisplayName,
                modelId = modelId,
                autoSafeRefine = enabled
            )
        }
    }

    // --- Synthesis Operations ---
    fun synthesizeSingle() {
        if (_synthesisState.value is SynthesisUiState.Loading) return
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            executeSingleSynthesis(
                customOutfitPrompt = null,
                customOutfitTitle = null,
                angleOffset = 0,
                statusLabel = "Synthesizing high-resolution frame..."
            )
        }
    }

    fun runBatchTryOn() {
        if (_synthesisState.value is SynthesisUiState.Loading || _isLooping.value) {
            emitToast("Generation already in progress", isError = true)
            return
        }
        val state = _rig.value
        val count = state.batchCount
        val costumePool = allCostumes.value.ifEmpty { StudioCatalog.builtinCostumes }
        activeGenerationJob = viewModelScope.launch {
            try {
                for (i in 0 until count) {
                    val swap = state.cycleCostumesInBatch
                    val chosen = if (swap) costumePool[i % costumePool.size] else state.activeCostume
                    val angle = if (swap) 0 else i * 30
                    _batchStatus.value = "Batch ${i + 1} / $count"
                    val ok = executeSingleSynthesis(
                        customOutfitPrompt = chosen.prompt,
                        customOutfitTitle = chosen.title,
                        angleOffset = angle,
                        statusLabel = "Batch take ${i + 1} of $count..."
                    )
                    if (!ok) break
                }
                emitToast("Batch try-on complete ($count takes)")
            } finally {
                _batchStatus.value = null
            }
        }
    }

    fun toggleContinuousLoop() {
        if (_isLooping.value) {
            _isLooping.value = false
            activeGenerationJob?.cancel()
            _batchStatus.value = null
            emitToast("Continuous loop stopped")
            return
        }
        _isLooping.value = true
        val costumePool = allCostumes.value.ifEmpty { StudioCatalog.builtinCostumes }
        activeGenerationJob = viewModelScope.launch {
            var step = 0
            try {
                while (_isLooping.value) {
                    val state = _rig.value
                    val chosen = if (state.cycleCostumesInBatch) {
                        costumePool[step % costumePool.size]
                    } else state.activeCostume
                    val angle = (step * 15) % 360
                    _batchStatus.value = "Loop Take #${step + 1}"
                    val ok = executeSingleSynthesis(
                        customOutfitPrompt = chosen.prompt,
                        customOutfitTitle = chosen.title,
                        angleOffset = angle,
                        statusLabel = "Continuous loop frame #${step + 1}..."
                    )
                    if (!ok) {
                        _isLooping.value = false
                        break
                    }
                    step++
                    delay(400)
                }
            } finally {
                _isLooping.value = false
                _batchStatus.value = null
            }
        }
    }

    private suspend fun executeSingleSynthesis(
        customOutfitPrompt: String?,
        customOutfitTitle: String?,
        angleOffset: Int,
        statusLabel: String
    ): Boolean {
        _synthesisState.value = SynthesisUiState.Loading(statusLabel)
        val state = _rig.value
        val prefs = preferences.value
        val prompt = compiledPrompt(customOutfitPrompt, angleOffset)
        val outfitTitle = customOutfitTitle ?: state.activeCostume.title

        return try {
            val bitmap = aiEngine.synthesizeImage(
                prompt = prompt,
                modelOption = prefs.modelOption,
                aspectRatioOption = state.aspectRatio,
                references = state.references
            )
            val saved = repository.saveGeneratedTake(
                bitmap = bitmap,
                title = "${state.archetype.label} • ${outfitTitle.take(26)}",
                outfitTitle = outfitTitle,
                prompt = prompt,
                aspectRatio = state.aspectRatio.ratioText
            )
            _synthesisState.value = SynthesisUiState.Success(bitmap, saved.title, saved.id)
            emitToast("Saved take to Vault")
            true
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            _synthesisState.value = SynthesisUiState.Error(
                "Image generation timed out after 60 seconds. Please retry."
            )
            false
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quota exceeded", e)
            _synthesisState.value = SynthesisUiState.Error(
                "Gemini image generation quota exceeded. Please try again later or check Firebase Blaze billing."
            )
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image synthesis failed", e)
            _synthesisState.value = SynthesisUiState.Error(
                e.localizedMessage ?: "Image synthesis failed."
            )
            false
        }
    }

    fun loadVaultTakeIntoViewport(take: VaultTakeEntity) {
        viewModelScope.launch {
            val bmp = repository.loadBitmapFromPath(take.filePath)
            if (bmp != null) {
                _synthesisState.value = SynthesisUiState.Success(bmp, take.title, take.id)
            } else {
                emitToast("Could not load image file", isError = true)
            }
        }
    }

    fun deleteTake(take: VaultTakeEntity) {
        viewModelScope.launch { repository.deleteTake(take) }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearVault()
            emitToast("Vault cleared")
        }
    }

    fun shareActiveBitmap() {
        val state = _synthesisState.value as? SynthesisUiState.Success ?: return
        viewModelScope.launch {
            val uri = repository.createShareUriForBitmap(state.bitmap, "aura_studio")
            _events.emit(StudioEvent.ShareUri(uri, "image/jpeg", "Share Studio Take"))
        }
    }

    fun shareVaultTake(take: VaultTakeEntity) {
        viewModelScope.launch {
            val uri = repository.createShareUriForFile(take.filePath)
            if (uri != null) {
                _events.emit(StudioEvent.ShareUri(uri, "image/jpeg", "Share ${take.title}"))
            } else {
                emitToast("File not found", isError = true)
            }
        }
    }

    fun exportAllVaultAsZip() {
        val takes = vaultTakes.value
        if (takes.isEmpty()) {
            emitToast("Vault is empty", isError = true)
            return
        }
        viewModelScope.launch {
            val zipUri = repository.exportVaultAsZip(takes)
            if (zipUri != null) {
                _events.emit(StudioEvent.ShareUri(zipUri, "application/zip", "Export Aura Studio ZIP"))
            } else {
                emitToast("Could not build ZIP archive", isError = true)
            }
        }
    }

    // --- Storyboard (Synced with Firestore) ---
    fun queueCurrentRigToStoryboard() {
        val state = _rig.value
        val nextIndex = ((storyboardQueue.value.maxOfOrNull { it.orderIndex } ?: 0) + 1).coerceIn(1, 500)
        val compiled = compiledPrompt()
        viewModelScope.launch {
            val result = firestoreRepository.addStoryboardScene(
                userId = currentUserId,
                orderIndex = nextIndex,
                wardrobeTitle = state.activeCostume.title,
                wardrobePrompt = state.activeCostume.prompt,
                prompt = compiled
            )
            if (result.isSuccess) {
                emitToast("Queued Scene #$nextIndex to Cloud Storyboard")
            } else {
                emitToast(result.exceptionOrNull()?.localizedMessage ?: "Failed to queue scene", isError = true)
            }
        }
    }

    fun autoPopulateStoryArc() {
        val state = _rig.value
        val beats = listOf(
            "Opening close-up framing neck, natural underarms, collarbone, and direct gaze",
            "Three-quarter turn highlighting side-torso cut and flattering neckline cleavage",
            "Waist-up framing bare midriff and pierced bellybutton crystal navel ring",
            "Seated three-quarter view highlighting natural soft upper arms, thighs, and bare legs"
        )
        val baseIndex = (storyboardQueue.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
        viewModelScope.launch {
            var addedCount = 0
            beats.forEachIndexed { offset, beat ->
                val orderIdx = (baseIndex + offset).coerceIn(1, 500)
                val fullPrompt = compiledPrompt(state.activeCostume.prompt, offset * 20) + " Focus: $beat."
                val res = firestoreRepository.addStoryboardScene(
                    userId = currentUserId,
                    orderIndex = orderIdx,
                    wardrobeTitle = state.activeCostume.title,
                    wardrobePrompt = state.activeCostume.prompt,
                    prompt = fullPrompt
                )
                if (res.isSuccess) addedCount++
            }
            if (addedCount > 0) {
                emitToast("Added $addedCount-Scene Arc to Cloud Storyboard")
            } else {
                emitToast("Could not sync storyboard scenes", isError = true)
            }
        }
    }

    fun removeStoryboardScene(id: Long) {
        val scene = storyboardQueue.value.firstOrNull { it.id == id } ?: return
        val docId = scene.cloudDocId ?: return
        viewModelScope.launch {
            firestoreRepository.deleteStoryboardScene(currentUserId, docId)
        }
    }

    fun clearStoryboard() {
        stopStoryboardStream()
        val ids = storyboardQueue.value.mapNotNull { it.cloudDocId }
        _sceneRuntime.value = emptyMap()
        viewModelScope.launch {
            firestoreRepository.clearStoryboardScenes(currentUserId, ids)
        }
    }

    fun runStoryboardStream() {
        if (_isStoryStreaming.value || storyboardQueue.value.isEmpty()) {
            emitToast("Queue at least one scene first", isError = true)
            return
        }
        _isStoryStreaming.value = true
        storyJob = viewModelScope.launch {
            try {
                val scenes = storyboardQueue.value
                val state = _rig.value
                val prefs = preferences.value
                for (scene in scenes) {
                    if (!_isStoryStreaming.value) break
                    val docId = scene.cloudDocId ?: continue
                    if (scene.status == StoryboardStatus.COMPLETED && scene.imagePath != null) continue

                    updateSceneRuntime(docId, SceneRuntime(StoryboardStatus.GENERATING))
                    try {
                        val bmp = aiEngine.synthesizeImage(
                            prompt = scene.prompt,
                            modelOption = prefs.modelOption,
                            aspectRatioOption = state.aspectRatio,
                            references = state.references
                        )
                        val saved = repository.saveGeneratedTake(
                            bitmap = bmp,
                            title = "Scene #${scene.orderIndex} • ${scene.wardrobeTitle.take(20)}",
                            outfitTitle = scene.wardrobeTitle,
                            prompt = scene.prompt,
                            aspectRatio = state.aspectRatio.ratioText
                        )
                        _synthesisState.value = SynthesisUiState.Success(bmp, saved.title, saved.id)
                        updateSceneRuntime(
                            docId,
                            SceneRuntime(StoryboardStatus.COMPLETED, imagePath = saved.filePath)
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Storyboard scene ${scene.id} failed", e)
                        updateSceneRuntime(
                            docId,
                            SceneRuntime(
                                StoryboardStatus.ERROR,
                                errorMessage = e.localizedMessage ?: "Generation failed"
                            )
                        )
                    }
                }
                emitToast("Storyboard stream finished")
            } finally {
                _isStoryStreaming.value = false
            }
        }
    }

    fun stopStoryboardStream() {
        _isStoryStreaming.value = false
        storyJob?.cancel()
    }

    private fun updateSceneRuntime(docId: String, runtime: SceneRuntime) {
        _sceneRuntime.update { current -> current + (docId to runtime) }
    }

    private fun emitToast(msg: String, isError: Boolean = false) {
        viewModelScope.launch { _events.emit(StudioEvent.Toast(msg, isError)) }
    }

    companion object {
        fun provideFactory(
            currentUserId: String,
            currentUserDisplayName: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AuraStudioViewModel(
                    repository = AuraRepository(app),
                    aiEngine = AuraAiEngine(app),
                    firestoreRepository = FirestoreRepository(db),
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName
                )
            }
        }
    }
}
