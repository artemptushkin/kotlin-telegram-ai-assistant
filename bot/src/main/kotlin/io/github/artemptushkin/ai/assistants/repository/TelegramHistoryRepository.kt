package io.github.artemptushkin.ai.assistants.repository

import com.github.kotlintelegrambot.entities.Update
import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.spring.data.firestore.Document
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import io.github.artemptushkin.ai.assistants.telegram.conversation.chatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Configuration
class RepositoryConfiguration {
    @Bean
    fun databaseUpdatesChannel() = Channel<Update>()
}

@Component
class DatabaseUpdatesListener(
    private val databaseUpdateChannel: Channel<Update>,
    private val telegramHistoryRepository: TelegramHistoryRepository,
) {
    fun listen() = CoroutineScope(Dispatchers.IO).launch {
        val update = databaseUpdateChannel.receive()
        telegramHistoryRepository.save(ChatHistory(update.message?.chatId()?.id))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryConfiguration::class.java)
    }
}

@Repository
interface TelegramHistoryRepository : FirestoreReactiveRepository<ChatHistory>

@Document(collectionName = "telegram-history")
data class ChatHistory(
    @DocumentId
    var id: Long? = null,
    var messages: List<Message>? = null,
)

data class Message(
    var id: Long? = null,
    var text: String? = null,
    var user: User? = null,
)

data class User(
    var id: Long? = null,
    var name: String? = null,
)