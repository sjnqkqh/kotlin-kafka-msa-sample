package msa.comment.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_post_id_created_at", columnList = "post_id, created_at DESC")
    ]
)
data class Comment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, name = "post_id")
    val postId: Long,

    @Column(nullable = false, name = "user_id")
    val userId: Long,

    @Column(nullable = false, length = 100, name = "author_name")
    val authorName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)