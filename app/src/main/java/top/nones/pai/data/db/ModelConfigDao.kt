package top.nones.pai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import top.nones.pai.data.model.ModelConfig

@Dao
interface ModelConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modelConfig: ModelConfig): Long

    @Update
    suspend fun update(modelConfig: ModelConfig)

    @Delete
    suspend fun delete(modelConfig: ModelConfig)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM model_configs ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<ModelConfig>
    
    @Query("SELECT * FROM model_configs ORDER BY isDefault DESC, name ASC")
    suspend fun getAllSortedByDefault(): List<ModelConfig>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getById(id: Long): ModelConfig?

    @Query("SELECT * FROM model_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ModelConfig?

    @Query("UPDATE model_configs SET isDefault = 0")
    suspend fun clearDefault()
}
