package msa.comment.service

import msa.comment.dto.CommentResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CommentRedisService(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    companion object {
        private const val COMMENT_LIST_KEY_PREFIX = "comments:post:"
        private val CACHE_DURATION = Duration.ofHours(12)
    }

    fun cacheCommentList(postId: Long, comments: List<CommentResponse>) {
        val key = "$COMMENT_LIST_KEY_PREFIX$postId"
        try {
            redisTemplate.opsForValue().set(key, comments, CACHE_DURATION)
        } catch (e: Exception) {
            println("Failed to cache comment list for post $postId: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getCachedCommentList(postId: Long): List<CommentResponse>? {
        val key = "$COMMENT_LIST_KEY_PREFIX$postId"
        return try {
            redisTemplate.opsForValue().get(key) as? List<CommentResponse>
        } catch (e: Exception) {
            println("Failed to get cached comment list for post $postId: ${e.message}")
            null
        }
    }

    fun evictCommentListCache(postId: Long) {
        val key = "$COMMENT_LIST_KEY_PREFIX$postId"
        try {
            redisTemplate.delete(key)
        } catch (e: Exception) {
            println("Failed to evict comment list cache for post $postId: ${e.message}")
        }
    }
}