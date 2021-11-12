import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.4"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm") version "1.5.21"
	kotlin("plugin.spring") version "1.5.21"
}

group = "io.curity"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux:2.5.6")
	implementation("org.springframework.session:spring-session-core:2.5.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
	implementation("org.bitbucket.b_c:jose4j:0.7.9")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.2")
	implementation("org.apache.commons:commons-crypto:1.1.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.6")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
