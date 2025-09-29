package msa.comment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = ["msa.comment", "msa.common"])
class CommentServerApplication

fun main(args: Array<String>) {
    runApplication<CommentServerApplication>(*args)
}
