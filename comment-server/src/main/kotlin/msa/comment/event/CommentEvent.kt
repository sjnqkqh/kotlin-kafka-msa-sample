package msa.comment.event

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class CommentCreatedEvent(
    val commentId: Long,
    val postId: Long,
    val author: String,
    val content: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val eventTime: LocalDateTime = LocalDateTime.now()
)

data class CommentUpdatedEvent(
    val commentId: Long,
    val postId: Long,
    val content: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val eventTime: LocalDateTime = LocalDateTime.now()
)

data class CommentDeletedEvent(
    val commentId: Long,
    val postId: Long,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val deletedAt: LocalDateTime = LocalDateTime.now(),
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val eventTime: LocalDateTime = LocalDateTime.now()
)