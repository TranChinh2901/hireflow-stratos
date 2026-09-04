package com.hireflow.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CandidateEntity::class,
        InterviewEntity::class,
        ScorecardEntity::class,
        StageHistoryEntity::class,
        HrTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HireFlowDatabase : RoomDatabase() {
    abstract fun hireFlowDao(): HireFlowDao

    companion object {
        @Volatile private var instance: HireFlowDatabase? = null

        fun getInstance(context: Context): HireFlowDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                HireFlowDatabase::class.java,
                "hireflow.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addSyncColumns(db, "candidates", listOf("organizationId", "remoteCvPath"))
                addSyncColumns(db, "interviews", listOf("remoteCandidateId", "organizationId", "interviewerUserId"))
                addSyncColumns(db, "scorecards", listOf("remoteCandidateId", "organizationId", "evaluatorId"))
                addSyncColumns(db, "stage_history", listOf("organizationId", "actorId"), withUpdatedAt = false)
                addSyncColumns(db, "hr_tasks", listOf("organizationId"))
            }

            private fun addSyncColumns(
                db: SupportSQLiteDatabase,
                table: String,
                nullableTextColumns: List<String>,
                withUpdatedAt: Boolean = true
            ) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
                nullableTextColumns.forEach { db.execSQL("ALTER TABLE `$table` ADD COLUMN `$it` TEXT") }
                if (withUpdatedAt) db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL(
                    "UPDATE `$table` SET `remoteId` = lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-' || " +
                        "lower(hex(randomblob(2))) || '-' || lower(hex(randomblob(2))) || '-' || lower(hex(randomblob(6)))"
                )
                if (withUpdatedAt) db.execSQL("UPDATE `$table` SET `updatedAt` = strftime('%s','now') * 1000")
            }
        }
    }
}
