package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.entities.Message
import com.theokanning.openai.assistants.thread.Thread
import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.repository.ChatMessage
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

    suspend fun clearHistoryById(id: String): ChatHistory {
        return historyRepository
            .findById(id)
            .flatMap { c ->
                c.threadId = null
                historyRepository.save(c)
            }.awaitSingle()
    }

    suspend fun saveThreadWithInitialPrompt(chatId: String, thread: Thread, initialPrompt: String): ChatHistory {
        return historyRepository
            .save(ChatHistory(
                id = chatId,
                threadId = thread.id,
                initialPrompt = initialPrompt
            ))
            .awaitSingle()
    }

    suspend fun saveThread(chatHistory: ChatHistory, thread: Thread, initialPrompt: String? = null): ChatHistory {
        chatHistory.threadId = thread.id
        initialPrompt?.let {
            chatHistory.initialPrompt = it
        }
        return historyRepository
            .save(chatHistory)
            .awaitSingle()
    }

    suspend fun saveThread(chatId: String, messages: MutableList<Pair<Message, String>>, thread: Thread): ChatHistory {
        return historyRepository
            .save(
                ChatHistory(
                    id = chatId,
                    threadId = thread.id,
                    messages = messages.map { it.first.toMessage(it.second) }.toMutableList()
                )
            )
            .awaitSingle()
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

    suspend fun fetchMessageById(chatId: String, messageIdReference: Long): ChatMessage? {
        return historyRepository
            .findById(chatId)
            .awaitSingleOrNull()
            .let { chatHistory ->
                chatHistory?.messages?.find { message -> message.id == messageIdReference }
            }
    }
}