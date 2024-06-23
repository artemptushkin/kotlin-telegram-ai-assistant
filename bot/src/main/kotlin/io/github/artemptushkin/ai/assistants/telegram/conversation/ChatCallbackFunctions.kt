package io.github.artemptushkin.ai.assistants.telegram.conversation

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

val buttons = listOf("Add words", "Delete words")

fun Message.chatId() = this.chat.id.toChat()

fun Long.toChat() = ChatId.fromId(this)

fun Message.isCommand(): Boolean = text?.startsWith("/") ?: false

fun Message.isButtonCommand(): Boolean = if (text != null) {
    buttons.any { text == it }
} else false