package msa.common.auth

data class UserContext(
    val id: Long,
    val email: String,
    val name: String,
    val userType: String
)