package io.github.artemptushkin.ai.assistants.words

sealed class WordsUpdateRequest(
    val words: List<String>
)

class AddRequest(words: List<String>): WordsUpdateRequest(words)
class DeleteRequest(words: List<String>): WordsUpdateRequest(words)