package io.github.artemptushkin.ai.assistants.repository

import com.github.kotlintelegrambot.entities.Message
import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.spring.data.firestore.Document
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import org.springframework.stereotype.Repository

@Repository
interface TelegramHistoryRepository : FirestoreReactiveRepository<ChatHistory>

@Document(collectionName = "telegram-history")
data class ChatHistory(
    @DocumentId
    var id: String? = null,
    var messages: MutableList<ChatMessage>? = null,
    var threadId: String? = null,
) {
    fun addMessage(tgMessage: Message?, role: String = "user") {
        if (this.messages == null) {
            this.messages = mutableListOf()
        }
        this.messages?.add(tgMessage?.toMessage(role)!!)
    }
}

data class ChatMessage(
    var id: Long? = null,
    var text: String? = null,
    val timestamp: Long? = null,
    var user: User? = null,
    val role: String? = null,
)

data class User(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
)