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
    version = 6,
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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `hr_tasks` ADD COLUMN `assigneeId` TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Repair the original offline fixture, which attached two interviews to
                // candidates who were not yet in the interview stage.
                db.execSQL(
                    """
                    UPDATE interviews
                    SET candidateId = (SELECT id FROM candidates WHERE organizationId IS NULL AND name = 'Phạm Quang Huy' LIMIT 1),
                        candidateName = 'Phạm Quang Huy',
                        position = 'DevOps Engineer'
                    WHERE organizationId IS NULL
                      AND candidateName = 'Trần Minh Khôi'
                      AND position = 'Backend Developer'
                      AND EXISTS (SELECT 1 FROM candidates WHERE organizationId IS NULL AND name = 'Phạm Quang Huy')
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE interviews
                    SET candidateId = (SELECT id FROM candidates WHERE organizationId IS NULL AND name = 'Hoàng Gia Bảo' LIMIT 1),
                        candidateName = 'Hoàng Gia Bảo',
                        position = 'Frontend Developer'
                    WHERE organizationId IS NULL
                      AND candidateName = 'Lê Thu Hà'
                      AND position = 'UI/UX Designer'
                      AND EXISTS (SELECT 1 FROM candidates WHERE organizationId IS NULL AND name = 'Hoàng Gia Bảo')
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE hr_tasks
                    SET title = 'Chuẩn bị phỏng vấn: Phạm Quang Huy',
                        subtitle = 'DevOps Engineer',
                        type = 'interview'
                    WHERE organizationId IS NULL
                      AND title = 'Đánh giá: Trần Minh Khôi'
                      AND subtitle = 'Backend Developer'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `candidates` ADD COLUMN `cvFileName` TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Trạng thái buổi phỏng vấn, suy từ completed để giữ dữ liệu cũ.
                db.execSQL("ALTER TABLE `interviews` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'SCHEDULED'")
                db.execSQL("UPDATE `interviews` SET `status` = CASE WHEN `completed` THEN 'COMPLETED' ELSE 'SCHEDULED' END")
                // Phiếu gắn từng buổi; phiếu cũ giữ interviewId NULL (tổng hợp cũ).
                db.execSQL("ALTER TABLE `scorecards` ADD COLUMN `interviewId` INTEGER")
                db.execSQL("ALTER TABLE `scorecards` ADD COLUMN `remoteInterviewId` TEXT")
                // Lý do đóng hồ sơ và phản hồi offer.
                db.execSQL("ALTER TABLE `candidates` ADD COLUMN `closeReason` TEXT")
                db.execSQL("ALTER TABLE `candidates` ADD COLUMN `offerSentAt` INTEGER")
                db.execSQL("ALTER TABLE `candidates` ADD COLUMN `offerResponse` TEXT")
            }
        }
    }
}
