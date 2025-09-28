package msa.post.controller

import msa.post.dto.PageResponse
import msa.post.dto.PostCreateRequest
import msa.post.dto.PostResponse
import msa.post.service.PostService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
) {

    @GetMapping("")
    fun getPostsByPage(
        @PageableDefault(size = 10, sort = ["createdAt"]) pageable: Pageable
    ): ResponseEntity<PageResponse<PostResponse>> {
        val posts = postService.getPostsByPage(pageable)
        return ResponseEntity.ok(posts)
    }

    @GetMapping("/{id}")
    fun getPostDetail(@PathVariable id: Long): ResponseEntity<PostResponse> {
        val post = postService.getPostById(id)
        return ResponseEntity.ok(post)
    }

    @PostMapping("")
    fun addPost(@RequestBody request: PostCreateRequest): ResponseEntity<PostResponse> {
        val createdPost = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost)
    }

    @DeleteMapping("/{id}")
    fun removePost(@PathVariable id: Long): ResponseEntity<Void> {
        postService.deletePost(id)
        return ResponseEntity.noContent().build()
    }
}
