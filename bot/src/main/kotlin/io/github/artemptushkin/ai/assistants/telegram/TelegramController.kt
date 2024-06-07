package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.UpdateMapper
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.network.serialization.GsonFactory
import io.github.artemptushkin.ai.assistants.configuration.RunService
import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.repository.TelegramHistoryRepository
import io.github.artemptushkin.ai.assistants.telegram.conversation.chatId
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/telegram")
class TelegramController(
    private val bot: Bot,
    private val telegramHistoryRepository: TelegramHistoryRepository,
) {

    private val updateMapper = UpdateMapper(GsonFactory.createForApiClient())

    @PostMapping("/webhook")
    suspend fun handleWebhook(@RequestBody telegramUpdate: String) = coroutineScope {
        updateMapper.jsonToUpdate(telegramUpdate)
            .let {
                async {
                    logger.debug("Saving the update to the database")
                    return@async telegramHistoryRepository.save(ChatHistory(it.message?.chatId()?.id.toString())).awaitSingle()
                }
                    .let {
                        logger.debug("The update has been saved: ${it.id}")
                    }
                launch {
                    logger.debug("Processing the update...")
                    bot.processUpdate(it)
                }
            }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TelegramController::class.java)!!
    }

}