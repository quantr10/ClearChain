package com.clearchain.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fcm_tokens")
data class FCMTokenEntity(
    @PrimaryKey val id: Int = 1,
    val token: String,
    val createdAt: Long = System.currentTimeMillis()
)