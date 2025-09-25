package msa.comment

import msa.comment.config.TestContainerConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.profiles.active=test"])
class CommentServerApplicationTests : TestContainerConfig() {

    @Test
    fun contextLoads() {
    }

}
