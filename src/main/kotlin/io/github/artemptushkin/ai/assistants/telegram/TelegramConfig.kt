package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.logging.LogLevel
import com.theokanning.openai.service.OpenAiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors


fun botCoroutineDispatcher(): CoroutineDispatcher = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
)
    .asCoroutineDispatcher()

@Configuration
@EnableConfigurationProperties(value = [TelegramProperties::class])
class TelegramConfiguration(
    private val telegramProperties: TelegramProperties,
    private val openAiService: OpenAiService
) {

    @Bean
    fun telegramBot(): Bot {
        return bot {
            logLevel = LogLevel.Error
            token = telegramProperties.bot.token
            coroutineDispatcher = botCoroutineDispatcher()
            dispatch {
                message {
                  //  openAiService.createThread()
                }
            }
        }.apply {
            this.startPolling()
        }
    }
}

@ConfigurationProperties("telegram")
data class TelegramProperties(
    val bot: BotProperties = BotProperties()
)

data class BotProperties(
    val token: String = "",
    val helpMessage: String = ""
)