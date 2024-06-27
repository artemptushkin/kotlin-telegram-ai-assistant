package io.github.artemptushkin.ai.assistants

data class OnboardingDto(
    var wordsNumber: Int? = null,
) {
    fun isAllSet(): Boolean = wordsNumber != null
}