package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.GroupUsersR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.GroupUsersSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class GroupUsersRepositoryImpl(
    private val r2dbcRepository: GroupUsersR2DBCRepository,
    private val springDataRepository: GroupUsersSpringDataRepository,
) : GroupUsersRepository {
    override fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> = springDataRepository.countByGroupIdAndUserId(groupId, userId)

    override fun findOneByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<GroupUser> = springDataRepository.findOneByGroupIdAndUserId(groupId, userId)

    override fun save(groupUser: GroupUser): Mono<GroupUser> = springDataRepository.save(groupUser)

    override fun findAllMembers(groupId: UUID): Flux<GroupUser> = r2dbcRepository.findAllMembers(groupId)

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
}
