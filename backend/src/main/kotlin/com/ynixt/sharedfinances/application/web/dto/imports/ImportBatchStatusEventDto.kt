package com.ynixt.sharedfinances.application.web.dto.imports

import com.ynixt.sharedfinances.domain.enums.ImportBatchStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ImportBatchStatusEventDto(
    val id: UUID,
    val status: ImportBatchStatus,
    val errorMessage: String?,
    val startedAt: OffsetDateTime?,
    val finishedAt: OffsetDateTime?,
    val retries: Int,
)
