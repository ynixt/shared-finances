package com.ynixt.sharedfinances.scenarios.accountdeletion

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import com.ynixt.sharedfinances.scenarios.accountdeletion.support.accountDeletionScenario
import org.junit.jupiter.api.Test
import java.util.UUID

class AccountDeletionScenarioDslTest {
    @Test
    fun `sole group member deletion removes group and user`() {
        lateinit var userId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                userId = user()
                groupId = group("Home", userId to UserGroupRole.ADMIN)
            }

            `when` {
                accountDeleted(userId)
            }

            then {
                userShouldNotExist(userId)
                groupShouldNotExist(groupId)
                complianceCleanupRecordedFor(userId)
                avatarDeletionRecordedFor(userId)
            }
        }
    }

    @Test
    fun `deleting an owner removes the group even when active members remain`() {
        lateinit var ownerId: UUID
        lateinit var memberId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                ownerId = user(firstName = "Owner")
                memberId = user(firstName = "Member")
                groupId =
                    group(
                        "Shared",
                        ownerId to UserGroupRole.ADMIN,
                        memberId to UserGroupRole.EDITOR,
                    )
            }

            `when` {
                accountDeleted(ownerId)
            }

            then {
                userShouldNotExist(ownerId)
                groupShouldNotExist(groupId)
                deletedGroupNotificationShouldInclude(groupId, ownerId, memberId)
                complianceCleanupRecordedFor(ownerId)
                avatarDeletionRecordedFor(ownerId)
            }
        }
    }

    @Test
    fun `deleting a non-owner removes only that membership`() {
        lateinit var ownerId: UUID
        lateinit var memberId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                ownerId = user(firstName = "Owner")
                memberId = user(firstName = "Member")
                groupId = group("Shared", ownerId to UserGroupRole.ADMIN, memberId to UserGroupRole.EDITOR)
            }
            `when` { accountDeleted(memberId) }
            then {
                groupShouldExist(groupId)
                memberShouldNotExist(groupId, memberId)
                memberShouldHaveRole(groupId, ownerId, UserGroupRole.ADMIN)
            }
        }
    }

    @Test
    fun `group scoped wallet and recurrence rows for deleted user are removed`() {
        lateinit var adminId: UUID
        lateinit var editorId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                adminId = user()
                editorId = user()
                groupId =
                    group(
                        "Shared",
                        adminId to UserGroupRole.ADMIN,
                        editorId to UserGroupRole.EDITOR,
                    )
                groupScopedWalletEvent(adminId, groupId)
                groupScopedRecurrence(adminId, groupId)
            }

            `when` {
                accountDeleted(adminId)
            }

            then {
                noWalletEventsForUserInGroup(adminId, groupId)
                noRecurrenceEventsForUserInGroup(adminId, groupId)
            }
        }
    }

    @Test
    fun `personal wallet and recurrence rows are removed so user deletion can complete`() {
        lateinit var userId: UUID

        accountDeletionScenario {
            given {
                userId = user()
                personalWalletEvent(userId)
                personalRecurrence(userId)
            }

            `when` {
                accountDeleted(userId)
            }

            then {
                userShouldNotExist(userId)
                noWalletEventsForUser(userId)
                noRecurrenceEventsForUser(userId)
                complianceCleanupRecordedFor(userId)
                avatarDeletionRecordedFor(userId)
            }
        }
    }

    @Test
    fun `deleting unknown user is a no-op`() {
        val ghost = UUID.randomUUID()

        accountDeletionScenario {
            `when` {
                accountDeleted(ghost)
            }

            then {
                complianceCleanupNotRecordedFor(ghost)
                avatarDeletionNotRecordedFor(ghost)
            }
        }
    }

    @Test
    fun `deleting the last non-owner admin does not promote other members`() {
        lateinit var ownerId: UUID
        lateinit var adminId: UUID
        lateinit var viewerId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                ownerId = user()
                adminId = user()
                viewerId = user()
                groupId =
                    group(
                        "Team",
                        ownerId to UserGroupRole.ADMIN,
                        adminId to UserGroupRole.ADMIN,
                        viewerId to UserGroupRole.VIEWER,
                    )
            }

            `when` {
                accountDeleted(adminId)
            }

            then {
                groupShouldExist(groupId)
                memberShouldHaveRole(groupId, ownerId, UserGroupRole.ADMIN)
                memberShouldHaveRole(groupId, viewerId, UserGroupRole.VIEWER)
                memberShouldNotExist(groupId, adminId)
            }
        }
    }

    @Test
    fun `deleting a user removes every owned group and preserves groups where they are only a member`() {
        lateinit var departingId: UUID
        lateinit var otherOwnerId: UUID
        lateinit var memberId: UUID
        lateinit var firstOwnedGroupId: UUID
        lateinit var secondOwnedGroupId: UUID
        lateinit var membershipOnlyGroupId: UUID

        accountDeletionScenario {
            given {
                departingId = user()
                otherOwnerId = user()
                memberId = user()
                firstOwnedGroupId = group("First", departingId to UserGroupRole.ADMIN, memberId to UserGroupRole.VIEWER)
                secondOwnedGroupId = group("Second", departingId to UserGroupRole.ADMIN, memberId to UserGroupRole.EDITOR)
                membershipOnlyGroupId =
                    group(
                        "Survives",
                        otherOwnerId to UserGroupRole.ADMIN,
                        departingId to UserGroupRole.ADMIN,
                        ownerUserId = otherOwnerId,
                    )
            }
            `when` { accountDeleted(departingId) }
            then {
                groupShouldNotExist(firstOwnedGroupId)
                groupShouldNotExist(secondOwnedGroupId)
                groupShouldExist(membershipOnlyGroupId)
                memberShouldNotExist(membershipOnlyGroupId, departingId)
                memberShouldHaveRole(membershipOnlyGroupId, otherOwnerId, UserGroupRole.ADMIN)
            }
        }
    }
}
