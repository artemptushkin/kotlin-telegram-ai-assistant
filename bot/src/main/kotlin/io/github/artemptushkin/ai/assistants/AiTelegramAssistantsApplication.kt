package io.github.artemptushkin.ai.assistants

import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableReactiveFirestoreRepositories
class AiTelegramAssistantsApplication

fun main(args: Array<String>) {
    runApplication<AiTelegramAssistantsApplication>(*args)
}