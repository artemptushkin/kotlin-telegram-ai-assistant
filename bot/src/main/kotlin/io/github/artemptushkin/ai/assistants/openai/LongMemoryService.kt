package io.github.artemptushkin.ai.assistants.openai

import com.theokanning.openai.assistants.message.MessageRequest
import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

interface LongMemoryService {

    suspend fun getMemory(telegramContext: TelegramContext): List<MessageRequest>
    suspend fun forget(telegramContext: TelegramContext)
}

@Service
@ConditionalOnMissingBean(LongMemoryService::class)
class NoopMemoryService : LongMemoryService {
    override suspend fun getMemory(telegramContext: TelegramContext): List<MessageRequest> = emptyList()
    override suspend fun forget(telegramContext: TelegramContext) {}
}