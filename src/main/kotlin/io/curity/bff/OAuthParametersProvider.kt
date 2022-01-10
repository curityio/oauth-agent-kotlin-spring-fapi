package io.curity.bff

interface OAuthParametersProvider
{
    fun getState(): String

    fun getCodeVerifier(): String
}

class OAuthParametersProviderImpl: OAuthParametersProvider {
    override fun getState() = generateRandomString()

    override fun getCodeVerifier() = generateRandomString()
}
