package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_tokens")
data class AuthTokenEntity(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val tokenType: String,
    val savedAt: Long = System.currentTimeMillis()
)