package com.clearchain.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AuthTokenEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ClearChainDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun authTokenDao(): AuthTokenDao

    companion object {
        const val DATABASE_NAME = "clearchain_db"
    }
}