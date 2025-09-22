package msa.post.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@TestConfiguration
class TestRedisConfig {

    @Bean
    @Primary
    fun redisConnectionFactory(
        @Value("\${spring.data.redis.host:localhost}") host: String,
        @Value("\${spring.data.redis.port:6379}") port: Int
    ): RedisConnectionFactory {
        return LettuceConnectionFactory(host, port)
    }

    @Bean("testRedisTemplate")
    fun testRedisTemplate(@Qualifier("redisConnectionFactory") connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()

        template.afterPropertiesSet()
        return template
    }
}