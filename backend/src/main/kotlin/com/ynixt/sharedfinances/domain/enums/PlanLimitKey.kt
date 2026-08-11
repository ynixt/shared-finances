package com.ynixt.sharedfinances.domain.enums

enum class PlanLimitKey(
    val scope: PlanLimitScope,
    val countableQuota: Boolean,
    val monthly: Boolean = false,
) {
    BANK_ACCOUNTS(PlanLimitScope.USER, countableQuota = true),
    CREDIT_CARDS(PlanLimitScope.USER, countableQuota = true),
    CATEGORIES(PlanLimitScope.USER, countableQuota = true),
    GOALS(PlanLimitScope.USER, countableQuota = true),
    ACTIVE_SCHEDULES(PlanLimitScope.USER, countableQuota = true),
    IMPORTS_PER_MONTH(PlanLimitScope.USER, countableQuota = true, monthly = true),
    SIMULATIONS_PER_MONTH(PlanLimitScope.USER, countableQuota = true, monthly = true),
    OWNED_GROUPS(PlanLimitScope.USER, countableQuota = true),
    INACTIVITY_RETENTION_MONTHS(PlanLimitScope.USER, countableQuota = false),
    IMPORT_MAX_LINES(PlanLimitScope.USER, countableQuota = false),
    GROUP_CATEGORIES(PlanLimitScope.GROUP, countableQuota = true),
    GROUP_GOALS(PlanLimitScope.GROUP, countableQuota = true),
    GROUP_ACTIVE_SCHEDULES(PlanLimitScope.GROUP, countableQuota = true),
    GROUP_MEMBERS(PlanLimitScope.GROUP, countableQuota = true),
}
