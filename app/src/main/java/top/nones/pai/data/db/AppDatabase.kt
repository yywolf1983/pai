package top.nones.pai.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import top.nones.pai.data.model.ChatSession
import top.nones.pai.data.model.Message
import top.nones.pai.data.model.ModelConfig

@Database(
    entities = [
        ChatSession::class,
        Message::class,
        ModelConfig::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
    abstract fun modelConfigDao(): ModelConfigDao
}
