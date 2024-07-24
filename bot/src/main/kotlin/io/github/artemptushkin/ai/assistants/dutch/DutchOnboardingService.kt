package io.github.artemptushkin.ai.assistants.dutch

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.openai.ThreadManagementService
import io.github.artemptushkin.ai.assistants.telegram.OnboardingDto
import io.github.artemptushkin.ai.assistants.telegram.OnboardingService
import io.github.artemptushkin.ai.assistants.telegram.TelegramHistoryService
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import io.github.artemptushkin.ai.assistants.telegram.conversation.ChatContext
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey
import io.github.artemptushkin.ai.assistants.telegram.conversation.postOnboardingButtons
import io.github.artemptushkin.ai.assistants.telegram.conversation.toChat
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dutch")
class DutchOnboardingService(
    private val openAiService: OpenAiService,
    private val chatContext: ChatContext,
    private val telegramProperties: TelegramProperties,
    private val threadManagementService: ThreadManagementService,
    private val historyService: TelegramHistoryService,
): OnboardingService {

    override fun onboardingInlineButtons(): ReplyMarkup? = dutchOnboardingInlineButtons()

    override suspend fun handleOnBoardingCallback(
        bot: Bot,
        callbackQuery: CallbackQuery,
    ) {
        val clientChat = callbackQuery.from.id.toChat()
        val data = callbackQuery.data
        val contextKey = ContextKey.onboardingKey(clientChat, callbackQuery.from.id)
        val onboardingDto = chatContext.get(contextKey) as OnboardingDto
        if (onboardingDto.isAllSet()) {
            bot.sendMessage(
                clientChat,
                "The onboarding has been already finished, if you willing to do it again please run /start command.",
                replyMarkup = postOnboardingButtons(telegramProperties)
            )
            return
        }
        if (data.isDifficultWordsSetting()) {
            logger.debug("Received callback query with words difficult setting, user id ${callbackQuery.from.id}")
            val words = data.getDifficultWordsNumber()
            onboardingDto.wordsNumber = words
            chatContext.save(contextKey, onboardingDto)
        }
        if (onboardingDto.isAllSet()) {
            chatContext.delete(contextKey)
            val initialPrompt =
                initialDutchLearnerPrompt(onboardingDto.wordsNumber!!) // todo it should come from bot configuration
            val chatHistory = historyService.fetchChatHistory(clientChat.id.toString())
            if (chatHistory?.threadId != null) {
                logger.debug("Deleting the thread on the finished onboarding process")
                openAiService.deleteThread(chatHistory.threadId)
            }
            threadManagementService.saveOnboardingThread(clientChat.id.toString(), chatHistory, initialPrompt)
            bot.sendMessage(
                clientChat,
                "You're onboarded! You can start learning by prompting the button or clicking on the keyboard buttons below.",
                replyMarkup = postOnboardingButtons(telegramProperties)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DutchOnboardingService::class.java)
    }
}