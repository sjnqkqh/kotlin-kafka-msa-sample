package msa.comment.dto

data class CommentCreateRequest(
    val postId: Long,
    val author: String,
    val password: String,
    val content: String
)