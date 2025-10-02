package msa.comment.service

import msa.comment.config.TestConfig
import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentUpdateRequest
import msa.comment.repository.CommentRepository
import msa.common.auth.UserContext
import msa.common.auth.UserContextProvider
import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test"])
@Import(TestConfig::class)
@Transactional
@DisplayName("댓글 서비스 테스트")
class CommentServiceTest {

    @Autowired
    private lateinit var commentService: CommentService

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var userContextProvider: UserContextProvider

    private val defaultUser = UserContext(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        userType = "NORMAL"
    )

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        // 각 테스트 전에 기본 사용자로 리셋
        whenever(userContextProvider.getCurrentUser()).thenReturn(defaultUser)
        whenever(userContextProvider.getCurrentUserOrNull()).thenReturn(defaultUser)
    }

    @Test
    @DisplayName("댓글 생성 성공")
    fun shouldCreateComment() {
        // Given
        val request = CommentCreateRequest(
            postId = 1L,
            content = "댓글 내용"
        )

        // When
        val response = commentService.createComment(request)

        // Then
        assertEquals(1L, response.postId)
        assertEquals("댓글 내용", response.content)
    }

    @Test
    @DisplayName("댓글 수정 성공")
    fun shouldUpdateComment() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "원본"
        )
        val createdComment = commentService.createComment(createRequest)

        val updateRequest = CommentUpdateRequest(
            content = "수정됨"
        )

        // When
        val response = commentService.updateComment(createdComment.id, updateRequest)

        // Then
        assertEquals("수정됨", response.content)
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 시도 시 실패")
    fun shouldFailUpdateWithDifferentUser() {
        // Given - 첫 번째 사용자로 댓글 생성
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "원본"
        )
        val createdComment = commentService.createComment(createRequest)

        // Mock을 다른 사용자로 변경
        whenever(userContextProvider.getCurrentUser()).thenReturn(
            UserContext(
                id = 2L, // 다른 사용자 ID
                email = "other@example.com",
                name = "Other User",
                userType = "NORMAL"
            )
        )

        val updateRequest = CommentUpdateRequest(
            content = "수정됨"
        )

        // When & Then
        val exception = assertThrows(CustomException::class.java) {
            commentService.updateComment(createdComment.id, updateRequest)
        }
        assertEquals(ErrorCode.COMMENT_ACCESS_DENIED, exception.errorCode)
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    fun shouldDeleteComment() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "삭제대상"
        )
        val createdComment = commentService.createComment(createRequest)

        // When
        commentService.deleteComment(createdComment.id)

        // Then
        val exists = commentRepository.existsById(createdComment.id)
        assertEquals(false, exists)
    }

    @Test
    @DisplayName("다른 사용자가 댓글 삭제 시도 시 실패")
    fun shouldFailDeleteWithDifferentUser() {
        // Given - 첫 번째 사용자로 댓글 생성
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "삭제대상"
        )
        val createdComment = commentService.createComment(createRequest)

        // Mock을 다른 사용자로 변경
        whenever(userContextProvider.getCurrentUser()).thenReturn(
            UserContext(
                id = 2L, // 다른 사용자 ID
                email = "other@example.com",
                name = "Other User",
                userType = "NORMAL"
            )
        )

        // When & Then
        val exception = assertThrows(CustomException::class.java) {
            commentService.deleteComment(createdComment.id)
        }
        assertEquals(ErrorCode.COMMENT_ACCESS_DENIED, exception.errorCode)
    }

    @Test
    @DisplayName("포스트별 댓글 조회 성공")
    fun shouldGetCommentsByPostId() {
        // Given
        val postId = 1L
        commentService.createComment(
            CommentCreateRequest(postId, "첫번째")
        )
        commentService.createComment(
            CommentCreateRequest(postId, "두번째")
        )

        // When
        val comments = commentService.getCommentsByPostId(postId)

        // Then
        assertEquals(2, comments.size)
        assertEquals("두번째", comments[0].content) // 최신순
        assertEquals("첫번째", comments[1].content)
    }
}