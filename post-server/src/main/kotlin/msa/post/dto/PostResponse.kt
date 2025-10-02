package msa.post.dto

import msa.post.model.Post
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val userId: Long,
    val authorName: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromPost(post: Post): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                userId = post.userId,
                authorName = post.authorName,
                createdAt = post.createdAt!!,
                updatedAt = post.updatedAt!!
            )
        }
    }
}
