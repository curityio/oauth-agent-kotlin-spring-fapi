package io.curity.bff

import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory


@Configuration
class WebClientConfiguration
{
    @Value("\${bff.ssl.key-store}")
    lateinit var sslKeyStore: String
    @Value("\${bff.ssl.key-store-password}")
    lateinit var sslKeystorePassword: String
    @Value("\${bff.ssl.trust-store}")
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
        FileInputStream(keyStoreLocation).use { fis ->
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(fis, keyStorePassword.toCharArray())
            return ks
        }
    }

    private fun createTrustStore(trustStoreLocation: String): KeyStore
    {
        val factory = CertificateFactory.getInstance("X.509")
        FileInputStream(trustStoreLocation).use { fis ->
            val cert = factory.generateCertificate(fis) as X509Certificate
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, null)
            ks.setCertificateEntry("development-root-ca", cert)
            return ks
        }
    }
}
