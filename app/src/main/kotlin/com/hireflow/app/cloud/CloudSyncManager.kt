package com.hireflow.app.cloud

import android.content.Context
import android.net.Uri
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.HireFlowRepository
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.data.SyncState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class CloudSyncManager(
    private val context: Context,
    private val repository: HireFlowRepository,
    private val backend: SupabaseBackend
) {
    suspend fun syncAll(profile: UserProfileDto) {
        if (!backend.isConfigured || backend.currentUserId() == null) return
        pushPending(profile)
        pullLatest()
    }

    fun realtimeCandidates(): Flow<List<CandidateDto>> = backend.realtimeCandidates()

    suspend fun mergeRealtime(candidates: List<CandidateDto>) {
        candidates.forEach { mergeCandidate(it) }
    }

    suspend fun uploadCandidateCv(candidate: CandidateEntity, uri: Uri, profile: UserProfileDto): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Không thể đọc file CV")
        val path = "${profile.organizationId}/${candidate.remoteId}/cv.pdf"
        backend.uploadCv(path, bytes)
        repository.attachCv(candidate.id, uri.toString(), path)
        return path
    }

    private suspend fun pushPending(profile: UserProfileDto) {
        repository.pendingCandidates().forEach { local ->
            backend.upsertCandidate(local.toDto(profile.organizationId))
            repository.markCandidateSynced(local.remoteId, profile.organizationId)
        }
        repository.pendingInterviews().forEach { local ->
            val candidate = repository.candidateByLocalId(local.candidateId) ?: return@forEach
            backend.upsertInterview(local.toDto(profile.organizationId, candidate.remoteId))
            repository.updateInterview(local.copy(organizationId = profile.organizationId, remoteCandidateId = candidate.remoteId, syncState = SyncState.SYNCED.name))
        }
        repository.pendingScorecards().forEach { local ->
            val candidate = repository.candidateByLocalId(local.candidateId) ?: return@forEach
            backend.upsertScorecard(local.toDto(profile.organizationId, candidate.remoteId, profile.id))
            repository.updateScorecard(local.copy(organizationId = profile.organizationId, remoteCandidateId = candidate.remoteId, evaluatorId = profile.id, syncState = SyncState.SYNCED.name))
        }
    }

    private suspend fun pullLatest() {
        backend.fetchCandidates().forEach { mergeCandidate(it) }
        backend.fetchInterviews().forEach { remote ->
            val candidate = repository.candidateByRemoteId(remote.candidateId) ?: return@forEach
            val local = repository.interviewByRemoteId(remote.id)
            if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return@forEach
            repository.upsertInterview(remote.toEntity(local?.id ?: 0, candidate.id, candidate.name, candidate.position))
        }
        backend.fetchScorecards().forEach { remote ->
            val candidate = repository.candidateByRemoteId(remote.candidateId) ?: return@forEach
            val local = repository.scorecardByRemoteId(remote.id)
            if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return@forEach
            repository.upsertScorecard(remote.toEntity(local?.id ?: 0, candidate.id))
        }
    }

    private suspend fun mergeCandidate(remote: CandidateDto) {
        val local = repository.candidateByRemoteId(remote.id)
        if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return
        repository.upsertCandidate(remote.toEntity(local?.id ?: 0, local?.cvUri))
    }
}

private fun CandidateEntity.toDto(orgId: String) = CandidateDto(
    id = remoteId, organizationId = orgId, fullName = name, position = position,
    email = email, phone = phone, experienceYears = experienceYears, skills = skillList,
    stage = stage.lowercase(), notes = note, cvPath = remoteCvPath,
    updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun CandidateDto.toEntity(localId: Long, localCvUri: String?) = CandidateEntity(
    id = localId, name = fullName, position = position, email = email, phone = phone,
    experienceYears = experienceYears, skills = skills.joinToString(", "), stage = stage.uppercase(),
    note = notes, cvUri = localCvUri, remoteId = id, organizationId = organizationId,
    remoteCvPath = cvPath, updatedAt = updatedAt.toEpoch(), syncState = SyncState.SYNCED.name
)

private fun InterviewEntity.toDto(orgId: String, candidateRemoteId: String) = InterviewDto(
    id = remoteId, organizationId = orgId, candidateId = candidateRemoteId,
    scheduledAt = Instant.ofEpochMilli(scheduledAt).toString(), durationMinutes = durationMinutes,
    format = format.lowercase(), interviewerName = interviewer, interviewerId = interviewerUserId,
    round = round, checklist = checklist.split(",").map(String::trim), completed = completed,
    updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun InterviewDto.toEntity(localId: Long, localCandidateId: Long, name: String, position: String) = InterviewEntity(
    id = localId, candidateId = localCandidateId, candidateName = name, position = position,
    scheduledAt = scheduledAt.toEpoch(), durationMinutes = durationMinutes,
    format = format.replaceFirstChar(Char::uppercase), interviewer = interviewerName,
    round = round, checklist = checklist.joinToString(", "), completed = completed,
    remoteId = id, remoteCandidateId = candidateId, organizationId = organizationId,
    interviewerUserId = interviewerId, updatedAt = updatedAt.toEpoch(), syncState = SyncState.SYNCED.name
)

private fun ScorecardEntity.toDto(orgId: String, candidateRemoteId: String, userId: String) = ScorecardDto(
    id = remoteId, organizationId = orgId, candidateId = candidateRemoteId, evaluatorId = userId,
    technical = technical, communication = communication, problemSolving = problemSolving,
    cultureFit = cultureFit, strengths = strengths, improvements = improvements, notes = notes,
    conclusion = conclusion, updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun ScorecardDto.toEntity(localId: Long, localCandidateId: Long) = ScorecardEntity(
    id = localId, candidateId = localCandidateId, technical = technical, communication = communication,
    problemSolving = problemSolving, cultureFit = cultureFit, strengths = strengths,
    improvements = improvements, notes = notes, conclusion = conclusion,
    remoteId = id, remoteCandidateId = candidateId, organizationId = organizationId,
    evaluatorId = evaluatorId, updatedAt = updatedAt.toEpoch(), syncState = SyncState.SYNCED.name
)

private fun String.toEpoch(): Long = runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(0L)
