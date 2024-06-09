package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.UpdateMapper
import com.github.kotlintelegrambot.network.serialization.GsonFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/telegram")
class TelegramController(
    private val bot: Bot,
    private val historyService: TelegramHistoryService,
) {

    private val updateMapper = UpdateMapper(GsonFactory.createForApiClient())

    @PostMapping("/webhook")
    suspend fun handleWebhook(@RequestBody telegramUpdate: String) = coroutineScope {
        updateMapper.jsonToUpdate(telegramUpdate)
            .let {
                val historySaveJob = async {
                    logger.debug("Saving the message update to the database")
                    it.message?.let { m -> historyService.saveOrAddMessage(m) }
                }
                launch {
                    logger.debug("Processing the update...")
                    bot.processUpdate(it)
                }
                historySaveJob.await().let {
                    logger.debug("The chat history update has been saved: ${it?.id}")
                }
            }
    }

    companion object {
        val logger = LoggerFactory.getLogger(TelegramController::class.java)!!
    }

}