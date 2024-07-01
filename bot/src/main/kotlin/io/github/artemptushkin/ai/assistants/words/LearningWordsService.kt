package io.github.artemptushkin.ai.assistants.words

import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class LearningWordsService(
    private val learningWordsRepository: LearningWordsRepository,
) {

    suspend fun getWords(telegramContext: TelegramContext): LearningWords? {
        return learningWordsRepository
            .findByBotIdAndLanguageAndChatId(telegramContext.botId, telegramContext.metadata["language"] as String, telegramContext.chatId)
            .awaitFirstOrNull()
    }

    suspend fun add(words: List<String>, telegramContext: TelegramContext): LearningWords {
        return learningWordsRepository
            .findByBotIdAndLanguageAndChatId(telegramContext.botId, telegramContext.metadata["language"] as String, telegramContext.chatId)
            .awaitFirstOrElse {
                LearningWords(
                    language = telegramContext.metadata["language"] as String,
                    words = words.toMutableList(),
                    botId = telegramContext.botId,
                    chatId = telegramContext.chatId
                )
            }
            .apply {
                this.words?.addAll(words)
                learningWordsRepository
                    .save(this)
                    .awaitSingle()
            }
    }

    suspend fun delete(words: List<String>, telegramContext: TelegramContext): LearningWords {
        return learningWordsRepository
            .findByBotIdAndLanguageAndChatId(telegramContext.botId, telegramContext.metadata["language"] as String, telegramContext.chatId)
            .awaitFirst()
            .apply {
                this.words?.removeAll(words)
                learningWordsRepository
                    .save(this)
                    .awaitSingle()
            }
    }

}