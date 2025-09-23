package msa.post.service

import msa.post.model.Post
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class PostRedisService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val zSetOps = redisTemplate.opsForZSet()
    private val valueOps = redisTemplate.opsForValue()
    private val recentPostsKey = "recent_posts_zset"
    private val postCacheKeyPrefix = "post:"

    fun calculateExpireTime(duration: Duration): Long {
        return Instant.now().plus(duration).epochSecond
    }

    fun addToRecentPosts(post: Post, expireEpochSecond: Long) {
        cleanupExpiredPosts()
        zSetOps.add(recentPostsKey, post, expireEpochSecond.toDouble())
    }

    private fun cleanupExpiredPosts() {
        val currentTime = Instant.now().epochSecond
        zSetOps.removeRangeByScore(recentPostsKey, Double.NEGATIVE_INFINITY, currentTime.toDouble())
    }

    fun getRecentPostsByRange(start: Long, end: Long): List<Post> {
        cleanupExpiredPosts()
        return zSetOps.reverseRange(recentPostsKey, start, end)?.map { it as Post } ?: emptyList()
    }


    fun cachePost(post: Post) {
        val key = postCacheKeyPrefix + post.id
        valueOps.set(key, post)
    }

    fun getCachedPost(id: Long): Post? {
        val key = postCacheKeyPrefix + id
        return valueOps.get(key) as? Post
    }

    fun removeCachedPost(id: Long) {
        val key = postCacheKeyPrefix + id
        redisTemplate.delete(key)
    }

    fun removeFromRecentPosts(id: Long) {
        val posts = zSetOps.range(recentPostsKey, 0, -1)?.map { it as Post } ?: emptyList()
        val postToRemove = posts.find { it.id == id }
        postToRemove?.let {
            zSetOps.remove(recentPostsKey, it)
        }
    }

}