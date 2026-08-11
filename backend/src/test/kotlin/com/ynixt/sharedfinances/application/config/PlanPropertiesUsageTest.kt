package com.ynixt.sharedfinances.application.config

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.test.assertEquals

class PlanPropertiesUsageTest {
    @Test
    fun `only user creation reads the configured default role`() {
        val usages =
            Files
                .walk(Path.of("src/main/kotlin"))
                .use { paths ->
                    paths
                        .filter { it.isRegularFile() && it.name.endsWith(".kt") }
                        .filter { it.name !in setOf("PlanProperties.kt", "PlanConfiguration.kt") }
                        .filter { it.readText().contains(".defaultRole") }
                        .map {
                            Path
                                .of("src/main/kotlin")
                                .relativize(it)
                                .toString()
                                .replace('\\', '/')
                        }.toList()
                }

        assertEquals(listOf("com/ynixt/sharedfinances/resources/services/UserServiceImpl.kt"), usages)
    }
}
