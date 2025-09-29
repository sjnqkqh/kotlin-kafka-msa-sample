package msa.post.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import java.time.Duration
import java.time.Instant

class PostRedisServiceTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var zSetOperations: ZSetOperations<String, Any>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>

    private lateinit var postRedisService: PostRedisService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        postRedisService = PostRedisService(redisTemplate)
    }

    @Test
    @DisplayName("만료 시간 계산이 정확하다")
    fun shouldCalculateExpireTimeCorrectly() {
        // given
        val duration = Duration.ofHours(12)

        // when
        val expireTime = postRedisService.calculateExpireTime(duration)

        // then
        val expectedTime = Instant.now().plusSeconds(duration.seconds).epochSecond
        // 계산 시간 차이 허용 (5초 이내)
        assertTrue(Math.abs(expireTime - expectedTime) <= 5)
    }

    @Test
    @DisplayName("빈 범위 조회 시 빈 리스트 반환")
    fun shouldReturnEmptyListWhenNoPostsInRange() {
        // given
        whenever(zSetOperations.reverseRange("recent_posts_zset", 0, 2))
            .thenReturn(emptySet())

        // when
        val posts = postRedisService.getRecentPostsByRange(0, 2)

        // then
        assertTrue(posts.isEmpty())
    }

    @Test
    @DisplayName("캐시된 게시물이 없으면 null 반환")
    fun shouldReturnNullWhenPostNotCached() {
        // given
        val postId = 1L
        whenever(valueOperations.get("post:$postId")).thenReturn(null)

        // when
        val cachedPost = postRedisService.getCachedPost(postId)

        // then
        assertNull(cachedPost)
    }
}