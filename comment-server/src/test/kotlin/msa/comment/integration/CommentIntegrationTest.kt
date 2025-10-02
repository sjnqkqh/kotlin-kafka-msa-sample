package msa.comment.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import msa.comment.config.TestConfig
import msa.comment.config.TestSecurityConfig
import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.common.auth.UserContext
import msa.common.auth.UserContextProvider
import msa.common.dto.ApiResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
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
@Import(TestConfig::class, TestSecurityConfig::class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("댓글 API 통합 테스트")
class CommentIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userContextProvider: UserContextProvider

    private val defaultUser = UserContext(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        userType = "NORMAL"
    )

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 기본 사용자로 리셋
        whenever(userContextProvider.getCurrentUser()).thenReturn(defaultUser)
        whenever(userContextProvider.getCurrentUserOrNull()).thenReturn(defaultUser)
    }

    @Test
    @DisplayName("댓글 생성 API 테스트")
    fun createCommentApi() {
        // Given
        val request = CommentCreateRequest(
            postId = 1L,
            content = "API 테스트 댓글"
        )

        // When & Then
        val result = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andReturn()

        println("Response: ${result.response.contentAsString}")

        mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.authorName").value("Test User"))
            .andExpect(jsonPath("$.data.content").value("API 테스트 댓글"))
    }

    @Test
    @DisplayName("댓글 수정 API 테스트")
    fun updateCommentApi() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "원본 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val commentId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("data").get("id").asLong()

        val updateRequest = CommentUpdateRequest(
            content = "수정된 댓글"
        )

        // When & Then
        mockMvc.perform(
            put("/api/comments/$commentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").value("수정된 댓글"))
    }

    @Test
    @DisplayName("댓글 삭제 API 테스트")
    fun deleteCommentApi() {
        // Given
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "삭제할 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()
        val commentResponse: ApiResponse<CommentResponse> =
            objectMapper.readValue(createResponse.response.contentAsString)
        val commentId = commentResponse.data!!.id

        // When & Then - DELETE 요청은 body 없이 전송
        mockMvc.perform(
            delete("/api/comments/$commentId")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("포스트별 댓글 조회 API 테스트")
    fun getCommentsByPostIdApi() {
        // Given
        val postId = 1L

        val request1 = CommentCreateRequest(postId, "첫번째")
        val request2 = CommentCreateRequest(postId, "두번째")

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
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].content").value("두번째")) // 최신순 (Desc)
            .andExpect(jsonPath("$.data[1].content").value("첫번째"))
    }

    @Test
    @DisplayName("다른 사용자가 댓글 수정 시도 시 400 에러")
    fun updateCommentWithDifferentUserApi() {
        // Given - 첫 번째 사용자로 댓글 생성
        val createRequest = CommentCreateRequest(
            postId = 1L,
            content = "원본 댓글"
        )

        val createResponse = mockMvc.perform(
            post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        ).andReturn()

        val commentId = objectMapper.readTree(createResponse.response.contentAsString)
            .get("data").get("id").asLong()

        // Mock을 다른 사용자로 변경
        whenever(userContextProvider.getCurrentUser()).thenReturn(
            UserContext(
                id = 2L,
                email = "other@example.com",
                name = "Other User",
                userType = "NORMAL"
            )
        )

        val updateRequest = CommentUpdateRequest(
            content = "수정 시도"
        )

        // When & Then - 다른 사용자의 수정 시도는 실패해야 함
        mockMvc.perform(
            put("/api/comments/$commentId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().is4xxClientError)
    }
}