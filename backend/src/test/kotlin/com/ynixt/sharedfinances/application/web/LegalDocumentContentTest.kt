package com.ynixt.sharedfinances.application.web

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalDocumentContentTest {
    @Test
    fun `legal prose in both languages quotes no numeric plan limit or retention period`() {
        listOf("en-US", "pt-BR").forEach { language ->
            val content = Files.readString(legalFile(language))
            val prose = content.replace(Regex("(?m)^\\s+p\\d+:\\s*>-\\s*$"), "")

            assertFalse(Regex("\\b\\d+\\b").containsMatchIn(prose), "$language legal prose contains a numeric value")
            assertFalse(content.contains("Existing accounts are not automatically required to re-accept"))
            assertFalse(content.contains("Contas existentes não precisam reaceitar automaticamente"))
            assertTrue(content.contains("lgdp-finances@gabrielsilva.dev"))
        }
    }

    private fun legalFile(language: String): Path {
        val fromBackend = Path.of("..", "frontend", "src", "i18n", "legal", "$language.yaml")
        if (Files.exists(fromBackend)) return fromBackend
        return Path.of("frontend", "src", "i18n", "legal", "$language.yaml")
    }
}
