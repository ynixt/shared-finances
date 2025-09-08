package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.Group
import com.ynixt.sharedfinances.domain.entities.GroupUser
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.NewGroupRequest
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.domain.services.GroupService
import com.ynixt.sharedfinances.domain.services.actionevents.GroupActionEventService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class GroupServiceImpl(
    private val groupRepository: GroupRepository,
    private val groupUserRepository: GroupUsersRepository,
    private val groupActionEventService: GroupActionEventService,
) : GroupService {
    override fun findAllGroups(userId: UUID): Mono<List<Group>> = groupRepository.findAllByUserIdOrderByName(userId).collectList()

    override fun findGroup(
        userId: UUID,
        id: UUID,
    ): Mono<Group> =
        groupRepository.findOneByUserIdAndId(
            userId = userId,
            id = id,
        )

    @Transactional
    override fun newGroup(
        userId: UUID,
        newGroupRequest: NewGroupRequest,
    ): Mono<Group> =
        groupRepository
            .save(
                Group(
                    name = newGroupRequest.name,
                ),
            ).flatMap { g ->
                groupUserRepository
                    .save(
                        GroupUser(
                            userId = userId,
                            groupId = g.id!!,
                            role = UserGroupRole.ADMIN,
                        ),
                    ).thenReturn(g)
            }.flatMap { g ->
                groupActionEventService
                    .sendInsertedGroup(
                        group = g,
                        userId = userId,
                    ).thenReturn(g)
            }

    override fun findAllMembers(
        userId: UUID,
        id: UUID,
    ): Mono<List<GroupUser>> =
        userIsInGroup(userId = userId, id = id).flatMap {
            if (it) {
                groupUserRepository
                    .findAllMembers(
                        id,
                    ).collectList()
            } else {
                Mono.empty()
            }
        }

    private fun userIsInGroup(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean> =
        groupUserRepository
            .countByGroupIdAndUserId(
                userId = userId,
                groupId = id,
            ).map { it > 0 }
}
