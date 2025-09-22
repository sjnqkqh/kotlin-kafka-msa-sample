package msa.post

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class PostServerApplication

fun main(args: Array<String>) {
    runApplication<PostServerApplication>(*args)
}
