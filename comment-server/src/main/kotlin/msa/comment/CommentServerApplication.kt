package msa.comment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommentServerApplication

fun main(args: Array<String>) {
    runApplication<CommentServerApplication>(*args)
}
