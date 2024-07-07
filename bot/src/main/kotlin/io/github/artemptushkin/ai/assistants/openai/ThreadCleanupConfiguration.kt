package io.github.artemptushkin.ai.assistants.openai

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ThreadCleanupConfiguration {

    @Bean
    fun classToStrategy(strategies: List<ThreadCleanupStrategy>): Map<String, ThreadCleanupStrategy> =
        strategies.associateBy { it.javaClass.canonicalName }
}