package com.hireflow.app.cloud

import com.hireflow.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseBackend {
    val isConfigured: Boolean = BuildConfig.SUPABASE_URL.startsWith("https://") &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val client: SupabaseClient? = if (isConfigured) {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    } else null

    fun currentUserId(): String? = client?.auth?.currentUserOrNull()?.id
    fun currentEmail(): String? = client?.auth?.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String) {
        requireNotNull(client) { "Supabase chưa được cấu hình" }.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(fullName: String, email: String, phone: String, password: String, role: String) {
        requireNotNull(client) { "Supabase chưa được cấu hình" }.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
                put("requested_role", if (role == "hr") "hr" else "admin")
            }
        }
    }

    suspend fun signOut() {
        client?.auth?.signOut()
    }

    suspend fun profile(): UserProfileDto {
        val userId = requireNotNull(currentUserId()) { "Chưa đăng nhập" }
        return requireNotNull(client).from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingle()
    }

    suspend fun updateProfile(fullName: String, phone: String, department: String, jobTitle: String) {
        val userId = requireNotNull(currentUserId()) { "Chưa đăng nhập" }
        requireNotNull(client).from("profiles").update(
            ProfileUpdateDto(fullName, phone, department, jobTitle)
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun upsertCandidate(candidate: CandidateDto) {
        requireNotNull(client).from("candidates").upsert(candidate)
    }

    suspend fun fetchCandidates(): List<CandidateDto> =
        requireNotNull(client).from("candidates").select().decodeList()

    @OptIn(SupabaseExperimental::class)
    fun realtimeCandidates(): Flow<List<CandidateDto>> =
        requireNotNull(client).from("candidates").selectAsFlow(CandidateDto::id)

    suspend fun upsertInterview(interview: InterviewDto) {
        requireNotNull(client).from("interviews").upsert(interview)
    }

    suspend fun fetchInterviews(): List<InterviewDto> =
        requireNotNull(client).from("interviews").select().decodeList()

    suspend fun upsertScorecard(scorecard: ScorecardDto) {
        requireNotNull(client).from("scorecards").upsert(scorecard)
    }

    suspend fun fetchScorecards(): List<ScorecardDto> =
        requireNotNull(client).from("scorecards").select().decodeList()

    suspend fun uploadCv(path: String, bytes: ByteArray) {
        requireNotNull(client).storage.from("candidate-cvs").upload(path, bytes) { upsert = true }
    }
}
