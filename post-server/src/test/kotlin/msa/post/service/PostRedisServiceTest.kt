package msa.post.service

import msa.post.config.RedisTestContainer
import msa.post.config.TestRedisConfig
import msa.post.model.Post
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [TestRedisConfig::class, PostRedisService::class])
@Testcontainers
class PostRedisServiceTest : RedisTestContainer() {

    @Autowired
    private lateinit var postRedisService: PostRedisService

    @Autowired
    private lateinit var testRedisTemplate: RedisTemplate<String, Any>

    @BeforeEach
    fun setUp() {
        testRedisTemplate.delete("recentPostList")
    }

    @Test
    fun `pushRedisRecentPostList should add post to redis list`() {
        // given
        val post = Post(
            id = 1L,
            title = "테스트 포스트",
            content = "테스트 내용"
        )

        // when
        postRedisService.pushRedisRecentPostList(post)

        // then
        val posts = postRedisService.getRecentPostListByRange(0, 0)
        assertEquals(1, posts.size)
        assertEquals("테스트 포스트", posts[0].title)
        assertEquals("테스트 내용", posts[0].content)
    }

    @Test
    fun `pushRedisRecentPostList should maintain list size limit`() {
        // given
        repeat(105) { index ->
            val post = Post(
                id = index.toLong(),
                title = "포스트 $index",
                content = "내용 $index"
            )
            postRedisService.pushRedisRecentPostList(post)
        }

        // when
        val allPosts = postRedisService.getRecentPostListByRange(0, -1)

        // then
        assertEquals(100, allPosts.size)
        assertEquals("포스트 104", allPosts[0].title) // 가장 최근 추가된 포스트가 첫 번째
        assertEquals("포스트 5", allPosts[99].title) // 100번째 포스트
    }

    @Test
    fun `pushRedisRecentPostList should add posts in LIFO order`() {
        // given
        val post1 = Post(id = 1L, title = "첫 번째", content = "내용1")
        val post2 = Post(id = 2L, title = "두 번째", content = "내용2")
        val post3 = Post(id = 3L, title = "세 번째", content = "내용3")

        // when
        postRedisService.pushRedisRecentPostList(post1)
        postRedisService.pushRedisRecentPostList(post2)
        postRedisService.pushRedisRecentPostList(post3)

        // then
        val posts = postRedisService.getRecentPostListByRange(0, 2)
        assertEquals(3, posts.size)
        assertEquals("세 번째", posts[0].title) // 가장 최근 추가
        assertEquals("두 번째", posts[1].title)
        assertEquals("첫 번째", posts[2].title) // 가장 먼저 추가
    }

    @Test
    fun `getRecentPostListByRange should return correct range of posts`() {
        // given
        repeat(10) { index ->
            val post = Post(
                id = index.toLong(),
                title = "포스트 $index",
                content = "내용 $index"
            )
            postRedisService.pushRedisRecentPostList(post)
        }

        // when
        val firstThree = postRedisService.getRecentPostListByRange(0, 2)
        val middleTwo = postRedisService.getRecentPostListByRange(3, 4)

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
    fun `getRecentPostListByRange should return empty list when no posts exist`() {
        // when
        val posts = postRedisService.getRecentPostListByRange(0, 10)

        // then
        assertTrue(posts.isEmpty())
    }

    @Test
    fun `getRecentPostListByRange should handle out of range indices`() {
        // given
        val post = Post(id = 1L, title = "단일 포스트", content = "내용")
        postRedisService.pushRedisRecentPostList(post)

        // when
        val posts = postRedisService.getRecentPostListByRange(5, 10)

        // then
        assertTrue(posts.isEmpty())
    }
}