package com.clearchain.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 -> v8: adds the verification-review fields to the cached user row
 * (verificationNotes, documentUrl, documentUrl2). Column names match the
 * UserEntity property names exactly — Room has no @ColumnInfo overrides on them.
 * (documentUrl2 is now deprecated — kept as a dormant column so this migration
 * still matches the schema; see UserEntity.)
 *
 * Without this, the version bump would fall back to fallbackToDestructiveMigration()
 * and wipe the local DB (including saved auth tokens), forcing every signed-in user
 * to log back in after updating the app.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN verificationNotes TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN documentUrl TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN documentUrl2 TEXT")
    }
}

/**
 * v8 -> v9: caches the grocery's avatar on each listing row, so a listing read
 * back offline shows the store's photo instead of falling back to its initial.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE listings ADD COLUMN groceryProfilePictureUrl TEXT")
    }
}
