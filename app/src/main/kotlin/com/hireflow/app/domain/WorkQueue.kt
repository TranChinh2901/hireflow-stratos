package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.InterviewStatus
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Việc cần làm suy ra từ dữ liệu tuyển dụng, dùng chung cho Dashboard và hồ sơ. */
enum class PrimaryAction {
    ATTACH_CV,
    ADVANCE,
    SCHEDULE,
    VIEW_INTERVIEW,
    COMPLETE_AND_REVIEW,
    REVIEW,
    DECIDE,
    RECORD_RESPONSE,
    NONE
}

data class NextAction(
    val headline: String,
    val detail: String?,
    val action: PrimaryAction,
    val interviewId: Long? = null
)

data class WorkItem(
    val key: String,
    val kind: WorkKind,
    val title: String,
    val subtitle: String,
    val candidateId: Long?,
    val interviewId: Long?
)

enum class WorkKind {
    OVERDUE,
    TODAY,
    REVIEW,
    DECISION,
    OFFER,
    SCREENING,
    UNSCHEDULED
}

object WorkQueue {
    fun interviewsOf(candidateId: Long, interviews: List<InterviewEntity>): List<InterviewEntity> =
        interviews.filter { it.candidateId == candidateId }.sortedBy { it.scheduledAt }

    fun missingReviews(
        candidates: List<CandidateEntity>,
        interviews: List<InterviewEntity>,
        scorecards: List<ScorecardEntity>
    ): List<CandidateEntity> = candidates.filter { candidate ->
        candidate.recruitmentStage == RecruitmentStage.INTERVIEW &&
            interviews.any { it.candidateId == candidate.id && it.interviewStatus == InterviewStatus.COMPLETED } &&
            interviews.filter { it.candidateId == candidate.id && it.interviewStatus == InterviewStatus.COMPLETED }.any { completed ->
                scorecards.none { it.candidateId == candidate.id && it.interviewId == completed.id }
            }
    }

    fun screeningQueue(candidates: List<CandidateEntity>): List<CandidateEntity> =
        candidates.filter { it.recruitmentStage == RecruitmentStage.SCREENING }

    fun decisionQueue(candidates: List<CandidateEntity>): List<CandidateEntity> =
        candidates.filter { it.recruitmentStage == RecruitmentStage.WAITING_DECISION }

    fun offerQueue(candidates: List<CandidateEntity>): List<CandidateEntity> =
        candidates.filter { it.recruitmentStage == RecruitmentStage.OFFER }

    fun unscheduled(
        candidates: List<CandidateEntity>,
        interviews: List<InterviewEntity>
    ): List<CandidateEntity> = candidates.filter { candidate ->
        candidate.recruitmentStage == RecruitmentStage.INTERVIEW &&
            interviews.none { it.candidateId == candidate.id }
    }

    fun todayInterviews(interviews: List<InterviewEntity>, now: Long = System.currentTimeMillis()): List<InterviewEntity> =
        interviews.filter { isSameDay(it.scheduledAt, now) }.sortedBy { it.scheduledAt }

    /** Việc của hôm nay: lịch đang lên diễn ra hôm nay + lịch quá giờ chưa xử lý. */
    fun todayWorkItems(
        interviews: List<InterviewEntity>,
        now: Long = System.currentTimeMillis(),
        limitPerGroup: Int = 5
    ): List<WorkItem> {
        val items = mutableListOf<WorkItem>()
        overdueInterviews(interviews, now).take(limitPerGroup).forEach { interview ->
            items += WorkItem(
                key = "overdue-${interview.id}",
                kind = WorkKind.OVERDUE,
                title = "Hoàn thành & đánh giá: ${interview.candidateName}",
                subtitle = "${interview.round} · quá giờ chưa xử lý",
                candidateId = interview.candidateId,
                interviewId = interview.id
            )
        }
        interviews.filter {
            it.interviewStatus == InterviewStatus.SCHEDULED && isSameDay(it.scheduledAt, now)
        }.sortedBy { it.scheduledAt }.take(limitPerGroup).forEach { interview ->
            items += WorkItem(
                key = "today-${interview.id}",
                kind = WorkKind.TODAY,
                title = "Phỏng vấn hôm nay: ${interview.candidateName}",
                subtitle = "${formatTime(interview.scheduledAt)} · ${interview.round}",
                candidateId = interview.candidateId,
                interviewId = interview.id
            )
        }
        return items
    }

    fun overdueInterviews(interviews: List<InterviewEntity>, now: Long = System.currentTimeMillis()): List<InterviewEntity> =
        interviews.filter { it.interviewStatus == InterviewStatus.SCHEDULED && it.scheduledAt <= now }.sortedBy { it.scheduledAt }

    fun latestPastDue(candidateId: Long, interviews: List<InterviewEntity>, now: Long): InterviewEntity? =
        interviewsOf(candidateId, interviews).filter { it.interviewStatus == InterviewStatus.SCHEDULED && it.scheduledAt <= now }.maxByOrNull { it.scheduledAt }

    fun nextUpcoming(candidateId: Long, interviews: List<InterviewEntity>, now: Long): InterviewEntity? =
        interviewsOf(candidateId, interviews).filter { it.interviewStatus == InterviewStatus.SCHEDULED && it.scheduledAt > now }.minByOrNull { it.scheduledAt }

    fun latestCompleted(candidateId: Long, interviews: List<InterviewEntity>): InterviewEntity? =
        interviewsOf(candidateId, interviews).filter { it.interviewStatus == InterviewStatus.COMPLETED }.maxByOrNull { it.scheduledAt }

    fun nextActionFor(
        candidate: CandidateEntity,
        interviews: List<InterviewEntity>,
        scorecards: List<ScorecardEntity>,
        now: Long = System.currentTimeMillis()
    ): NextAction {
        if (candidate.recruitmentStage == RecruitmentStage.HIRED || candidate.recruitmentStage == RecruitmentStage.REJECTED) {
            return NextAction("Hồ sơ đã kết thúc", "Xem lại CV, lịch và đánh giá ở chế độ đọc.", PrimaryAction.NONE)
        }
        val hasCv = candidate.cvUri != null || candidate.remoteCvPath != null
        return when (candidate.recruitmentStage) {
            RecruitmentStage.APPLIED -> if (!hasCv) {
                NextAction("Đang ở Tiếp nhận", "Còn thiếu CV. Bổ sung để bắt đầu sàng lọc.", PrimaryAction.ATTACH_CV)
            } else {
                NextAction("Sẵn sàng sàng lọc", "Đã có CV. Bắt đầu sàng lọc ứng viên này.", PrimaryAction.ADVANCE)
            }
            RecruitmentStage.SCREENING -> if (!hasCv) {
                NextAction("Đang sàng lọc", "Còn thiếu CV, chưa thể đạt sàng lọc.", PrimaryAction.ATTACH_CV)
            } else {
                NextAction("Đang sàng lọc", "Đạt sàng lọc để chuyển sang Phỏng vấn, sau đó đặt lịch ngay trên hồ sơ.", PrimaryAction.ADVANCE)
            }
            RecruitmentStage.INTERVIEW -> {
                val mine = interviewsOf(candidate.id, interviews)
                val linkedCount = scorecards.count { it.candidateId == candidate.id && it.interviewId != null }
                val legacyCount = scorecards.count { it.candidateId == candidate.id && it.isLegacy }
                if (mine.isEmpty()) {
                    NextAction("Chưa đặt lịch", "Đã đạt sàng lọc nhưng chưa có buổi phỏng vấn nào.", PrimaryAction.SCHEDULE)
                } else {
                    val pastDue = latestPastDue(candidate.id, interviews, now)
                    if (pastDue != null) {
                        NextAction(
                            "Buổi đã diễn ra, chưa hoàn thành",
                            "Hoàn thành buổi ${pastDue.round} rồi đánh giá ngay, không cần chọn lại.",
                            PrimaryAction.COMPLETE_AND_REVIEW,
                            pastDue.id
                        )
                    } else if (mine.none { it.interviewStatus == InterviewStatus.COMPLETED }) {
                        val upcoming = nextUpcoming(candidate.id, interviews, now) ?: mine.minByOrNull { it.scheduledAt }!!
                        NextAction("Đã có lịch phỏng vấn", "Buổi ${upcoming.round} lúc ${formatTime(upcoming.scheduledAt)}.", PrimaryAction.VIEW_INTERVIEW, upcoming.id)
                    } else if (mine.filter { it.interviewStatus == InterviewStatus.COMPLETED }.any { completed ->
                            scorecards.none { it.candidateId == candidate.id && it.interviewId == completed.id }
                        }
                    ) {
                        NextAction(
                            "Đã phỏng vấn, thiếu đánh giá",
                            if (legacyCount > 0 && linkedCount == 0) "Có $legacyCount phiếu cũ chưa gắn buổi (cần đối soát) — hãy chấm lại theo từng buổi."
                            else "Viết đánh giá cho từng buổi đã hoàn thành.",
                            PrimaryAction.REVIEW
                        )
                    } else {
                        NextAction("Đủ điều kiện chốt phỏng vấn", "Các buổi còn hiệu lực đã hoàn thành và có phiếu riêng.", PrimaryAction.ADVANCE)
                    }
                }
            }
            RecruitmentStage.WAITING_DECISION -> {
                val mine = scorecards.filter { it.candidateId == candidate.id }
                val hires = mine.count { it.conclusion in setOf("Hire", "Strong Hire") }
                if (hires > 0) {
                    NextAction("Đủ điều kiện đề xuất offer", "$hires phiếu Hire/Strong Hire trên ${mine.size} phiếu.", PrimaryAction.ADVANCE)
                } else {
                    val summary = if (mine.isEmpty()) "chưa có phiếu nào" else mine.groupingBy { it.conclusion }.eachCount()
                        .entries.joinToString { "${it.key} x${it.value}" }
                    NextAction("Cần ra quyết định", "Tổng hợp ${mine.size} phiếu: $summary.", PrimaryAction.DECIDE)
                }
            }
            RecruitmentStage.OFFER -> NextAction(
                "Chờ phản hồi offer",
                "Đồng ý → Đã tuyển (MVP: đồng ý offer). Từ chối → đóng hồ sơ kèm lý do.",
                PrimaryAction.RECORD_RESPONSE
            )
            else -> NextAction("Hồ sơ đã kết thúc", null, PrimaryAction.NONE)
        }
    }

    fun workItems(
        candidates: List<CandidateEntity>,
        interviews: List<InterviewEntity>,
        scorecards: List<ScorecardEntity>,
        now: Long = System.currentTimeMillis(),
        limitPerGroup: Int = 5
    ): List<WorkItem> {
        val items = mutableListOf<WorkItem>()
        overdueInterviews(interviews, now).take(limitPerGroup).forEach { interview ->
            items += WorkItem(
                key = "overdue-${interview.id}",
                kind = WorkKind.OVERDUE,
                title = "Hoàn thành & đánh giá: ${interview.candidateName}",
                subtitle = "${interview.round} · quá giờ chưa xử lý",
                candidateId = interview.candidateId,
                interviewId = interview.id
            )
        }
        todayInterviews(interviews, now).take(limitPerGroup).forEach { interview ->
            items += WorkItem(
                key = "today-${interview.id}",
                kind = WorkKind.TODAY,
                title = "Phỏng vấn hôm nay: ${interview.candidateName}",
                subtitle = "${formatTime(interview.scheduledAt)} · ${interview.round}",
                candidateId = interview.candidateId,
                interviewId = interview.id
            )
        }
        missingReviews(candidates, interviews, scorecards).take(limitPerGroup).forEach { candidate ->
            val completed = latestCompleted(candidate.id, interviews)
            items += WorkItem(
                key = "review-${candidate.id}",
                kind = WorkKind.REVIEW,
                title = "Chờ đánh giá: ${candidate.name}",
                subtitle = completed?.let { "Đã xong ${it.round}" } ?: "Đã hoàn thành phỏng vấn",
                candidateId = candidate.id,
                interviewId = completed?.id
            )
        }
        decisionQueue(candidates).take(limitPerGroup).forEach { candidate ->
            items += WorkItem(
                key = "decision-${candidate.id}",
                kind = WorkKind.DECISION,
                title = "Chờ quyết định: ${candidate.name}",
                subtitle = candidate.position,
                candidateId = candidate.id,
                interviewId = null
            )
        }
        offerQueue(candidates).take(limitPerGroup).forEach { candidate ->
            items += WorkItem(
                key = "offer-${candidate.id}",
                kind = WorkKind.OFFER,
                title = "Theo dõi offer: ${candidate.name}",
                subtitle = candidate.position,
                candidateId = candidate.id,
                interviewId = null
            )
        }
        screeningQueue(candidates).take(limitPerGroup).forEach { candidate ->
            items += WorkItem(
                key = "screening-${candidate.id}",
                kind = WorkKind.SCREENING,
                title = "Sàng lọc CV: ${candidate.name}",
                subtitle = candidate.position,
                candidateId = candidate.id,
                interviewId = null
            )
        }
        unscheduled(candidates, interviews).take(limitPerGroup).forEach { candidate ->
            items += WorkItem(
                key = "unscheduled-${candidate.id}",
                kind = WorkKind.UNSCHEDULED,
                title = "Chưa đặt lịch: ${candidate.name}",
                subtitle = candidate.position,
                candidateId = candidate.id,
                interviewId = null
            )
        }
        return items
    }

    fun isSameDay(a: Long, b: Long): Boolean {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
        return formatter.format(Date(a)) == formatter.format(Date(b))
    }

    fun formatTime(time: Long): String =
        SimpleDateFormat("HH:mm dd/MM", Locale.forLanguageTag("vi-VN")).format(Date(time))
}
