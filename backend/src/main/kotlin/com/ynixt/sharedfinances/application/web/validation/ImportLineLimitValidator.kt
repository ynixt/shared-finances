package com.ynixt.sharedfinances.application.web.validation

import com.ynixt.sharedfinances.application.config.ImportProperties
import com.ynixt.sharedfinances.domain.exceptions.http.ImportLineLimitExceededException
import org.springframework.stereotype.Component

@Component
class ImportLineLimitValidator(
    private val importProperties: ImportProperties,
) {
    fun validate(lineCount: Int) {
        if (lineCount > importProperties.maxLines) {
            throw ImportLineLimitExceededException(importProperties.maxLines)
        }
    }
}
