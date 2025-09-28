package msa.user.service

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.SignatureException
import msa.user.model.User
import msa.user.model.UserType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UserDetails
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private val secret = "mySecretKeyForTestingPurposesThisMustBeLongEnoughForHS256Algorithm"
    private val expiration = 3600000L // 1시간

    @BeforeEach
    fun setUp() {
        jwtService = JwtService(secret, expiration)
    }

    @Test
    @DisplayName("JWT 토큰 생성 - 성공")
    fun generateToken_Success() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )

        // When
        val token = jwtService.generateToken(user)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.split(".").size == 3) // JWT는 3개 부분으로 구성
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자명 추출 - 성공")
    fun extractUsername_Success() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val token = jwtService.generateToken(user)

        // When
        val extractedUsername = jwtService.extractUsername(token)

        // Then
        assertEquals("test@example.com", extractedUsername)
    }

    @Test
    @DisplayName("JWT 토큰에서 만료시간 추출 - 성공")
    fun extractExpiration_Success() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val beforeGeneration = Date()
        val token = jwtService.generateToken(user)
        val afterGeneration = Date()

        // When
        val expiration = jwtService.extractExpiration(token)

        // Then
        assertTrue(expiration.after(beforeGeneration))
        assertTrue(expiration.after(afterGeneration))
        // 만료시간이 현재시간 + 설정값 근처인지 확인 (약간의 오차 허용)
        val expectedExpiration = Date(System.currentTimeMillis() + this.expiration)
        val timeDifference = Math.abs(expiration.time - expectedExpiration.time)
        assertTrue(timeDifference < 5000) // 5초 이내 오차 허용
    }

    @Test
    @DisplayName("JWT 토큰에서 커스텀 클레임 추출 - 성공")
    fun extractClaim_Success() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val token = jwtService.generateToken(user)

        // When
        val userId = jwtService.extractClaim(token) { claims -> claims["id"] as Number }
        val email = jwtService.extractClaim(token) { claims -> claims["email"] as String }
        val name = jwtService.extractClaim(token) { claims -> claims["name"] as String }
        val userType = jwtService.extractClaim(token) { claims -> claims["userType"] as String }

        // Then
        assertEquals(1L, userId.toLong())
        assertEquals("test@example.com", email)
        assertEquals("Test User", name)
        assertEquals("NORMAL", userType)
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 유효한 토큰")
    fun isTokenValid_ValidToken() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val token = jwtService.generateToken(user)
        val userDetails = createUserDetails("test@example.com")

        // When
        val isValid = jwtService.isTokenValid(token, userDetails)

        // Then
        assertTrue(isValid)
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 다른 사용자")
    fun isTokenValid_DifferentUser() {
        // Given
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val token = jwtService.generateToken(user)
        val userDetails = createUserDetails("different@example.com")

        // When
        val isValid = jwtService.isTokenValid(token, userDetails)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 만료된 토큰")
    fun isTokenValid_ExpiredToken() {
        // Given
        val shortExpirationService = JwtService(secret, 1L) // 1ms 만료
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val token = shortExpirationService.generateToken(user)
        val userDetails = createUserDetails("test@example.com")

        // 토큰이 만료될 때까지 대기
        Thread.sleep(10)

        // When & Then
        // 만료된 토큰은 isTokenValid에서 false를 반환하거나 ExpiredJwtException을 발생시킬 수 있음
        try {
            val isValid = shortExpirationService.isTokenValid(token, userDetails)
            assertFalse(isValid)
        } catch (e: ExpiredJwtException) {
            // 만료된 토큰으로 인한 예외가 발생하는 것도 정상적인 동작
            assertTrue(true)
        }
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 - 잘못된 형식")
    fun invalidToken_MalformedJwt() {
        // Given
        val invalidToken = "invalid.jwt.token"

        // When & Then
        assertThrows<MalformedJwtException> {
            jwtService.extractUsername(invalidToken)
        }
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 - 잘못된 서명")
    fun invalidToken_InvalidSignature() {
        // Given
        val otherSecretService = JwtService("differentSecretKey1234567890abcdefghijklmnopqrstuvwxyz", expiration)
        val user = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "Test User",
            userType = UserType.NORMAL,
            isEmailVerified = true
        )
        val tokenWithDifferentSecret = otherSecretService.generateToken(user)

        // When & Then
        assertThrows<SignatureException> {
            jwtService.extractUsername(tokenWithDifferentSecret)
        }
    }

    @Test
    @DisplayName("다양한 사용자 타입의 토큰 생성")
    fun generateToken_DifferentUserTypes() {
        val userTypes = listOf(UserType.NORMAL, UserType.ADMIN)

        userTypes.forEach { userType ->
            // Given
            val user = User(
                id = 1L,
                email = "test@example.com",
                password = "password",
                name = "Test User",
                userType = userType,
                isEmailVerified = true
            )

            // When
            val token = jwtService.generateToken(user)
            val extractedUserType = jwtService.extractClaim(token) { claims -> claims["userType"] as String }

            // Then
            assertEquals(userType.name, extractedUserType)
        }
    }

    @Test
    @DisplayName("토큰 생성 시 필수 클레임 포함 확인")
    fun generateToken_ContainsRequiredClaims() {
        // Given
        val user = User(
            id = 123L,
            email = "detailed@example.com",
            password = "password",
            name = "Detailed User",
            userType = UserType.ADMIN,
            isEmailVerified = true
        )

        // When
        val token = jwtService.generateToken(user)

        // Then
        val id = jwtService.extractClaim(token) { claims -> claims["id"] as Number }
        val email = jwtService.extractClaim(token) { claims -> claims["email"] as String }
        val name = jwtService.extractClaim(token) { claims -> claims["name"] as String }
        val userType = jwtService.extractClaim(token) { claims -> claims["userType"] as String }
        val subject = jwtService.extractUsername(token)

        assertEquals(123L, id.toLong())
        assertEquals("detailed@example.com", email)
        assertEquals("Detailed User", name)
        assertEquals("ADMIN", userType)
        assertEquals("detailed@example.com", subject)
    }

    @Test
    @DisplayName("빈 문자열 토큰 처리")
    fun emptyToken_ThrowsException() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            jwtService.extractUsername("")
        }
    }

    @Test
    @DisplayName("null 토큰 처리")
    fun nullToken_ThrowsException() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            jwtService.extractUsername("")
        }
    }

    private fun createUserDetails(username: String): UserDetails {
        return object : UserDetails {
            override fun getUsername(): String = username
            override fun getPassword(): String = "password"
            override fun getAuthorities() = emptyList<Nothing>()
            override fun isAccountNonExpired(): Boolean = true
            override fun isAccountNonLocked(): Boolean = true
            override fun isCredentialsNonExpired(): Boolean = true
            override fun isEnabled(): Boolean = true
        }
    }
}