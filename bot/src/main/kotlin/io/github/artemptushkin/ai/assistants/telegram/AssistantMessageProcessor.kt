package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.entities.ReplyMarkup
import io.github.artemptushkin.ai.assistants.configuration.dutchStoriesInlineButtons
import org.springframework.stereotype.Component

const val buttonNotations = "### buttons ###"

@Component
class AssistantMessageProcessor {

    fun format(messageText: String): AssistantMessage {
        if (messageText.contains(buttonNotations)) {
            return AssistantMessage(messageText.removeNotations(), dutchStoriesInlineButtons()) // todo if we have someone else besides Dutch button it must be refactored
        } else {
            return AssistantMessage(messageText)
        }
    }
}

fun String.removeNotations(): String = this.substringBefore("### buttons ###").replace("\n\n", "")

data class AssistantMessage(val text: String, val replyMarkup: ReplyMarkup? = null)