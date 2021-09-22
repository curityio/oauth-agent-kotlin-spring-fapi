package io.curity.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer




@SpringBootApplication
class BackendForFrontendApplication {

	@Bean
	fun corsConfigurer(configuration: BFFConfiguration): WebMvcConfigurer
	{
		return object : WebMvcConfigurer
		{
			override fun addCorsMappings(registry: CorsRegistry)
			{
				registry
					.addMapping("/**")
					.allowedOrigins(*configuration.trustedWebOrigins.toTypedArray())
					.allowCredentials(true)
					.allowedMethods("POST", "GET", "OPTIONS")
			}
		}
	}
}

fun main(args: Array<String>) {
	runApplication<BackendForFrontendApplication>(*args)
}
