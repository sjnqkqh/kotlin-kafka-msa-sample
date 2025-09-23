package msa.post.service

import msa.post.event.PostCreatedEvent
import msa.post.event.PostDeletedEvent
import msa.post.model.Post
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class PostEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(PostEventPublisher::class.java)

    companion object {
        private const val POST_CREATED_TOPIC = "post.created"
        private const val POST_DELETED_TOPIC = "post.deleted"
    }

    fun publishPostCreated(post: Post) {
        try {
            val event = PostCreatedEvent(
                postId = post.id!!,
                title = post.title,
                content = post.content,
                createdAt = post.createdAt!!
            )

            kafkaTemplate.send(POST_CREATED_TOPIC, post.id.toString(), event)
            logger.info("Published PostCreatedEvent for postId: ${post.id}")
        } catch (e: Exception) {
            logger.error("Failed to publish PostCreatedEvent for postId: ${post.id}", e)
        }
    }

    fun publishPostDeleted(postId: Long) {
        try {
            val event = PostDeletedEvent(postId = postId)

            kafkaTemplate.send(POST_DELETED_TOPIC, postId.toString(), event)
            logger.info("Published PostDeletedEvent for postId: $postId")
        } catch (e: Exception) {
            logger.error("Failed to publish PostDeletedEvent for postId: $postId", e)
        }
    }
}