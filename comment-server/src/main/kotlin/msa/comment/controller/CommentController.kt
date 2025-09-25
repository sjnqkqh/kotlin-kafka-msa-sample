package msa.comment.controller

import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentDeleteRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.comment.service.CommentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CommentController(
    private val commentService: CommentService
) {

    @PostMapping("/comments")
    fun createComment(@RequestBody request: CommentCreateRequest): ResponseEntity<CommentResponse> {
        return try {
            val comment = commentService.createComment(request)
            ResponseEntity.ok(comment)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/comments/{id}")
    fun updateComment(
        @PathVariable id: Long,
        @RequestBody request: CommentUpdateRequest
    ): ResponseEntity<CommentResponse> {
        return try {
            val comment = commentService.updateComment(id, request)
            ResponseEntity.ok(comment)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/comments/{id}")
    fun deleteComment(
        @PathVariable id: Long,
        @RequestBody request: CommentDeleteRequest
    ): ResponseEntity<Void> {
        return try {
            commentService.deleteComment(id, request.password)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/posts/{postId}/comments")
    fun getCommentsByPostId(@PathVariable postId: Long): ResponseEntity<List<CommentResponse>> {
        return try {
            val comments = commentService.getCommentsByPostId(postId)
            ResponseEntity.ok(comments)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
}