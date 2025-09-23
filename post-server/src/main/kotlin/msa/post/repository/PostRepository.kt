package msa.post.repository

import msa.post.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface PostRepository : JpaRepository<Post, Long> {
    fun findAllByOrderByCreatedAtDesc(page: Pageable): Page<Post>
}