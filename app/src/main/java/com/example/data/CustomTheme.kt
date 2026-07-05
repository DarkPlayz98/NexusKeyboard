package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_themes")
data class CustomTheme(
    @PrimaryKey val name: String,
    val backgroundColorHex: String,
    val keyBackgroundColorHex: String,
    val keyTextColorHex: String,
    val accentColorHex: String = "#0A84FF",
    val headerBackgroundColorHex: String = "#121212",
    val headerIconColorHex: String = "#8E8E93"
)
