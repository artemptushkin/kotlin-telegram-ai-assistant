package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.UpdateMapper
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.network.serialization.GsonFactory
import kotlinx.coroutines.channels.Channel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/telegram")
class TelegramController(
    private val bot: Bot,
    private val databaseUpdateChannel: Channel<Update>
) {

    private val updateMapper = UpdateMapper(GsonFactory.createForApiClient())

    @PostMapping("/webhook")
    suspend fun handleWebhook(@RequestBody telegramUpdate: String) {
        updateMapper.jsonToUpdate(telegramUpdate)
            .let {
                databaseUpdateChannel.send(it)
                bot.processUpdate(it)
            }
    }
}