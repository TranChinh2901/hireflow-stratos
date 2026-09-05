package com.hireflow.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HireFlowDao {
    @Query("SELECT * FROM candidates ORDER BY createdAt DESC")
    fun observeCandidates(): Flow<List<CandidateEntity>>

    @Query("SELECT * FROM candidates WHERE id = :id LIMIT 1")
    fun observeCandidate(id: Long): Flow<CandidateEntity?>

    @Query("SELECT * FROM candidates WHERE id = :id LIMIT 1")
    suspend fun candidateById(id: Long): CandidateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: CandidateEntity): Long

    @Update
    suspend fun updateCandidate(candidate: CandidateEntity)

    @Query("UPDATE interviews SET candidateName = :name, position = :position, updatedAt = :updatedAt, syncState = 'PENDING' WHERE candidateId = :candidateId")
    suspend fun updateInterviewCandidateSnapshot(candidateId: Long, name: String, position: String, updatedAt: Long)

    @Query("UPDATE candidates SET cvUri = :uri WHERE id = :candidateId")
    suspend fun updateCv(candidateId: Long, uri: String)

    @Query("UPDATE candidates SET cvUri = :uri, remoteCvPath = :remotePath, updatedAt = :updatedAt, syncState = 'PENDING' WHERE id = :candidateId")
    suspend fun updateCv(candidateId: Long, uri: String, remotePath: String?, updatedAt: Long)

    @Query("SELECT * FROM candidates WHERE organizationId = :organizationId AND syncState != 'SYNCED'")
    suspend fun pendingCandidates(organizationId: String): List<CandidateEntity>

    @Query("SELECT * FROM candidates WHERE remoteId = :remoteId LIMIT 1")
    suspend fun candidateByRemoteId(remoteId: String): CandidateEntity?

    @Query("UPDATE candidates SET syncState = :state, organizationId = :organizationId WHERE remoteId = :remoteId")
    suspend fun markCandidateSynced(remoteId: String, organizationId: String, state: String = "SYNCED")

    @Query("SELECT COUNT(*) FROM candidates")
    suspend fun candidateCount(): Int

    @Query("SELECT COUNT(*) FROM candidates WHERE organizationId IS NULL")
    suspend fun offlineCandidateCount(): Int

    @Query("SELECT * FROM interviews ORDER BY scheduledAt ASC")
    fun observeInterviews(): Flow<List<InterviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity): Long

    @Update
    suspend fun updateInterview(interview: InterviewEntity)

    @Query("SELECT * FROM interviews WHERE organizationId = :organizationId AND syncState != 'SYNCED'")
    suspend fun pendingInterviews(organizationId: String): List<InterviewEntity>

    @Query("SELECT * FROM interviews WHERE candidateId = :candidateId ORDER BY scheduledAt DESC")
    suspend fun interviewsForCandidate(candidateId: Long): List<InterviewEntity>

    @Query("SELECT * FROM interviews WHERE organizationId = :organizationId ORDER BY scheduledAt")
    suspend fun interviewsForOrganization(organizationId: String): List<InterviewEntity>

    @Query("SELECT * FROM interviews WHERE organizationId IS NULL ORDER BY scheduledAt")
    suspend fun offlineInterviews(): List<InterviewEntity>

    @Query("SELECT * FROM interviews WHERE remoteId = :remoteId LIMIT 1")
    suspend fun interviewByRemoteId(remoteId: String): InterviewEntity?

    @Query("SELECT COUNT(*) FROM interviews")
    suspend fun interviewCount(): Int

    @Query("SELECT * FROM scorecards ORDER BY createdAt DESC")
    fun observeScorecards(): Flow<List<ScorecardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScorecard(scorecard: ScorecardEntity): Long

    @Update
    suspend fun updateScorecard(scorecard: ScorecardEntity)

    @Query("SELECT * FROM scorecards WHERE organizationId = :organizationId AND syncState != 'SYNCED'")
    suspend fun pendingScorecards(organizationId: String): List<ScorecardEntity>

    @Query("SELECT * FROM scorecards WHERE candidateId = :candidateId ORDER BY createdAt DESC")
    suspend fun scorecardsForCandidate(candidateId: Long): List<ScorecardEntity>

    @Query("""
        SELECT * FROM scorecards
        WHERE candidateId = :candidateId
          AND ((:evaluatorId IS NULL AND evaluatorId IS NULL) OR evaluatorId = :evaluatorId)
        ORDER BY createdAt DESC LIMIT 1
    """)
    suspend fun scorecardForEvaluator(candidateId: Long, evaluatorId: String?): ScorecardEntity?

    @Query("SELECT * FROM scorecards WHERE remoteId = :remoteId LIMIT 1")
    suspend fun scorecardByRemoteId(remoteId: String): ScorecardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: StageHistoryEntity)

    @Query("SELECT * FROM stage_history WHERE organizationId = :organizationId AND syncState != 'SYNCED'")
    suspend fun pendingHistory(organizationId: String): List<StageHistoryEntity>

    @Query("SELECT * FROM stage_history WHERE remoteId = :remoteId LIMIT 1")
    suspend fun historyByRemoteId(remoteId: String): StageHistoryEntity?

    @Query("UPDATE stage_history SET syncState = 'SYNCED', organizationId = :organizationId, actorId = :actorId WHERE remoteId = :remoteId")
    suspend fun markHistorySynced(remoteId: String, organizationId: String, actorId: String)

    @Query("SELECT * FROM stage_history WHERE candidateId = :candidateId ORDER BY changedAt DESC")
    fun observeHistory(candidateId: Long): Flow<List<StageHistoryEntity>>

    @Query("SELECT * FROM stage_history ORDER BY changedAt DESC")
    fun observeAllHistory(): Flow<List<StageHistoryEntity>>

    @Query("SELECT * FROM hr_tasks ORDER BY completed ASC, dueAt ASC")
    fun observeTasks(): Flow<List<HrTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<HrTaskEntity>)

    @Update
    suspend fun updateTask(task: HrTaskEntity)

    @Query("SELECT * FROM hr_tasks WHERE organizationId = :organizationId AND syncState != 'SYNCED'")
    suspend fun pendingTasks(organizationId: String): List<HrTaskEntity>

    @Query("SELECT * FROM hr_tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun taskByRemoteId(remoteId: String): HrTaskEntity?

    @Query("SELECT COUNT(*) FROM hr_tasks")
    suspend fun taskCount(): Int

    @Query("SELECT COUNT(*) FROM hr_tasks WHERE organizationId IS NULL")
    suspend fun offlineTaskCount(): Int
}
