package com.clearchain.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.FCMTokenDao
import com.clearchain.app.data.local.dao.InventoryDao
import com.clearchain.app.data.local.dao.ListingDao
import com.clearchain.app.data.local.dao.NotificationDao
import com.clearchain.app.data.local.dao.PickupRequestDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.FCMTokenEntity
import com.clearchain.app.data.local.entity.InventoryEntity
import com.clearchain.app.data.local.entity.ListingEntity
import com.clearchain.app.data.local.entity.NotificationEntity
import com.clearchain.app.data.local.entity.PickupRequestEntity
import com.clearchain.app.data.local.entity.UserEntity

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
    version = 5,
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
