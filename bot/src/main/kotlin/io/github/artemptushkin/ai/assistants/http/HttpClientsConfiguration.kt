package io.github.artemptushkin.ai.assistants.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import java.util.Base64

@Configuration
@EnableConfigurationProperties(
    value = [
        HttpClientsProperties::class
    ]
)
class HttpClientsConfiguration(
    private val httpClientsProperties: HttpClientsProperties,
) {

    @Bean
    fun httpRequestFunction(
        objectMapper: ObjectMapper,
        hostToHttpClients: Map<String, OpenAiHttpClient>
    ): HttpRequestFunction =
        HttpRequestFunction(
            objectMapper, hostToHttpClients
        )

    @Bean
    fun hostToHttpClients(objectMapper: ObjectMapper): Map<String, OpenAiHttpClient> = httpClientsProperties
        .clients
        .map { entry ->
            val clientProperties = entry.value
            OpenAiHttpClient(clientProperties.host, RestClient
                .builder()
                .baseUrl(clientProperties.baseUrl())
                .also { builder ->
                    clientProperties.authorizationBearer?.let {
                        builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $it")
                    }
                    clientProperties.basicAuth?.let {
                        builder.defaultHeader(HttpHeaders.AUTHORIZATION, it.basicAuth())
                    }
                }
                .build(),
                clientProperties.defaultQueries?.associate { query ->
                    query.key to query.value
                } ?: emptyMap()
            )
        }
        .associateBy { it.host }
}

@ConfigurationProperties("http-clients")
data class HttpClientsProperties(
    val clients: Map<String, HttpClientProperties>
)

data class HttpClientProperties(
    val host: String,
    val authorizationBearer: String? = null,
    val basicAuth: BasicAuth? = null,
    val defaultQueries: List<DefaultQueriesProperties> = emptyList(),
) {
    fun baseUrl() = "https//${host}"
}

data class BasicAuth(
    val username: String,
    val password: String,
) {
    fun basicAuth() = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"
}

data class DefaultQueriesProperties(
    val key: String,
    val value: String,
)