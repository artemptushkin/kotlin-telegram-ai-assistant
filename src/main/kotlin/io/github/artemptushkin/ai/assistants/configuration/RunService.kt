package io.github.artemptushkin.ai.assistants.configuration

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.theokanning.openai.assistants.message.MessageListSearchParameters
import com.theokanning.openai.assistants.run.Run
import com.theokanning.openai.assistants.run.RunCreateRequest
import com.theokanning.openai.assistants.run.SubmitToolOutputRequestItem
import com.theokanning.openai.assistants.run.SubmitToolOutputsRequest
import com.theokanning.openai.assistants.thread.Thread
import com.theokanning.openai.service.OpenAiService
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import io.github.artemptushkin.ai.assistants.telegram.conversation.ChatContext
import io.github.artemptushkin.ai.assistants.telegram.conversation.ContextKey
import io.github.artemptushkin.ai.assistants.telegram.conversation.toChat
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

class RunService(
    private val openAiService: OpenAiService,
    private val chatContext: ChatContext,
    private val telegramProperties: TelegramProperties,
    private val openAiFunctions: Map<String, OpenAiFunction>,
    runsServiceDispatcher: CoroutineDispatcher,
) {

    private val coroutineScope = CoroutineScope(runsServiceDispatcher)

    suspend fun createAndRun(bot: Bot, message: Message) {
        val chat = message.chat.id.toChat()
        val thread = chatContext.get(ContextKey.thread(chat))
        bot.sendChatAction(chat, ChatAction.TYPING)
        if (thread == null) {
            bot.sendMessage(chat, "No current thread, create one with /thread")
        } else {
            thread as Thread
            val run = openAiService.createRun(
                thread.id, RunCreateRequest
                    .builder()
                    .assistantId(telegramProperties.bot.assistantId)
                    .build()
            ) // todo it can shoot timeout from here
            chatContext.save(ContextKey.run(chat), run)
            logger.info("Run ${run.id} has been created on the thread ${thread.id}")
            bot.sendChatAction(chat, ChatAction.TYPING)
        }
        coroutineScope.launch {
            val attempt = AtomicInteger(0)
            async {
                while (isActive && attempt.get() < MAX_ATTEMPTS) {
                    if (attempt.get() + 1 == MAX_ATTEMPTS) {
                        bot.sendMessage(
                            chat,
                            "Proceeding with the last attempt out of $MAX_ATTEMPTS to get an answer from the assistant..."
                        )
                    }
                    delay(Duration.parse("3s"))
                    bot.sendChatAction(chat, ChatAction.TYPING)
                    val currentAttempt = attempt.getAndIncrement()
                    logger.debug("Executing attempt number $currentAttempt")
                    val currentThread = chatContext.get(ContextKey.thread(chat)) as Thread // todo is null check
                    val storedRun = chatContext.get(ContextKey.run(chat)) as Run // todo is null check
                    logger.info("Retrieving run ${storedRun.id} on the thread ${currentThread.id}")
                    openAiService
                        .retrieveRun(currentThread.id, storedRun.id)
                        .also { run ->
                            logger.info("Received run status: ${run.status}")
                            when (run.status) {
                                "completed" -> {
                                    openAiService
                                        .listMessages(currentThread.id, MessageListSearchParameters())
                                        .data
                                        .filter { it.runId == run.id }
                                        .flatMap { it.content }
                                        .map { it.text.value }
                                        .forEach {
                                            bot.sendMessage(chat, it, parseMode = ParseMode.MARKDOWN)
                                        }
                                        .also { this.cancel() }
                                }

                                "failed" -> {
                                    openAiService
                                        .listMessages(currentThread.id, MessageListSearchParameters())
                                        .data
                                        .filter { it.runId == run.id }
                                        .sortedBy { it.createdAt }
                                        .flatMap { it.content }
                                        .map { it.text.value }
                                        .forEach {
                                            bot.sendMessage(chat, it, parseMode = ParseMode.MARKDOWN)
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
                                                        val result = it.handle(rawArgs)
                                                        logger.info("Submitting tool outputs, thread: ${currentThread.id}, run: ${run.id}")
                                                        SubmitToolOutputRequestItem
                                                            .builder()
                                                            .toolCallId(toolCall.id)
                                                            .output(result)
                                                            .build()
                                                    } else {
                                                        logger.error("Received an unknown function - ${toolFunction.name}, please implement it")
                                                        bot.sendMessage(
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
                                            currentThread.id, run.id, SubmitToolOutputsRequest
                                                .builder()
                                                .toolOutputs(it)
                                                .build()
                                        )
                                    }
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
        const val MAX_ATTEMPTS = 10
    }
}