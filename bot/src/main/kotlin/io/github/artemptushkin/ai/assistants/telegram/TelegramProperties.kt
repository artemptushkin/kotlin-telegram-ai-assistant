package io.github.artemptushkin.ai.assistants.telegram

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("telegram")
data class TelegramProperties(
    val bot: BotProperties = BotProperties(),
    val webhook: WebHookProperties = WebHookProperties()
)

data class WebHookProperties(
    val url: String? = null,
    val secretToken: String? = null
)

data class BotProperties(
    val token: String = "",
    val helpMessage: String = "",
    val startMessage: String = "",
    val buttons: List<Button> = emptyList(),
    val metadata: Map<String, String> = mutableMapOf(),
    val isOnboardingEnabled: Boolean = false
)

data class Button(
    val id: String,
    val text: String,
    val isBlocking: Boolean = false,
    val assistantText: String,
)

fun BotProperties.id() = this.token.substringBefore(":")