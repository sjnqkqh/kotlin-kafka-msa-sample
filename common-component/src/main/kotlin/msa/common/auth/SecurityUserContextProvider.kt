package msa.common.auth

import msa.common.exception.CustomException
import msa.common.exception.ErrorCode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Spring Security의 SecurityContext를 사용하여 현재 사용자 정보를 제공하는 구현체
 *
 * 프로덕션 환경에서 사용되며, JwtAuthenticationFilter에 의해 설정된
 * SecurityContext에서 UserContext를 추출합니다.
 */
@Component
class SecurityUserContextProvider : UserContextProvider {

    override fun getCurrentUser(): UserContext {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw CustomException(ErrorCode.ACCESS_DENIED)

        return when (val principal = authentication.principal) {
            is UserContext -> principal
            else -> throw CustomException(ErrorCode.ACCESS_DENIED)
        }
    }

    override fun getCurrentUserOrNull(): UserContext? {
        return try {
            getCurrentUser()
        } catch (e: CustomException) {
            null
        }
    }
}
