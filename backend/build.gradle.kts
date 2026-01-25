
import io.github.ynixt.anothertypescriptgenerator.GenerateTypescriptInterfacesTask


plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.github.ynixt.another-typescript-generator") version "1.2.0"
    id("tech.mappie.plugin") version "2.2.21-2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "com.ynixt"
version = "3.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
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

    implementation("com.graphql-java:graphql-java-extended-scalars:24.0")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-api:3.0.0")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.0")

    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

    implementation("io.r2dbc:r2dbc-pool")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.apache.commons:commons-pool2:2.13.0")
    implementation("commons-codec:commons-codec:1.20.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("com.github.slugify:slugify:3.0.7")
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.0")

    implementation(platform("software.amazon.awssdk:bom:2.25.60"))
    implementation("software.amazon.awssdk:s3")

    runtimeOnly("commons-logging:commons-logging")
    runtimeOnly("org.postgresql:postgresql") // flyway
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.bouncycastle:bcprov-jdk18on:1.80")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

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
