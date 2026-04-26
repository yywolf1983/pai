package top.nones.pai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import top.nones.pai.data.model.ChatSession

@Dao
interface ChatSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chatSession: ChatSession): Long

    @Update
    suspend fun update(chatSession: ChatSession)

    @Delete
    suspend fun delete(chatSession: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMillis DESC")
    suspend fun getAll(): List<ChatSession>
    
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMillis DESC")
    suspend fun getAllSortedByUpdatedAt(): List<ChatSession>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChatSession?
}
