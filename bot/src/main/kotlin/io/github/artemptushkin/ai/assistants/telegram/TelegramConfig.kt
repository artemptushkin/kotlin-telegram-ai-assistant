package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import com.theokanning.openai.ListSearchParameters
import com.theokanning.openai.assistants.message.MessageRequest
import com.theokanning.openai.assistants.thread.ThreadRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.OnboardingDto
import io.github.artemptushkin.ai.assistants.configuration.*
import io.github.artemptushkin.ai.assistants.repository.ChatMessage
import io.github.artemptushkin.ai.assistants.repository.toMessage
import io.github.artemptushkin.ai.assistants.repository.toMessageRequest
import io.github.artemptushkin.ai.assistants.telegram.conversation.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import java.util.concurrent.Executors


fun botCoroutineDispatcher(): CoroutineDispatcher = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
).asCoroutineDispatcher()

fun openAiRunsListenerDispatcher(): CoroutineDispatcher = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
).asCoroutineDispatcher()

@Configuration
@EnableConfigurationProperties(value = [TelegramProperties::class])
class TelegramConfiguration(
    private val telegramProperties: TelegramProperties,
    private val chatContext: ChatContext,
    private val environment: Environment,
    private val historyService: TelegramHistoryService,
) {
    @Bean
    fun runsServiceDispatcher() = openAiRunsListenerDispatcher()

    @Bean
    fun telegramBot(
        openAiService: OpenAiService,
        @Qualifier("openAiFunctions")
        openAiFunctions: Map<String, OpenAiFunction>,
    ): Bot {
        val runService =
            RunService(
                openAiService,
                chatContext,
                telegramProperties,
                openAiFunctions,
                historyService,
                runsServiceDispatcher()
            )
        val dispatcher = botCoroutineDispatcher()
        return bot {
            logLevel = LogLevel.Error
            token = telegramProperties.bot.token
            coroutineDispatcher = dispatcher
            if (environment.acceptsProfiles(Profiles.of("webhook"))) {
                webhook {
                    url = telegramProperties.webhook.url
                        ?: throw IllegalStateException("telegramProperties.webhook.url is not defined") // to set secret token
                    secretToken = telegramProperties.webhook.secretToken
                        ?: throw IllegalStateException("telegramProperties.webhook.token is not defined")
                    allowedUpdates = listOf("message")
                    maxConnections = 80
                    createOnStart = false
                }
            }
            dispatch {
                command("help") {
                    val chat = this.message.chatId()
                    bot.sendMessageLoggingError(chat, telegramProperties.bot.helpMessage)
                }
                command("start") {
                    val chat = this.message.chatId()
                    bot.sendMessageLoggingError(chat,
                        "Hi! I will help you learn Dutch! Let’s choose your training level.",
                        replyMarkup = KeyboardReplyMarkup(
                            keyboard = listOf(
                                buttons.map { KeyboardButton(it) }
                            )
                        ))
                    chatContext.save(ContextKey.onboardingKey(chat, message.from!!.id), OnboardingDto())
                }
                callbackQuery {
                    val clientChat = callbackQuery.from.id.toChat()
                    val data = callbackQuery.data
                    val contextKey = ContextKey.onboardingKey(clientChat, callbackQuery.from.id)
                    val onboardingDto = chatContext.get(contextKey) as OnboardingDto
                    if (onboardingDto.isAllSet()) {
                        bot.sendMessage(clientChat, "The onboarding has been finished, if you willing to do it again please run /start command.")
                        return@callbackQuery
                    }
                    if (data.isDifficultWordsSetting()) {
                        logger.debug("Received callback query with words difficult setting, user id ${callbackQuery.from.id}")
                        val words = data.getDifficultWordsNumber()
                        onboardingDto.wordsNumber = words
                        chatContext.save(contextKey, onboardingDto)
                    }
                    if (onboardingDto.isAllSet()) {
                        val initialPrompt = initialDutchLearnerPrompt(onboardingDto.wordsNumber!!) // todo it should come from bot configuration
                        /*
                         todo it should
                         * delete the current thread if exists
                         * create a new thread with one single initialPrompt message
                         * set the id of the created thread to the history record
                         * set the initialPrompt to the history record
                         */
                        bot.sendMessage(clientChat, "Thank you, the onboarding has been finished and our conversation is ready for work. You can proceed with buttons below in the menu or by prompting me")
                    }
                }
                command("currentThread") {
                    val chat = this.message.chatId()
                    val threadId = historyService.fetchCurrentThread(chat.id.toString())
                    if (threadId == null) {
                        bot.sendMessageLoggingError(chat, "No current thread, create one with /thread")
                    } else {
                        bot.sendMessageLoggingError(chat, "Current thread is: $threadId")
                    }
                }
                command("thread") {
                    val chat = this.message.chatId()
                    val thread = openAiService.createThread(ThreadRequest())
                    historyService.saveThread(chat.id.toString(), mutableListOf(), thread)
                    logger.debug("Thread has been created ${thread.id}")
                    bot.sendMessageLoggingError(chat, "Thread has been created ${thread.id}")
                }
                command("reset") {
                    val chat = this.message.chatId()
                    val threadId = historyService.fetchCurrentThread(chat.id.toString())
                    if (threadId == null) {
                        bot.sendMessageLoggingError(chat, "No current thread, create one with /thread")
                    } else {
                        chatContext.get(ContextKey.run(chat))?.let {
                            try {
                                openAiService
                                    .listRuns(threadId, ListSearchParameters())
                                    .getData()
                                    .filter { it.status == "queued" }
                                    .forEach {
                                        logger.warn("Cancelling the queued run ${it.id}")
                                        openAiService.cancelRun(threadId, it.id)
                                    }
                                chatContext.delete(ContextKey.run(chat))
                            } catch (e: Exception) {
                                logger.error("Exception during removal of runs ${e.message}", e)
                            }
                        }
                        openAiService.deleteThread(threadId)
                        logger.debug("Thread has been deleted $threadId")
                        bot.sendMessageLoggingError(chat, "Thread has been deleted $threadId")
                    }
                    historyService.clearHistoryById(chat.id.toString())
                    logger.debug("Thread history has been cleared ${chat.id}")
                }
                command("run") {
                    runService.createAndRun(bot, message)
                }
                message {
                    val chat = this.message.chatId()
                    val chatHistory = if (!environment.acceptsProfiles(Profiles.of("webhook"))) {
                        logger.debug("Polling is enabled, we save the message before processing it to preserve the chat history")
                        historyService.saveOrAddMessage(this.message)
                    } else {
                        historyService.fetchChatHistory(chat.id.toString())
                    }
                    if (message.text != null && !message.isCommand() && !message.isButtonCommand()) {
                        bot.sendChatAction(chat, ChatAction.TYPING)
                        val threadId = chatHistory?.threadId
                        if (threadId == null) {
                            if (chatHistory == null) {
                                logger.debug("Thread doesn't exist, creating a new one for user ${this.message.from?.id} using current message as initial history")
                                val newThread = openAiService.createThread(
                                    ThreadRequest
                                        .builder()
                                        .messages(listOf(this.message.toMessageRequest("user")))
                                        .build()
                                )
                                historyService.saveThread(
                                    chat.id.toString(),
                                    mutableListOf(this.message to "user"),
                                    newThread
                                )
                                logger.debug("Thread created")
                            } else {
                                logger.debug("Thread doesn't exist, creating a new one for user ${this.message.from?.id} attaching the history")
                                val messagesToBeSaved =
                                    populateCurrentMessageIfNotExists(chatHistory.messages, this.message)
                                        ?.map { it.toMessageRequest() }
                                val newThread = openAiService.createThread(
                                    ThreadRequest
                                        .builder()
                                        .messages(listOf(this.message.toMessageRequest("user")))
                                        .build()
                                )
                                historyService.saveThread(chatHistory, newThread)
                                logger.debug("Thread created")
                            }
                        } else {
                            logger.debug("Creating a new message on the existent thread $threadId")
                            try {
                                openAiService
                                    .listRuns(threadId, ListSearchParameters())
                                    .getData()
                                    .filter { it.status == "queued" || it.status == "in_progress" || it.status == "requires_action" || it.status == "cancelling" } // todo refactor here
                                    .forEach {
                                        logger.warn("Cancelling the active run ${it.id} as another message is coming")
                                        openAiService.cancelRun(threadId, it.id)
                                    }
                                val openAiMessage = openAiService.createMessage(
                                    threadId, this.message.toMessageRequest("user")
                                )
                                logger.debug("Open AI message created: ${openAiMessage.id}")
                            } catch (e: Exception) {
                                logger.error("Handled exception during the create message processing: ${e.message}")
                            }
                        }
                        val contextKey = ContextKey.chatAwaitKey(chat, this.message.from!!.id)
                        if (chatContext.get(contextKey) != null) {
                            chatContext.delete(contextKey)
                        }
                        try {
                            runService.createAndRun(bot, message)
                        } catch (e: Exception) {
                            logger.error(e.message, e)
                            bot.sendMessageLoggingError(
                                chat,
                                "Unexpected error handled during the process, please repeat the message. If it doesn't help send /reset command to start a new session with the assistant."
                            )
                        }
                    } else if (message.isButtonCommand()) {
                        val clientChat = this.message.chatId()
                        val chatHistory = historyService.fetchChatHistory(chat.id.toString())
                        if (chatHistory?.threadId == null) {
                            bot.sendMessageLoggingError(
                                chat,
                                "I'm sorry but I don't remember what we talked about. Please start from the beginning with /start"
                            )
                        } else {
                            val openAiMessage = openAiService.createMessage(
                                chatHistory.threadId, this.message.toMessageRequest("user")
                            )
                            chatContext.save(
                                ContextKey.chatAwaitKey(clientChat, this.message.from!!.id),
                                message.text!!
                            )
                            logger.debug("Open AI message created: ${openAiMessage.id}")
                            val assistantText = if (message.text == "Add words") {
                                "Please type the list of words to add"
                            } else {
                                "Please type the list of words to delete"
                            }
                            bot.sendMessage(clientChat, assistantText)
                            val openAiAssistantMessage = openAiService.createMessage(
                                chatHistory.threadId, MessageRequest.MessageRequestBuilder()
                                    .role("assistant")
                                    .content(assistantText)
                                    .build()
                            )
                            logger.debug("Open AI assistant message has been created: ${openAiAssistantMessage.id}")
                        }
                    }
                }
            }
        }.apply {
            if (environment.acceptsProfiles(Profiles.of("webhook"))) {
                logger.info("Starting telegram webhooks...")
                this.startWebhook()
            } else {
                logger.info("Starting telegram polling...")
                this.startPolling()
            }
        }
    }

    private fun populateCurrentMessageIfNotExists(
        savedMessages: MutableList<ChatMessage>?,
        message: Message
    ): MutableList<ChatMessage>? {
        if (savedMessages != null && savedMessages.none { it.id == message.messageId }) {
            savedMessages.add(message.toMessage())
        }
        return savedMessages
    }

    companion object {
        val logger = LoggerFactory.getLogger(TelegramConfiguration::class.java)!!
    }
}

@ConfigurationProperties("telegram")
data class TelegramProperties(
    val bot: BotProperties = BotProperties(),
    val webhook: WebHookProperties = WebHookProperties()
)

data class WebHookProperties(
    val url: String? = null,
    val secretToken: String? = null
)

data class BotProperties(
    val token: String = "",
    val helpMessage: String = "",
    val assistantId: String = ""
)