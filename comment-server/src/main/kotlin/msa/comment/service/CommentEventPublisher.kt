package msa.comment.service

import msa.comment.event.CommentCreatedEvent
import msa.comment.event.CommentDeletedEvent
import msa.comment.event.CommentUpdatedEvent
import msa.comment.model.Comment
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class CommentEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    fun publishCommentCreated(comment: Comment) {
        try {
            val event = CommentCreatedEvent(
                commentId = comment.id!!,
                postId = comment.postId,
                author = comment.author,
                content = comment.content,
                createdAt = comment.createdAt!!
            )
            kafkaTemplate.send("comment.created", event)
        } catch (e: Exception) {
            println("Failed to publish comment created event: ${e.message}")
        }
    }

    fun publishCommentUpdated(comment: Comment) {
        try {
            val event = CommentUpdatedEvent(
                commentId = comment.id!!,
                postId = comment.postId,
                content = comment.content,
                updatedAt = comment.updatedAt!!
            )
            kafkaTemplate.send("comment.updated", event)
        } catch (e: Exception) {
            println("Failed to publish comment updated event: ${e.message}")
        }
    }

    fun publishCommentDeleted(comment: Comment) {
        try {
            val event = CommentDeletedEvent(
                commentId = comment.id!!,
                postId = comment.postId
            )
            kafkaTemplate.send("comment.deleted", event)
        } catch (e: Exception) {
            println("Failed to publish comment deleted event: ${e.message}")
        }
    }
}