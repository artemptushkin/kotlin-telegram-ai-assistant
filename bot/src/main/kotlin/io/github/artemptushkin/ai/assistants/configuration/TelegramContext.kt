package io.github.artemptushkin.ai.assistants.configuration

import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import io.github.artemptushkin.ai.assistants.telegram.id
import org.springframework.stereotype.Component

@Component
class ContextFactory(
    private val telegramProperties: TelegramProperties,
) {
    fun buildContext(chatId: String): TelegramContext = TelegramContext(
        chatId = chatId,
        botId = telegramProperties.bot.id(),
        metadata = telegramProperties.bot.metadata,
    )
}

data class TelegramContext(
    val botId: String,
    val chatId: String,
    val metadata: Map<String, Any>,
)
