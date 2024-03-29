package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.logging.LogLevel
import com.theokanning.openai.ListSearchParameters
import com.theokanning.openai.messages.MessageRequest
import com.theokanning.openai.service.OpenAiService
import com.theokanning.openai.threads.Thread
import com.theokanning.openai.threads.ThreadRequest
import io.github.artemptushkin.ai.assistants.configuration.RunService
import io.github.artemptushkin.ai.assistants.telegram.conversation.ChatContext
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey.Companion.thread
import io.github.artemptushkin.ai.assistants.telegram.conversation.isCommand
import io.github.artemptushkin.ai.assistants.telegram.conversation.toChat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    private val openAiService: OpenAiService,
    private val chatContext: ChatContext,
    private val runService: RunService
) {

    @Bean
    fun runsServiceDispatcher() = openAiRunsListenerDispatcher()

    @Bean
    fun telegramBot(): Bot {
        val dispatcher = botCoroutineDispatcher()
        return bot {
            logLevel = LogLevel.Error
            token = telegramProperties.bot.token
            coroutineDispatcher = dispatcher
            dispatch {
                command("start") {
                    val chat = this.message.chat.id.toChat()
                    bot.sendMessage(chat, "I'm happy to assist you, please type your prompt")
                }
                command("currentThread") {
                    val chat = this.message.chat.id.toChat()
                    val thread = chatContext.get(thread(chat))
                    if (thread == null) {
                        bot.sendMessage(chat, "No current thread, create one with /thread")
                    } else {
                        thread as Thread
                        bot.sendMessage(chat, "Current thread is: ${thread.id}")
                    }
                }
                command("thread") {
                    val chat = this.message.chat.id.toChat()
                    val thread = openAiService.createThread(ThreadRequest())
                    chatContext.save(thread(chat), thread)
                    logger.debug("Thread has been created ${thread.id}")
                    bot.sendMessage(chat, "Thread has been created ${thread.id}")
                }
                command("reset") {
                    val chat = this.message.chat.id.toChat()
                    val thread = chatContext.get(thread(chat))
                    if (thread == null) {
                        bot.sendMessage(chat, "No current thread, create one with /thread")
                    } else {
                        thread as Thread
                        openAiService.deleteThread(thread.id)
                        logger.debug("Thread has been deleted ${thread.id}")
                        bot.sendMessage(chat, "Thread has been deleted ${thread.id}")
                    }
                }
                command("run") {
                    runService.createAndRun(bot, message)
                }
                message {
                    val chat = this.message.chat.id.toChat()
                    if (message.text != null && !message.isCommand()) {
                        val thread = chatContext.get(thread(chat))
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
                        runService.createAndRun(bot, message)
                    }
                }
            }
        }.apply {
            this.startPolling()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TelegramConfiguration::class.java)!!
    }
}

@ConfigurationProperties("telegram")
data class TelegramProperties(
    val bot: BotProperties = BotProperties()
)

data class BotProperties(
    val token: String = "",
    val helpMessage: String = "",
    val assistantId: String = ""
)