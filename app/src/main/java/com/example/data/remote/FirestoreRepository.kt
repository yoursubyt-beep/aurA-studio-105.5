package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.CostumeCategory
import com.example.data.GeminiImageModelOption
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(userId)
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CloudUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomCostumes(userId: String): Flow<List<CloudCustomCostume>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(userId).collection("costumes")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudCustomCostume::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStoryboardScenes(userId: String): Flow<List<CloudStoryboardScene>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val colRef = db.collection("users").document(userId).collection("storyboard_scenes")
        val path = colRef.path
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudStoryboardScene::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<CloudStoryboardScene> { it.orderIndex }.thenBy { it.createdAt ?: Timestamp(0, 0) })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        modelId: String,
        autoSafeRefine: Boolean
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        val path = docRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "Aura Creator" }.take(80)
                val safeModelId = validateModelId(modelId)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "modelId" to safeModelId,
                    "autoSafeRefine" to autoSafeRefine,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        displayName: String,
        modelId: String,
        autoSafeRefine: Boolean
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        val path = docRef.path
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeName = displayName.trim().ifBlank { "Aura Creator" }.take(80)
            val safeModelId = validateModelId(modelId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "modelId" to safeModelId,
                        "autoSafeRefine" to autoSafeRefine,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "modelId" to safeModelId,
                        "autoSafeRefine" to autoSafeRefine,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCustomCostume(
        userId: String,
        title: String,
        prompt: String,
        category: String = CostumeCategory.CUSTOM.name
    ): Result<String> {
        var docPath = "users/$userId/costumes"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeTitle = title.trim().ifBlank { "Custom Look" }.take(120)
            val safePrompt = prompt.trim().take(1200)
            require(safePrompt.length >= 3) { "Costume prompt must be at least 3 characters." }
            val safeCategory = if (category in ALLOWED_COSTUME_CATEGORIES) category else CostumeCategory.CUSTOM.name

            val docRef = db.collection("users").document(uid).collection("costumes").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "prompt" to safePrompt,
                "category" to safeCategory,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomCostume(userId: String, costumeId: String): Result<Unit> {
        val docPath = "users/$userId/costumes/$costumeId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(VALID_ID_REGEX.matches(costumeId)) { "Invalid costume ID format." }
            db.collection("users").document(uid).collection("costumes").document(costumeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addStoryboardScene(
        userId: String,
        orderIndex: Int,
        wardrobeTitle: String,
        wardrobePrompt: String,
        prompt: String
    ): Result<String> {
        var docPath = "users/$userId/storyboard_scenes"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeOrder = orderIndex.coerceIn(1, 500)
            val safeWardrobeTitle = wardrobeTitle.trim().ifBlank { "Studio Scene" }.take(140)
            val safeWardrobePrompt = wardrobePrompt.trim().ifBlank { safeWardrobeTitle }.take(1200)
            val safePrompt = prompt.trim().take(3000)
            require(safePrompt.length >= 3) { "Scene prompt must be at least 3 characters." }

            val docRef = db.collection("users").document(uid).collection("storyboard_scenes").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "orderIndex" to safeOrder,
                "wardrobeTitle" to safeWardrobeTitle,
                "wardrobePrompt" to safeWardrobePrompt,
                "prompt" to safePrompt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteStoryboardScene(userId: String, sceneId: String): Result<Unit> {
        val docPath = "users/$userId/storyboard_scenes/$sceneId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(VALID_ID_REGEX.matches(sceneId)) { "Invalid scene ID format." }
            db.collection("users").document(uid).collection("storyboard_scenes").document(sceneId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearStoryboardScenes(userId: String, sceneIds: List<String>): Result<Unit> {
        val colPath = "users/$userId/storyboard_scenes"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user clear." }
            val validIds = sceneIds.filter { VALID_ID_REGEX.matches(it) }
            if (validIds.isEmpty()) return Result.success(Unit)
            val colRef = db.collection("users").document(uid).collection("storyboard_scenes")
            validIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id -> batch.delete(colRef.document(id)) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath)
            Result.failure(e)
        }
    }

    private fun validateModelId(modelId: String): String =
        if (GeminiImageModelOption.entries.any { it.modelId == modelId }) modelId
        else GeminiImageModelOption.NANO_BANANA_2_1.modelId

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")
        val ALLOWED_COSTUME_CATEGORIES = setOf("CUSTOM", "SILHOUETTE", "LAYERED", "MIDRIFF", "LIMBS", "HERITAGE")
    }
}
