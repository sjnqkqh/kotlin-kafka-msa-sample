package msa.comment.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.ConfluentKafkaContainer
import java.time.Duration

/**
 * 통합 테스트 컨테이너 설정
 * MySQL, Redis, Kafka를 모두 관리하는 기본 테스트 인프라 클래스
 */
@Testcontainers
//@TestConfiguration(proxyBeanMethods = false)
abstract class TestContainerConfig {
    companion object {

        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer("mysql:8.0")
            .withDatabaseName("comment_db_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withEnv("MYSQL_ROOT_PASSWORD", "root_password")
            .withStartupTimeout(Duration.ofMinutes(3))
            .withConnectTimeoutSeconds(20)

        @Container
        @JvmStatic
        val redisContainer = GenericContainer("redis:7")
            .withExposedPorts(6379)

        @Container
        @JvmStatic
        val kafkaContainer = ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")

        @JvmStatic
        @DynamicPropertySource
        fun configureTestProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }

            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
        }
    }
}