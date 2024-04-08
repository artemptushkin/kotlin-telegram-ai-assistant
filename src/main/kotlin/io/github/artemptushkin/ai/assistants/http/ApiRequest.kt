package io.github.artemptushkin.ai.assistants.http

import org.springframework.http.HttpMethod
import java.net.URI
import java.net.URL

data class ApiRequest(
    val url: String,
    val method: String,
    val queries: String?,
    val headers: List<String>?
)

fun ApiRequest.httpMethod() = HttpMethod.valueOf(method)

fun ApiRequest.uri(k: String, v: String): URI {
    return if (queries.isNullOrEmpty()) {
        URI.create(url)
    } else {
        val filteredQueriesStr = this.filterQueriesString(k, v)
        URI.create("$url?$filteredQueriesStr")
    }
}

fun String.toQueryMap(): Map<String, String> {
    val queryMap = mutableMapOf<String, String>()
    val keyValuePairs = this.split("&")
    for (pair in keyValuePairs) {
        val (key, value) = pair.split("=")
        queryMap[key] = value
    }
    return queryMap
}

fun ApiRequest.pathWithQueries(): String {
    val path = if (url.startsWith("http")) {
        URL(url).path
    } else {
        url
    }

    return if (queries.isNullOrEmpty()) {
        path
    } else {
        "$path?$queries"
    }
}

fun ApiRequest.staticHeaders(): Map<String, String> = this.headers
    ?.map { it.split(":").map(String::trim) }
    ?.filter { it.size == 2 } // todo technically it can be multi value map - fix here
    ?.map {
        val (header, value) = it
        return@map header to value
    }
    ?.filter { !it.second.startsWith('<') && !it.second.endsWith('>') }
    ?.toMap() ?: emptyMap()

fun ApiRequest.filterQueries(): MutableMap<String, String> = this.queries
    ?.toQueryMap()
    ?.filter { !it.value.startsWith('<') && !it.value.endsWith('>') }
    ?.toMutableMap() ?: mutableMapOf()

fun ApiRequest.filterQueriesString(k: String, v: String): String = this.filterQueries()
    .also {
        it[k] = v
    }
    .entries
    .joinToString("&")