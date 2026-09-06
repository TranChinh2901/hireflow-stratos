package com.hireflow.app.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncState { PENDING, SYNCED, FAILED }

enum class InterviewStatus(val label: String) {
    SCHEDULED("Đã lên lịch"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy"),
    NO_SHOW("Vắng mặt")
}

enum class OfferResponse(val label: String) {
    ACCEPTED("Đồng ý offer"),
    DECLINED("Từ chối offer")
}

enum class RecruitmentStage(val label: String) {
    APPLIED("Ứng tuyển"),
    SCREENING("Sàng lọc"),
    INTERVIEW("Phỏng vấn"),
    WAITING_DECISION("Chờ quyết định"),
    OFFER("Đề nghị"),
    HIRED("Đã tuyển"),
    REJECTED("Từ chối");

    fun next(): RecruitmentStage = when (this) {
        APPLIED -> SCREENING
        SCREENING -> INTERVIEW
        INTERVIEW -> WAITING_DECISION
        WAITING_DECISION -> OFFER
        OFFER -> HIRED
        HIRED, REJECTED -> this
    }
}

@Entity(tableName = "candidates")
data class CandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: String,
    val email: String,
    val phone: String,
    val experienceYears: Int,
    val skills: String,
    val stage: String = RecruitmentStage.APPLIED.name,
    val note: String = "",
    val cvUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    val organizationId: String? = null,
    val remoteCvPath: String? = null,
    val cvFileName: String? = null,
    val closeReason: String? = null,
    val offerSentAt: Long? = null,
    val offerResponse: String? = null,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PENDING'") val syncState: String = SyncState.PENDING.name
) {
    val recruitmentStage: RecruitmentStage
        get() = runCatching { RecruitmentStage.valueOf(stage) }.getOrDefault(RecruitmentStage.APPLIED)
    val skillList: List<String>
        get() = skills.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

@Entity(tableName = "interviews")
data class InterviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidateId: Long,
    val candidateName: String,
    val position: String,
    val scheduledAt: Long,
    val durationMinutes: Int = 60,
    val format: String,
    val interviewer: String,
    val round: String = "Vòng 1: HR",
    val checklist: String = "Giới thiệu bản thân, Kinh nghiệm liên quan, Tình huống thực tế",
    val completed: Boolean = false,
    @ColumnInfo(defaultValue = "'SCHEDULED'") val status: String = InterviewStatus.SCHEDULED.name,
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    val remoteCandidateId: String? = null,
    val organizationId: String? = null,
    val interviewerUserId: String? = null,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PENDING'") val syncState: String = SyncState.PENDING.name
) {
    val interviewStatus: InterviewStatus
        get() = runCatching { InterviewStatus.valueOf(status) }.getOrDefault(InterviewStatus.SCHEDULED)
}

@Entity(tableName = "scorecards")
data class ScorecardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidateId: Long,
    // Buổi phỏng vấn được đánh giá. Null = phiếu tổng hợp cũ (trước đợt 3),
    // vẫn đọc được nhưng không thỏa điều kiện đánh giá buổi mới.
    val interviewId: Long? = null,
    val technical: Int,
    val communication: Int,
    val problemSolving: Int,
    val cultureFit: Int,
    val strengths: String,
    val improvements: String,
    val notes: String,
    val conclusion: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    val remoteCandidateId: String? = null,
    val remoteInterviewId: String? = null,
    val organizationId: String? = null,
    val evaluatorId: String? = null,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PENDING'") val syncState: String = SyncState.PENDING.name
) {
    val average: Double
        get() = (technical + communication + problemSolving + cultureFit) / 4.0
    val isLegacy: Boolean
        get() = interviewId == null
}

@Entity(tableName = "stage_history")
data class StageHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val candidateId: Long,
    val fromStage: String,
    val toStage: String,
    val changedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    val organizationId: String? = null,
    val actorId: String? = null,
    @ColumnInfo(defaultValue = "'PENDING'") val syncState: String = SyncState.PENDING.name
)

@Entity(tableName = "hr_tasks")
data class HrTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val type: String,
    val completed: Boolean = false,
    val dueAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val remoteId: String = UUID.randomUUID().toString(),
    val organizationId: String? = null,
    val assigneeId: String? = null,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PENDING'") val syncState: String = SyncState.PENDING.name
)
