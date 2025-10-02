package msa.comment.dto

data class CommentCreateRequest(
    val postId: Long,
    val content: String
)