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
    fun `sole admin with editor promotes editor when admin deletes account`() {
        lateinit var adminId: UUID
        lateinit var editorId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                adminId = user(firstName = "Admin")
                editorId = user(firstName = "Editor")
                groupId =
                    group(
                        "Shared",
                        adminId to UserGroupRole.ADMIN,
                        editorId to UserGroupRole.EDITOR,
                    )
            }

            `when` {
                accountDeleted(adminId)
            }

            then {
                userShouldNotExist(adminId)
                groupShouldExist(groupId)
                memberShouldNotExist(groupId, adminId)
                memberShouldHaveRole(groupId, editorId, UserGroupRole.ADMIN)
                complianceCleanupRecordedFor(adminId)
                avatarDeletionRecordedFor(adminId)
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
    fun `sole admin with only viewer promotes viewer when admin deletes account`() {
        lateinit var adminId: UUID
        lateinit var viewerId: UUID
        lateinit var groupId: UUID

        accountDeletionScenario {
            given {
                adminId = user()
                viewerId = user()
                groupId =
                    group(
                        "Team",
                        adminId to UserGroupRole.ADMIN,
                        viewerId to UserGroupRole.VIEWER,
                    )
            }

            `when` {
                accountDeleted(adminId)
            }

            then {
                groupShouldExist(groupId)
                memberShouldHaveRole(groupId, viewerId, UserGroupRole.ADMIN)
                memberShouldNotExist(groupId, adminId)
            }
        }
    }
}
