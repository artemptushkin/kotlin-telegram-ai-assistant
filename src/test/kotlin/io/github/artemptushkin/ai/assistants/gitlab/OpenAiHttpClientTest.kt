package io.github.artemptushkin.ai.assistants.gitlab

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.artemptushkin.ai.assistants.http.HttpRequestFunction
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OpenAiHttpClientTest {

    @Autowired
    lateinit var httpRequestFunction: HttpRequestFunction

    @Test
    fun `it-calls-gitlab-successfully`() {
        val assistantFunctionObject = "{ \"url\": \"https://gitlab.com/api/v4/merge_requests\", \"method\": \"GET\", \"queries\": \"scope=assigned_to_me&state=opened&view=simple\", \"headers\": [ \"PRIVATE-TOKEN: <your_access_token>\" ] }"

        runBlocking {
            val response = httpRequestFunction.handle(ObjectMapper().readTree(assistantFunctionObject))

            assertThat(response).isNotEmpty()
        }
    }

    @Test
    fun `it-calls-alpha-vantage-successfully`() {
        val assistantFunctionObject = "{\"url\":\"https://www.alphavantage.co/query\",\"method\":\"GET\",\"queries\":\"function=GLOBAL_QUOTE&symbol=META&apikey=<your_api_key>\"}"

        runBlocking {
            val response = httpRequestFunction.handle(ObjectMapper().readTree(assistantFunctionObject))

            assertThat(response).isNotEmpty()
        }
    }
}