package msa.post.service

import msa.post.config.TestContainerConfig
import msa.post.config.TestRedisConfig
import msa.post.model.Post
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant

@SpringBootTest(
    classes = [TestRedisConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
class PostRedisServiceTest : TestContainerConfig() {

    @Autowired
    private lateinit var postRedisService: PostRedisService

    @Autowired
    private lateinit var testRedisTemplate: RedisTemplate<String, Any>

    @BeforeEach
    fun setUp() {
        testRedisTemplate.delete("recent_posts_zset")
    }

    @Test
    @DisplayName("최근 게시물 목록에 새 게시물 추가")
    fun shouldAddPostToRedisListWhenPushingToRecentPostList() {
        // given
        val post = Post(
            id = 1L,
            title = "테스트 포스트",
            content = "테스트 내용"
        )

        // when
        val expireTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(post, expireTime)

        // then
        val posts = postRedisService.getRecentPostsByRange(0, 0)
        assertEquals(1, posts.size)
        assertEquals("테스트 포스트", posts[0].title)
        assertEquals("테스트 내용", posts[0].content)
    }

    @Test
    @DisplayName("게시물 LIFO 순서로 추가")
    fun shouldAddPostsInLifoOrderWhenPushing() {
        // given
        val post1 = Post(id = 1L, title = "첫 번째", content = "내용1")
        val post2 = Post(id = 2L, title = "두 번째", content = "내용2")
        val post3 = Post(id = 3L, title = "세 번째", content = "내용3")

        // when
        val expireTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(post1, expireTime)
        postRedisService.addToRecentPosts(post2, expireTime)
        postRedisService.addToRecentPosts(post3, expireTime)

        // then
        val posts = postRedisService.getRecentPostsByRange(0, 2)
        assertEquals(3, posts.size)
        assertEquals("세 번째", posts[0].title) // 가장 최근 추가
        assertEquals("두 번째", posts[1].title)
        assertEquals("첫 번째", posts[2].title) // 가장 먼저 추가
    }

    @Test
    @DisplayName("지정된 범위의 게시물 목록 반환")
    fun shouldReturnCorrectRangeOfPostsWhenRequested() {
        // given
        repeat(10) { index ->
            val post = Post(
                id = index.toLong(),
                title = "포스트 $index",
                content = "내용 $index"
            )
            val expireTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(post, expireTime)
        }

        // when
        val firstThree = postRedisService.getRecentPostsByRange(0, 2)
        val middleTwo = postRedisService.getRecentPostsByRange(3, 4)

        // then
        assertEquals(3, firstThree.size)
        assertEquals("포스트 9", firstThree[0].title)
        assertEquals("포스트 8", firstThree[1].title)
        assertEquals("포스트 7", firstThree[2].title)

        assertEquals(2, middleTwo.size)
        assertEquals("포스트 6", middleTwo[0].title)
        assertEquals("포스트 5", middleTwo[1].title)
    }

    @Test
    @DisplayName("게시물이 없을 때 빈 목록을 반환한다")
    fun shouldReturnEmptyListWhenNoPostsExist() {
        // when
        val posts = postRedisService.getRecentPostsByRange(0, 10)

        // then
        assertTrue(posts.isEmpty())
    }

    @Test
    @DisplayName("범위를 벗어난 인덱스 처리")
    fun shouldHandleOutOfRangeIndicesGracefully() {
        // given
        val post = Post(id = 1L, title = "단일 포스트", content = "내용")
        val expireTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(post, expireTime)

        // when
        val posts = postRedisService.getRecentPostsByRange(5, 10)

        // then
        assertTrue(posts.isEmpty())
    }

    @Test
    @DisplayName("과거 시간으로 설정된 게시물이 자동으로 정리된다")
    fun shouldCleanupPostsWithPastExpireTime() {
        // given
        val expiredPost = Post(
            id = 1L,
            title = "만료된 포스트",
            content = "이 포스트는 만료되어야 함"
        )

        // 1시간 전 시간으로 만료 시간 설정
        val pastExpireTime = Instant.now().minusSeconds(3600).epochSecond
        postRedisService.addToRecentPosts(expiredPost, pastExpireTime)

        // when: 조회 시 내부적으로 cleanupExpiredPosts() 호출됨
        val posts = postRedisService.getRecentPostsByRange(0, 10)

        // then: 만료된 게시물이 정리되어 빈 리스트 반환
        assertTrue(posts.isEmpty())
    }

    @Test
    @DisplayName("만료된 게시물과 유효한 게시물이 올바르게 분리된다")
    fun shouldSeparateExpiredAndValidPosts() {
        // given
        val expiredPost = Post(id = 1L, title = "만료된 포스트", content = "만료됨")
        val validPost = Post(id = 2L, title = "유효한 포스트", content = "유효함")

        // 만료된 게시물: 1시간 전 시간으로 설정
        val expiredTime = Instant.now().minusSeconds(3600).epochSecond
        postRedisService.addToRecentPosts(expiredPost, expiredTime)

        // 유효한 게시물: 12시간 후 만료
        val validTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(validPost, validTime)

        // when: 조회
        val posts = postRedisService.getRecentPostsByRange(0, 10)

        // then: 유효한 게시물만 반환
        assertEquals(1, posts.size)
        assertEquals("유효한 포스트", posts[0].title)
        assertEquals(2L, posts[0].id)
    }

    @Test
    @DisplayName("여러 만료된 게시물 중 유효한 게시물만 조회된다")
    fun shouldReturnOnlyValidPostsFromMixedExpiredPosts() {
        // given
        val expiredPost1 = Post(id = 1L, title = "만료1", content = "내용1")
        val expiredPost2 = Post(id = 2L, title = "만료2", content = "내용2")
        val validPost1 = Post(id = 3L, title = "유효1", content = "내용3")
        val validPost2 = Post(id = 4L, title = "유효2", content = "내용4")

        // 만료된 게시물들 (2시간 전)
        val expiredTime = Instant.now().minusSeconds(7200).epochSecond
        postRedisService.addToRecentPosts(expiredPost1, expiredTime)
        postRedisService.addToRecentPosts(expiredPost2, expiredTime)

        // 유효한 게시물들 (12시간 후 만료)
        val validTime = postRedisService.calculateExpireTime(Duration.ofHours(12))
        postRedisService.addToRecentPosts(validPost1, validTime)
        postRedisService.addToRecentPosts(validPost2, validTime)

        // when
        val posts = postRedisService.getRecentPostsByRange(0, 10)

        // then: 유효한 게시물 2개만 반환
        assertEquals(2, posts.size)
        val titles = posts.map { it.title }.toSet()
        assertTrue(titles.contains("유효1"))
        assertTrue(titles.contains("유효2"))
        assertFalse(titles.contains("만료1"))
        assertFalse(titles.contains("만료2"))
    }

    @Test
    @DisplayName("정확히 만료 시점의 게시물 처리 확인")
    fun shouldHandlePostsAtExactExpirationTime() {
        // given
        val post = Post(id = 1L, title = "경계 테스트", content = "만료 시점 테스트")

        // 현재 시간으로 만료 시간 설정 (이미 만료됨)
        val exactExpireTime = Instant.now().epochSecond
        postRedisService.addToRecentPosts(post, exactExpireTime)

        // when: 조회
        val posts = postRedisService.getRecentPostsByRange(0, 10)

        // then: 만료된 게시물은 조회되지 않음
        assertTrue(posts.isEmpty())
    }
}