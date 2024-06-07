package io.github.artemptushkin.ai.assistants.repository

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("local")
@RequestMapping("/history")
class HistoryController(
    private val telegramHistoryRepository: TelegramHistoryRepository,
) {

    @GetMapping
    suspend fun get(): List<ChatHistory> = telegramHistoryRepository.findAll().collectList().awaitSingle()

    @PostMapping
    suspend fun save(): ChatHistory = telegramHistoryRepository.save(
        ChatHistory(
            "2", listOf(
                Message(
                    1, "first", User(
                        1, "John"
                    )
                )
            )
        )
    ).awaitSingle()
}