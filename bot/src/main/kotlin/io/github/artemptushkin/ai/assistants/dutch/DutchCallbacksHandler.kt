package io.github.artemptushkin.ai.assistants.dutch

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.ChatAction
import com.theokanning.openai.assistants.message.MessageRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.configuration.RunService
import io.github.artemptushkin.ai.assistants.telegram.CallbacksHandler
import io.github.artemptushkin.ai.assistants.telegram.OnboardingService
import io.github.artemptushkin.ai.assistants.telegram.TelegramHistoryService
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import io.github.artemptushkin.ai.assistants.telegram.conversation.toChat
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dutch")
class DutchCallbacksHandler(
    private val historyService: TelegramHistoryService,
    private val openAiService: OpenAiService,
    private val telegramProperties: TelegramProperties,
    private val onboardingService: OnboardingService,
): CallbacksHandler {

    override suspend fun handleCallback(bot: Bot, callbackQuery: CallbackQuery, runService: RunService) {
        val clientChat = callbackQuery.from.id.toChat()
        val data = callbackQuery.data
        if (data.isOnboardingCallback() && telegramProperties.bot.isOnboardingEnabled) {
            onboardingService.handleOnBoardingCallback(bot, callbackQuery)
        } else if (data.isRequestCallback()) {
            val assistantCallbackResponse = data.getRequestActionAssistantResponse()
            bot.sendMessage(clientChat, assistantCallbackResponse)
            bot.sendChatAction(clientChat, ChatAction.TYPING)
            val message = historyService.fetchMessageById(clientChat.id.toString(), callbackQuery.message?.messageId!!)
            val ch = historyService.fetchChatHistory(clientChat.id.toString())
            if (message != null) {
                openAiService.createMessage(ch?.threadId!!, MessageRequest.builder()
                    .role("user")
                    .content(data.getRequestActionUserImplicitPrompt(message.text!!))
                    .build())
                runService.createAndRun(bot, callbackQuery.message!!)
            } else {
                bot.sendMessage(clientChat, "I'm sorry I don't remember this message you clicked on")
            }
        }
    }
}