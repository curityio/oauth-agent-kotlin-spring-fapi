package io.curity.oauthagent.exception

sealed class OAuthAgentException(
    message: String,
    cause: Throwable?,
    val statusCode: Int,
    val code: String,
    private val logMessage: String?
) : RuntimeException(message, cause) {

    fun getLogFields(): ArrayList<String> {

        // Include non-verbose fields in all errors including 4xx errors, such as when a token expires
        val fields = ArrayList<String>()
        fields.add(statusCode.toString())
        fields.add(code)
        message?.apply {
            fields.add(this)
        }
        logMessage?.apply {
            fields.add(this)
        }

        // Only log verbose stack traces for 5xx errors, when there is a server problem, to prevent people confusion
        if (statusCode >= 500) {
            fields.add(stackTraceToString())
        }

        return fields
    }
}
