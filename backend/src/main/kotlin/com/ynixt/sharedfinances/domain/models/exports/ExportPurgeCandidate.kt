package com.ynixt.sharedfinances.domain.models.exports

import java.util.UUID

data class ExportPurgeCandidate(
    val batchId: UUID,
    val userId: UUID,
    val fileKey: String,
)
