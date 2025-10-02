package msa.comment.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * 테스트 환경용 Security 설정
 * 모든 요청을 허용하여 인증 없이 테스트 가능하도록 설정
 */
@TestConfiguration
@Profile("test")
class TestSecurityConfig {
    // TODO: Test 전용 Config 존재에 따라 통합 테스트가 느슨하게 설정되는 상태이므로, 추후 일반 서버와 동일한 시큐리티 설정으로 테스트가 통과하도록 수정되어야 함
    @Bean
    @Order(1) // SecurityConfig보다 높은 우선순위
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher("/api/**") // API 경로에만 적용
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll() // 모든 요청 허용
            }
            .formLogin { it.disable() }
            .build()
    }
}
