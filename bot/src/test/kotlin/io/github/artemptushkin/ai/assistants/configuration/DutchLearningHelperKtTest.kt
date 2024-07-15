package io.github.artemptushkin.ai.assistants.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class DutchLearningHelperKtTest {

    @Test
    fun `it-parses-buttons-as-expected`() {
        val value = """
            fada123123
            text
            
            
            "### buttons ###
            Translation
            List of words
            "### buttons ###
        """

        val actualButtons = value.parseButtons()

        assertThat(actualButtons).hasSize(2)
        assertThat(actualButtons).containsExactlyElementsOf(listOf("Translation", "List of words"))
    }

    @Test
    fun `it-parses-buttons-to-layout-as-expected`() {
        val buttons = listOf("Translation", "List of words")

        val layout = buttons.buttonsToLayout()

        assertThat(layout.inlineKeyboard[0].size).isEqualTo(buttons.size)
    }
}