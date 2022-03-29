package io.curity.oauthagent

import org.jose4j.jwk.RsaJsonWebKey
import org.jose4j.jwk.RsaJwkGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestsConfiguration {

    @Bean
    RsaJsonWebKey jsonWebKey() {
        def rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
        rsaJsonWebKey.setKeyId("key1")

        rsaJsonWebKey
    }
}
