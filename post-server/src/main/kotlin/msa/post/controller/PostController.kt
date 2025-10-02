package msa.post.controller

import msa.common.auth.UserContextProvider
import msa.common.dto.ApiResponse
import msa.common.dto.PageResponse
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.service.PostService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(
    private val postService: PostService,
    private val userContextProvider: UserContextProvider
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
        val currentUser = userContextProvider.getCurrentUser()
        val createdPost = postService.createPost(currentUser, request)
        return ApiResponse.success(createdPost)
    }

    @DeleteMapping("/{id}")
    fun removePost(@PathVariable id: Long): ApiResponse<Unit> {
        val currentUser = userContextProvider.getCurrentUser()
        postService.deletePost(currentUser, id)
        return ApiResponse.success()
    }
}
