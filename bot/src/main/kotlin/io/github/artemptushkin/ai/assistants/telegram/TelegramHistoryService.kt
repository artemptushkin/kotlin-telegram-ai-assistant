package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.entities.Message
import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.repository.TelegramHistoryRepository
import io.github.artemptushkin.ai.assistants.repository.toMessage
import io.github.artemptushkin.ai.assistants.telegram.conversation.chatId
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class TelegramHistoryService(
    private val historyRepository: TelegramHistoryRepository
) {

    suspend fun saveOrAddMessage(message: Message, role: String = "user"): ChatHistory {
        val chatId = message.chatId().id.toString()
        return historyRepository.findById(chatId).awaitSingleOrNull()?.let { currentChat ->
            currentChat.addMessage(message, role)
            historyRepository.save(currentChat).awaitSingle()
        }
            ?: historyRepository
                .save(
                    ChatHistory(
                        id = chatId,
                        messages = mutableListOf(message.toMessage(role))
                    )
                )
                .awaitSingle()
    }

    suspend fun clearHistoryById(id: String) {
         historyRepository
            .findById(id)
            .flatMap { c ->
                c.messages?.clear()
                historyRepository.save(c)
            }.awaitSingle()
    }
}