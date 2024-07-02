package io.github.artemptushkin.ai.assistants.words

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.spring.data.firestore.Document
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import org.springframework.stereotype.Repository

@Repository
interface LearningWordsRepository : FirestoreReactiveRepository<LearningWords>

@Document(collectionName = "learning-words")
data class LearningWords(
    @DocumentId
    var id: String? = null,
    var language: String? = null,
    var words: MutableList<String>? = null,
    var chatId: String? = null,
    var botId: String? = null,
) {
    fun responseToAssistant(): String {
        return "Words have been updated, the current list is: ${words?.joinToString()}"
    }
}

fun makeId(bot: String, chat: String): String = "$bot:$chat"