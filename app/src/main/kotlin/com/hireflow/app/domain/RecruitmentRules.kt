package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity

object RecruitmentRules {
    fun shouldShowInterview(candidate: CandidateEntity, interview: InterviewEntity): Boolean =
        interview.completed || candidate.recruitmentStage !in setOf(RecruitmentStage.HIRED, RecruitmentStage.REJECTED)

    fun canAdvance(
        candidate: CandidateEntity,
        interviews: List<InterviewEntity>,
        scorecards: List<ScorecardEntity>
    ): Boolean = advanceBlockReason(candidate, interviews, scorecards) == null

    fun advanceBlockReason(
        candidate: CandidateEntity,
        interviews: List<InterviewEntity>,
        scorecards: List<ScorecardEntity>
    ): String? = when (candidate.recruitmentStage) {
        RecruitmentStage.APPLIED -> null
        RecruitmentStage.SCREENING -> if (candidate.cvUri == null && candidate.remoteCvPath == null) {
            "Hãy đính kèm CV trước khi chuyển ứng viên sang phỏng vấn."
        } else null
        RecruitmentStage.INTERVIEW -> when {
            interviews.none { it.candidateId == candidate.id && it.completed } ->
                "Hãy hoàn thành ít nhất một lịch phỏng vấn trước khi ra quyết định."
            scorecards.none { it.candidateId == candidate.id } ->
                "Hãy lưu ít nhất một phiếu đánh giá trước khi ra quyết định."
            else -> null
        }
        RecruitmentStage.WAITING_DECISION -> if (
            scorecards.none {
                it.candidateId == candidate.id && it.conclusion in setOf("Hire", "Strong Hire")
            }
        ) {
            "Chỉ ứng viên có kết luận Hire hoặc Strong Hire mới được chuyển sang Offer."
        } else null
        RecruitmentStage.OFFER -> null
        RecruitmentStage.HIRED, RecruitmentStage.REJECTED -> "Quy trình của ứng viên đã kết thúc."
    }

    fun canReview(candidate: CandidateEntity, interviews: List<InterviewEntity>): Boolean =
        reviewBlockReason(candidate, interviews) == null

    fun reviewBlockReason(candidate: CandidateEntity, interviews: List<InterviewEntity>): String? = when {
        candidate.recruitmentStage !in setOf(RecruitmentStage.INTERVIEW, RecruitmentStage.WAITING_DECISION) ->
            "Chỉ có thể đánh giá ứng viên đang ở vòng phỏng vấn hoặc chờ quyết định."
        interviews.none { it.candidateId == candidate.id && it.completed } ->
            "Hãy đánh dấu lịch phỏng vấn đã hoàn thành trước khi chấm điểm."
        else -> null
    }

    fun canSchedule(
        candidate: CandidateEntity,
        scheduledAt: Long,
        interviewer: String,
        interviews: List<InterviewEntity>,
        now: Long = System.currentTimeMillis(),
        durationMinutes: Int = 60
    ): Boolean = scheduleBlockReason(candidate, scheduledAt, interviewer, interviews, now, durationMinutes) == null

    fun scheduleBlockReason(
        candidate: CandidateEntity,
        scheduledAt: Long,
        interviewer: String,
        interviews: List<InterviewEntity>,
        now: Long = System.currentTimeMillis(),
        durationMinutes: Int = 60
    ): String? = when {
        candidate.recruitmentStage != RecruitmentStage.INTERVIEW ->
            "Chỉ có thể tạo lịch cho ứng viên đang ở vòng phỏng vấn."
        scheduledAt <= now -> "Thời gian phỏng vấn phải ở tương lai."
        interviewer.isBlank() -> "Hãy nhập người phỏng vấn."
        interviews.any { existing ->
            !existing.completed && existing.interviewer.equals(interviewer.trim(), ignoreCase = true) &&
                overlaps(scheduledAt, durationMinutes, existing.scheduledAt, existing.durationMinutes)
        } -> "Người phỏng vấn đã có lịch trùng thời gian này."
        else -> null
    }

    private fun overlaps(startA: Long, durationA: Int, startB: Long, durationB: Int): Boolean {
        val endA = startA + durationA * 60_000L
        val endB = startB + durationB * 60_000L
        return startA < endB && startB < endA
    }
}
