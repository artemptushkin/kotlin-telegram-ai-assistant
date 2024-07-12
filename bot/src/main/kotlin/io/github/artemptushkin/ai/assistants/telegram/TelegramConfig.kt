package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import com.theokanning.openai.ListSearchParameters
import com.theokanning.openai.OpenAiHttpException
import com.theokanning.openai.assistants.message.MessageRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.configuration.*
import io.github.artemptushkin.ai.assistants.openai.LongMemoryService
import io.github.artemptushkin.ai.assistants.openai.ThreadManagementService
import io.github.artemptushkin.ai.assistants.repository.toAssistantMessageRequest
import io.github.artemptushkin.ai.assistants.repository.toMessageRequest
import io.github.artemptushkin.ai.assistants.telegram.conversation.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    private val openAiProperties: OpenAiProperties,
    private val chatContext: ChatContext,
    private val environment: Environment,
    private val historyService: TelegramHistoryService,
    private val threadManagementService: ThreadManagementService,
    private val assistantMessageProcessor: AssistantMessageProcessor,
    private val contextFactory: ContextFactory,
    private val longMemoryService: LongMemoryService,
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
                contextFactory,
                openAiProperties,
                openAiFunctions,
                historyService,
                assistantMessageProcessor,
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
                    bot.sendMessageLoggingError(
                        chat,
                        "Hi! I will help you learn Dutch! Let’s choose your training level.",
                        replyMarkup = settingsInlineButtons()
                    )
                    chatContext.save(ContextKey.onboardingKey(chat, message.from!!.id), OnboardingDto())
                }
                callbackQuery {
                    val clientChat = callbackQuery.from.id.toChat()
                    val data = callbackQuery.data
                    if (data.isSettingsCallback()) {
                        val contextKey = ContextKey.onboardingKey(clientChat, callbackQuery.from.id)
                        val onboardingDto = chatContext.get(contextKey) as OnboardingDto
                        if (onboardingDto.isAllSet()) {
                            bot.sendMessage(
                                clientChat,
                                "The onboarding has been already finished, if you willing to do it again please run /start command.",
                                replyMarkup = postOnboardingButtons(telegramProperties)
                            )
                            return@callbackQuery
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
                            bot.sendMessage(clientChat, "You're onboarded! You can start learning by prompting the button or clicking on the keyboard buttons below.", replyMarkup = postOnboardingButtons(telegramProperties))
                        }
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
                command("currentThread") {
                    val chat = this.message.chatId()
                    val threadId = historyService.fetchCurrentThread(chat.id.toString())
                    if (threadId == null) {
                        bot.sendMessageLoggingError(chat, "No current thread, create one with /thread")
                    } else {
                        bot.sendMessageLoggingError(chat, "Current thread is: $threadId")
                    }
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
                        try {
                            openAiService.deleteThread(threadId)
                            logger.debug("Thread has been deleted $threadId")
                        } catch (e: OpenAiHttpException) {
                            logger.warn(e.message)
                        }
                        bot.sendMessageLoggingError(chat, "Thread has been deleted $threadId")
                    }
                    historyService.clearHistoryById(chat.id.toString())
                    logger.debug("Thread history has been cleared ${chat.id}")
                    longMemoryService.forget(contextFactory.buildContext(chat.id.toString()))
                }
                command("run") {
                    runService.createAndRun(bot, message)
                }
                message {
                    val chat = this.message.chatId()
                    val contextKey = ContextKey.onboardingKey(chat, message.from!!.id)
                    val onboardingDto = chatContext.get(contextKey)
                    if (onboardingDto != null && onboardingDto is OnboardingDto && !onboardingDto.isAllSet()) {
                        bot.sendMessage(
                            chat,
                            "You're in progress of the onboarding executed by the /start command. Please finish it"
                        )
                        return@message
                    }
                    val chatHistory = if (!environment.acceptsProfiles(Profiles.of("webhook"))) {
                        logger.debug("Polling is enabled, we save the message before processing it to preserve the chat history")
                        val history = historyService.saveOrAddMessage(this.message)
                        threadManagementService.deleteThreadIfAcceptable(chat.id.toString(), history)
                    } else {
                        val history = historyService.fetchChatHistory(chat.id.toString())
                        threadManagementService.deleteThreadIfAcceptable(chat.id.toString(), history)
                    }
                    if (message.text != null && !message.isCommand() && !message.isBlockingButtonCommand(telegramProperties.bot.buttons)) {
                        bot.sendChatAction(chat, ChatAction.TYPING)
                        val threadId = chatHistory?.threadId
                        if (threadId == null) {
                            val memoryMessages = longMemoryService.getMemory(contextFactory.buildContext(chat.id.toString()))
                            threadManagementService.saveThread(chat.id.toString(), chatHistory, message, memoryMessages)
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
                        val awaitContextKey = ContextKey.chatAwaitKey(chat, this.message.from!!.id)
                        if (chatContext.get(awaitContextKey) != null) {
                            chatContext.delete(awaitContextKey)
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
                    } else if (message.isBlockingButtonCommand(telegramProperties.bot.buttons)) {
                        val clientChat = this.message.chatId()
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
                            val assistantText = ButtonFunctions.buttonToFunction[message.text]!!
                            bot.sendMessage(clientChat, assistantText)
                            val openAiAssistantMessage = openAiService.createMessage(
                                chatHistory.threadId, assistantText.toAssistantMessageRequest()
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

    companion object {
        val logger = LoggerFactory.getLogger(TelegramConfiguration::class.java)!!
    }
}