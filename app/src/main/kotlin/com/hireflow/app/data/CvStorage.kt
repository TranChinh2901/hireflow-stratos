package com.hireflow.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

object CvStorage {
    private const val DIR = "candidate_cvs"

    fun cvDir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun cvFile(context: Context, remoteId: String): File =
        File(cvDir(context), "$remoteId.pdf")

    private fun legacyCacheFile(context: Context, remoteId: String): File =
        File(File(context.cacheDir, DIR), "$remoteId.pdf")

    fun providerUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Lấy tên file gốc từ SAF Uri (Drive/Downloads) để hiển thị đúng tên user đã chọn. */
    fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1 && cursor.moveToFirst()) cursor.getString(index) else null
        }?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Copy file thật từ SAF Uri vào bộ nhớ riêng của app, trả về File nội bộ. */
    fun saveFromUri(context: Context, remoteId: String, sourceUri: Uri): File {
        val dest = cvFile(context, remoteId)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Không thể đọc file CV")
        require(dest.length() > 0) { "File CV rỗng" }
        return dest
    }

    fun saveBytes(context: Context, remoteId: String, bytes: ByteArray): File {
        require(bytes.isNotEmpty()) { "File CV rỗng" }
        val dest = cvFile(context, remoteId)
        dest.writeBytes(bytes)
        return dest
    }

    /** Xóa file CV local đã lưu trong app (dùng khi xóa ứng viên). */
    fun deleteLocalFile(context: Context, remoteId: String) {
        runCatching { cvFile(context, remoteId).takeIf { it.exists() }?.delete() }
        runCatching { File(File(context.cacheDir, DIR), "$remoteId.pdf").takeIf { it.exists() }?.delete() }
    }

    /**
     * Tìm file CV local đã lưu (ưu tiên filesDir, fallback cache cũ để migrate).
     * Nếu migrate từ cache sang filesDir thành công thì trả về file mới.
     */
    fun findLocalFile(context: Context, remoteId: String): File? {
        val primary = cvFile(context, remoteId)
        if (primary.exists() && primary.length() > 0) return primary
        val legacy = legacyCacheFile(context, remoteId)
        if (legacy.exists() && legacy.length() > 0) {
            runCatching {
                legacy.copyTo(primary, overwrite = true)
                return primary.takeIf { it.exists() && it.length() > 0 }
            }
        }
        return null
    }
}
