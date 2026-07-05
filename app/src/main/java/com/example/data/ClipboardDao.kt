package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_history ORDER BY isPinned DESC, timestamp DESC")
    fun getAll(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM clipboard_history WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query("UPDATE clipboard_history SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Int, isPinned: Boolean)

    @Query("SELECT EXISTS(SELECT 1 FROM clipboard_history WHERE text = :text LIMIT 1)")
    suspend fun exists(text: String): Boolean
}
