package io.github.artemptushkin.ai.assistants.words

import io.github.artemptushkin.ai.assistants.configuration.TelegramContext
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
            .findById(makeId(telegramContext.botId, telegramContext.chatId))
            .awaitFirstOrNull()
    }

    suspend fun add(words: List<String>, telegramContext: TelegramContext): LearningWords {
        return learningWordsRepository
            .findById(makeId(telegramContext.botId, telegramContext.chatId))
            .awaitFirstOrElse {
                LearningWords(
                    id = makeId(telegramContext.botId, telegramContext.chatId),
                    language = telegramContext.metadata["language"] as String,
                    words = words.distinct().toMutableList(), // no sure why but it duplicates words sometimes
                    botId = telegramContext.botId,
                    chatId = telegramContext.chatId
                )
            }
            .apply {
                this.words?.addAll(words.distinct())
                learningWordsRepository
                    .save(this)
                    .awaitSingle()
            }
    }

    suspend fun delete(words: List<String>, telegramContext: TelegramContext): LearningWords? {
        return learningWordsRepository
            .findById(makeId(telegramContext.botId, telegramContext.chatId))
            .awaitFirstOrNull()
            ?.apply {
                this.words?.removeAll(words)
                learningWordsRepository
                    .save(this)
                    .awaitSingle()
            }
    }

}