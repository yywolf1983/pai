package top.nones.pai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_configs")
data class ModelConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val endpoint: String,
    val apiKey: String,
    val modelName: String,
    val systemPrompt: String = "",
    val isLocal: Boolean = false,
    val isDefault: Boolean = false
)
