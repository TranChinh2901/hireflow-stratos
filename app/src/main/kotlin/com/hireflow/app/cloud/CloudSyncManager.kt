package com.hireflow.app.cloud

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.HireFlowRepository
import com.hireflow.app.data.HrTaskEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.data.StageHistoryEntity
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
    fun realtimeInterviews(): Flow<List<InterviewDto>> = backend.realtimeInterviews()
    fun realtimeScorecards(): Flow<List<ScorecardDto>> = backend.realtimeScorecards()

    suspend fun mergeRealtime(candidates: List<CandidateDto>) {
        candidates.forEach { mergeCandidate(it) }
    }

    suspend fun mergeRealtimeInterviews(interviews: List<InterviewDto>) {
        interviews.forEach { mergeInterview(it) }
    }

    suspend fun mergeRealtimeScorecards(scorecards: List<ScorecardDto>) {
        scorecards.forEach { mergeScorecard(it) }
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
        repository.pendingCandidates(profile.organizationId).forEach { local ->
            val ready = if (local.cvUri != null && local.remoteCvPath == null) {
                val remotePath = uploadCandidateCv(local, local.cvUri.toUri(), profile)
                local.copy(remoteCvPath = remotePath)
            } else local
            backend.upsertCandidate(ready.toDto(profile.organizationId))
            repository.markCandidateSynced(local.remoteId, profile.organizationId)
        }
        repository.pendingInterviews(profile.organizationId).forEach { local ->
            val candidate = repository.candidateByLocalId(local.candidateId) ?: return@forEach
            backend.upsertInterview(local.toDto(profile.organizationId, candidate.remoteId))
            repository.updateInterview(local.copy(organizationId = profile.organizationId, remoteCandidateId = candidate.remoteId, syncState = SyncState.SYNCED.name))
        }
        repository.pendingScorecards(profile.organizationId).forEach { local ->
            val candidate = repository.candidateByLocalId(local.candidateId) ?: return@forEach
            backend.upsertScorecard(local.toDto(profile.organizationId, candidate.remoteId, profile.id))
            repository.updateScorecard(local.copy(organizationId = profile.organizationId, remoteCandidateId = candidate.remoteId, evaluatorId = profile.id, syncState = SyncState.SYNCED.name))
        }
        repository.pendingHistory(profile.organizationId).forEach { local ->
            val candidate = repository.candidateByLocalId(local.candidateId) ?: return@forEach
            val actorId = local.actorId ?: profile.id
            backend.upsertHistory(local.toDto(profile.organizationId, candidate.remoteId, actorId))
            repository.markHistorySynced(local.remoteId, profile.organizationId, actorId)
        }
        repository.pendingTasks(profile.organizationId).forEach { local ->
            backend.upsertTask(local.toDto(profile.organizationId))
            repository.updateTask(local.copy(syncState = SyncState.SYNCED.name))
        }
    }

    private suspend fun pullLatest() {
        backend.fetchCandidates().forEach { mergeCandidate(it) }
        backend.fetchInterviews().forEach { mergeInterview(it) }
        backend.fetchScorecards().forEach { mergeScorecard(it) }
        backend.fetchHistory().forEach { remote ->
            if (repository.historyByRemoteId(remote.id) != null) return@forEach
            val candidate = repository.candidateByRemoteId(remote.candidateId) ?: return@forEach
            repository.upsertHistory(remote.toEntity(candidate.id))
        }
        backend.fetchTasks().forEach { remote ->
            val local = repository.taskByRemoteId(remote.id)
            if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return@forEach
            repository.upsertTask(remote.toEntity(local?.id ?: 0))
        }
    }

    private suspend fun mergeCandidate(remote: CandidateDto) {
        val local = repository.candidateByRemoteId(remote.id)
        if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return
        repository.upsertCandidate(remote.toEntity(local?.id ?: 0, local?.cvUri))
    }

    private suspend fun mergeInterview(remote: InterviewDto) {
        val candidate = repository.candidateByRemoteId(remote.candidateId) ?: return
        val local = repository.interviewByRemoteId(remote.id)
        if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return
        repository.upsertInterview(remote.toEntity(local?.id ?: 0, candidate.id, candidate.name, candidate.position))
    }

    private suspend fun mergeScorecard(remote: ScorecardDto) {
        val candidate = repository.candidateByRemoteId(remote.candidateId) ?: return
        val local = repository.scorecardByRemoteId(remote.id)
        if (local != null && SyncConflictResolver.keepLocal(local.syncState, local.updatedAt, remote.updatedAt.toEpoch())) return
        repository.upsertScorecard(remote.toEntity(local?.id ?: 0, candidate.id))
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

private fun StageHistoryEntity.toDto(orgId: String, candidateRemoteId: String, userId: String) = StageHistoryDto(
    id = remoteId,
    organizationId = orgId,
    candidateId = candidateRemoteId,
    fromStage = fromStage.lowercase(),
    toStage = toStage.lowercase(),
    actorId = userId,
    changedAt = Instant.ofEpochMilli(changedAt).toString()
)

private fun StageHistoryDto.toEntity(localCandidateId: Long) = StageHistoryEntity(
    candidateId = localCandidateId,
    fromStage = fromStage.uppercase(),
    toStage = toStage.uppercase(),
    changedAt = changedAt.toEpoch(),
    remoteId = id,
    organizationId = organizationId,
    actorId = actorId,
    syncState = SyncState.SYNCED.name
)

private fun HrTaskEntity.toDto(orgId: String) = HrTaskDto(
    id = remoteId,
    organizationId = orgId,
    title = title,
    subtitle = subtitle,
    type = type,
    completed = completed,
    dueAt = Instant.ofEpochMilli(dueAt).toString(),
    assigneeId = assigneeId,
    updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun HrTaskDto.toEntity(localId: Long) = HrTaskEntity(
    id = localId,
    title = title,
    subtitle = subtitle,
    type = type,
    completed = completed,
    dueAt = dueAt.toEpoch(),
    remoteId = id,
    organizationId = organizationId,
    assigneeId = assigneeId,
    updatedAt = updatedAt.toEpoch(),
    syncState = SyncState.SYNCED.name
)

private fun String.toEpoch(): Long = runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(0L)
