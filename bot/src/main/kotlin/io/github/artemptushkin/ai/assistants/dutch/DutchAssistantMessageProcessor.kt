package io.github.artemptushkin.ai.assistants.dutch

import io.github.artemptushkin.ai.assistants.openai.AssistantMessage
import io.github.artemptushkin.ai.assistants.openai.AssistantMessageProcessor
import io.github.artemptushkin.ai.assistants.openai.removeNotations
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

const val buttonNotations = "### buttons ###"

@Component
@Profile("dutch")
class DutchAssistantMessageProcessor: AssistantMessageProcessor {

    override fun format(messageText: String): AssistantMessage {
        return if (messageText.contains(buttonNotations)) {
            AssistantMessage(messageText.removeNotations(), messageText.parseButtons().buttonsToLayout())
        } else {
            AssistantMessage(messageText)
        }
    }
}