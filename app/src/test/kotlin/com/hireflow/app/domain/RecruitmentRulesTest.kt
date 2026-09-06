package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.InterviewStatus
import com.hireflow.app.data.OfferResponse
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecruitmentRulesTest {
    private val now = 2_000_000L

    @Test
    fun `screening candidate needs a CV before entering interview`() {
        val candidate = candidate(RecruitmentStage.SCREENING)

        assertFalse(RecruitmentRules.canAdvance(candidate, emptyList(), emptyList()))
        assertTrue(
            RecruitmentRules.canAdvance(
                candidate.copy(remoteCvPath = "workspace/candidate/cv.pdf"),
                emptyList(),
                emptyList()
            )
        )
    }

    @Test
    fun `interview candidate needs completed sessions each with their own scorecard`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val completed = interview(candidate, completed = true)

        assertFalse(RecruitmentRules.canAdvance(candidate, listOf(completed), emptyList()))
        // Phieu tong hop cu (khong gan buoi) khong mo khoa buoi moi.
        assertFalse(RecruitmentRules.canAdvance(candidate, listOf(completed), listOf(scorecard(candidate))))
        assertTrue(
            RecruitmentRules.canAdvance(
                candidate,
                listOf(completed),
                listOf(scorecard(candidate).copy(interviewId = completed.id))
            )
        )
    }

    @Test
    fun `cancelled sessions are excluded but pending ones block closing`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val done = interview(candidate, completed = true, id = 21)
        val doneScore = scorecard(candidate).copy(interviewId = done.id)
        val cancelled = interview(candidate, id = 22).copy(status = InterviewStatus.CANCELLED.name)
        val pending = interview(candidate, id = 23)

        assertTrue(RecruitmentRules.canAdvance(candidate, listOf(done, cancelled), listOf(doneScore)))
        assertFalse(RecruitmentRules.canAdvance(candidate, listOf(done, pending), listOf(doneScore)))
    }

    @Test
    fun `no-show sessions do not block closing once completed ones are reviewed`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val done = interview(candidate, completed = true, id = 31)
        val noShow = interview(candidate, id = 32).copy(status = InterviewStatus.NO_SHOW.name)

        assertTrue(
            RecruitmentRules.canAdvance(
                candidate,
                listOf(done, noShow),
                listOf(scorecard(candidate).copy(interviewId = done.id))
            )
        )
    }

    @Test
    fun `waiting decision only advances to offer for a positive conclusion`() {
        val candidate = candidate(RecruitmentStage.WAITING_DECISION)

        assertFalse(RecruitmentRules.canAdvance(candidate, emptyList(), listOf(scorecard(candidate, "Reject"))))
        assertTrue(RecruitmentRules.canAdvance(candidate, emptyList(), listOf(scorecard(candidate, "Strong Hire"))))
    }

    @Test
    fun `scheduling rejects terminal stages past times and overlapping interviewer slots`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val existing = interview(candidate, scheduledAt = now + 60_000, interviewer = "An")

        assertFalse(RecruitmentRules.canSchedule(candidate.copy(stage = RecruitmentStage.REJECTED.name), now + 120_000, "An", emptyList(), now))
        assertFalse(RecruitmentRules.canSchedule(candidate, now - 1, "An", emptyList(), now))
        assertFalse(RecruitmentRules.canSchedule(candidate, now + 90_000, "An", listOf(existing), now))
        assertTrue(RecruitmentRules.canSchedule(candidate, now + 4_000_000, "An", listOf(existing), now))
    }

    @Test
    fun `scheduling ignores cancelled sessions but blocks candidate overlap`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val cancelled = interview(candidate, scheduledAt = now + 60_000, interviewer = "An", id = 41)
            .copy(status = InterviewStatus.CANCELLED.name)
        val sameCandidate = interview(candidate, scheduledAt = now + 60_000, interviewer = "Binh", id = 42)

        assertTrue(RecruitmentRules.canSchedule(candidate, now + 90_000, "An", listOf(cancelled), now))
        assertFalse(RecruitmentRules.canSchedule(candidate, now + 90_000, "Binh", listOf(sameCandidate), now))
    }

    @Test
    fun `offer advances only after the candidate accepts`() {
        val candidate = candidate(RecruitmentStage.OFFER)

        assertFalse(RecruitmentRules.canAdvance(candidate, emptyList(), emptyList()))
        assertTrue(
            RecruitmentRules.canAdvance(
                candidate.copy(offerResponse = OfferResponse.ACCEPTED.name),
                emptyList(),
                emptyList()
            )
        )
    }

    @Test
    fun `scorecard requires a completed interview and an active interview stage`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)

        assertFalse(RecruitmentRules.canReview(candidate, emptyList()))
        assertTrue(RecruitmentRules.canReview(candidate, listOf(interview(candidate, completed = true))))
        assertFalse(
            RecruitmentRules.canReview(
                candidate.copy(stage = RecruitmentStage.HIRED.name),
                listOf(interview(candidate, completed = true))
            )
        )
    }

    @Test
    fun `scorecard for a session requires that specific session completed`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val done = interview(candidate, completed = true, id = 51)
        val upcoming = interview(candidate, scheduledAt = now + 60_000, id = 52)

        assertTrue(RecruitmentRules.canReview(candidate, listOf(done, upcoming), done.id))
        assertFalse(RecruitmentRules.canReview(candidate, listOf(done, upcoming), upcoming.id))
        assertFalse(RecruitmentRules.canReview(candidate, listOf(done, upcoming), 999L))
    }

    @Test
    fun `all sessions stay visible as history`() {
        val rejected = candidate(RecruitmentStage.REJECTED)

        assertTrue(RecruitmentRules.shouldShowInterview(rejected, interview(rejected)))
        assertTrue(RecruitmentRules.shouldShowInterview(rejected, interview(rejected, completed = true)))
    }

    private fun candidate(stage: RecruitmentStage) = CandidateEntity(
        id = 10,
        name = "Candidate",
        position = "Android Developer",
        email = "",
        phone = "",
        experienceYears = 2,
        skills = "Kotlin",
        stage = stage.name
    )

    private fun interview(
        candidate: CandidateEntity,
        completed: Boolean = false,
        scheduledAt: Long = now - 60_000,
        interviewer: String = "Interviewer",
        id: Long = 20
    ) = InterviewEntity(
        id = id,
        candidateId = candidate.id,
        candidateName = candidate.name,
        position = candidate.position,
        scheduledAt = scheduledAt,
        format = "Online",
        interviewer = interviewer,
        completed = completed,
        status = if (completed) InterviewStatus.COMPLETED.name else InterviewStatus.SCHEDULED.name
    )

    private fun scorecard(candidate: CandidateEntity, conclusion: String = "Hire") = ScorecardEntity(
        candidateId = candidate.id,
        technical = 4,
        communication = 4,
        problemSolving = 4,
        cultureFit = 4,
        strengths = "",
        improvements = "",
        notes = "",
        conclusion = conclusion
    )
}
