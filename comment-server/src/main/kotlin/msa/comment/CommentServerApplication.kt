package msa.comment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CommentServerApplication

fun main(args: Array<String>) {
    runApplication<CommentServerApplication>(*args)
}
