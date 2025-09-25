package msa.comment.repository

import msa.comment.model.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {

    fun findByPostIdOrderByCreatedAtDesc(postId: Long): List<Comment>

    fun countByPostId(postId: Long): Long
}