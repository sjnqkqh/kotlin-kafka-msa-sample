package msa.post.controller

import msa.common.dto.ApiResponse
import msa.common.dto.PageResponse
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.service.PostService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
) {

    @GetMapping("")
    fun getPostsByPage(
        @PageableDefault(size = 10, sort = ["createdAt"]) pageable: Pageable
    ): ApiResponse<PageResponse<PostResponse>> {
        val posts = postService.getPostsByPage(pageable)
        return ApiResponse.success(posts)
    }

    @GetMapping("/{id}")
    fun getPostDetail(@PathVariable id: Long): ApiResponse<PostResponse> {
        val post = postService.getPostById(id)
        return ApiResponse.success(post)
    }

    @PostMapping("")
    fun addPost(@RequestBody request: PostCreateRequest): ApiResponse<PostResponse> {
        val createdPost = postService.createPost(request)
        return ApiResponse.success(createdPost)
    }

    @DeleteMapping("/{id}")
    fun removePost(@PathVariable id: Long): ApiResponse<Unit> {
        postService.deletePost(id)
        return ApiResponse.success()
    }
}
