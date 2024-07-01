package io.github.artemptushkin.ai.assistants.configuration

data class TelegramContext(
    val botId: String,
    val chatId: String,
    val metadata: Map<String, Any>,
)