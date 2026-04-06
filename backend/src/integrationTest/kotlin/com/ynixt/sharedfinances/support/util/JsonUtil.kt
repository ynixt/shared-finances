package com.ynixt.sharedfinances.support.util

import java.io.BufferedReader
import java.io.InputStreamReader

object JsonUtil {
    fun readJsonFromResources(fileName: String): String {
        val inputStream =
            JsonUtil::class.java.classLoader
                .getResourceAsStream(fileName)
                ?: throw IllegalArgumentException("File not found on resources: $fileName")

        return BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readText()
        }
    }
}
