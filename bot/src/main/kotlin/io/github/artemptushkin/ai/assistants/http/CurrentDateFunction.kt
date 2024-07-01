package io.github.artemptushkin.ai.assistants.http

import com.fasterxml.jackson.databind.JsonNode
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CurrentDateFunction: OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String = LocalDateTime.now().toString()

    override fun name(): String = "get-current-date"
}