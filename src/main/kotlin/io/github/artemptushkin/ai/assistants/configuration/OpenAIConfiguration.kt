package io.github.artemptushkin.ai.assistants.configuration

import com.theokanning.openai.service.OpenAiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAIConfiguration {

    @Bean
    fun openAiService(@Value("\${openai.apiKey}") apiKey: String): OpenAiService = OpenAiService(apiKey)
}