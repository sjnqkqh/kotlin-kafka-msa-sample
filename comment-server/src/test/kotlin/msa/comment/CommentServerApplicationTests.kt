package msa.comment

import msa.comment.config.TestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest(properties = ["spring.profiles.active=test"])
@Import(TestConfig::class)
class CommentServerApplicationTests {

    @Test
    fun contextLoads() {
    }

}
