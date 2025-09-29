package msa.post.service

import msa.common.dto.PageResponse
import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.model.Post
import msa.post.repository.PostRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postRedisService: PostRedisService,
    private val postEventPublisher: PostEventPublisher
) {
    private val recentPostRemainHours: Long = 12

    private fun createPageResponse(
        posts: List<Post>,
        currentPage: Int,
        pageSize: Int,
        totalElements: Long
    ): PageResponse<PostResponse> {
        val postResponses = posts.map { PostResponse.fromPost(it) }
        val totalPages = if (pageSize > 0) {
            ((totalElements + pageSize - 1) / pageSize).toInt()
        } else 0

        return PageResponse(
            content = postResponses,
            page = currentPage,
            size = pageSize,
            totalElements = totalElements,
            totalPages = totalPages,
            first = currentPage == 0,
            last = currentPage >= totalPages - 1
        )
    }


    fun getPostsByPage(pageable: Pageable): PageResponse<PostResponse> {
        // "Query then Validate" 패턴: 바로 Redis에서 조회 후 결과 크기로 판단
        val startIndex = pageable.pageNumber * pageable.pageSize
        val endIndex = startIndex + pageable.pageSize - 1

        // 요청한 만큼 데이터를 가져왔다면 Redis 데이터 사용
        val recentPosts = postRedisService.getRecentPostsByRange(startIndex.toLong(), endIndex.toLong())
        return if (recentPosts.size == pageable.pageSize) {
            val totalElements = postRepository.count()
            createPageResponse(recentPosts, pageable.pageNumber, pageable.pageSize, totalElements)
        } else {
            // Redis에 충분한 데이터가 없으면 DB에서 조회
            val page = postRepository.findAllByOrderByCreatedAtDesc(pageable)
            createPageResponse(page.content, page.number, page.size, page.totalElements)
        }
    }

    fun getPostById(id: Long): PostResponse {
        val cachedPost = postRedisService.getCachedPost(id)
        if (cachedPost != null) {
            return PostResponse.fromPost(cachedPost)
        }

        val post = postRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.POST_NOT_FOUND) }

        postRedisService.cachePost(post)

        return PostResponse.fromPost(post)
    }

    @Transactional
    fun createPost(request: PostCreateRequest): PostResponse {
        val post = Post(
            title = request.title,
            content = request.content
        )
        val savedPost = postRepository.save(post)
        val expireTime = postRedisService.calculateExpireTime(Duration.ofHours(recentPostRemainHours))
        postRedisService.addToRecentPosts(savedPost, expireTime)
        postRedisService.cachePost(savedPost)

        // Kafka 이벤트 발행
        postEventPublisher.publishPostCreated(savedPost)

        return PostResponse.fromPost(savedPost)
    }


    @Transactional
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw CustomException(ErrorCode.POST_NOT_FOUND)
        }

        // 캐시 갱신 로직 추가
        postRedisService.removeCachedPost(id)
        postRedisService.removeFromRecentPosts(id)

        postRepository.deleteById(id)

        // Kafka 이벤트 발행
        postEventPublisher.publishPostDeleted(id)
    }

}