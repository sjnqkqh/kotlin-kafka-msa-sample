package msa.post.service

import msa.post.dto.PageResponse
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.model.Post
import msa.post.repository.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postRedisService: PostRedisService
) {

    fun getPostsByPage(pageable: Pageable): PageResponse<PostResponse> {
        val page: Page<Post> = postRepository.findAll(pageable)
        val postResponses = page.content.map { it.toResponse() }

        return PageResponse(
            content = postResponses,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            first = page.isFirst,
            last = page.isLast
        )
    }

    fun getPostById(id: Long): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Post not found with id: $id") }
        return post.toResponse()
    }

    @Transactional
    fun createPost(request: PostCreateRequest): PostResponse {
        val post = Post(
            title = request.title,
            content = request.content
        )
        val savedPost = postRepository.save(post)
        postRedisService.pushRedisRecentPostList(savedPost)

        return savedPost.toResponse()
    }


    @Transactional
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw IllegalArgumentException("Post not found with id: $id")
        }
        postRepository.deleteById(id)
    }

    private fun Post.toResponse(): PostResponse {
        return PostResponse(
            id = this.id!!,
            title = this.title,
            content = this.content,
            createdAt = this.createdAt!!,
            updatedAt = this.updatedAt!!
        )
    }
}