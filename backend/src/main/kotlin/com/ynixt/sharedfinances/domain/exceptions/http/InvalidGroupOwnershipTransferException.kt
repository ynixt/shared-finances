package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidGroupOwnershipTransferException(
    reason: Reason,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n =
            when (reason) {
                Reason.SELF_TRANSFER -> "apiErrors.groupOwnership.selfTransfer"
                Reason.TARGET_NOT_MEMBER -> "apiErrors.groupOwnership.targetNotMember"
                Reason.OWNER_MUST_BE_ADMIN -> "apiErrors.groupOwnership.ownerMustBeAdmin"
            },
        alternativeMessage =
            when (reason) {
                Reason.SELF_TRANSFER -> "Ownership cannot be transferred to the current owner."
                Reason.TARGET_NOT_MEMBER -> "The new owner must be a current member of the group."
                Reason.OWNER_MUST_BE_ADMIN -> "The group owner must retain the ADMIN role."
            },
    ) {
    enum class Reason {
        SELF_TRANSFER,
        TARGET_NOT_MEMBER,
        OWNER_MUST_BE_ADMIN,
    }
}
