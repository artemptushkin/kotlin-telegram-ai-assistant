package io.github.artemptushkin.ai.assistants.repository

import com.github.kotlintelegrambot.entities.Message
import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.spring.data.firestore.Document
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Repository
interface TelegramHistoryRepository : FirestoreReactiveRepository<ChatHistory>

@Document(collectionName = "telegram-history")
data class ChatHistory(
    @DocumentId
    var id: String? = null,
    var messages: MutableList<ChatMessage>? = null,
    var threadId: String? = null,
    var initialPrompt: String? = null,
) {
    fun addMessage(tgMessage: Message?, role: String = "user") {
        if (this.messages == null) {
            this.messages = mutableListOf()
        }
        this.messages?.add(tgMessage?.toMessage(role)!!)
    }
}

fun ChatHistory.getLatestMessageDate(): LocalDate? {
    return this.messages?.maxByOrNull { it.timestamp ?: 0L }?.timestamp?.let {
        Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
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