package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.CostumeCategory
import com.example.data.CostumePreset
import com.example.data.GeminiImageModelOption
import com.example.data.StoryboardScene
import com.example.data.StoryboardStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val modelId: String = GeminiImageModelOption.NANO_BANANA_2_1.modelId,
    val autoSafeRefine: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolveModelOption(): GeminiImageModelOption =
        GeminiImageModelOption.entries.firstOrNull { it.modelId == modelId }
            ?: GeminiImageModelOption.NANO_BANANA_2_1
}

@Keep
@IgnoreExtraProperties
data class CloudCustomCostume(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val prompt: String = "",
    val category: String = CostumeCategory.CUSTOM.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): CostumePreset {
        val resolvedCategory = enumValues<CostumeCategory>()
            .firstOrNull { it.name == category } ?: CostumeCategory.CUSTOM
        return CostumePreset(
            id = id,
            category = resolvedCategory,
            title = title,
            prompt = prompt,
            isCustom = true
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudStoryboardScene(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderIndex: Int = 1,
    val wardrobeTitle: String = "",
    val wardrobePrompt: String = "",
    val prompt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(
        status: StoryboardStatus = StoryboardStatus.QUEUED,
        imagePath: String? = null,
        errorMessage: String? = null
    ): StoryboardScene = StoryboardScene(
        id = id.hashCode().toLong(),
        orderIndex = orderIndex,
        prompt = prompt,
        wardrobeTitle = wardrobeTitle,
        wardrobePrompt = wardrobePrompt,
        status = status,
        imagePath = imagePath,
        errorMessage = errorMessage,
        cloudDocId = id
    )
}
