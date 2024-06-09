package io.github.artemptushkin.ai.assistants.repository

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.spring.data.firestore.Document
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import com.theokanning.openai.assistants.message.MessageRequest
import org.springframework.stereotype.Repository

@Repository
interface TelegramHistoryRepository : FirestoreReactiveRepository<ChatHistory>

@Document(collectionName = "telegram-history")
data class ChatHistory(
    @DocumentId
    var id: String? = null,
    var messages: MutableList<Message>? = null,
) {
    fun addMessage(tgMessage: com.github.kotlintelegrambot.entities.Message?, role: String = "user") {
        if (this.messages == null) {
            this.messages = mutableListOf()
        }
        this.messages?.add(tgMessage?.toMessage(role)!!)
    }
}

data class Message(
    var id: Long? = null,
    var text: String? = null,
    val timestamp: Long? = null,
    var user: User? = null,
    val role: String? = null,
)  {
    fun toMessageRequest(): MessageRequest = MessageRequest.builder()
        .content(this.text)
        .role(this.role)
        .build()
}

data class User(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
)

fun com.github.kotlintelegrambot.entities.Message.toMessage(role: String = "user"): Message = Message(
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