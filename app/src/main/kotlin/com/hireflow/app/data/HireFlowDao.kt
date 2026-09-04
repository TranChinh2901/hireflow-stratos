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

    @Query("UPDATE candidates SET cvUri = :uri WHERE id = :candidateId")
    suspend fun updateCv(candidateId: Long, uri: String)

    @Query("UPDATE candidates SET cvUri = :uri, remoteCvPath = :remotePath, updatedAt = :updatedAt, syncState = 'PENDING' WHERE id = :candidateId")
    suspend fun updateCv(candidateId: Long, uri: String, remotePath: String?, updatedAt: Long)

    @Query("SELECT * FROM candidates WHERE syncState != 'SYNCED'")
    suspend fun pendingCandidates(): List<CandidateEntity>

    @Query("SELECT * FROM candidates WHERE remoteId = :remoteId LIMIT 1")
    suspend fun candidateByRemoteId(remoteId: String): CandidateEntity?

    @Query("UPDATE candidates SET syncState = :state, organizationId = :organizationId WHERE remoteId = :remoteId")
    suspend fun markCandidateSynced(remoteId: String, organizationId: String, state: String = "SYNCED")

    @Query("SELECT COUNT(*) FROM candidates")
    suspend fun candidateCount(): Int

    @Query("SELECT * FROM interviews ORDER BY scheduledAt ASC")
    fun observeInterviews(): Flow<List<InterviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity): Long

    @Update
    suspend fun updateInterview(interview: InterviewEntity)

    @Query("SELECT * FROM interviews WHERE syncState != 'SYNCED'")
    suspend fun pendingInterviews(): List<InterviewEntity>

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

    @Query("SELECT * FROM scorecards WHERE syncState != 'SYNCED'")
    suspend fun pendingScorecards(): List<ScorecardEntity>

    @Query("SELECT * FROM scorecards WHERE remoteId = :remoteId LIMIT 1")
    suspend fun scorecardByRemoteId(remoteId: String): ScorecardEntity?

    @Insert
    suspend fun insertHistory(history: StageHistoryEntity)

    @Query("SELECT * FROM stage_history WHERE candidateId = :candidateId ORDER BY changedAt DESC")
    fun observeHistory(candidateId: Long): Flow<List<StageHistoryEntity>>

    @Query("SELECT * FROM hr_tasks ORDER BY completed ASC, dueAt ASC")
    fun observeTasks(): Flow<List<HrTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<HrTaskEntity>)

    @Update
    suspend fun updateTask(task: HrTaskEntity)

    @Query("SELECT COUNT(*) FROM hr_tasks")
    suspend fun taskCount(): Int
}
