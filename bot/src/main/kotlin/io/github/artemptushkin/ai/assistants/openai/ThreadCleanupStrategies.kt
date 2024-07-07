package io.github.artemptushkin.ai.assistants.openai

import io.github.artemptushkin.ai.assistants.repository.ChatHistory
import io.github.artemptushkin.ai.assistants.repository.getLatestMessageDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

interface ThreadCleanupStrategy {
    fun isAcceptableForCleanup(history: ChatHistory): Boolean
}

@Component
class NewDayStrategy : ThreadCleanupStrategy {
    override fun isAcceptableForCleanup(history: ChatHistory): Boolean {
        return run {
            val result = history.getLatestMessageDate()?.isBefore(LocalDate.now()) == true
            logger.debug("Thread ${history.threadId} defined as a clean up candidate based on a new date")
            result
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(NewDayStrategy::class.java)!!
    }
}
