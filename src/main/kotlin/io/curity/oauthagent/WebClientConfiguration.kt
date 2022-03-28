package io.curity.oauthagent

import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.ResourceUtils
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory


@Configuration
class WebClientConfiguration
{
    @Value("\${oauthagent.ssl.key-store}")
    lateinit var sslKeyStore: String
    @Value("\${oauthagent.ssl.key-store-password}")
    lateinit var sslKeystorePassword: String
    @Value("\${oauthagent.ssl.trust-store}")
    lateinit var sslTrustStore: String

    @Bean
    fun webClient(): WebClient?
    {
        val keyStore = createKeyStore(sslKeyStore, sslKeystorePassword)

        val keyManager = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManager.init(keyStore, sslKeystorePassword.toCharArray())

        val trustStore = createTrustStore(sslTrustStore)
        val trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManager.init(trustStore)

        val sslContext = SslContextBuilder.forClient()
            .keyManager(keyManager)
            .trustManager(trustManager)
            .build()
        val httpClient = HttpClient.create().secure { it.sslContext(sslContext) }

        return WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }

    private fun createKeyStore(keyStoreLocation: String, keyStorePassword: String): KeyStore
    {
        val keyStoreURL = ResourceUtils.getURL(keyStoreLocation)
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())

        keyStoreURL.openStream().use {
            ks.load(it, keyStorePassword.toCharArray())
        }

        return ks
    }

    private fun createTrustStore(trustStoreLocation: String): KeyStore
    {
        val trustStoreURL = ResourceUtils.getURL(trustStoreLocation)
        val factory = CertificateFactory.getInstance("X.509")
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)

        trustStoreURL.openStream().use {
            val cert = factory.generateCertificate(it) as X509Certificate
            ks.setCertificateEntry("development-root-ca", cert)
        }

        return ks
    }
}
