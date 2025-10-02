package msa.common.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenValidator(
    @Value("\${jwt.secret}")
    private val secret: String,
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())
    private val TOKEN_PREFIX = "jwt:token:"

    fun validateTokenAndGetUser(token: String): UserContext {
        try {
            val claims = extractAllClaims(token)

            // 토큰 만료 검증
            if (claims.expiration.before(Date())) {
                throw CustomException(ErrorCode.EXPIRED_TOKEN)
            }

            val userId = claims["id"] as Long

            // Redis에서 토큰 유효성 검증
            val key = generateTokenKey(userId)
            val storedToken = redisTemplate.opsForValue().get(key) as? String
            if (storedToken != token) {
                throw CustomException(ErrorCode.INVALID_TOKEN)
            }

            return UserContext(
                id = userId,
                email = claims["email"] as String,
                name = claims["name"] as String,
                userType = claims["userType"] as String
            )
        } catch (e: CustomException) {
            throw e
        } catch (e: Exception) {
            throw CustomException(ErrorCode.INVALID_TOKEN)
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun generateTokenKey(userId: Long): String {
        return TOKEN_PREFIX + userId
    }
}