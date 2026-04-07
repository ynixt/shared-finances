package com.ynixt.sharedfinances.scenarios.wallet.support

import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import com.ynixt.sharedfinances.scenarios.user.support.UserScenarioSetupOps
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

fun walletScenario(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
    block: suspend WalletScenarioDsl.() -> Unit,
): WalletScenarioDsl =
    runBlocking {
        WalletScenarioDsl(initialDate).apply {
            block()
        }
    }

class WalletScenarioDsl(
    initialDate: LocalDate = LocalDate.of(2026, 1, 1),
) {
    private val runtime = ScenarioRuntime(initialDate)
    private val context = WalletScenarioContext()
    private val userSetupOps = UserScenarioSetupOps(runtime = runtime, context = context)

    private val resolver =
        WalletScenarioResolver(
            runtime = runtime,
            context = context,
            createDefaultUser = ::createDefaultUser,
        )
    private val walletSetupOps = WalletScenarioSetupOps(runtime = runtime, context = context, resolver = resolver)
    val given = WalletScenarioGiven(userSetupOps = userSetupOps, walletSetupOps = walletSetupOps)
    val whenActions = WalletScenarioWhen(runtime = runtime, context = context, resolver = resolver)
    val then = WalletScenarioThen(resolver = resolver)

    suspend fun given(block: suspend WalletScenarioGiven.() -> Unit): WalletScenarioDsl =
        chain {
            this@WalletScenarioDsl.given.block()
        }

    suspend fun `when`(block: suspend WalletScenarioWhen.() -> Unit): WalletScenarioDsl =
        chain {
            this@WalletScenarioDsl.whenActions.block()
        }

    suspend fun then(block: suspend WalletScenarioThen.() -> Unit): WalletScenarioDsl =
        chain {
            this@WalletScenarioDsl.then.block()
        }

    private suspend fun chain(action: suspend () -> Unit): WalletScenarioDsl {
        action()
        return this
    }

    private suspend fun createDefaultUser() {
        userSetupOps.createUser()
    }
}
