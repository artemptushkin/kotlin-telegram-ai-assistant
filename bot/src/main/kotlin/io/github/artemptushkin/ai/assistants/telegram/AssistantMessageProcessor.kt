package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.entities.ReplyMarkup
import io.github.artemptushkin.ai.assistants.configuration.buttonsToLayout
import io.github.artemptushkin.ai.assistants.configuration.parseButtons
import org.springframework.stereotype.Component

const val buttonNotations = "### buttons ###"

@Component
class AssistantMessageProcessor {

    fun format(messageText: String): AssistantMessage {
        return if (messageText.contains(buttonNotations)) {
            AssistantMessage(messageText.removeNotations(), messageText.parseButtons().buttonsToLayout())
        } else {
            AssistantMessage(messageText)
        }
    }
}

fun String.removeNotations(): String = this.substringBefore(buttonNotations).replace("\n\n", "")

data class AssistantMessage(val text: String, val replyMarkup: ReplyMarkup? = null)