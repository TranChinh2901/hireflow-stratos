package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.InterviewStatus
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkQueueTest {
    private fun candidate(stage: RecruitmentStage, id: Long = 1) = CandidateEntity(
        id = id,
        name = "An",
        position = "Android",
        email = "",
        phone = "",
        experienceYears = 2,
        skills = "Kotlin",
        stage = stage.name
    )

    private fun interview(candidateId: Long = 1, completed: Boolean = false, scheduledAt: Long = 0, id: Long = 1) =
        InterviewEntity(
            id = id,
            candidateId = candidateId,
            candidateName = "An",
            position = "Android",
            scheduledAt = scheduledAt,
            format = "Online",
            interviewer = "HR",
            completed = completed,
            status = if (completed) InterviewStatus.COMPLETED.name else InterviewStatus.SCHEDULED.name
        )

    private fun scorecard(candidateId: Long = 1, conclusion: String = "Hire") = ScorecardEntity(
        candidateId = candidateId,
        technical = 4,
        communication = 4,
        problemSolving = 4,
        cultureFit = 4,
        strengths = "",
        improvements = "",
        notes = "",
        conclusion = conclusion
    )

    @Test
    fun appliedWithoutCvAsksForCv() {
        val action = WorkQueue.nextActionFor(candidate(RecruitmentStage.APPLIED), emptyList(), emptyList())
        assertEquals(PrimaryAction.ATTACH_CV, action.action)
    }

    @Test
    fun interviewWithoutScheduleAsksForSchedule() {
        val action = WorkQueue.nextActionFor(candidate(RecruitmentStage.INTERVIEW), emptyList(), emptyList())
        assertEquals(PrimaryAction.SCHEDULE, action.action)
    }

    @Test
    fun pastDueInterviewAsksCompleteAndReview() {
        val interviews = listOf(interview(completed = false, scheduledAt = 1_000, id = 7))
        val action = WorkQueue.nextActionFor(candidate(RecruitmentStage.INTERVIEW), interviews, emptyList(), now = 2_000)
        assertEquals(PrimaryAction.COMPLETE_AND_REVIEW, action.action)
        assertEquals(7L, action.interviewId)
    }

    @Test
    fun completedWithoutScorecardAsksReview() {
        val interviews = listOf(interview(completed = true, scheduledAt = 1_000))
        val action = WorkQueue.nextActionFor(candidate(RecruitmentStage.INTERVIEW), interviews, emptyList(), now = 2_000)
        assertEquals(PrimaryAction.REVIEW, action.action)
    }

    @Test
    fun missingReviewsUsesFullScorecardSet() {
        val candidates = listOf(candidate(RecruitmentStage.INTERVIEW))
        val interviews = listOf(interview(completed = true, scheduledAt = 1_000, id = 11))
        assertEquals(1, WorkQueue.missingReviews(candidates, interviews, emptyList()).size)
        // Phieu tong hop cu khong tinh cho buoi moi.
        assertEquals(1, WorkQueue.missingReviews(candidates, interviews, listOf(scorecard())).size)
        assertTrue(WorkQueue.missingReviews(candidates, interviews, listOf(scorecard().copy(interviewId = 11))).isEmpty())
    }

    @Test
    fun workItemsRouteToCorrectTargets() {
        val candidates = listOf(candidate(RecruitmentStage.SCREENING, id = 3))
        val items = WorkQueue.workItems(candidates, emptyList(), emptyList())
        assertTrue(items.any { it.key == "screening-3" && it.candidateId == 3L && it.interviewId == null })
    }

    @Test
    fun todayWorkItemsOnlyIncludesTodayAndOverdue() {
        val now = 10_000_000L
        val today = interview(scheduledAt = now + 3_600_000, id = 31)
        val overdue = interview(scheduledAt = now - 2 * 86_400_000, id = 32)
        val future = interview(scheduledAt = now + 5 * 86_400_000, id = 33)
        val done = interview(completed = true, scheduledAt = now + 3_600_000, id = 34)
        val cancelled = interview(scheduledAt = now + 3_600_000, id = 35).copy(status = InterviewStatus.CANCELLED.name)
        val items = WorkQueue.todayWorkItems(listOf(today, overdue, future, done, cancelled), now)
        assertTrue(items.any { it.key == "today-31" })
        assertTrue(items.any { it.key == "overdue-32" })
        assertTrue(items.none { it.interviewId == 33L || it.interviewId == 34L || it.interviewId == 35L })
    }
}
