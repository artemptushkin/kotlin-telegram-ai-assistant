package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.types.TelegramBotResult
import org.slf4j.LoggerFactory

fun Bot.sendMessageLoggingError(
    chat: ChatId.Id,
    message: String,
    parseMode: ParseMode? = null,
    replyMarkup: ReplyMarkup? = null
): TelegramBotResult<Message> {
    return this.sendMessage(chat, message, parseMode = parseMode, replyMarkup = replyMarkup)
        .onError { error ->
            when (error) {
                is TelegramBotResult.Error.TelegramApi -> logger.error("${error.javaClass.simpleName}: ${error.errorCode}, description: ${error.description}")
                is TelegramBotResult.Error.HttpError -> logger.error("${error.javaClass.simpleName}: code: ${error.httpCode}, description: ${error.description}")
                is TelegramBotResult.Error.InvalidResponse -> logger.error("${error.javaClass.simpleName}: ${error.httpCode}, statusMessage: ${error.httpStatusMessage}, body.result: ${error.body?.result}, body.errorCode: ${error.body?.errorCode}, body.errorDescription: ${error.body?.errorDescription}")
                is TelegramBotResult.Error.Unknown -> logger.error(error.javaClass.simpleName, error.exception)
            }
        }
}

fun Bot.sendMessageMarkdownOrPlain(chat: ChatId.Id, message: String): TelegramBotResult<Message> {
    val escapedMessage = escapeMarkdownV2Symbols.fold(message) { acc, symbol ->
        acc.replace(symbol.toString(), "\\$symbol")
    }
    val messageResult = sendMessageLoggingError(
        chat, escapedMessage,
        ParseMode.MARKDOWN_V2
    )
    return if (messageResult.isError) {
        this.sendMessage(chat, message)
    } else {
        messageResult
    }
}

private val escapeMarkdownV2Symbols = listOf('_', '-', "*", "(", ".", "!", "[", "`")

private val logger = LoggerFactory.getLogger("io.github.artemptushkin.ai.assistants.telegram.BotHelper")