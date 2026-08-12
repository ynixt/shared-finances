package com.ynixt.sharedfinances.resources.services.exports

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class TransactionXlsxWriterTest {
    @Test
    fun `generates a workbook with both export sheets`() =
        runTest {
            val bytes =
                TransactionXlsxWriter(RecurrenceXlsxSheetWriter())
                    .write(emptyFlow(), emptyFlow())
                    .toList()
                    .fold(byteArrayOf()) { workbook, chunk -> workbook + chunk }

            assertTrue(bytes.size > 4)
            assertTrue(bytes.copyOfRange(0, 2).contentEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte())))

            val workbookXml = workbookXml(bytes)
            assertContains(workbookXml, "name=\"Transactions\"")
            assertContains(workbookXml, "name=\"Recurrences\"")
        }

    private fun workbookXml(bytes: ByteArray): String {
        val path = Files.createTempFile("transaction-export-", ".xlsx")
        return try {
            Files.write(path, bytes)
            ZipFile(path.toFile()).use { archive ->
                archive.getInputStream(archive.getEntry("xl/workbook.xml")).readBytes().decodeToString()
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
