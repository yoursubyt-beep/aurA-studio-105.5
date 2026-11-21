package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.AspectRatioOption
import com.example.data.GeminiImageModelOption
import com.example.data.ReferenceAttachment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

private const val TAG = "AuraAiEngine"
private const val GENERATION_TIMEOUT_MS = 60_000L
private const val MAX_REF_DIMENSION = 1024

class AuraAiEngine(private val context: Context) {

    private fun mapAspectRatio(option: AspectRatioOption): AspectRatio = when (option) {
        AspectRatioOption.PORTRAIT_3_4 -> AspectRatio.PORTRAIT_3x4
        AspectRatioOption.VERTICAL_9_16 -> AspectRatio.PORTRAIT_9x16
        AspectRatioOption.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
        AspectRatioOption.WIDESCREEN_16_9 -> AspectRatio.LANDSCAPE_16x9
    }

    suspend fun synthesizeImage(
        prompt: String,
        modelOption: GeminiImageModelOption,
        aspectRatioOption: AspectRatioOption,
        references: List<ReferenceAttachment>
    ): Bitmap {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(mapAspectRatio(aspectRatioOption))
                .build()
        }

        val model = Firebase.ai.generativeModel(
            modelName = modelOption.modelId,
            generationConfig = config
        )

        val refBitmaps = loadScaledReferenceBitmaps(references)

        val response = withTimeout(GENERATION_TIMEOUT_MS) {
            if (refBitmaps.isEmpty()) {
                model.generateContent(prompt)
            } else {
                model.generateContent(
                    content {
                        refBitmaps.forEach { (roleLabel, bmp) ->
                            text("Attach reference ($roleLabel):")
                            image(bmp)
                        }
                        text(prompt)
                    }
                )
            }
        }

        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()

        return imagePart?.image
            ?: throw IllegalStateException("The model did not return an image. Adjust your prompt or try again.")
    }

    private suspend fun loadScaledReferenceBitmaps(
        references: List<ReferenceAttachment>
    ): List<Pair<String, Bitmap>> = withContext(Dispatchers.IO) {
        references.mapNotNull { ref ->
            runCatching {
                val bmp = decodeScaledBitmap(ref.uriString)
                if (bmp != null) ref.role.label to bmp else null
            }.onFailure { err ->
                Log.w(TAG, "Failed to decode reference image ${ref.displayName}", err)
            }.getOrNull()
        }
    }

    private fun decodeScaledBitmap(uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        val raw = if (uri.scheme == "file" || uri.scheme == null) {
            val filePath = uri.path ?: uriString
            BitmapFactory.decodeFile(filePath)
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return null

        val maxEdge = maxOf(raw.width, raw.height)
        if (maxEdge <= MAX_REF_DIMENSION) return raw

        val scale = MAX_REF_DIMENSION.toFloat() / maxEdge.toFloat()
        val targetW = (raw.width * scale).toInt().coerceAtLeast(1)
        val targetH = (raw.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(raw, targetW, targetH, true)
    }

    fun copyUriToInternalReferenceCache(uri: Uri): Pair<String, String> {
        val refDir = File(context.filesDir, "references").apply { mkdirs() }
        val fileName = "ref_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
        val destFile = File(refDir, fileName)
        val bmp = decodeScaledBitmap(uri.toString())
        if (bmp != null) {
            destFile.outputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return Uri.fromFile(destFile).toString() to fileName
        }
        return uri.toString() to (uri.lastPathSegment ?: fileName)
    }
}
