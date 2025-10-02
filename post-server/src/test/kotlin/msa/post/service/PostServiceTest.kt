package msa.post.service

import msa.common.auth.UserContext
import msa.common.auth.UserContextProvider
import msa.common.dto.PageResponse
import msa.common.exception.CustomException
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.model.Post
import msa.post.repository.PostRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@DisplayName("게시물 서비스 테스트")
class PostServiceTest {

    @Mock
    lateinit var postRepository: PostRepository

    @Mock
    lateinit var postRedisService: PostRedisService

    @Mock
    lateinit var postEventPublisher: PostEventPublisher

    @Mock
    lateinit var userContextProvider: UserContextProvider

    lateinit var postService: PostService

    private val testUser = UserContext(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        userType = "NORMAL"
    )

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        postService = PostService(postRepository, postRedisService, postEventPublisher, userContextProvider)

        // 기본 Mock 설정
        whenever(userContextProvider.getCurrentUser()).thenReturn(testUser)
        whenever(userContextProvider.getCurrentUserOrNull()).thenReturn(testUser)
    }

    @Test
    @DisplayName("Redis에 충분한 데이터가 있으면 Redis에서 페이지 반환")
    fun shouldReturnPageFromRedisWhenSufficientDataExists() {
        // given
        val pageable = PageRequest.of(0, 2)
        val redisPosts = listOf(
            createPost(1L, "Redis Post 1"),
            createPost(2L, "Redis Post 2")
        )

        whenever(postRedisService.getRecentPostsByRange(0L, 1L)).thenReturn(redisPosts)
        whenever(postRepository.count()).thenReturn(50L)

        // when
        val result: PageResponse<PostResponse> = postService.getPostsByPage(pageable)

        // then
        assertEquals(2, result.content.size)
        assertEquals("Redis Post 1", result.content[0].title)
        assertEquals(0, result.page)
        assertEquals(2, result.size)
        assertEquals(50L, result.totalElements)
    }

    @Test
    @DisplayName("Redis에 데이터가 부족하면 DB에서 페이지 반환")
    fun shouldReturnPageFromDatabaseWhenRedisDataInsufficient() {
        // given
        val pageable = PageRequest.of(0, 3)
        val redisPosts = listOf(createPost(1L, "Redis Post"))
        val dbPosts = listOf(
            createPost(1L, "DB Post 1"),
            createPost(2L, "DB Post 2"),
            createPost(3L, "DB Post 3")
        )
        val dbPage = PageImpl(dbPosts, pageable, 20L)

        whenever(postRedisService.getRecentPostsByRange(0L, 2L)).thenReturn(redisPosts)
        whenever(postRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(dbPage)

        // when
        val result: PageResponse<PostResponse> = postService.getPostsByPage(pageable)

        // then
        assertEquals(3, result.content.size)
        assertEquals("DB Post 1", result.content[0].title)
        assertEquals(20L, result.totalElements)
    }

    @Test
    @DisplayName("캐시에 있는 게시물 조회 시 Redis에서 반환")
    fun shouldReturnPostFromCacheWhenPostExistsInCache() {
        // given
        val postId = 1L
        val cachedPost = createPost(postId, "Cached Post")

        whenever(postRedisService.getCachedPost(postId)).thenReturn(cachedPost)

        // when
        val result: PostResponse = postService.getPostById(postId)

        // then
        assertEquals(postId, result.id)
        assertEquals("Cached Post", result.title)
    }

    @Test
    @DisplayName("캐시에 없는 게시물 조회 시 DB에서 조회")
    fun shouldReturnPostFromDatabaseWhenNotInCache() {
        // given
        val postId = 1L
        val dbPost = createPost(postId, "DB Post")

        whenever(postRedisService.getCachedPost(postId)).thenReturn(null)
        whenever(postRepository.findById(postId)).thenReturn(Optional.of(dbPost))

        // when
        val result: PostResponse = postService.getPostById(postId)

        // then
        assertEquals(postId, result.id)
        assertEquals("DB Post", result.title)
    }

    @Test
    @DisplayName("존재하지 않는 게시물 조회 시 예외 발생")
    fun shouldThrowExceptionWhenPostNotFound() {
        // given
        val postId = 999L

        whenever(postRedisService.getCachedPost(postId)).thenReturn(null)
        whenever(postRepository.findById(postId)).thenReturn(Optional.empty())

        // when & then
        assertThrows(CustomException::class.java) {
            postService.getPostById(postId)
        }
    }

    @Test
    @DisplayName("게시물 생성 성공")
    fun shouldCreatePost() {
        // given
        val request = PostCreateRequest(
            title = "Test Title",
            content = "Test Content"
        )
        val savedPost = createPost(1L, "Test Title", "Test Content", userId = testUser.id, authorName = testUser.name)

        whenever(postRepository.save(any<Post>())).thenReturn(savedPost)
        whenever(postRedisService.calculateExpireTime(any())).thenReturn(3600L)
        doNothing().whenever(postRedisService).addToRecentPosts(any<Post>(), any<Long>())
        doNothing().whenever(postRedisService).cachePost(any<Post>())
        doNothing().whenever(postEventPublisher).publishPostCreated(any<Post>())

        // when
        val result = postService.createPost(request)

        // then
        assertEquals("Test Title", result.title)
        assertEquals("Test Content", result.content)
        assertEquals(testUser.name, result.authorName)
    }

    @Test
    @DisplayName("다른 사용자가 게시물 삭제 시도 시 실패")
    fun shouldFailDeleteWithDifferentUser() {
        // given
        val postId = 1L
        val post = createPost(postId, "Test Post", userId = 1L)

        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))

        // Mock을 다른 사용자로 변경
        whenever(userContextProvider.getCurrentUser()).thenReturn(
            UserContext(
                id = 2L,
                email = "other@example.com",
                name = "Other User",
                userType = "NORMAL"
            )
        )

        // when & then
        assertThrows(CustomException::class.java) {
            postService.deletePost(postId)
        }
    }

    @Test
    @DisplayName("게시물 삭제 성공")
    fun shouldDeletePost() {
        // given
        val postId = 1L
        val post = createPost(postId, "Test Post", userId = 1L)

        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))

        // when
        postService.deletePost(postId)

        // then - verify 호출을 확인할 수 있음
        // 실제 삭제는 통합 테스트에서 검증
    }

    private fun createPost(
        id: Long,
        title: String,
        content: String = "Test Content",
        userId: Long = 1L,
        authorName: String = "Test User"
    ): Post {
        return Post(
            id = id,
            title = title,
            content = content,
            userId = userId,
            authorName = authorName,
            lastCommentAppendedAt = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}