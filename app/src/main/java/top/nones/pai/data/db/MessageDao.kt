package top.nones.pai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import top.nones.pai.data.model.Message

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Delete
    suspend fun delete(message: Message)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChatId(chatId: Long)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMillis ASC")
    suspend fun getByChatId(chatId: Long): List<Message>
}
