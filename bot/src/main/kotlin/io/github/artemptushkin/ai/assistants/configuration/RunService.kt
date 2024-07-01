package io.github.artemptushkin.ai.assistants.configuration

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.Message
import com.theokanning.openai.assistants.message.MessageListSearchParameters
import com.theokanning.openai.assistants.run.Run
import com.theokanning.openai.assistants.run.RunCreateRequest
import com.theokanning.openai.assistants.run.SubmitToolOutputRequestItem
import com.theokanning.openai.assistants.run.SubmitToolOutputsRequest
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.telegram.TelegramHistoryService
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import io.github.artemptushkin.ai.assistants.telegram.conversation.ChatContext
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey
import io.github.artemptushkin.ai.assistants.telegram.conversation.toChat
import io.github.artemptushkin.ai.assistants.telegram.sendMessageLoggingError
import io.github.artemptushkin.ai.assistants.telegram.sendMessageMarkdownOrPlain
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

class RunService(
    private val openAiService: OpenAiService,
    private val chatContext: ChatContext,
    private val telegramProperties: TelegramProperties,
    private val openAiFunctions: Map<String, OpenAiFunction>,
    private val historyService: TelegramHistoryService,
    runsServiceDispatcher: CoroutineDispatcher,
) {

    private val coroutineScope = CoroutineScope(runsServiceDispatcher)

    suspend fun createAndRun(bot: Bot, message: Message) {
        val chat = message.chat.id.toChat()
        val currentThreadId = historyService.fetchCurrentThread(chat.id.toString())
        bot.sendChatAction(chat, ChatAction.TYPING)
        if (currentThreadId == null) {
            bot.sendMessageLoggingError(chat, "No current thread, create one with /thread")
        } else {
            val run = openAiService.createRun(
                currentThreadId, RunCreateRequest
                    .builder()
                    .assistantId(telegramProperties.bot.assistantId)
                    .build()
            ) // todo it can shoot timeout from here
            chatContext.save(ContextKey.run(chat), run)
            logger.info("Run ${run.id} has been created on the thread $currentThreadId")
            bot.sendChatAction(chat, ChatAction.TYPING)
        }
        coroutineScope.launch {
            val attempt = AtomicInteger(0)
            async {
                while (isActive && attempt.get() < MAX_ATTEMPTS) {
                    if (attempt.get() + 1 == MAX_ATTEMPTS) {
                        bot.sendMessageLoggingError(
                            chat,
                            "Proceeding with the last attempt out of $MAX_ATTEMPTS to get an answer from the assistant..."
                        )
                    }
                    delay(Duration.parse("1s"))
                    bot.sendChatAction(chat, ChatAction.TYPING)
                    val currentAttempt = attempt.getAndIncrement()
                    logger.debug("Executing attempt number $currentAttempt")
                    val storedRun = (chatContext.get(ContextKey.run(chat)) ?: run {
                        logger.debug("Run doesn't exist during the run process")
                        this.cancel()
                    }) as Run
                    logger.info("Retrieving run ${storedRun.id} on the thread $currentThreadId")
                    openAiService
                        .retrieveRun(currentThreadId, storedRun.id)
                        .also { run ->
                            logger.info("Received run status: ${run.status}")
                            when (run.status) {
                                "completed" -> {
                                    logger.debug("Run ${run.id} has bean completed, ${run.usage}")
                                    openAiService
                                        .listMessages(currentThreadId, MessageListSearchParameters())
                                        .data
                                        .filter { it.runId == run.id }
                                        .flatMap { it.content }
                                        .map { it.text.value }
                                        .forEach {
                                            it.chunkToTelegramMessageLimits()
                                                .forEach { message ->
                                                    val sendMessageResult = bot.sendMessageMarkdownOrPlain(chat, message)
                                                    logger.debug("Saving assistant message")
                                                    historyService.saveOrAddMessage(
                                                        sendMessageResult.get(),
                                                        "assistant"
                                                    )
                                                }
                                        }
                                        .also { this.cancel() }
                                }

                                "failed" -> {
                                    openAiService
                                        .listMessages(currentThreadId, MessageListSearchParameters())
                                        .data
                                        .filter { it.runId == run.id }
                                        .sortedBy { it.createdAt }
                                        .flatMap { it.content }
                                        .map { it.text.value }
                                        .forEach {
                                            it.chunkToTelegramMessageLimits()
                                                .forEach { message ->
                                                    val sendMessageResult = bot.sendMessageLoggingError(chat, message)
                                                    logger.debug("Saving assistant message")
                                                    historyService.saveOrAddMessage(
                                                        sendMessageResult.get(),
                                                        "assistant"
                                                    )
                                                }
                                        }
                                        .also { this.cancel() }
                                }

                                "in_progress" -> {
                                    bot.sendChatAction(chat, ChatAction.TYPING)
                                }

                                "requires_action" -> {
                                    bot.sendChatAction(chat, ChatAction.TYPING)
                                    run.requiredAction.submitToolOutputs.toolCalls.map { toolCall ->
                                        if (toolCall.type == "function") {
                                            val toolFunction = toolCall.function
                                            val rawArgs = toolFunction.arguments
                                            openAiFunctions[toolFunction.name]
                                                .let {
                                                    if (it != null) {
                                                        logger.debug("Executing function '${it.name()}'")
                                                        val result = it.handle(rawArgs, TelegramContext(telegramProperties.bot.token.substringBefore(":"), chat.id.toString(), hashMapOf("language" to "Dutch")))
                                                        logger.info("Submitting tool outputs, thread: $currentThreadId, run: ${run.id}")
                                                        SubmitToolOutputRequestItem
                                                            .builder()
                                                            .toolCallId(toolCall.id)
                                                            .output(result)
                                                            .build()
                                                    } else {
                                                        logger.error("Received an unknown function - ${toolFunction.name}, please implement it")
                                                        bot.sendMessageLoggingError(
                                                            chat,
                                                            "Received an unknown function - ${toolFunction.name}, please ask the administrator to implement it, see /help for info"
                                                        )
                                                        null
                                                    }
                                                }
                                        } else {
                                            null
                                        }
                                    }.let {
                                        openAiService.submitToolOutputs(
                                            currentThreadId, run.id, SubmitToolOutputsRequest
                                                .builder()
                                                .toolOutputs(it)
                                                .build()
                                        )
                                    }
                                }

                                "cancelled" -> {
                                    logger.debug("The run ${run.id} has been cancelled for the chat $chat")
                                }

                                else -> {}
                            }
                        }
                }
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(RunService::class.java)!!
        const val MAX_ATTEMPTS = 50
    }
}