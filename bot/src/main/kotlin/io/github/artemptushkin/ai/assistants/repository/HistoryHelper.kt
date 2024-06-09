package io.github.artemptushkin.ai.assistants.repository

import com.github.kotlintelegrambot.entities.Message
import com.theokanning.openai.assistants.message.MessageRequest

fun ChatMessage.toMessageRequest(): MessageRequest = MessageRequest.builder()
    .content(this.text)
    .role(this.role)
    .build()

fun Message.toMessageRequest(role: String): MessageRequest =
    MessageRequest.builder()
        .content(this.text)
        .role(role)
        .build()

fun Message.toMessage(role: String = "user"): ChatMessage = ChatMessage(
    id = this.messageId,
    text = this.text,
    timestamp = this.date,
    role = role,
    user = User(
        id = this.from?.id,
        firstName = this.from?.firstName,
        lastName = this.from?.lastName
    ),
)