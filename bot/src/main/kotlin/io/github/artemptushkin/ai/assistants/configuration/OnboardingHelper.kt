package io.github.artemptushkin.ai.assistants.configuration

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

fun settingsInlineButtons(): ReplyMarkup = InlineKeyboardMarkup.create(
    listOf(
        InlineKeyboardButton.CallbackData("Easy - 5 words", "settings.difficult:5"),
        InlineKeyboardButton.CallbackData("Medium - 7 words", "settings.difficult:7"),
        InlineKeyboardButton.CallbackData("Hard - 10 words", "settings.difficult:10"),
    )
)

fun String.isDifficultWordsSetting(): Boolean = this.startsWith("settings.difficult")

fun String.getDifficultWordsNumber() = this.substringAfter("settings.difficult:").toInt()

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