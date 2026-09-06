package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.InterviewStatus
import com.hireflow.app.data.OfferResponse
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity

object RecruitmentRules {
    /** Buổi còn hiệu lực: chưa hủy. Vắng mặt là trạng thái kết thúc đã xử lý. */
    fun effectiveInterviews(candidate: CandidateEntity, interviews: List<InterviewEntity>): List<InterviewEntity> =
        interviews.filter { it.candidateId == candidate.id && it.interviewStatus != InterviewStatus.CANCELLED }

    fun completedInterviews(candidate: CandidateEntity, interviews: List<InterviewEntity>): List<InterviewEntity> =
        effectiveInterviews(candidate, interviews).filter { it.interviewStatus == InterviewStatus.COMPLETED }

    /** Phiếu gắn buổi (bỏ phiếu tổng hợp cũ) của một buổi cụ thể. */
    fun scorecardsForInterview(interviewId: Long, scorecards: List<ScorecardEntity>): List<ScorecardEntity> =
        scorecards.filter { it.interviewId == interviewId }

    fun shouldShowInterview(candidate: CandidateEntity, interview: InterviewEntity): Boolean = true

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
        RecruitmentStage.INTERVIEW -> {
            val effective = effectiveInterviews(candidate, interviews)
            val mine = scorecards.filter { it.candidateId == candidate.id }
            val linked = mine.filter { it.interviewId != null }
            when {
                effective.isEmpty() ->
                    "Chưa có buổi phỏng vấn còn hiệu lực. Hãy đặt lịch mới."
                effective.none { it.interviewStatus == InterviewStatus.COMPLETED } ->
                    "Hãy hoàn thành ít nhất một lịch phỏng vấn trước khi ra quyết định."
                effective.any { it.interviewStatus == InterviewStatus.SCHEDULED } ->
                    "Còn buổi chưa hoàn thành. Hãy hoàn thành, hủy hoặc ghi vắng mặt trước khi chốt."
                effective.filter { it.interviewStatus == InterviewStatus.COMPLETED }.any { completed ->
                    linked.none { it.interviewId == completed.id }
                } -> if (mine.any { it.isLegacy }) {
                    "Mỗi buổi đã hoàn thành cần một phiếu riêng. Phiếu cũ chưa gắn buổi (cần đối soát) — hãy chấm lại theo từng buổi."
                } else {
                    "Mỗi buổi đã hoàn thành cần một phiếu đánh giá riêng."
                }
                else -> null
            }
        }
        RecruitmentStage.WAITING_DECISION -> if (
            scorecards.none {
                it.candidateId == candidate.id && it.conclusion in setOf("Hire", "Strong Hire")
            }
        ) {
            "Chỉ ứng viên có kết luận Hire hoặc Strong Hire mới được chuyển sang Offer."
        } else null
        RecruitmentStage.OFFER -> if (candidate.offerResponse != OfferResponse.ACCEPTED.name) {
            "Hãy ghi nhận phản hồi offer. Chỉ ứng viên đồng ý offer mới được chuyển sang Đã tuyển."
        } else null
        RecruitmentStage.HIRED, RecruitmentStage.REJECTED -> "Quy trình của ứng viên đã kết thúc."
    }

    fun canReview(
        candidate: CandidateEntity,
        interviews: List<InterviewEntity>,
        interviewId: Long? = null
    ): Boolean = reviewBlockReason(candidate, interviews, interviewId) == null

    fun reviewBlockReason(
        candidate: CandidateEntity,
        interviews: List<InterviewEntity>,
        interviewId: Long? = null
    ): String? {
        if (candidate.recruitmentStage !in setOf(RecruitmentStage.INTERVIEW, RecruitmentStage.WAITING_DECISION)) {
            return "Chỉ có thể đánh giá ứng viên đang ở vòng phỏng vấn hoặc chờ quyết định."
        }
        if (interviews.none { it.candidateId == candidate.id && it.interviewStatus == InterviewStatus.COMPLETED }) {
            return "Hãy đánh dấu lịch phỏng vấn đã hoàn thành trước khi chấm điểm."
        }
        if (interviewId != null) {
            val target = interviews.firstOrNull { it.id == interviewId }
                ?: return "Không tìm thấy buổi phỏng vấn."
            if (target.candidateId != candidate.id) return "Buổi phỏng vấn không thuộc ứng viên này."
            if (target.interviewStatus != InterviewStatus.COMPLETED) return "Chỉ đánh giá buổi đã hoàn thành."
        }
        return null
    }

    fun canSchedule(
        candidate: CandidateEntity,
        scheduledAt: Long,
        interviewer: String,
        interviews: List<InterviewEntity>,
        now: Long = System.currentTimeMillis(),
        durationMinutes: Int = 60,
        excludeInterviewId: Long? = null
    ): Boolean = scheduleBlockReason(candidate, scheduledAt, interviewer, interviews, now, durationMinutes, excludeInterviewId) == null

    fun scheduleBlockReason(
        candidate: CandidateEntity,
        scheduledAt: Long,
        interviewer: String,
        interviews: List<InterviewEntity>,
        now: Long = System.currentTimeMillis(),
        durationMinutes: Int = 60,
        excludeInterviewId: Long? = null
    ): String? {
        // Chỉ lịch đang lên (chưa hoàn thành/hủy/vắng) mới giữ chỗ.
        val blocking = interviews.filter {
            it.id != excludeInterviewId && it.interviewStatus == InterviewStatus.SCHEDULED
        }
        return when {
            candidate.recruitmentStage != RecruitmentStage.INTERVIEW ->
                "Chỉ có thể tạo lịch cho ứng viên đang ở vòng phỏng vấn."
            scheduledAt <= now -> "Thời gian phỏng vấn phải ở tương lai."
            interviewer.isBlank() -> "Hãy nhập người phỏng vấn."
            blocking.any { existing ->
                existing.interviewer.equals(interviewer.trim(), ignoreCase = true) &&
                    overlaps(scheduledAt, durationMinutes, existing.scheduledAt, existing.durationMinutes)
            } -> "Người phỏng vấn đã có lịch trùng thời gian này."
            blocking.any { existing ->
                existing.candidateId == candidate.id &&
                    overlaps(scheduledAt, durationMinutes, existing.scheduledAt, existing.durationMinutes)
            } -> "Ứng viên đã có lịch trùng thời gian này."
            else -> null
        }
    }

    private fun overlaps(startA: Long, durationA: Int, startB: Long, durationB: Int): Boolean {
        val endA = startA + durationA * 60_000L
        val endB = startB + durationB * 60_000L
        return startA < endB && startB < endA
    }
}
