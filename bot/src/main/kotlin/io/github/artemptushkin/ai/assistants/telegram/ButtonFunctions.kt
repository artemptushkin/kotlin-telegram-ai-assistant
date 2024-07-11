package io.github.artemptushkin.ai.assistants.telegram

class ButtonFunctions {

    companion object {
        val buttonToFunction: Map<String, String> = mutableMapOf(
            "Add words" to "Please type the list of words to add",
            "Delete words" to "Please type the list of words to delete",
            "Create story" to "Preparing a story \uD83D\uDCD6 for you"
        )
    }
}