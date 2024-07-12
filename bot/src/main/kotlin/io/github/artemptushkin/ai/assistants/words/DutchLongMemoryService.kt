package io.github.artemptushkin.ai.assistants.words

import com.theokanning.openai.assistants.message.MessageRequest
import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import io.github.artemptushkin.ai.assistants.openai.LongMemoryService
import io.github.artemptushkin.ai.assistants.repository.toUserMessageRequest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dutch")
class DutchLongMemoryService(
    private val learningWordsService: LearningWordsService,
) : LongMemoryService {
    override suspend fun getMemory(telegramContext: TelegramContext): List<MessageRequest> {
        val l = learningWordsService.getWords(telegramContext)
        if (l?.words?.isNotEmpty() == true) {
            return listOfNotNull(l.userResponse().toUserMessageRequest())
        }
        return emptyList()
    }

    override suspend fun forget(telegramContext: TelegramContext) {
        learningWordsService.deleteAll(telegramContext)
        logger.debug("Learning words has been cleared ${telegramContext.chatId}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DutchLongMemoryService::class.java)
    }
}