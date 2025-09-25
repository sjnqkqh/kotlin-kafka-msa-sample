package msa.comment.service

import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.comment.model.Comment
import msa.comment.repository.CommentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentRedisService: CommentRedisService,
    private val commentEventPublisher: CommentEventPublisher
) {

    fun createComment(request: CommentCreateRequest): CommentResponse {
        val comment = Comment(
            postId = request.postId,
            author = request.author,
            password = request.password,
            content = request.content
        )

        val savedComment = commentRepository.save(comment)

        commentEventPublisher.publishCommentCreated(savedComment)
        commentRedisService.evictCommentListCache(request.postId)

        return CommentResponse.from(savedComment)
    }

    fun updateComment(commentId: Long, request: CommentUpdateRequest): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다.") }

        if (comment.password != request.password) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        comment.content = request.content
        val updatedComment = commentRepository.save(comment)

        commentEventPublisher.publishCommentUpdated(updatedComment)
        commentRedisService.evictCommentListCache(comment.postId)

        return CommentResponse.from(updatedComment)
    }

    fun deleteComment(commentId: Long, password: String) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다.") }

        if (comment.password != password) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        commentRepository.delete(comment)

        commentEventPublisher.publishCommentDeleted(comment)
        commentRedisService.evictCommentListCache(comment.postId)
    }

    @Transactional(readOnly = true)
    fun getCommentsByPostId(postId: Long): List<CommentResponse> {
        val cachedComments = commentRedisService.getCachedCommentList(postId)
        if (cachedComments != null) {
            return cachedComments
        }

        val comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
        val commentResponses = comments.map { CommentResponse.from(it) }

        commentRedisService.cacheCommentList(postId, commentResponses)

        return commentResponses
    }
}