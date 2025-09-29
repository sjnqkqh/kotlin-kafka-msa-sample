package msa.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["msa.user", "msa.common"])
class UserServerApplication

fun main(args: Array<String>) {
    runApplication<UserServerApplication>(*args)
}