package com.hireflow.app.domain

import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
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
    fun `interview candidate needs a completed interview and scorecard before decision`() {
        val candidate = candidate(RecruitmentStage.INTERVIEW)
        val completed = interview(candidate, completed = true)

        assertFalse(RecruitmentRules.canAdvance(candidate, listOf(completed), emptyList()))
        assertTrue(RecruitmentRules.canAdvance(candidate, listOf(completed), listOf(scorecard(candidate))))
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
    fun `unfinished interviews disappear after the candidate reaches a terminal stage`() {
        val rejected = candidate(RecruitmentStage.REJECTED)

        assertFalse(RecruitmentRules.shouldShowInterview(rejected, interview(rejected)))
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
        interviewer: String = "Interviewer"
    ) = InterviewEntity(
        id = 20,
        candidateId = candidate.id,
        candidateName = candidate.name,
        position = candidate.position,
        scheduledAt = scheduledAt,
        format = "Online",
        interviewer = interviewer,
        completed = completed
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
