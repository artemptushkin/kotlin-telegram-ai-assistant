package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import com.theokanning.openai.ListSearchParameters
import com.theokanning.openai.assistants.message.MessageRequest
import com.theokanning.openai.assistants.run.Run
import com.theokanning.openai.assistants.thread.Thread
import com.theokanning.openai.assistants.thread.ThreadRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import io.github.artemptushkin.ai.assistants.configuration.RunService
import io.github.artemptushkin.ai.assistants.telegram.conversation.ChatContext
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey.Companion.thread
import io.github.artemptushkin.ai.assistants.telegram.conversation.chatId
import io.github.artemptushkin.ai.assistants.telegram.conversation.isCommand
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
    private val environment: Environment
) {
    @Bean
    fun runsServiceDispatcher() = openAiRunsListenerDispatcher()

    @Bean
    fun telegramBot(
        openAiService: OpenAiService,
        @Qualifier("openAiFunctions")
        openAiFunctions: Map<String, OpenAiFunction>,
    ): Bot {
        val runService = RunService(openAiService, chatContext, telegramProperties, openAiFunctions, runsServiceDispatcher())
        val dispatcher = botCoroutineDispatcher()
        return bot {
            logLevel = LogLevel.Error
            token = telegramProperties.bot.token
            coroutineDispatcher = dispatcher
            if (environment.acceptsProfiles(Profiles.of("webhook"))) {
                webhook {
                    url = telegramProperties.webhook.url ?: throw IllegalStateException("telegramProperties.webhook.url is not defined") // to set secret token
                    secretToken = telegramProperties.webhook.secretToken ?: throw IllegalStateException("telegramProperties.webhook.token is not defined")
                    allowedUpdates = listOf("message")
                    maxConnections = 40
                }
            }
            dispatch {
                command("help") {
                    val chat = this.message.chatId()
                    bot.sendMessage(chat, telegramProperties.bot.helpMessage)
                }
                command("start") {
                    val chat = this.message.chatId()
                    bot.sendMessage(chat, "I'm happy to assist you, please type your prompt")
                }
                command("currentThread") {
                    val chat = this.message.chatId()
                    val thread = chatContext.get(thread(chat))
                    if (thread == null) {
                        bot.sendMessage(chat, "No current thread, create one with /thread")
                    } else {
                        thread as Thread
                        bot.sendMessage(chat, "Current thread is: ${thread.id}")
                    }
                }
                command("thread") {
                    val chat = this.message.chatId()
                    val thread = openAiService.createThread(ThreadRequest())
                    chatContext.save(thread(chat), thread)
                    logger.debug("Thread has been created ${thread.id}")
                    bot.sendMessage(chat, "Thread has been created ${thread.id}")
                }
                command("reset") {
                    val chat = this.message.chatId()
                    val thread = chatContext.get(thread(chat))
                    if (thread == null) {
                        bot.sendMessage(chat, "No current thread, create one with /thread")
                    } else {
                        thread as Thread
                        chatContext.get(ContextKey.run(chat))?.let {
                            openAiService.cancelRun(thread.id, (it as Run).id)
                            chatContext.delete(ContextKey.run(chat))
                        }
                        openAiService.deleteThread(thread.id)
                        chatContext.delete(thread(chat))
                        logger.debug("Thread has been deleted ${thread.id}")
                        bot.sendMessage(chat, "Thread has been deleted ${thread.id}")
                    }
                }
                command("run") {
                    runService.createAndRun(bot, message)
                }
                message {
                    val chat = this.message.chatId()
                    if (message.text != null && !message.isCommand()) {
                        val thread = chatContext.get(thread(chat))
                        bot.sendChatAction(chat, ChatAction.TYPING)
                        if (thread == null) {
                            logger.debug("Thread doesn't exist, creating a new one for user ${this.message.from?.id}")
                            val newThread = openAiService.createThread(
                                ThreadRequest
                                    .builder()
                                    .messages(
                                        listOf(
                                            MessageRequest
                                                .builder()
                                                .role("user")
                                                .content(this.message.text!!)
                                                .build()
                                        )
                                    )
                                    .build()
                            )
                            logger.debug("Thread created")
                            chatContext.save(thread(chat), newThread) // todo make it common with another command
                        } else {
                            thread as Thread
                            logger.debug("Creating a new message on the thread ${thread.id}")
                            try {
                                val openAiMessage = openAiService.createMessage(
                                    thread.id, MessageRequest
                                        .builder()
                                        .role("user")
                                        .content(this.message.text!!)
                                        .build()
                                )
                                logger.debug("Open AI message created: ${openAiMessage.id}")
                            } catch (e: Exception) {
                                logger.error("Handled exception during the create message processing: ${e.message}")
                                openAiService
                                    .listRuns(thread.id, ListSearchParameters())
                                    .getData()
                                    .filter { it.status == "queued" }
                                    .forEach {
                                        logger.warn("Cancelling the queued run ${it.id} as another message is coming")
                                        openAiService.cancelRun(thread.id, it.id)
                                    }
                                val openAiMessage = openAiService.createMessage(
                                    thread.id, MessageRequest
                                        .builder()
                                        .role("user")
                                        .content(this.message.text!!)
                                        .build()
                                )
                                logger.debug("Open AI message created after the previous run cancellation, messageid: ${openAiMessage.id}")
                            }
                        }
                        try {
                            runService.createAndRun(bot, message)
                        } catch (e: Exception) {
                            logger.error(e.message, e)
                            bot.sendMessage(chat, "Unexpected error handled during the process, please repeat the message. If it doesn't help send /reset command to start a new session with the assistant.")
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