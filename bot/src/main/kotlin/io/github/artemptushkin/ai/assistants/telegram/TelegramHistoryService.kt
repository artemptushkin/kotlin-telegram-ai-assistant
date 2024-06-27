package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.entities.Message
import com.theokanning.openai.assistants.thread.Thread
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
                c.threadId = null
                historyRepository.save(c)
            }.awaitSingle()
    }

    suspend fun saveThread(chatHistory: ChatHistory, thread: Thread): String? {
        chatHistory.threadId = thread.id
        return historyRepository
            .save(chatHistory)
            .mapNotNull { it.threadId }
            .awaitSingleOrNull()
    }

    suspend fun saveThread(chatId: String, messages: MutableList<Pair<Message, String>>, thread: Thread): String? {
        return historyRepository
            .save(
                ChatHistory(
                    id = chatId,
                    threadId = thread.id,
                    messages = messages.map { it.first.toMessage(it.second) }.toMutableList()
                )
            )
            .mapNotNull { it.threadId }
            .awaitSingleOrNull()
    }

    suspend fun fetchCurrentThread(chatId: String): String? {
        return historyRepository
            .findById(chatId)
            .mapNotNull { it.threadId }
            .awaitSingleOrNull()
    }

    suspend fun fetchChatHistory(chatId: String): ChatHistory? {
        return historyRepository
            .findById(chatId)
            .awaitSingleOrNull()
    }
}