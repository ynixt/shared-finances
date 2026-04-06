package com.ynixt.sharedfinances

import com.ynixt.sharedfinances.support.IntegrationTestContainers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests : IntegrationTestContainers() {
    @BeforeEach
    fun setup() {
        super.beforeEach()
    }

    @Test
    fun contextLoads() {
    }
}
