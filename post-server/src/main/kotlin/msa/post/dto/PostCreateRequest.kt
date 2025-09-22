package msa.post.dto

data class PostCreateRequest(
    val title: String,
    val content: String,
    val userId: Long
)
