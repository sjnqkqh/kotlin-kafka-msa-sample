package msa.comment.service

import msa.comment.dto.CommentCreateRequest
import msa.comment.dto.CommentResponse
import msa.comment.dto.CommentUpdateRequest
import msa.comment.model.Comment
import msa.comment.repository.CommentRepository
import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
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
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }

        if (comment.password != request.password) {
            throw CustomException(ErrorCode.INVALID_COMMENT_PASSWORD)
        }

        comment.content = request.content
        val updatedComment = commentRepository.save(comment)

        commentEventPublisher.publishCommentUpdated(updatedComment)
        commentRedisService.evictCommentListCache(comment.postId)

        return CommentResponse.from(updatedComment)
    }

    fun deleteComment(commentId: Long, password: String) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException(ErrorCode.COMMENT_NOT_FOUND) }

        if (comment.password != password) {
            throw CustomException(ErrorCode.INVALID_COMMENT_PASSWORD)
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