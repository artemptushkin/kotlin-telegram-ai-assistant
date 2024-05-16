package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/telegram")
class TelegramController(
    private val bot: Bot
) {

    @PostMapping("/webhook")
    suspend fun handleWebhook(@RequestBody telegramUpdate: String) {
        bot.processUpdate(telegramUpdate)
    }
}