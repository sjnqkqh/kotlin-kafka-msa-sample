package msa.post

import msa.post.config.TestContainerConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostServerApplicationTests : TestContainerConfig() {

    @Test
    fun contextLoads() {
    }

}
