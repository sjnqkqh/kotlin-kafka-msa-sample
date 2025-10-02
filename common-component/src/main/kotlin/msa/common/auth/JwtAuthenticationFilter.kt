package msa.common.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import msa.common.exception.CustomException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenValidator: JwtTokenValidator
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)

                try {
                    val userContext = jwtTokenValidator.validateTokenAndGetUser(token)

                    // SecurityContext에 인증 정보 설정
                    val authToken = UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        emptyList()
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken

                } catch (e: CustomException) {
                    // 토큰 검증 실패 - 로그만 남기고 필터 체인 계속 진행
                    logger.debug("JWT validation failed: ${e.message}")
                }
            }

            filterChain.doFilter(request, response)

        } catch (e: Exception) {
            logger.error("JWT filter error", e)
            filterChain.doFilter(request, response)
        }
    }
}