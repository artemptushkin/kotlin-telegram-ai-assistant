package io.github.artemptushkin.ai.assistants.dutch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.artemptushkin.ai.assistants.configuration.OpenAiFunction
import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
@Profile("dutch")
class AddWordsFunction(
    private val objectMapper: ObjectMapper,
    private val learningWordsService: LearningWordsService,
) : OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String {
        return runBlocking {
            try {
                val addRequest = objectMapper.treeToValue(from, AddRequest::class.java)
                logger.debug("Add words function calling: ${addRequest.words}")
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

    companion object {
        val logger = LoggerFactory.getLogger(AddWordsFunction::class.java)!!
    }

}

@Component
@Profile("dutch")
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
                    ?.responseToAssistant() ?: "The current list is already empty"
            } catch (e: Exception) {
                logger.error("Exception handled during ${name()} function", e)
                "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
            }
        }
    }

    override fun name(): String = "delete-words"

    companion object {
        val logger = LoggerFactory.getLogger(DeleteWordsFunction::class.java)!!
    }
}

@Component
@Profile("dutch")
class GetWordsFunction(
    private val learningWordsService: LearningWordsService,
) : OpenAiFunction {
    override fun handle(from: JsonNode, telegramContext: TelegramContext): String {
        return runBlocking {
            try {
                val response = learningWordsService
                    .getWords(telegramContext)
                    ?.responseToAssistant() ?: "No words found, please add some words"
                return@runBlocking response.also { logger.info("Fetched the following words: $it") }
            } catch (e: Exception) {
                logger.error("Exception handled during ${name()} function", e)
                "Exception handled during the HTTP call. Assistant should evaluate the following error message: ${e.message}. Assistant should repeat the function execution if possible. If it's not possible then assistant should respond to user they should contact the administrator at art.ptushkin@gmail.com"
            }
        }
    }

    override fun name(): String = "get-words"

    companion object {
        private val logger = LoggerFactory.getLogger(GetWordsFunction::class.java)!!
    }
}