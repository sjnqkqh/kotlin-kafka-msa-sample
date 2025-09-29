package msa.post.config

import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.core.KafkaTemplate

/**
 * 테스트용 설정 클래스
 * Redis와 Kafka를 Mock 객체로 대체하여 빠른 단위 테스트 지원
 */
@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun mockRedisTemplate(): RedisTemplate<String, Any> {
        return mock<RedisTemplate<String, Any>>()
    }

    @Bean
    @Primary
    fun mockKafkaTemplate(): KafkaTemplate<String, Any> {
        return mock<KafkaTemplate<String, Any>>()
    }
}