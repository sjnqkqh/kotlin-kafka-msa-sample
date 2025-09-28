package msa.user.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "verification_codes")
data class VerificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val code: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: VerificationType,

    @Column(nullable = false)
    val expiresAt: LocalDateTime,

    @Column(nullable = false)
    val isUsed: Boolean = false,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isValid(): Boolean = !isUsed && !isExpired()
}

enum class VerificationType {
    SIGNUP,
    PASSWORD_RESET
}