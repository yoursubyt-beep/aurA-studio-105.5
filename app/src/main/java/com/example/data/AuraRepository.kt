package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AuraDatabase
import com.example.data.local.CustomCostumeEntity
import com.example.data.local.VaultTakeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val Context.studioDataStore by preferencesDataStore(name = "aura_studio_prefs")

data class StudioPreferences(
    val modelOption: GeminiImageModelOption = GeminiImageModelOption.NANO_BANANA_2_1,
    val autoSafeRefine: Boolean = true
)

class AuraRepository(private val context: Context) {
    private val db = AuraDatabase.getInstance(context)
    private val vaultDao = db.vaultTakeDao()
    private val costumeDao = db.customCostumeDao()

    private val modelKey = stringPreferencesKey("gemini_image_model")
    private val autoRefineKey = booleanPreferencesKey("auto_safe_refine")

    val vaultTakes: Flow<List<VaultTakeEntity>> = vaultDao.observeAll()

    val customCostumes: Flow<List<CostumePreset>> = costumeDao.observeAll().map { entities ->
        entities.map { entity ->
            CostumePreset(
                id = entity.id,
                category = CostumeCategory.CUSTOM,
                title = entity.title,
                prompt = entity.prompt,
                isCustom = true
            )
        }
    }

    val preferences: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        val modelId = prefs[modelKey] ?: GeminiImageModelOption.NANO_BANANA_2_1.modelId
        val model = GeminiImageModelOption.entries.firstOrNull { it.modelId == modelId }
            ?: GeminiImageModelOption.NANO_BANANA_2_1
        StudioPreferences(
            modelOption = model,
            autoSafeRefine = prefs[autoRefineKey] ?: true
        )
    }

    suspend fun setModelOption(option: GeminiImageModelOption) {
        context.studioDataStore.edit { it[modelKey] = option.modelId }
    }

    suspend fun setAutoSafeRefine(enabled: Boolean) {
        context.studioDataStore.edit { it[autoRefineKey] = enabled }
    }

    suspend fun saveCustomCostume(title: String, prompt: String) {
        val id = "cust_${System.currentTimeMillis()}"
        costumeDao.insert(CustomCostumeEntity(id = id, title = title, prompt = prompt))
    }

    suspend fun deleteCustomCostume(id: String) {
        costumeDao.deleteById(id)
    }

    suspend fun saveGeneratedTake(
        bitmap: Bitmap,
        title: String,
        outfitTitle: String,
        prompt: String,
        aspectRatio: String
    ): VaultTakeEntity = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
        val fileName = "aura_${System.currentTimeMillis()}.jpg"
        val file = File(vaultDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        val entity = VaultTakeEntity(
            title = title,
            outfitTitle = outfitTitle,
            prompt = prompt,
            aspectRatio = aspectRatio,
            filePath = file.absolutePath
        )
        val insertedId = vaultDao.insert(entity)
        entity.copy(id = insertedId)
    }

    suspend fun saveStoryboardFrameFile(bitmap: Bitmap, sceneId: Long): String =
        withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
            val file = File(vaultDir, "story_${sceneId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        }

    suspend fun deleteTake(take: VaultTakeEntity) = withContext(Dispatchers.IO) {
        runCatching { File(take.filePath).delete() }
        vaultDao.deleteById(take.id)
    }

    suspend fun clearVault() = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault")
        vaultDir.listFiles()?.forEach { runCatching { it.delete() } }
        vaultDao.clearAll()
    }

    suspend fun loadBitmapFromPath(path: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    suspend fun createShareUriForFile(filePath: String): Uri? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun createShareUriForBitmap(bitmap: Bitmap, fileNamePrefix: String): Uri =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    suspend fun exportVaultAsZip(takes: List<VaultTakeEntity>): Uri? = withContext(Dispatchers.IO) {
        if (takes.isEmpty()) return@withContext null
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "Aura_Studio_${takes.size}_Takes_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            takes.forEachIndexed { index, take ->
                val imgFile = File(take.filePath)
                if (imgFile.exists()) {
                    val entryName = "frame_${(index + 1).toString().padStart(3, '0')}.jpg"
                    zipOut.putNextEntry(ZipEntry(entryName))
                    imgFile.inputStream().use { input -> input.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
    }
}
