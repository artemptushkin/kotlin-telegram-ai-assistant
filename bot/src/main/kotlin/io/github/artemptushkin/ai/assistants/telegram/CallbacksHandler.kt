package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.CallbackQuery
import io.github.artemptushkin.ai.assistants.configuration.RunService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

interface CallbacksHandler {
    suspend fun handleCallback(bot: Bot, callbackQuery: CallbackQuery, runService: RunService)
}

@Service
@ConditionalOnMissingBean(CallbacksHandler::class)
class NoopCallbacksHandler(
    private val telegramProperties: TelegramProperties,
) : CallbacksHandler {
    override suspend fun handleCallback(bot: Bot, callbackQuery: CallbackQuery, runService: RunService) {
        logger.warn("Callback has been received in with no callback handler implementation, bot ${telegramProperties.bot.id()}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NoopCallbacksHandler::class.java)
    }

}