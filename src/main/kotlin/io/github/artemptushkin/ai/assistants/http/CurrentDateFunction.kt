package io.github.artemptushkin.ai.assistants.http

import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CurrentDateFunction: OpenAiFunction {
    override fun handle(from: String): String = LocalDateTime.now().toString()

    override fun name(): String = "get-current-date"
}