package msa.post.service

import msa.post.model.Post
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class PostRedisService(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val listOps = redisTemplate.opsForList()
    private val recentPostSize = 100L

    fun pushRedisRecentPostList(post: Post) {
        listOps.leftPush("recentPostList", post)
        listOps.trim("recentPostList", 0, recentPostSize - 1)
    }

    fun getRecentPostListByRange(start: Long, end: Long): List<Post> {
        return listOps.range("recentPostList", start, end)?.map { it as Post } ?: emptyList()
    }

}