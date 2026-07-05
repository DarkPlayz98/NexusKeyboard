package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomThemeDao {
    @Query("SELECT * FROM custom_themes")
    fun getAll(): Flow<List<CustomTheme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(theme: CustomTheme)

    @Query("DELETE FROM custom_themes WHERE name = :name")
    suspend fun delete(name: String)
}
