package msa.comment.integration

import com.fasterxml.jackson.databind.ObjectMapper
import msa.comment.config.TestConfig
import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentDeleteRequest
import msa.comment.dto.CommentUpdateRequest
import msa.comment.repository.CommentRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.profiles.active=test"])
@Import(TestConfig::class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("댓글 API 통합 테스트")
class CommentIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Test
    @DisplayName("댓글 생성 API 테스트")
    fun createCommentApi() {
        // Given
        val request = CommentCreateRequest(
            postId = 1L,
            author = "테스터",
            password = "1234",
            content = "API 테스트 댓글"
        )

        // When & Then
        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.postId").value(1))
            .andExpect(jsonPath("$.author").value("테스터"))
            .andExpect(jsonPath("$.content").value("API 테스트 댓글"))
    }

    @Test
    @DisplayName("댓글 수정 API 테스트")
    fun updateCommentApi() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "테스터",
            password = "1234",
            content = "원본 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val commentId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("id").asLong()

        val updateRequest = CommentUpdateRequest(
            password = "1234",
            content = "수정된 댓글"
        )

        // When & Then
        mockMvc.perform(
            put("/api/comments/$commentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("수정된 댓글"))
    }

    @Test
    @DisplayName("댓글 삭제 API 테스트")
    fun deleteCommentApi() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "테스터",
            password = "1234",
            content = "삭제할 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val commentId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("id").asLong()

        val deleteRequest = CommentDeleteRequest(password = "1234")

        // When & Then
        mockMvc.perform(
            delete("/api/comments/$commentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("포스트별 댓글 조회 API 테스트")
    fun getCommentsByPostIdApi() {
        // Given
        val postId = 1L

        val request1 = CommentCreateRequest(postId, "첫번째", "1234", "첫 댓글")
        val request2 = CommentCreateRequest(postId, "두번째", "1234", "둘째 댓글")

        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        )

        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        )

        // When & Then
        mockMvc.perform(get("/api/posts/$postId/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].content").value("둘째 댓글")) // 최신순
            .andExpect(jsonPath("$[1].content").value("첫 댓글"))
    }

    @Test
    @DisplayName("잘못된 비밀번호로 댓글 수정 시 400 에러")
    fun updateCommentWithWrongPasswordApi() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            author = "테스터",
            password = "1234",
            content = "원본 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val commentId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("id").asLong()

        val updateRequest = CommentUpdateRequest(
            password = "wrong",
            content = "수정 시도"
        )

        // When & Then
        mockMvc.perform(
            put("/api/comments/$commentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
    }
}