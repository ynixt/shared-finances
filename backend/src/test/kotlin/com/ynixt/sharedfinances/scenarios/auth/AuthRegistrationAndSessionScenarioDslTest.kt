package com.ynixt.sharedfinances.scenarios.auth

import com.ynixt.sharedfinances.scenarios.auth.support.authScenario
import org.junit.jupiter.api.Test
import java.util.UUID

class AuthRegistrationAndSessionScenarioDslTest {
    @Test
    fun `login persists session with ttl around one month`() {
        authScenario {
            given {
                user()
            }

            `when` {
                loginCurrentUser()
            }

            then {
                currentSessionTtlShouldBeBetweenDays(minimumDays = 25, maximumDays = 35)
            }
        }
    }

    @Test
    fun `register rejects missing terms consent`() {
        authScenario {
            `when` {
                attemptRegister(
                    acceptTerms = false,
                    acceptPrivacy = true,
                )
            }

            then {
                registrationShouldFailForMissingTermsConsent()
            }
        }
    }

    @Test
    fun `register persists legal metadata and optional gravatar opt out`() {
        val email = "reg-${UUID.randomUUID()}@example.com"

        authScenario {
            `when` {
                register(
                    email = email,
                    acceptTerms = true,
                    acceptPrivacy = true,
                    gravatarOptIn = false,
                )
            }

            then {
                registeredUserShouldPersistLegalMetadata(email = email)
                registeredUserPhotoShouldBeNull(email = email)
            }
        }
    }

    @Test
    fun `deleting current account clears session and refresh indexes`() {
        authScenario {
            given {
                user()
            }

            `when` {
                loginCurrentUser()
            }

            then {
                currentSessionKeyShouldExist()
                currentUserSessionIndexShouldExist()
                currentRefreshIndexShouldExist()
            }

            `when` {
                deleteCurrentAccount()
            }

            then {
                currentSessionArtifactsShouldBeDeleted()
            }
        }
    }
}
