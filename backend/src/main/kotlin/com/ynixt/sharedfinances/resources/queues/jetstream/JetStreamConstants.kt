package com.ynixt.sharedfinances.resources.queues.jetstream

object JetStreamConstants {
    const val ENTRY_RECURRENCE_STREAM = "ENTRY_RECURRENCE_STREAM"
    const val GENERATE_ENTRY_RECURRENCE_QUEUE = "generate-entry-recurrence"

    const val ENTRY_RECURRENCE_DLQ_STREAM = "ENTRY_RECURRENCE_DLQ_STREAM"
    const val GENERATE_ENTRY_RECURRENCE_DLQ_QUEUE = "generate-entry-recurrence-dlq"

    const val SIMULATION_JOB_DISPATCH_STREAM = "SF_JOB_DISPATCH"
    const val SIMULATION_JOB_DISPATCH_SUBJECT = "sf.jobs.dispatch"
    const val SIMULATION_JOB_WORKER_CONSUMER = "sf-job-workers"
}
