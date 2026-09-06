package com.hireflow.app.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    val email: String? = null,
    val phone: String = "",
    val department: String = "Human Resources",
    @SerialName("job_title") val jobTitle: String = "Recruitment Specialist"
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val department: String,
    @SerialName("job_title") val jobTitle: String
)

@Serializable
data class CandidateDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("full_name") val fullName: String,
    val position: String,
    val email: String,
    val phone: String,
    @SerialName("experience_years") val experienceYears: Int,
    val skills: List<String>,
    val stage: String,
    val notes: String,
    @SerialName("cv_path") val cvPath: String? = null,
    @SerialName("cv_name") val cvName: String? = null,
    @SerialName("close_reason") val closeReason: String? = null,
    @SerialName("offer_sent_at") val offerSentAt: String? = null,
    @SerialName("offer_response") val offerResponse: String? = null,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class InterviewDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("candidate_id") val candidateId: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val format: String,
    @SerialName("interviewer_name") val interviewerName: String,
    @SerialName("interviewer_id") val interviewerId: String? = null,
    val round: String,
    val checklist: List<String>,
    val completed: Boolean = false,
    val status: String = "scheduled",
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ScorecardDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("candidate_id") val candidateId: String,
    @SerialName("interview_id") val interviewId: String? = null,
    @SerialName("evaluator_id") val evaluatorId: String,
    val technical: Int,
    val communication: Int,
    @SerialName("problem_solving") val problemSolving: Int,
    @SerialName("culture_fit") val cultureFit: Int,
    val strengths: String,
    val improvements: String,
    val notes: String,
    val conclusion: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class StageHistoryDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("candidate_id") val candidateId: String,
    @SerialName("from_stage") val fromStage: String,
    @SerialName("to_stage") val toStage: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("changed_at") val changedAt: String
)

@Serializable
data class HrTaskDto(
    val id: String,
    @SerialName("organization_id") val organizationId: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val completed: Boolean,
    @SerialName("due_at") val dueAt: String,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("updated_at") val updatedAt: String
)
