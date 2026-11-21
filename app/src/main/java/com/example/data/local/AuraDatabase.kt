package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_takes")
data class VaultTakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val outfitTitle: String,
    val prompt: String,
    val aspectRatio: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_costumes")
data class CustomCostumeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val prompt: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface VaultTakeDao {
    @Query("SELECT * FROM vault_takes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultTakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(take: VaultTakeEntity): Long

    @Query("DELETE FROM vault_takes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_takes")
    suspend fun clearAll()
}

@Dao
interface CustomCostumeDao {
    @Query("SELECT * FROM custom_costumes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomCostumeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(costume: CustomCostumeEntity)

    @Query("DELETE FROM custom_costumes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [VaultTakeEntity::class, CustomCostumeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun vaultTakeDao(): VaultTakeDao
    abstract fun customCostumeDao(): CustomCostumeDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura_studio_db"
                ).build().also { INSTANCE = it }
            }
    }
}
