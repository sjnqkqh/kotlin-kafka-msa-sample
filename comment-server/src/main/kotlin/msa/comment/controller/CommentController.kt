package msa.comment.controller

import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentDeleteRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.comment.service.CommentService
import msa.common.dto.ApiResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping("/comments")
    fun createComment(@RequestBody request: CommentCreateRequest): ApiResponse<CommentResponse> {
        val comment = commentService.createComment(request)
        return ApiResponse.success(comment)
    }

    @PutMapping("/comments/{id}")
    fun updateComment(
        @PathVariable id: Long,
        @RequestBody request: CommentUpdateRequest
    ): ApiResponse<CommentResponse> {
        val comment = commentService.updateComment(id, request)
        return ApiResponse.success(comment)
    }

    @DeleteMapping("/comments/{id}")
    fun deleteComment(
        @PathVariable id: Long,
        @RequestBody request: CommentDeleteRequest
    ): ApiResponse<Unit> {
        commentService.deleteComment(id, request.password)
        return ApiResponse.success()
    }

    @GetMapping("/posts/{postId}/comments")
    fun getCommentsByPostId(@PathVariable postId: Long): ApiResponse<List<CommentResponse>> {
        val comments = commentService.getCommentsByPostId(postId)
        return ApiResponse.success(comments)
    }
}