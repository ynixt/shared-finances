package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.mapper.WalletItemMapper
import com.ynixt.sharedfinances.domain.repositories.WalletItemRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertEquals

class WalletItemServiceImplTest {
    private val repository = Mockito.mock(WalletItemRepository::class.java)
    private val service = WalletItemServiceImpl(repository, Mockito.mock(WalletItemMapper::class.java))

    @Test
    fun `applies a normalized name query before paginating enabled wallet items`() =
        runTest {
            val userId = UUID.randomUUID()
            val pageable = PageRequest.of(2, 10)
            Mockito
                .`when`(repository.countByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, "nubank"))
                .thenReturn(Mono.just(21L))
            Mockito
                .`when`(repository.findAllByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, "nubank", pageable))
                .thenReturn(Flux.empty())

            val result = service.findAllItems(userId, pageable, query = "  nubank  ")

            assertEquals(10, result.size)
            assertEquals(2, result.number)
            Mockito.verify(repository).countByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, "nubank")
            Mockito.verify(repository).findAllByUserIdAndEnabledAndNameContainingIgnoreCase(userId, true, "nubank", pageable)
            Mockito.verify(repository, Mockito.never()).findAllByUserIdAndEnabled(userId, true, pageable)
        }
}
