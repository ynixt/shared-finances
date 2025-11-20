package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import reactor.core.publisher.Mono

interface EntryRecurrenceConfigRepository {
    fun save(entryInstallmentConfig: EntryRecurrenceConfigEntity): Mono<EntryRecurrenceConfigEntity>
}
