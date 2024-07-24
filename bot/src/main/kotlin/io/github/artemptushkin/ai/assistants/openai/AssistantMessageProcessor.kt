package io.github.artemptushkin.ai.assistants.openai

import com.github.kotlintelegrambot.entities.ReplyMarkup
import io.github.artemptushkin.ai.assistants.dutch.buttonNotations
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

interface AssistantMessageProcessor {
    fun format(messageText: String): AssistantMessage
}

@Component
@ConditionalOnMissingBean(AssistantMessageProcessor::class)
class NoopAssistantMessageProcessor : AssistantMessageProcessor {
    override fun format(messageText: String): AssistantMessage = AssistantMessage(messageText)
}

fun String.removeNotations(): String = this.substringBefore(buttonNotations).replace("\n\n", "")

data class AssistantMessage(val text: String, val replyMarkup: ReplyMarkup? = null)