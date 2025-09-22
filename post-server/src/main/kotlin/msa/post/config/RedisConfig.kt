package msa.post.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableConfigurationProperties
class RedisConfig(
    private val redisProperties: RedisProperties
) {


    @Bean
    fun connectionFactory(): LettuceConnectionFactory {
        return LettuceConnectionFactory(redisProperties.host, redisProperties.port)
    }

    @Bean
    fun redisTemplate(cf: RedisConnectionFactory): RedisTemplate<String, Any> {
        var template = RedisTemplate<String, Any>()
        template.setConnectionFactory(cf)

        val keySerializers= StringRedisSerializer()
        val valueSerializers= GenericJackson2JsonRedisSerializer()

        template.keySerializer = keySerializers
        template.valueSerializer = valueSerializers
        template.hashKeySerializer = keySerializers
        template.hashValueSerializer = valueSerializers

        template.afterPropertiesSet()
        return template
    }

}

