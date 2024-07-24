package io.github.artemptushkin.ai.assistants.dutch

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

val buttonsToCallbackData = mapOf(
    "Translation" to "request.translation",
    "List of words" to "request.list-of-words",
)

fun dutchOnboardingInlineButtons(): ReplyMarkup = InlineKeyboardMarkup.create(
    listOf(
        InlineKeyboardButton.CallbackData("Easy - 5 words", "onboarding.difficult:5"),
        InlineKeyboardButton.CallbackData("Medium - 7 words", "onboarding.difficult:7"),
        InlineKeyboardButton.CallbackData("Hard - 10 words", "onboarding.difficult:10"),
    )
)

fun String.parseButtons(): List<String> = this
    .substringAfter(buttonNotations)
    .substringBefore(buttonNotations)
    .split('\n')
    .map(String::trim)
    .filter { it.isNotBlank() && it.length > 2 }

fun List<String>.buttonsToLayout(): InlineKeyboardMarkup = InlineKeyboardMarkup.create(
    this.map { InlineKeyboardButton.CallbackData(it, buttonsToCallbackData[it]!!) }
)

fun String.getMessageIdFromCallback(): String = this.substringAfterLast(":")

fun String.getRequestAction(): String = this.substringAfter("request.").substringBefore(":")

fun String.getRequestActionAssistantResponse(): String {
    return when (this.getRequestAction()) {
        "translation" -> {
            "I'm proceeding with translation now..."
        }
        "list-of-words" -> {
            "I'm collecting list of used words now..."
        }
        else -> {
            "This action is unknown, I'm sorry I don't know how to proceed"
        }
    }
}

fun String.getRequestActionUserImplicitPrompt(message: String): String {
    return when (this.getRequestAction()) {
        "translation" -> {
            "Please translate: $message"
        }
        "list-of-words" -> {
            "Please give me the list of used words in this message: $message"
        }
        else -> {
            "This action is unknown, I'm sorry I don't know how to proceed"
        }
    }
}

fun String.isOnboardingCallback(): Boolean = this.startsWith("onboarding")
fun String.isRequestCallback(): Boolean = this.startsWith("request")

fun String.isDifficultWordsSetting(): Boolean = this.startsWith("onboarding.difficult")

fun String.getDifficultWordsNumber() = this.substringAfter("onboarding.difficult:").toInt()

fun initialDutchLearnerPrompt(wordsNumber: Int) = """
    I am learning Dutch and need your help learning words.
    I will give you the list of the words with translations, and every time I ask you to “create a story” you must
    randomly choose $wordsNumber (or less if not available) words from the list and create a short story in Dutch with them.
    Your message must have only a story, no additional sentences or questions.
    After that, I could ask you to give me a translation and the list of used words. I will also ask you to practice words.
    If I ask “Practice words Dutch to English” you should give me a list of $wordsNumber randomly chosen words
    from the list in format word in Dutch - word in English, word in English should be hidden using telegram spoiler markdown.
    If I ask “Practice words English to Dutch” you should give me a list of $wordsNumber randomly chosen words
    from the list in format word in English - word in Dutch, word in Dutch should be hidden using telegram spoiler markdown.
    I could also ask you to delete some words and add new words.
""".trimIndent()