package io.github.artemptushkin.ai.assistants.openai

import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.telegram.TelegramHistoryService
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ThreadManagementService(
    private val openAiService: OpenAiService,
    private val historyService: TelegramHistoryService,
    private val telegramProperties: TelegramProperties,
    @Qualifier("classToStrategy")
    private val classToStrategy: Map<String, ThreadCleanupStrategy>,
) {
    suspend fun deleteThreadIfAcceptable(chatId: String, chatHistory: ChatHistory?): ChatHistory? {
        if (chatHistory?.threadId == null) {
            return chatHistory
        }
        val strategies = resolveStrategies()
        if (strategies.isEmpty()) {
            logger.error("No strategies have not been registered! Thread can not be deleted!")
            return chatHistory
        }
        if (strategies.any { it.isAcceptableForCleanup(chatHistory) }) {
            openAiService.deleteThread(chatHistory.threadId)
            return historyService.clearHistoryById(chatHistory.id!!)
        }
        return chatHistory
    }

    private fun resolveStrategies(): List<ThreadCleanupStrategy> {
        return telegramProperties.bot.openAi.threadCleanupStrategies?.mapNotNull {
            classToStrategy[it]
        } ?: emptyList()
    }

    companion object {
        val logger = LoggerFactory.getLogger(ThreadManagementService::class.java)!!
    }
}