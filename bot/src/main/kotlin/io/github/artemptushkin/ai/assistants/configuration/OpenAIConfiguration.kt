package io.github.artemptushkin.ai.assistants.configuration

import com.theokanning.openai.service.OpenAiService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAIConfiguration {

    @Bean
    fun openAiService(openAiProperties: OpenAiProperties): OpenAiService = OpenAiService(openAiProperties.apiKey, Duration.ofSeconds(60))

    @Bean
    fun openAiFunctions(functionsList: List<OpenAiFunction>): Map<String, OpenAiFunction> = functionsList.associateBy { it.name() }
}

@ConfigurationProperties("openai")
data class OpenAiProperties(
    val apiKey: String? = null,
    val assistantId: String? = null,
    val threadCleanupStrategies: List<String>? = null // canonical class names
)