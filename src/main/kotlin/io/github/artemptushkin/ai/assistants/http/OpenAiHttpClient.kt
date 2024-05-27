package io.github.artemptushkin.ai.assistants.http

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

class OpenAiHttpClient(
    val host: String,
    private val restClient: RestClient,
    private val defaultQueries: Map<String, String> = emptyMap()
) {
    fun execute(apiRequest: ApiRequest): String {
        try {
            val httpUrl = apiRequest.url.toHttpUrl()
            val uriWithQueries = apiRequest.uri(defaultQueries)
            return if (httpUrl.host == host) {
                restClient
                    .method(apiRequest.httpMethod())
                    .uri(uriWithQueries)
                    .also {
                        if (!apiRequest.headers.isNullOrEmpty()) {
                            it.headers { apiRequest.staticHeaders() }
                        }
                    }
                    .retrieve()
                    .body(String::class.java) ?: "No response body available"
                    .also {
                        logger.debug("Http Response received: $it")
                    }
            } else {
                logger.warn("Received unexpected host: ${httpUrl.host}")
                "Can not proceed with this operation code"
            }
        } catch (e: Exception) {
            logger.error("Exception handled during the HTTP call", e)
            return "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(OpenAiHttpClient::class.java)!!
    }
}
