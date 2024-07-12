package io.github.artemptushkin.ai.assistants.openai

import com.github.kotlintelegrambot.entities.Message
import com.theokanning.openai.assistants.message.MessageRequest
import com.theokanning.openai.assistants.thread.ThreadRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.configuration.OpenAiProperties
import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.repository.toUserMessageRequest
import io.github.artemptushkin.ai.assistants.telegram.TelegramHistoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ThreadManagementService(
    private val openAiService: OpenAiService,
    private val historyService: TelegramHistoryService,
    private val openAiProperties: OpenAiProperties,
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
            logger.debug("Deleting the thread ${chatHistory.threadId}")
            openAiService.deleteThread(chatHistory.threadId)
            return historyService.clearHistoryById(chatHistory.id!!)
        }
        return chatHistory
    }

    suspend fun saveThread(
        clientChat: String,
        chatHistory: ChatHistory?,
        telegramMessage: Message,
        initialMessages: List<MessageRequest>
    ): ChatHistory {
        if (chatHistory == null) {
            logger.debug("Thread doesn't exist, creating a new one for user ${telegramMessage.from?.id}, no known history exists")
            val newThread = openAiService.createThread(
                ThreadRequest
                    .builder()
                    .messages(initialMessages)
                    .build()
            )
            return historyService.saveThread(
                clientChat,
                mutableListOf(telegramMessage to "user"),
                newThread
            ).also {
                logger.debug("Thread created")
            }
        } else {
            logger.debug("Thread doesn't exist, creating a new one for user ${telegramMessage.from?.id} attaching the initial prompt and set of initial messages")
            val messages = mutableListOf(chatHistory.initialPrompt?.toUserMessageRequest()).also {
                it.addAll(initialMessages)
            }
            val newThread = openAiService.createThread(
                ThreadRequest
                    .builder()
                    .messages(messages)
                    .build()
            )
            return historyService.saveThread(chatHistory, newThread)
                .also {
                    logger.debug("Thread created")
                }
        }
    }

    suspend fun saveOnboardingThread(
        clientChat: String,
        chatHistory: ChatHistory?,
        initialPrompt: String
    ): ChatHistory {
        val newThread = openAiService.createThread(
            ThreadRequest
                .builder()
                .messages(
                    listOf(
                        initialPrompt.toUserMessageRequest()
                    )
                )
                .build()
        )
        logger.debug("New thread ${newThread.id} with the initial prompt has been created during the onboarding process")
        return if (chatHistory != null) {
            historyService.saveThread(chatHistory, newThread, initialPrompt)
        } else {
            historyService.saveThreadWithInitialPrompt(clientChat, newThread, initialPrompt)
        }
    }

    private fun resolveStrategies(): List<ThreadCleanupStrategy> {
        return openAiProperties.threadCleanupStrategies?.mapNotNull {
            classToStrategy[it]
        } ?: emptyList()
    }

    companion object {
        val logger = LoggerFactory.getLogger(ThreadManagementService::class.java)!!
    }
}