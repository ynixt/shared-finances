package com.ynixt.sharedfinances.domain.models.plan

data class ResolvedPlanLimit(
    val value: Int?,
) {
    val unlimited: Boolean = value == null

    companion object {
        fun unlimited() = ResolvedPlanLimit(null)

        fun finite(value: Int) = ResolvedPlanLimit(value)
    }
}
