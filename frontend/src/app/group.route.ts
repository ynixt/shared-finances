import { Route } from '@angular/router';

import { groupPermissionGuard } from './guards/group-permission.guard';
import { planLimitsEnabledGuard } from './guards/plan-limits-enabled.guard';
import { GroupPermissions__Obj } from './models/generated/com/ynixt/sharedfinances/domain/enums';

export const groupRoute: Route = {
  path: 'groups',
  children: [
    {
      path: '',
      redirectTo: '/app',
      pathMatch: 'full',
    },
    {
      path: 'new',
      loadComponent: () =>
        import('./pages/finances/groups-page/new-group-page/new-group-page.component').then(m => m.NewGroupPageComponent),
      data: {
        pageTitleKey: 'pageTitle.newGroup',
      },
    },
    {
      path: ':id/edit',
      loadComponent: () =>
        import('./pages/finances/groups-page/edit-group-page/edit-group-page.component').then(m => m.EditGroupPageComponent),
      data: {
        pageTitleKey: 'pageTitle.editGroup',
      },
    },
    {
      path: ':groupId/goals',
      children: [
        {
          path: '',
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goals-list-page.component').then(
              m => m.FinancialGoalsListPageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.groupGoals' },
        },
        {
          path: 'new',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
              m => m.FinancialGoalUpsertPageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.groupNewGoal', groupPermission: GroupPermissions__Obj.MANAGE_GOALS },
        },
        {
          path: ':id/allocate',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
              m => m.FinancialGoalAllocationFormPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.goalAllocate',
            goalLedgerMode: 'allocate',
            groupPermission: GroupPermissions__Obj.MANAGE_GOALS,
          },
        },
        {
          path: ':id/reverse',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
              m => m.FinancialGoalAllocationFormPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.goalReverse',
            goalLedgerMode: 'deallocate',
            groupPermission: GroupPermissions__Obj.MANAGE_GOALS,
          },
        },
        {
          path: ':id/schedule',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-schedule-page.component').then(
              m => m.FinancialGoalSchedulePageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.goalSchedule', groupPermission: GroupPermissions__Obj.MANAGE_GOALS },
        },
        {
          path: ':id/schedules/:scheduleId/edit',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-edit-schedule-page.component').then(
              m => m.FinancialGoalEditSchedulePageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.goalEditSchedule', groupPermission: GroupPermissions__Obj.MANAGE_GOALS },
        },
        {
          path: ':id/movements/:movementId/edit',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-edit-ledger-movement-page.component').then(
              m => m.FinancialGoalEditLedgerMovementPageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.goalEditLedgerMovement', groupPermission: GroupPermissions__Obj.MANAGE_GOALS },
        },
        {
          path: ':id/edit',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
              m => m.FinancialGoalUpsertPageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.editGoal', groupPermission: GroupPermissions__Obj.MANAGE_GOALS },
        },
        {
          path: ':id',
          loadComponent: () =>
            import('./pages/finances/financial-goals-page/financial-goal-detail-page.component').then(
              m => m.FinancialGoalDetailPageComponent,
            ),
          data: { pageTitleKey: 'pageTitle.goalDetail' },
        },
      ],
    },
    {
      path: ':groupId/simulations',
      loadComponent: () =>
        import('./pages/finances/financial-simulations-page/group-simulation-jobs-page.component').then(
          m => m.GroupSimulationJobsPageComponent,
        ),
      data: { pageTitleKey: 'pageTitle.simulationJobs' },
    },
    {
      path: ':id/limits',
      canActivate: [planLimitsEnabledGuard],
      loadComponent: () =>
        import('./pages/finances/groups-page/group-limits-page/group-limits-page.component').then(m => m.GroupLimitsPageComponent),
      data: {
        pageTitleKey: 'pageTitle.groupLimits',
      },
    },
    {
      path: ':id/debts/adjustments/new',
      loadComponent: () =>
        import('./pages/finances/groups-page/group-debt-adjustment-page/group-debt-adjustment-page.component').then(
          m => m.GroupDebtAdjustmentPageComponent,
        ),
      data: {
        pageTitleKey: 'pageTitle.groupDebtAdjustment',
      },
    },
    {
      path: ':id/debts/adjustments/:debtId',
      loadComponent: () =>
        import('./pages/finances/groups-page/group-debt-adjustment-page/group-debt-adjustment-page.component').then(
          m => m.GroupDebtAdjustmentPageComponent,
        ),
      data: {
        pageTitleKey: 'pageTitle.groupDebtAdjustment',
      },
    },
    {
      path: ':id/debts/settlements/new',
      loadComponent: () =>
        import('./pages/finances/groups-page/group-debt-settlement-page/group-debt-settlement-page.component').then(
          m => m.GroupDebtSettlementPageComponent,
        ),
      data: {
        pageTitleKey: 'pageTitle.groupDebtSettlement',
      },
    },
    {
      path: ':id/debts',
      loadComponent: () =>
        import('./pages/finances/groups-page/group-debts-page/group-debts-page.component').then(m => m.GroupDebtsPageComponent),
      data: {
        pageTitleKey: 'pageTitle.groupDebts',
      },
    },
    {
      path: ':id',
      loadComponent: () =>
        import('./pages/finances/groups-page/overview-group-page/overview-group-page.component').then(m => m.OverviewGroupPageComponent),
      data: {
        pageTitleKey: 'pageTitle.overviewGroup',
      },
    },
    {
      path: ':id/team',
      loadComponent: () =>
        import('./pages/finances/groups-page/manage-group-team-page/manage-group-team-page.component').then(
          m => m.ManageGroupTeamPageComponent,
        ),
      data: {
        pageTitleKey: 'pageTitle.manageGroupTeam',
      },
    },
    {
      path: ':id/bankAccounts',
      children: [
        {
          path: '',
          loadComponent: () =>
            import('./pages/finances/groups-page/group-bank-accounts-page/group-bank-accounts-page.component').then(
              m => m.GroupBankAccountsPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.groupBankAccounts',
          },
        },
        {
          path: 'new',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/groups-page/associate-bank-account-group-page/associate-bank-account-group-page.component').then(
              m => m.AssociateBankAccountGroupPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.associateBankAccountGroup',
            groupPermission: GroupPermissions__Obj.ADD_BANK_ACCOUNT,
          },
        },
      ],
    },
    {
      path: ':id/creditCards',
      children: [
        {
          path: '',
          loadComponent: () =>
            import('./pages/finances/groups-page/group-credit-cards-page/group-credit-cards-page.component').then(
              m => m.GroupCreditCardsPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.groupCreditCards',
          },
        },
        {
          path: 'new',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/groups-page/associate-credit-card-group-page/associate-credit-card-group-page.component').then(
              m => m.AssociateCreditCardGroupPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.associateCreditCardGroup',
            groupPermission: GroupPermissions__Obj.ADD_CREDIT_CARD,
          },
        },
      ],
    },
    {
      path: ':id/categories',
      children: [
        {
          path: '',
          loadComponent: () =>
            import('./pages/finances/groups-page/group-categories-page/group-categories-page.component').then(
              m => m.GroupCategoriesPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.categories',
          },
        },
        {
          path: 'new',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/groups-page/new-group-category-page/new-group-category-page.component').then(
              m => m.NewGroupCategoryPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.newCategory',
            groupPermission: GroupPermissions__Obj.NEW_CATEGORY,
          },
        },
        {
          path: 'edit/:categoryId',
          canActivate: [groupPermissionGuard],
          loadComponent: () =>
            import('./pages/finances/groups-page/edit-group-category-page/edit-group-category-page.component').then(
              m => m.EditGroupCategoryPageComponent,
            ),
          data: {
            pageTitleKey: 'pageTitle.editCategory',
            groupPermission: GroupPermissions__Obj.EDIT_CATEGORY,
          },
        },
      ],
    },
  ],
};
