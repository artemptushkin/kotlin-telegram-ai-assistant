package io.github.artemptushkin.ai.assistants.gitlab

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.artemptushkin.ai.assistants.http.HttpRequestFunction
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(value = [
    GitlabProperties::class,
    AlphaVantageProperties::class
])
class GitlabConfig(
    private val gitlabProperties: GitlabProperties,
    private val alphaVantageProperties: AlphaVantageProperties,
) {

    @Bean
    fun gitlabRestClient(): RestClient = RestClient
        .builder()
        .baseUrl(gitlabProperties.host)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${gitlabProperties.token}")
        .build()

    @Bean
    fun gitlabHttpRequestFunction(objectMapper: ObjectMapper): HttpRequestFunction = HttpRequestFunction(
        objectMapper, gitlabProperties.host, gitlabRestClient()
    )

    @Bean
    fun alphaVantageClient(): RestClient = RestClient
        .builder()
        .baseUrl("https://${alphaVantageProperties.host}")
        .build()

    @Bean
    fun alphaVantageRequestFunction(objectMapper: ObjectMapper): HttpRequestFunction = HttpRequestFunction(
        objectMapper, alphaVantageProperties.host, alphaVantageClient(), mapOf(
            "apikey" to alphaVantageProperties.token
        )
    )
}

@ConfigurationProperties("gitlab")
data class GitlabProperties(
    val host: String,
    val token: String
)

@ConfigurationProperties("alpha-vantage")
data class AlphaVantageProperties(
    val host: String,
    val token: String
)