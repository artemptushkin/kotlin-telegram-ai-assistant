package io.github.artemptushkin.ai.assistants.telegram

data class OnboardingDto(
    var wordsNumber: Int? = null,
) {
    fun isAllSet(): Boolean = wordsNumber != null
}