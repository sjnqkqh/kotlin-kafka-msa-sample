package msa.user.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {

    @Bean
    @Primary
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .securityMatcher("/**")
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests.anyRequest().permitAll()
            }
            .build()
    }
}