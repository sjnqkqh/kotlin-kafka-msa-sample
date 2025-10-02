package msa.user.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TokenRedisService(
    private val redisTemplate: RedisTemplate<String, Any>,
    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long
) {
    private val TOKEN_PREFIX = "jwt:token:"

    fun storeToken(userId: Long, token: String) {
        val key = generateTokenKey(userId)
        redisTemplate.opsForValue().set(key, token, jwtExpiration, TimeUnit.MILLISECONDS)
    }

    fun isTokenValid(userId: Long, token: String): Boolean {
        val key = generateTokenKey(userId)
        val storedToken = redisTemplate.opsForValue().get(key) as? String
        return storedToken == token
    }

    fun removeToken(userId: Long) {
        val key = generateTokenKey(userId)
        redisTemplate.delete(key)
    }

    private fun generateTokenKey(userId: Long): String {
        return TOKEN_PREFIX + userId
    }
}