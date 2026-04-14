package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.GroupUsersDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.GroupUsersSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupUsersRepositoryImpl(
    private val dcRepository: GroupUsersDatabaseClientRepository,
    private val springDataRepository: GroupUsersSpringDataRepository,
) : GroupUsersRepository {
    override fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> = springDataRepository.countByGroupIdAndUserId(groupId, userId)

    override fun findOneByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<GroupUserEntity> = springDataRepository.findOneByGroupIdAndUserId(groupId, userId)

    override fun save(groupUser: GroupUserEntity): Mono<GroupUserEntity> = springDataRepository.save(groupUser)

    override fun findAllMembers(groupId: UUID): Flux<GroupUserEntity> = dcRepository.findAllMembers(groupId)

    override fun findAllOptedInUserIds(groupId: UUID): Flux<UUID> = dcRepository.findAllOptedInUserIds(groupId)

    override fun updateRole(
        userId: UUID,
        groupId: UUID,
        newRole: UserGroupRole,
    ): Mono<Long> =
        springDataRepository.updateRole(
            userId = userId,
            groupId = groupId,
            newRole = newRole,
        )

    override fun updatePlanningSimulatorOptIn(
        userId: UUID,
        groupId: UUID,
        allowPlanningSimulator: Boolean,
    ): Mono<Long> =
        springDataRepository.updatePlanningSimulatorOptIn(
            userId = userId,
            groupId = groupId,
            allowPlanningSimulator = allowPlanningSimulator,
        )

    override fun deleteByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> = springDataRepository.deleteByGroupIdAndUserId(groupId, userId)
}
