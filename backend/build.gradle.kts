
import io.github.ynixt.anothertypescriptgenerator.GenerateTypescriptInterfacesTask


plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.github.ynixt.another-typescript-generator") version "1.2.0"
    id("tech.mappie.plugin") version "2.3.0-2.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "com.ynixt"
version = "3.0.0-alpha.1"

springBoot {
    buildInfo()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestTask =
    tasks.register("integrationTest", Test::class) {
        description = "Runs the integration tests."
        group = "verification"

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath

        shouldRunAfter(tasks.named("test"))
    }

fun loadEnvFile(envFile: File): Map<String, String> {
    if (!envFile.exists()) return emptyMap()
    return envFile
        .readLines()
        .asSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null
            val key = line.substring(0, separatorIndex).trim()
            val value = line.substring(separatorIndex + 1).trim().removeSurrounding("\"")
            key to value
        }.toMap()
}

val integrationEnvFile =
    listOf(
        file("../test.env"),
        file("test.env"),
    ).firstOrNull { it.exists() }

integrationTestTask.configure {
    if (integrationEnvFile != null) {
        environment(loadEnvFile(integrationEnvFile))
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.testImplementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<GenerateTypescriptInterfacesTask>("generateTypescriptInterfaces") {
    outputPath = "../frontend/src/app/models/generated"
    classPackages =
        listOf(
            "com.ynixt.sharedfinances.application.web.dto",
            "com.ynixt.sharedfinances.domain.enums",
        )
    generateEnumObject = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("com.graphql-java:graphql-java-extended-scalars:24.0")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.1")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.1")

    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.openfeign:feign-core:13.6")
    implementation("io.github.openfeign:feign-jackson:13.6")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("io.r2dbc:r2dbc-pool")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.nats:jnats:2.25.1")

    implementation("org.apache.commons:commons-pool2:2.13.0")
    implementation("commons-codec:commons-codec:1.20.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.github.slugify:slugify:3.0.7")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.0")

    implementation("com.github.kagkarlsson:db-scheduler:16.7.1")
    implementation("com.zaxxer:HikariCP")

    implementation(platform("software.amazon.awssdk:bom:2.25.60"))
    implementation("software.amazon.awssdk:s3")

    runtimeOnly("commons-logging:commons-logging")
    runtimeOnly("org.postgresql:postgresql") // flyway
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.80")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    integrationTestImplementation("org.springframework.boot:spring-boot-testcontainers")
    integrationTestImplementation("org.springframework.boot:spring-boot-data-r2dbc-test")
    integrationTestImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    integrationTestImplementation("org.testcontainers:testcontainers")
    integrationTestImplementation("org.testcontainers:testcontainers-r2dbc")
    integrationTestImplementation("org.testcontainers:testcontainers-junit-jupiter")
    integrationTestImplementation("org.testcontainers:testcontainers-postgresql")
    integrationTestImplementation("com.redis:testcontainers-redis:2.2.4")
    integrationTestImplementation("io.github.amadeusitgroup.testcontainers:nats:1.0.12")
}
