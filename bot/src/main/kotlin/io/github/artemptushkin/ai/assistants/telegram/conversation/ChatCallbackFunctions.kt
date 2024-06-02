package io.github.artemptushkin.ai.assistants.telegram.conversation

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message

fun Message.chatId() = this.chat.id.toChat()

fun Long.toChat() = ChatId.fromId(this)

fun Message.isCommand(): Boolean = text?.startsWith("/") ?: false