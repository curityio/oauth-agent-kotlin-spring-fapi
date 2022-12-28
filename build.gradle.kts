import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.7"
	id("io.spring.dependency-management") version "1.1.0"
	id("com.adarshr.test-logger") version "3.2.0"
	kotlin("jvm") version "1.8.0"
	kotlin("plugin.spring") version "1.8.0"
	id("groovy")
}

group = "io.curity"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux:2.7.7")
	implementation("org.springframework.session:spring-session-core:2.7.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.1")
	implementation("org.bitbucket.b_c:jose4j:0.9.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
	implementation("org.apache.commons:commons-crypto:1.1.0")

	implementation("org.apache.groovy:groovy:4.0.7")
	implementation("org.apache.groovy:groovy-json:4.0.7")
	testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.7")
	testImplementation(platform("org.spockframework:spock-bom:2.3-groovy-4.0"))
	testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
	testImplementation("org.spockframework:spock-spring:2.3-groovy-4.0")
	testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
	testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
		incremental = false
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	jvmArgs = listOf(
		"-Djavax.net.ssl.trustStore=src/test/resources/certs/example.server.test.p12",
		"-Djavax.net.ssl.trustStorePassword=Password1",
		"-Dsun.net.http.allowRestrictedHeaders=true"
	)
	include("**/*Spec.class")
	testLogging.showStandardStreams = false
}
