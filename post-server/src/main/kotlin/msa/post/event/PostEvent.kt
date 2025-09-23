package msa.post.event

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class PostCreatedEvent(
    val postId: Long,
    val title: String,
    val content: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val eventTime: LocalDateTime = LocalDateTime.now()
)

data class PostDeletedEvent(
    val postId: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val deletedAt: LocalDateTime = LocalDateTime.now(),
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val eventTime: LocalDateTime = LocalDateTime.now()
)