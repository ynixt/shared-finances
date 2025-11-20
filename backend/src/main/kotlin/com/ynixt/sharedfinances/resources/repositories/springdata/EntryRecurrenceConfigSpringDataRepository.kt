package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import org.springframework.data.repository.Repository

interface EntryRecurrenceConfigSpringDataRepository :
    EntryRecurrenceConfigRepository,
    Repository<EntryRecurrenceConfigEntity, String>
