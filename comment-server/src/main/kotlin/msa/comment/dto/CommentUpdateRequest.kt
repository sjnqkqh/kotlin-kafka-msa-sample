package msa.comment.dto

data class CommentUpdateRequest(
    val password: String,
    val content: String
)