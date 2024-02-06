package io.github.artemptushkin.ai.assistants.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

class HttpRequestFunction(
    private val objectMapper: ObjectMapper,
    private val host: String,
    private val restClient: RestClient
) : OpenAiFunction {
    override fun handle(from: String): String {
        try {
            logger.debug("Proceeding with the proposed HTTP request request: $from")
            val apiRequest = objectMapper.readValue<ApiRequest>(from)
            val httpUrl = apiRequest.url.toHttpUrl()
            return if (httpUrl.host == host) {
                restClient
                    .method(apiRequest.httpMethod())
                    .uri(apiRequest.uri())
                    .also {
                        if (!apiRequest.headers.isNullOrEmpty()) {
                            it.headers { apiRequest.staticHeaders() }
                        }
                    }
                    .retrieve()
                    .body(String::class.java) ?: "No response body available" // todo it should return full http response as a json
                    .also {
                        logger.debug("Http Response received: $it")
                    }
            } else {
                logger.warn("Received unexpected host: ${httpUrl.host}")
                "Can not proceed with this operation code"
            }
        } catch (e: Exception) {
            logger.error("Exception handled during the HTTP call", e)
            return "Exception handled during the HTTP call"
        }
    }

    override fun name(): String = "http-request"

    companion object {
        val logger = LoggerFactory.getLogger(HttpRequestFunction::class.java)!!
    }
}
