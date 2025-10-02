package msa.comment.controller

import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.comment.service.CommentService
import msa.common.auth.UserContextProvider
import msa.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class CommentController(
    private val commentService: CommentService,
    private val userContextProvider: UserContextProvider
) {

    @PostMapping("/comments")
    fun createComment(@RequestBody request: CommentCreateRequest): ApiResponse<CommentResponse> {
        val currentUser = userContextProvider.getCurrentUser()
        val comment = commentService.createComment(currentUser, request)
        return ApiResponse.success(comment)
    }

    @PutMapping("/comments/{id}")
    fun updateComment(
        @PathVariable id: Long,
        @RequestBody request: CommentUpdateRequest
    ): ApiResponse<CommentResponse> {
        val currentUser = userContextProvider.getCurrentUser()
        val comment = commentService.updateComment(id, currentUser, request)
        return ApiResponse.success(comment)
    }

    @DeleteMapping("/comments/{id}")
    fun deleteComment(@PathVariable id: Long): ApiResponse<Unit> {
        val currentUser = userContextProvider.getCurrentUser()
        commentService.deleteComment(id, currentUser)
        return ApiResponse.success()
    }

    @GetMapping("/posts/{postId}/comments")
    fun getCommentsByPostId(@PathVariable postId: Long): ApiResponse<List<CommentResponse>> {
        val comments = commentService.getCommentsByPostId(postId)
        return ApiResponse.success(comments)
    }
}