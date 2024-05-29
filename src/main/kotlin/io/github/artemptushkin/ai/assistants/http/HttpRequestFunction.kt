package io.github.artemptushkin.ai.assistants.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory

class HttpRequestFunction(
    private val objectMapper: ObjectMapper,
    private val hostToHttpClients: Map<String, OpenAiHttpClient>
): OpenAiFunction {
    override fun handle(from: JsonNode): String {
        logger.debug("Proceeding with the proposed HTTP request request: $from")
        val apiRequest = objectMapper.treeToValue(from, ApiRequest::class.java) // todo handle exception here
        val httpUrl = apiRequest.url.toHttpUrl()
        val httpRequestFunction = hostToHttpClients[httpUrl.host] ?: throw IllegalStateException("I'm not allowed to request this server")
        return httpRequestFunction.execute(apiRequest)
    }

    override fun name(): String = "http-request"

    companion object {
        val logger = LoggerFactory.getLogger(HttpRequestFunction::class.java)!!
    }
}