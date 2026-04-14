package com.ynixt.sharedfinances.scenarios.accountdeletion.support

import com.ynixt.sharedfinances.domain.entities.groups.GroupEntity
import com.ynixt.sharedfinances.domain.entities.groups.GroupUserEntity
import com.ynixt.sharedfinances.domain.enums.GroupPermissions
import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.domain.models.groups.GroupWithRole
import com.ynixt.sharedfinances.domain.repositories.GroupRepository
import com.ynixt.sharedfinances.domain.repositories.GroupUsersRepository
import com.ynixt.sharedfinances.scenarios.support.nowOffset
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Shared in-memory group + membership store for account-deletion scenario tests.
 */
internal class InMemoryAccountDeletionGroupStore :
    GroupRepository,
    GroupUsersRepository {
    private val groups = linkedMapOf<UUID, GroupEntity>()
    private val memberships = mutableListOf<MembershipRow>()

    private data class MembershipRow(
        var id: UUID,
        val groupId: UUID,
        val userId: UUID,
        var role: UserGroupRole,
        var allowPlanningSimulator: Boolean,
    )

    override fun findAllByUserIdOrderByName(userId: UUID): Flux<GroupWithRole> {
        val result =
            memberships
                .filter { it.userId == userId }
                .mapNotNull { row ->
                    val g = groups[row.groupId] ?: return@mapNotNull null
                    GroupWithRole(
                        id = g.id,
                        createdAt = g.createdAt,
                        updatedAt = g.updatedAt,
                        name = g.name,
                        role = row.role,
                    ).also { it.permissions = GroupPermissions.entries.toSet() }
                }.sortedBy { it.name }
        return Flux.fromIterable(result)
    }

    override fun findOneByUserIdAndId(
        userId: UUID,
        id: UUID,
    ): Mono<GroupWithRole> =
        Mono.justOrEmpty(
            memberships
                .firstOrNull { it.userId == userId && it.groupId == id }
                ?.let { row ->
                    val g = groups[id] ?: return@let null
                    GroupWithRole(
                        id = g.id,
                        createdAt = g.createdAt,
                        updatedAt = g.updatedAt,
                        name = g.name,
                        role = row.role,
                    ).also { it.permissions = GroupPermissions.entries.toSet() }
                },
        )

    override fun edit(
        id: UUID,
        newName: String,
    ): Mono<Long> =
        Mono.just(
            groups[id]?.let {
                val updated =
                    GroupEntity(name = newName).also { copy ->
                        copy.id = it.id
                        copy.createdAt = it.createdAt
                        copy.updatedAt = nowOffset()
                    }
                groups[id] = updated
                1L
            } ?: 0L,
        )

    override fun findById(id: UUID): Mono<GroupEntity> = Mono.justOrEmpty(groups[id])

    override fun deleteById(id: UUID): Mono<Long> {
        if (!groups.containsKey(id)) {
            return Mono.just(0L)
        }
        groups.remove(id)
        memberships.removeIf { it.groupId == id }
        return Mono.just(1L)
    }

    override fun existsById(id: UUID): Mono<Boolean> = Mono.just(groups.containsKey(id))

    override fun <S : GroupEntity> save(entity: S): Mono<S> {
        val id = entity.id ?: UUID.randomUUID()
        entity.id = id
        entity.createdAt = entity.createdAt ?: nowOffset()
        entity.updatedAt = nowOffset()
        @Suppress("UNCHECKED_CAST")
        groups[id] = entity as GroupEntity
        return Mono.just(entity)
    }

    override fun <S : GroupEntity> saveAll(entity: Iterable<S>): Flux<S> = Flux.fromIterable(entity).flatMap { save(it) }

    override fun findAllByIdIn(id: Collection<UUID>): Flux<GroupEntity> = Flux.fromIterable(id.mapNotNull { groups[it] })

    override fun countByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> =
        Mono.just(
            memberships.count { it.groupId == groupId && it.userId == userId }.toLong(),
        )

    override fun findOneByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<GroupUserEntity> =
        Mono.justOrEmpty(
            memberships
                .firstOrNull { it.groupId == groupId && it.userId == userId }
                ?.toEntity(),
        )

    override fun save(groupUser: GroupUserEntity): Mono<GroupUserEntity> {
        val id = groupUser.id ?: UUID.randomUUID()
        memberships.removeIf { it.groupId == groupUser.groupId && it.userId == groupUser.userId }
        memberships.add(
            MembershipRow(
                id = id,
                groupId = groupUser.groupId,
                userId = groupUser.userId,
                role = groupUser.role,
                allowPlanningSimulator = groupUser.allowPlanningSimulator,
            ),
        )
        val saved =
            GroupUserEntity(
                groupId = groupUser.groupId,
                userId = groupUser.userId,
                role = groupUser.role,
                allowPlanningSimulator = groupUser.allowPlanningSimulator,
            ).also { it.id = id }
        return Mono.just(saved)
    }

    override fun findAllMembers(groupId: UUID): Flux<GroupUserEntity> =
        Flux.fromIterable(
            memberships.filter { it.groupId == groupId }.map { it.toEntity() },
        )

    override fun findAllOptedInUserIds(groupId: UUID): Flux<UUID> =
        Flux.fromIterable(
            memberships
                .filter { it.groupId == groupId && it.allowPlanningSimulator }
                .map { it.userId },
        )

    override fun updateRole(
        userId: UUID,
        groupId: UUID,
        newRole: UserGroupRole,
    ): Mono<Long> {
        val row = memberships.firstOrNull { it.groupId == groupId && it.userId == userId } ?: return Mono.just(0L)
        row.role = newRole
        return Mono.just(1L)
    }

    override fun updatePlanningSimulatorOptIn(
        userId: UUID,
        groupId: UUID,
        allowPlanningSimulator: Boolean,
    ): Mono<Long> {
        val row = memberships.firstOrNull { it.groupId == groupId && it.userId == userId } ?: return Mono.just(0L)
        row.allowPlanningSimulator = allowPlanningSimulator
        return Mono.just(1L)
    }

    override fun deleteByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> {
        val before = memberships.size
        memberships.removeIf { it.groupId == groupId && it.userId == userId }
        return Mono.just((before - memberships.size).toLong())
    }

    private fun MembershipRow.toEntity(): GroupUserEntity =
        GroupUserEntity(
            groupId = groupId,
            userId = userId,
            role = role,
            allowPlanningSimulator = allowPlanningSimulator,
        ).also { it.id = id }
}
