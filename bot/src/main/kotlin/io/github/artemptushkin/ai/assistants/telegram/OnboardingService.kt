package io.github.artemptushkin.ai.assistants.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.CallbackQuery
import com.github.kotlintelegrambot.entities.ReplyMarkup
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

interface OnboardingService {
    fun onboardingInlineButtons(): ReplyMarkup?
    suspend fun handleOnBoardingCallback(bot: Bot, callbackQuery: CallbackQuery)
}

@Service
@ConditionalOnMissingBean(OnboardingService::class)
class NoopOnboardingService : OnboardingService {
    override fun onboardingInlineButtons(): ReplyMarkup? = null

    override suspend fun handleOnBoardingCallback(bot: Bot, callbackQuery: CallbackQuery) {}
}