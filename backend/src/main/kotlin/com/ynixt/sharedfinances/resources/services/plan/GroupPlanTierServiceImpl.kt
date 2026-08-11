package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.exceptions.http.GroupNotFoundException
import com.ynixt.sharedfinances.domain.repositories.GroupPlanTierRepository
import com.ynixt.sharedfinances.domain.services.plan.GroupPlanTierService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupPlanTierServiceImpl(
    private val repository: GroupPlanTierRepository,
) : GroupPlanTierService {
    override suspend fun resolve(groupId: UUID): GroupPlanTier = repository.resolve(groupId) ?: throw GroupNotFoundException(groupId)
}
