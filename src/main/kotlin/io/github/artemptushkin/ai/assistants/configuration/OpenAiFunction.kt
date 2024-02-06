package io.github.artemptushkin.ai.assistants.configuration

interface OpenAiFunction {

    /**
     * It consumes the raw value of the same request body that OpenAI assistant sends when it requires an action submitted
     * output of this function will be set as-as the `output` in the OpenAI API https://platform.openai.com/docs/api-reference/runs/submitToolOutputs
     */
    fun handle(from: String): String

    /**
     * it must match the name of the function in openai assistant configuration
     */
    fun name(): String
}