package msa.comment.service

import msa.comment.config.TestContainerConfig
import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentUpdateRequest
import msa.comment.repository.CommentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test"])
@Transactional
@DisplayName("댓글 서비스 테스트")
class CommentServiceTest :TestContainerConfig(){

    @Autowired
    private lateinit var commentService: CommentService

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Test
    @DisplayName("댓글 생성 성공")
    fun shouldCreateComment() {
        // Given
        val request = CommentCreateRequest(
            postId = 1L,
            author = "작성자",
            password = "1234",
            content = "댓글 내용"
        )

        // When
        val response = commentService.createComment(request)

        // Then
        assertEquals(1L, response.postId)
        assertEquals("작성자", response.author)
        assertEquals("댓글 내용", response.content)
    }

    @Test
    @DisplayName("댓글 수정 성공")
    fun shouldUpdateComment() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "작성자",
            password = "1234",
            content = "원본"
        )
        val createdComment = commentService.createComment(createRequest)

        val updateRequest = CommentUpdateRequest(
            password = "1234",
            content = "수정됨"
        )

        // When
        val response = commentService.updateComment(createdComment.id, updateRequest)

        // Then
        assertEquals("수정됨", response.content)
    }

    @Test
    @DisplayName("잘못된 비밀번호로 수정 시 실패")
    fun shouldFailUpdateWithWrongPassword() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "작성자",
            password = "1234",
            content = "원본"
        )
        val createdComment = commentService.createComment(createRequest)

        val updateRequest = CommentUpdateRequest(
            password = "틀림",
            content = "수정됨"
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            commentService.updateComment(createdComment.id, updateRequest)
        }
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    fun shouldDeleteComment() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "작성자",
            password = "1234",
            content = "삭제대상"
        )
        val createdComment = commentService.createComment(createRequest)

        // When
        commentService.deleteComment(createdComment.id, "1234")

        // Then
        val exists = commentRepository.existsById(createdComment.id)
        assertEquals(false, exists)
    }

    @Test
    @DisplayName("잘못된 비밀번호로 삭제 시 실패")
    fun shouldFailDeleteWithWrongPassword() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "작성자",
            password = "1234",
            content = "삭제대상"
        )
        val createdComment = commentService.createComment(createRequest)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            commentService.deleteComment(createdComment.id, "틀림")
        }
    }

    @Test
    @DisplayName("포스트별 댓글 조회 성공")
    fun shouldGetCommentsByPostId() {
        // Given
        val postId = 1L
        commentService.createComment(
            CommentCreateRequest(postId, "작성자A", "1234", "첫번째")
        )
        commentService.createComment(
            CommentCreateRequest(postId, "작성자B", "1234", "두번째")
        )

        // When
        val comments = commentService.getCommentsByPostId(postId)

        // Then
        assertEquals(2, comments.size)
        assertEquals("두번째", comments[0].content) // 최신순
        assertEquals("첫번째", comments[1].content)
    }
}