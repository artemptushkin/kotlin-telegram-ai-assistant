package io.github.artemptushkin.ai.assistants.words

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import io.github.artemptushkin.ai.assistants.http.OpenAiHttpClient.Companion.logger
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component


@Component
class AddWordsFunction(
    private val objectMapper: ObjectMapper,
    private val learningWordsService: LearningWordsService,
) : OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String {
        return runBlocking {
            try {
                val addRequest = objectMapper.treeToValue(from, AddRequest::class.java)
                learningWordsService
                    .add(addRequest.words, telegramContext)
                    .responseToAssistant()
            } catch (e: Exception) {
                logger.error("Exception handled during ${name()} function", e)
                "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
            }
        }
    }

    override fun name(): String = "add-words"

}

@Component
class DeleteWordsFunction(
    private val objectMapper: ObjectMapper,
    private val learningWordsService: LearningWordsService,
) : OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String {
        return runBlocking {
            try {
                val deleteRequest = objectMapper.treeToValue(from, DeleteRequest::class.java)
                learningWordsService
                    .delete(deleteRequest.words, telegramContext)
                    .responseToAssistant()
            } catch (e: Exception) {
                logger.error("Exception handled during ${name()} function", e)
                "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
            }
        }
    }

    override fun name(): String = "delete-words"
}

@Component
class GetWordsFunction(
    private val learningWordsService: LearningWordsService,
) : OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String {
        return runBlocking {
            try {
                learningWordsService
                    .getWords(telegramContext)
                    ?.responseToAssistant() ?: "No words found, please add some words"
            } catch (e: Exception) {
                logger.error("Exception handled during ${name()} function", e)
                "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
            }
        }
    }

    override fun name(): String = "get-words"
}