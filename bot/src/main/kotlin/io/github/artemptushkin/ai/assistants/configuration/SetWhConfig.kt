package io.github.artemptushkin.ai.assistants.configuration

import com.github.kotlintelegrambot.Bot
import io.github.artemptushkin.ai.assistants.telegram.TelegramProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class SetWhConfig(
    private val bot: Bot,
    private val telegramProperties: TelegramProperties
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

   // @Scheduled(fixedRate = 5 * 60 * 1000L) // 5 minutes in milliseconds
    fun scheduleTaskWithCoroutine() {
        scope.launch {
            logger.info("Running set wh task")
            bot.setWebhook(
                url = telegramProperties.webhook.url ?: throw IllegalStateException("telegramProperties.webhook.url is not defined"), // to set secret token,
                secretToken = telegramProperties.webhook.secretToken ?: throw IllegalStateException("telegramProperties.webhook.token is not defined"),
                allowedUpdates = listOf("message")
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(SetWhConfig::class.java)!!
    }
}