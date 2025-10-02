package msa.comment.dto

import msa.comment.model.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val authorName: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                postId = comment.postId,
                userId = comment.userId,
                authorName = comment.authorName,
                content = comment.content,
                createdAt = comment.createdAt!!,
                updatedAt = comment.updatedAt!!
            )
        }
    }
}