package io.github.artemptushkin.ai.assistants.gitlab

import io.github.artemptushkin.ai.assistants.http.ApiRequest
import io.github.artemptushkin.ai.assistants.http.pathWithQueries
import io.github.artemptushkin.ai.assistants.http.staticHeaders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ApiRequestTest {

    @Test
    fun `staticHeaders should filter and create a valid header map`() {
        // Given
        val apiRequest = ApiRequest(
            url = "https://example.com",
            method = "GET",
            queries = "param=value",
            headers = listOf(
                "PRIVATE-TOKEN: <your_access_token>",
                "SOME-HEADER: value"
            )
        )

        // When
        val result = apiRequest.staticHeaders()

        // Then
        val expectedMap = mapOf(
            "SOME-HEADER" to "value"
        )
        assertThat(result).isEqualTo(expectedMap)
    }

    @Test
    fun `pathWithQueries should return path with queries without protocol and host`() {
        // Given
        val apiRequest = ApiRequest(
            url = "https://example.com/api/v1/resource",
            method = "GET",
            queries = "param1=value1&param2=value2",
            headers = emptyList()
        )

        // When
        val result = apiRequest.pathWithQueries()

        // Then
        assertThat(result).isEqualTo("/api/v1/resource?param1=value1&param2=value2")
    }

    @Test
    fun `pathWithQueries should return path without queries when queries are blank`() {
        // Given
        val apiRequest = ApiRequest(
            url = "https://example.com/api/v1/resource",
            method = "GET",
            queries = "",
            headers = emptyList()
        )

        // When
        val result = apiRequest.pathWithQueries()

        // Then
        assertThat(result).isEqualTo("/api/v1/resource")
    }

    @Test
    fun `pathWithQueries should handle URLs without protocol and host`() {
        // Given
        val apiRequest = ApiRequest(
            url = "/api/v1/resource",
            method = "GET",
            queries = "param=value",
            headers = emptyList()
        )

        // When
        val result = apiRequest.pathWithQueries()

        // Then
        assertThat(result).isEqualTo("/api/v1/resource?param=value")
    }
}