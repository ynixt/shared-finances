package com.ynixt.sharedfinances.domain.models

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ListEntryRequestTest {
    @Test
    fun `should reject userIds when groupIds is empty`() {
        val ownerId = UUID.randomUUID()

        assertThatThrownBy {
            ListEntryRequest(
                walletItemId = null,
                groupIds = emptySet(),
                userIds = setOf(ownerId),
                pageRequest = CursorPageRequest(size = 20),
                minimumDate = LocalDate.of(2026, 4, 1),
                maximumDate = LocalDate.of(2026, 4, 30),
                billId = null,
                billDate = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Filter userIds requires at least one groupId")
    }

    @Test
    fun `should allow userIds when groupIds is provided`() {
        val ownerId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        assertThatCode {
            ListEntryRequest(
                walletItemId = null,
                groupIds = setOf(groupId),
                userIds = setOf(ownerId),
                pageRequest = CursorPageRequest(size = 20),
                minimumDate = LocalDate.of(2026, 4, 1),
                maximumDate = LocalDate.of(2026, 4, 30),
                billId = null,
                billDate = null,
            )
        }.doesNotThrowAnyException()
    }
}
