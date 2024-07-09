package io.github.artemptushkin.ai.assistants.telegram.conversation

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import io.github.artemptushkin.ai.assistants.configuration.buttons

fun Message.chatId() = this.chat.id.toChat()

fun Long.toChat() = ChatId.fromId(this)

fun Message.isCommand(): Boolean = text?.startsWith("/") ?: false

fun Message.isBlockingButtonCommand(): Boolean = this.isButtonCommand() && this.text != "Create a story"

fun Message.isButtonCommand(): Boolean = if (text != null) {
    buttons.any { text == it }
} else false

