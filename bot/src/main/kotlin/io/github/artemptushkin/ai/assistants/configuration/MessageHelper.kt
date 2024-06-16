package io.github.artemptushkin.ai.assistants.configuration;

const val telegramMessageLimit = 4000

fun String.chunkToTelegramMessageLimits(): List<String> = this.chunked(telegramMessageLimit)

