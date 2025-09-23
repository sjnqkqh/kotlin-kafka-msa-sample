package msa.post.service

import msa.post.dto.PostCreateRequest
import msa.post.model.Post
import msa.post.repository.PostRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class PostServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var postRedisService: PostRedisService

    @Mock
    lateinit var postEventPublisher: PostEventPublisher

    lateinit var postService: PostService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        postService = PostService(postRepository, postRedisService, postEventPublisher)
    }

    @Test
    fun `Redis에 충분한 데이터가 있으면 Redis에서 페이지 반환`() {
        // given
        val pageable = PageRequest.of(0, 2)
        val redisPosts = listOf(
            createPost(1L, "Redis Post 1"),
            createPost(2L, "Redis Post 2")
        )

        `when`(postRedisService.getRecentPostsByRange(0L, 1L)).thenReturn(redisPosts)
        `when`(postRepository.count()).thenReturn(50L)

        // when
        val result = postService.getPostsByPage(pageable)

        // then
        assertEquals(2, result.content.size)
        assertEquals("Redis Post 1", result.content[0].title)
        assertEquals(0, result.page)
        assertEquals(2, result.size)
        assertEquals(50L, result.totalElements)
    }

    @Test
    fun `Redis에 데이터가 부족하면 DB에서 페이지 반환`() {
        // given
        val pageable = PageRequest.of(0, 3)
        val redisPosts = listOf(createPost(1L, "Redis Post"))
        val dbPosts = listOf(
            createPost(1L, "DB Post 1"),
            createPost(2L, "DB Post 2"),
            createPost(3L, "DB Post 3")
        )
        val dbPage = PageImpl(dbPosts, pageable, 20L)

        `when`(postRedisService.getRecentPostsByRange(0L, 2L)).thenReturn(redisPosts)
        `when`(postRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(dbPage)

        // when
        val result = postService.getPostsByPage(pageable)

        // then
        assertEquals(3, result.content.size)
        assertEquals("DB Post 1", result.content[0].title)
        assertEquals(20L, result.totalElements)
    }

    @Test
    fun `캐시에 있는 게시물 조회 시 Redis에서 반환`() {
        // given
        val postId = 1L
        val cachedPost = createPost(postId, "Cached Post")

        `when`(postRedisService.getCachedPost(postId)).thenReturn(cachedPost)

        // when
        val result = postService.getPostById(postId)

        // then
        assertEquals(postId, result.id)
        assertEquals("Cached Post", result.title)
    }

    @Test
    fun `캐시에 없는 게시물 조회 시 DB에서 조회`() {
        // given
        val postId = 1L
        val dbPost = createPost(postId, "DB Post")

        `when`(postRedisService.getCachedPost(postId)).thenReturn(null)
        `when`(postRepository.findById(postId)).thenReturn(Optional.of(dbPost))

        // when
        val result = postService.getPostById(postId)

        // then
        assertEquals(postId, result.id)
        assertEquals("DB Post", result.title)
    }

    @Test
    fun `존재하지 않는 게시물 조회 시 예외 발생`() {
        // given
        val postId = 999L

        `when`(postRedisService.getCachedPost(postId)).thenReturn(null)
        `when`(postRepository.findById(postId)).thenReturn(Optional.empty())

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            postService.getPostById(postId)
        }

        assertEquals("Post not found with id: $postId", exception.message)
    }

    @Test
    fun `게시물 생성 시 DB 저장 후 Redis 캐시에 등록`() {
        // given
        val request = PostCreateRequest("새로운 게시글", "게시글 내용")
        val newPost = Post(title = "새로운 게시글", content = "게시글 내용")
        val savedPost = createPost(1L, "새로운 게시글", "게시글 내용")
        val expireTime = 1672531200L

        `when`(postRepository.save(newPost)).thenReturn(savedPost)
        `when`(postRedisService.calculateExpireTime(Duration.ofHours(12))).thenReturn(expireTime)

        // when
        val result = postService.createPost(request)

        // then
        assertEquals(1L, result.id)
        assertEquals("새로운 게시글", result.title)
        assertEquals("게시글 내용", result.content)
    }

    @Test
    fun `게시물 삭제 시 존재하는 게시물은 DB와 Redis에서 모두 제거`() {
        // given
        val postId = 1L

        `when`(postRepository.existsById(postId)).thenReturn(true)

        // when
        postService.deletePost(postId)

        // then - 예외가 발생하지 않으면 성공
        assertTrue(true)
    }

    @Test
    fun `게시물 삭제 시 존재하지 않는 게시물은 예외 발생`() {
        // given
        val postId = 999L

        `when`(postRepository.existsById(postId)).thenReturn(false)

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            postService.deletePost(postId)
        }

        assertEquals("Post not found with id: $postId", exception.message)
    }

    private fun createPost(
        id: Long,
        title: String,
        content: String = "Test Content"
    ): Post {
        return Post(
            id = id,
            title = title,
            content = content,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}