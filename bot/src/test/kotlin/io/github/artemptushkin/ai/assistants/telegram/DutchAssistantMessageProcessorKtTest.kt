package io.github.artemptushkin.ai.assistants.telegram

import io.github.artemptushkin.ai.assistants.openai.removeNotations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class DutchAssistantMessageProcessorKtTest {
    @Test
    fun `it-formats-text-correctly`() {
        val text = "Er was eens een kat genaamd Minoes die altijd graag speelde met haar hondenvriend Max. Elke ochtend renden ze samen door het park, jagend achter vlinders en lachend in de zon.\n\n### buttons ###\nTranslation\nList of words\n### buttons ###".trimIndent()

        val expected = """Er was eens een kat genaamd Minoes die altijd graag speelde met haar hondenvriend Max. Elke ochtend renden ze samen door het park, jagend achter vlinders en lachend in de zon."""

        val actual = text.removeNotations()

        assertThat(actual).isEqualTo(expected)
    }
}