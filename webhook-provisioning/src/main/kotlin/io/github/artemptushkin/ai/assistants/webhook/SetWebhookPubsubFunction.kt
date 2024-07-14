package io.github.artemptushkin.ai.assistants.webhook

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Logger

class SetWebhookPubsubFunction : BackgroundFunction<PubsubMessage> {

    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    private val url = "https://api.telegram.org/bot${System.getenv("TELEGRAM_BOT_TOKEN")}/setWebhook"
    private val jsonPayload = """
        {
            "url": "${System.getenv("TELEGRAM_WEBHOOK_URL")}",
            "secret_token": "${System.getenv("WEBHOOK_TOKEN")}",
            "allowed_updates": ["message", "callback_query"],
            "max_connections": 80
        }
    """.trimIndent()

    override fun accept(message: PubsubMessage, context: Context) {
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        ).run {
            logger.info("Set webhook response: ${this.body()}")
        }
    }

    companion object {
        private val logger = Logger.getLogger(SetWebhookPubsubFunction::class.java.name)
    }
}

data class PubsubMessage(
    val data: String? = null
)