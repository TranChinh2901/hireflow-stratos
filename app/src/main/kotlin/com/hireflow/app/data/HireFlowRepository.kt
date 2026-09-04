package com.hireflow.app.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HireFlowRepository(private val dao: HireFlowDao) {
    val candidates = dao.observeCandidates()
    val interviews = dao.observeInterviews()
    val scorecards = dao.observeScorecards()
    val tasks = dao.observeTasks()

    fun candidate(id: Long): Flow<CandidateEntity?> = dao.observeCandidate(id)
    fun history(id: Long): Flow<List<StageHistoryEntity>> = dao.observeHistory(id)

    suspend fun addCandidate(candidate: CandidateEntity): Long = dao.insertCandidate(candidate)

    suspend fun updateCandidate(candidate: CandidateEntity) = dao.updateCandidate(
        candidate.copy(updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING.name)
    )

    suspend fun attachCv(candidateId: Long, uri: String, remotePath: String? = null) =
        dao.updateCv(candidateId, uri, remotePath, System.currentTimeMillis())

    suspend fun moveNext(candidate: CandidateEntity) {
        val next = candidate.recruitmentStage.next()
        if (next == candidate.recruitmentStage) return
        dao.updateCandidate(candidate.copy(stage = next.name, updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING.name))
        dao.insertHistory(
            StageHistoryEntity(
                candidateId = candidate.id,
                fromStage = candidate.stage,
                toStage = next.name
            )
        )
    }

    suspend fun reject(candidate: CandidateEntity) {
        if (candidate.recruitmentStage == RecruitmentStage.REJECTED) return
        dao.updateCandidate(candidate.copy(stage = RecruitmentStage.REJECTED.name, updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING.name))
        dao.insertHistory(
            StageHistoryEntity(candidateId = candidate.id, fromStage = candidate.stage, toStage = RecruitmentStage.REJECTED.name)
        )
    }

    suspend fun addInterview(interview: InterviewEntity): Long = dao.insertInterview(interview.copy(syncState = SyncState.PENDING.name))

    suspend fun saveScorecard(scorecard: ScorecardEntity) = dao.insertScorecard(scorecard.copy(syncState = SyncState.PENDING.name))

    suspend fun toggleTask(task: HrTaskEntity) = dao.updateTask(task.copy(completed = !task.completed))

    internal suspend fun pendingCandidates() = dao.pendingCandidates()
    internal suspend fun pendingInterviews() = dao.pendingInterviews()
    internal suspend fun pendingScorecards() = dao.pendingScorecards()
    internal suspend fun candidateByLocalId(id: Long) = dao.candidateById(id)
    internal suspend fun candidateByRemoteId(id: String) = dao.candidateByRemoteId(id)
    internal suspend fun interviewByRemoteId(id: String) = dao.interviewByRemoteId(id)
    internal suspend fun scorecardByRemoteId(id: String) = dao.scorecardByRemoteId(id)
    internal suspend fun upsertCandidate(candidate: CandidateEntity) = dao.insertCandidate(candidate)
    internal suspend fun upsertInterview(interview: InterviewEntity) = dao.insertInterview(interview)
    internal suspend fun upsertScorecard(scorecard: ScorecardEntity) = dao.insertScorecard(scorecard)
    internal suspend fun updateInterview(interview: InterviewEntity) = dao.updateInterview(interview)
    internal suspend fun updateScorecard(scorecard: ScorecardEntity) = dao.updateScorecard(scorecard)
    internal suspend fun markCandidateSynced(id: String, orgId: String) = dao.markCandidateSynced(id, orgId)

    suspend fun seedDemoData() {
        if (dao.candidateCount() == 0) {
            val candidates = listOf(
                CandidateEntity(name = "Nguyễn Văn An", position = "Android Developer", email = "nguyenvanan@email.com", phone = "0901 234 567", experienceYears = 3, skills = "Kotlin, Jetpack Compose, MVVM, Coroutines, Android SDK, Git", stage = RecruitmentStage.INTERVIEW.name, note = "Ứng viên có nền tảng tốt về Android, tư duy logic khá tốt. Cần đánh giá thêm về kiến thức hệ thống và clean architecture."),
                CandidateEntity(name = "Trần Minh Khôi", position = "Backend Developer", email = "khoi.tran@email.com", phone = "0912 456 890", experienceYears = 4, skills = "Java, Spring Boot, PostgreSQL", stage = RecruitmentStage.SCREENING.name, note = "Kinh nghiệm sản phẩm fintech."),
                CandidateEntity(name = "Lê Thu Hà", position = "UI/UX Designer", email = "halth@email.com", phone = "0988 210 456", experienceYears = 2, skills = "Figma, UI Design, Prototyping", stage = RecruitmentStage.APPLIED.name, note = "Portfolio sạch, cần hỏi sâu về research."),
                CandidateEntity(name = "Phạm Quang Huy", position = "DevOps Engineer", email = "huy.pham@email.com", phone = "0934 178 220", experienceYears = 5, skills = "Docker, Kubernetes, AWS", stage = RecruitmentStage.INTERVIEW.name),
                CandidateEntity(name = "Đỗ Mai Phương", position = "QA Engineer", email = "phuong.do@email.com", phone = "0977 332 119", experienceYears = 2, skills = "Manual Test, SQL, Jira", stage = RecruitmentStage.SCREENING.name),
                CandidateEntity(name = "Hoàng Gia Bảo", position = "Frontend Developer", email = "bao.hoang@email.com", phone = "0905 742 831", experienceYears = 3, skills = "React, TypeScript, Next.js", stage = RecruitmentStage.INTERVIEW.name),
                CandidateEntity(name = "Nguyễn Thùy Linh", position = "Product Owner", email = "linh.nguyen@email.com", phone = "0908 448 290", experienceYears = 5, skills = "Agile, Product Strategy, Jira", stage = RecruitmentStage.OFFER.name),
                CandidateEntity(name = "Vũ Quốc Anh", position = "Backend Developer", email = "anh.vu@email.com", phone = "0966 731 520", experienceYears = 4, skills = "Node.js, PostgreSQL, Redis", stage = RecruitmentStage.WAITING_DECISION.name),
                CandidateEntity(name = "Phan Trung Hiếu", position = "Android Developer", email = "hieu.phan@email.com", phone = "0903 662 411", experienceYears = 4, skills = "Kotlin, Compose, Android SDK", stage = RecruitmentStage.HIRED.name),
                CandidateEntity(name = "Bùi Lan Anh", position = "UI/UX Designer", email = "anh.bui@email.com", phone = "0918 442 738", experienceYears = 3, skills = "Figma, Research, Design System", stage = RecruitmentStage.HIRED.name)
            )
            val ids = candidates.map { dao.insertCandidate(it) }
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0)
            }.timeInMillis
            dao.insertInterview(InterviewEntity(candidateId = ids[0], candidateName = "Nguyễn Văn An", position = "Android Developer", scheduledAt = today, format = "Online", interviewer = "Trần Hoàng Nam", round = "Vòng 2: Technical"))
            dao.insertInterview(InterviewEntity(candidateId = ids[1], candidateName = "Trần Minh Khôi", position = "Backend Developer", scheduledAt = today + 90 * 60_000, format = "Onsite", interviewer = "Nguyễn Thị Linh", round = "Vòng 1: HR"))
            dao.insertInterview(InterviewEntity(candidateId = ids[2], candidateName = "Lê Thu Hà", position = "UI/UX Designer", scheduledAt = today + 270 * 60_000, format = "Online", interviewer = "Trần Hoàng Nam", round = "Vòng 2: Technical"))
        }
        if (dao.taskCount() == 0) {
            dao.insertTasks(
                listOf(
                    HrTaskEntity(title = "Phỏng vấn: Nguyễn Văn An", subtitle = "09:30 – 10:30 · Android Developer", type = "interview"),
                    HrTaskEntity(title = "Đánh giá: Trần Minh Khôi", subtitle = "Backend Developer", type = "review"),
                    HrTaskEntity(title = "Review CV: 5 ứng viên mới", subtitle = "Ưu tiên trong hôm nay", type = "cv"),
                    HrTaskEntity(title = "Gửi offer: 1 ứng viên", subtitle = "Đang chờ phản hồi", type = "offer")
                )
            )
        }
    }
}
