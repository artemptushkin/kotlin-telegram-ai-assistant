package io.github.artemptushkin.ai.assistants

import com.github.kotlintelegrambot.Bot
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication
class AiTelegramAssistantsApplication

fun main(args: Array<String>) {
    runApplication<AiTelegramAssistantsApplication>(*args)
}

@Component
class ShutdownTelegramListener(
    private val bot: Bot
) {

    @PreDestroy
    fun onShutdown() {
        logger.warn("I remind telegram about the webhook so it keeps calling the webhook even if the response time was slow")
        bot.getWebhookInfo()
        logger.warn("telegram getWebhookInfo has been called")
    }

    companion object {
        val logger = LoggerFactory.getLogger(ShutdownTelegramListener::class.java)!!
    }
}
