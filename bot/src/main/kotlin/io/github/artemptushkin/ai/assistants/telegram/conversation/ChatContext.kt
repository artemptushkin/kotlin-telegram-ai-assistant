package io.github.artemptushkin.ai.assistants.telegram.conversation

import com.github.kotlintelegrambot.entities.ChatId
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Component
class ChatContext(private val storage: MutableMap<String, Any> = ConcurrentHashMap()) {

    fun get(contextKey: ContextKey<out Any>): Any? {
        return storage[contextKey.id()]
    }

    fun save(contextKey: ContextKey<out Any>, value: Any): String {
        this.storage[contextKey.id()] = value
        return contextKey.id()
    }

    fun delete(contextKey: ContextKey<out Any>) {
        this.storage.remove(contextKey.id())
    }
}

data class ContextKey<T: Any>(
    val chatId: ChatId.Id,
    val keyId: String,
    val keyType: String,
    val type: KClass<T>
) {

    fun id() = "${chatId.id}:$keyId:$keyType"

    companion object {

        val keyToClass: Map<String, KClass<*>> = mapOf(
            "thread" to String::class,
            "run" to String::class,
            "command" to String::class
        )
        fun thread(chatId: ChatId.Id) = ContextKey(chatId, "current-thread", "thread", keyToClass["thread"]!!)

        fun run(chatId: ChatId.Id) = ContextKey(chatId, "current-run", "thread", keyToClass["run"]!!)
    }


}