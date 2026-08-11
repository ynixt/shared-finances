package com.ynixt.sharedfinances.domain.enums

enum class ImportBatchStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    UNDO_QUEUED,
    UNDO_RUNNING,
    UNDO_FAILED,
    UNDONE,
}
