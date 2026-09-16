package com.clearchain.app.domain.usecase.profile

import com.clearchain.app.domain.repository.OrganizationRepository
import javax.inject.Inject

class UploadVerificationDocumentUseCase @Inject constructor(
    private val repository: OrganizationRepository
) {
    /** @return the stored document URL on success. */
    suspend operator fun invoke(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        // Mirrors the Supabase "documents" bucket's own configured allow-list/size limit
        // (Dashboard -> Storage -> documents) so a rejected file fails fast here instead
        // of round-tripping to the server just to bounce off Supabase's own policy.
        val allowed = setOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
        )
        if (mimeType.lowercase() !in allowed)
            return Result.failure(Exception("Only PDF, Word documents, or images (JPEG, PNG, WebP, HEIC, HEIF) are accepted"))
        if (bytes.isEmpty())
            return Result.failure(Exception("The selected file is empty"))
        if (bytes.size > 10 * 1024 * 1024)
            return Result.failure(Exception("File must be under 10 MB"))

        return repository.uploadVerificationDocument(bytes, fileName, mimeType)
    }
}
