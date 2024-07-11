package io.github.artemptushkin.ai.assistants.telegram.conversation

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import io.github.artemptushkin.ai.assistants.telegram.Button
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties

fun postOnboardingButtons(telegramProperties: TelegramProperties): ReplyMarkup = KeyboardReplyMarkup(
    keyboard = listOf(
        telegramProperties.bot.buttons.map { KeyboardButton(it.text) }
    )
)
fun Message.chatId() = this.chat.id.toChat()

fun Long.toChat() = ChatId.fromId(this)

fun Message.isCommand(): Boolean = text?.startsWith("/") ?: false

fun Message.isBlockingButtonCommand(buttons: List<Button>): Boolean =
    isButtonCommand(buttons) && buttons.any { it.isBlocking }

fun Message.isButtonCommand(buttons: List<Button>): Boolean = if (this.text != null) {
    buttons.any { this.text == it.text }
} else false

