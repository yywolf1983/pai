package top.nones.pai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Message(
    val id: Long = 0,
    val chatId: Long,
    val content: String,
    val role: String,
    val attachments: List<Attachment> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)

data class Attachment(
    val name: String,
    val path: String,
    val type: String
)
