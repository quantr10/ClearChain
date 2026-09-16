package com.clearchain.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearchain.app.data.local.dao.*
import com.clearchain.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        AuthTokenEntity::class,
        FCMTokenEntity::class,
        ListingEntity::class,
        PickupRequestEntity::class,
        InventoryEntity::class,
        NotificationEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ClearChainDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun authTokenDao(): AuthTokenDao
    abstract fun fcmTokenDao(): FCMTokenDao
    abstract fun listingDao(): ListingDao
    abstract fun pickupRequestDao(): PickupRequestDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "clearchain_db"
    }
}
