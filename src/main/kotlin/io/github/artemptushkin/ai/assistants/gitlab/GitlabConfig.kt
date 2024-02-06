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
@EnableConfigurationProperties(GitlabProperties::class)
class GitlabConfig(private val gitlabProperties: GitlabProperties) {

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
}

@ConfigurationProperties("gitlab")
data class GitlabProperties(
    val host: String,
    val token: String
)