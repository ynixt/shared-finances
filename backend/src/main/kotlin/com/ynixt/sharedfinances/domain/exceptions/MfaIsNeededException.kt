package com.ynixt.sharedfinances.domain.exceptions

import java.util.UUID

class MfaIsNeededException(
    val challengeId: UUID,
) : Exception()
