package msa.sample.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinKafkaMsaSampleApplication

fun main(args: Array<String>) {
    runApplication<KotlinKafkaMsaSampleApplication>(*args)
}
