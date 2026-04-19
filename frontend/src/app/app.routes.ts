import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';
import { notLoggedGuard } from './guards/not-logged.guard';
import { emailConfirmationFlowsEnabledGuard, passwordRecoveryEnabledGuard } from './guards/open-auth-feature.guards';

export const routes: Routes = [
  {
    'path': '',
    canActivate: [notLoggedGuard],
    loadComponent: () => import('./pages/showcase-page/showcase-page.component').then(m => m.ShowcasePageComponent),
    data: {
      pageTitleKey: 'pageTitle.home',
    },
  },
  {
    'path': 'login',
    canActivate: [notLoggedGuard],
    loadComponent: () => import('./pages/login-page/login-page.component').then(m => m.LoginPageComponent),
    data: {
      pageTitleKey: 'pageTitle.login',
    },
  },
  {
    'path': 'register',
    canActivate: [notLoggedGuard],
    loadComponent: () => import('./pages/registration-page/registration-page.component').then(m => m.RegistrationPageComponent),
    data: {
      pageTitleKey: 'pageTitle.register',
    },
  },
  {
    path: 'pending-email-confirmation',
    canActivate: [notLoggedGuard],
    canMatch: [emailConfirmationFlowsEnabledGuard],
    loadComponent: () =>
      import('./pages/pending-email-confirmation-page/pending-email-confirmation-page.component').then(
        m => m.PendingEmailConfirmationPageComponent,
      ),
    data: { pageTitleKey: 'pageTitle.pendingEmail' },
  },
  {
    path: 'confirm-email',
    canActivate: [notLoggedGuard],
    canMatch: [emailConfirmationFlowsEnabledGuard],
    loadComponent: () => import('./pages/confirm-email-page/confirm-email-page.component').then(m => m.ConfirmEmailPageComponent),
    data: { pageTitleKey: 'pageTitle.confirmEmail' },
  },
  {
    path: 'forgot-password',
    canActivate: [notLoggedGuard],
    canMatch: [passwordRecoveryEnabledGuard],
    loadComponent: () => import('./pages/forgot-password-page/forgot-password-page.component').then(m => m.ForgotPasswordPageComponent),
    data: { pageTitleKey: 'pageTitle.forgotPassword' },
  },
  {
    path: 'reset-password',
    canActivate: [notLoggedGuard],
    canMatch: [passwordRecoveryEnabledGuard],
    loadComponent: () => import('./pages/reset-password-page/reset-password-page.component').then(m => m.ResetPasswordPageComponent),
    data: { pageTitleKey: 'pageTitle.resetPassword' },
  },
  {
    path: 'legal/terms',
    loadComponent: () => import('./pages/legal/legal-document-page.component').then(m => m.LegalDocumentPageComponent),
    data: {
      pageTitleKey: 'pageTitle.legalTerms',
      legalDoc: 'terms',
    },
  },
  {
    path: 'legal/privacy',
    loadComponent: () => import('./pages/legal/legal-document-page.component').then(m => m.LegalDocumentPageComponent),
    data: {
      pageTitleKey: 'pageTitle.legalPrivacy',
      legalDoc: 'privacy',
    },
  },
  {
    path: 'welcome',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/onboarding-page/onboarding-page.component').then(m => m.OnboardingPageComponent),
    data: {
      pageTitleKey: 'pageTitle.onboarding',
    },
  },
  {
    'path': 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/finances/finances-home-page/finances-page.component').then(m => m.FinancesPageComponent),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/finances/finances-overview-page/finances-overview-page.component').then(m => m.FinancesOverviewPageComponent),
        data: {
          pageTitleKey: 'pageTitle.app',
        },
      },
      {
        path: 'settings',
        loadComponent: () => import('./pages/finances/user-settings/user-settings.component').then(m => m.UserSettingsComponent),
        data: {
          pageTitleKey: 'pageTitle.settings',
        },
      },
      {
        path: 'transactions',
        children: [
          {
            path: 'scheduler-manager/edit/:recurrenceConfigId',
            loadComponent: () =>
              import('./pages/finances/transactions-page/edit-scheduled-transaction-page/edit-scheduled-transaction-page.component').then(
                m => m.EditScheduledTransactionPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.editTransaction',
            },
          },
          {
            path: 'scheduler-manager',
            loadComponent: () =>
              import('./pages/finances/transactions-page/scheduled-execution-manager-page/scheduled-execution-manager-page.component').then(
                m => m.ScheduledExecutionManagerPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.scheduleManager',
            },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/transactions-page/new-transaction-page/new-transaction-page.component').then(
                m => m.NewTransactionPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.newTransaction',
            },
          },
          {
            path: 'edit/:id',
            loadComponent: () =>
              import('./pages/finances/transactions-page/edit-transaction-page/edit-transaction-page.component').then(
                m => m.EditTransactionPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.editTransaction',
            },
          },
        ],
      },
      {
        path: 'bankAccounts',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/finances-bank-accounts-page.component').then(
                m => m.FinancesBankAccountsPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.bankAccounts',
            },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/new-bank-account-page/new-bank-account-page.component').then(
                m => m.NewBankAccountPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.newBankAccount',
            },
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/view-bank-account-page/view-bank-account-page.component').then(
                m => m.ViewBankAccountPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.bankAccount',
            },
          },
          {
            path: 'edit/:id',
            loadComponent: () =>
              import('./pages/finances/finances-bank-accounts-page/edit-bank-account-page/edit-bank-account-page.component').then(
                m => m.EditBankAccountPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.editBankAccount',
            },
          },
        ],
      },
      {
        path: 'creditCards',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/finances/finances-credit-cards-page/finances-credit-cards-page.component').then(
                m => m.FinancesCreditCardsPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.creditCards',
            },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/finances-credit-cards-page/new-credit-card-page/new-credit-card-page.component').then(
                m => m.NewCreditCardPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.creditCards',
            },
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./pages/finances/finances-credit-cards-page/view-credit-card-page/view-credit-card-page.component').then(
                m => m.ViewCreditCardPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.creditCard',
            },
          },
          {
            path: 'edit/:id',
            loadComponent: () =>
              import('./pages/finances/finances-credit-cards-page/edit-credit-card-page/edit-credit-card-page.component').then(
                m => m.EditCreditCardPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.creditCards',
            },
          },
        ],
      },
      {
        path: 'categories',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/finances/user-categories-page/user-categories-page.component').then(m => m.UserCategoriesPageComponent),
            data: {
              pageTitleKey: 'pageTitle.categories',
            },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/user-categories-page/new-user-category-page/new-user-category-page.component').then(
                m => m.NewUserCategoryPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.newCategory',
            },
          },
          {
            path: 'edit/:id',
            loadComponent: () =>
              import('./pages/finances/user-categories-page/edit-user-category-page/edit-user-category-page.component').then(
                m => m.EditUserCategoryPageComponent,
              ),
            data: {
              pageTitleKey: 'pageTitle.editCategory',
            },
          },
        ],
      },
      {
        path: 'exchange-rates',
        loadComponent: () =>
          import('./pages/finances/finances-exchange-rates-page/finances-exchange-rates-page.component').then(
            m => m.FinancesExchangeRatesPageComponent,
          ),
        data: {
          pageTitleKey: 'pageTitle.exchangeRates',
        },
      },
      {
        path: 'goals',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goals-list-page.component').then(
                m => m.FinancialGoalsListPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goals' },
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
                m => m.FinancialGoalUpsertPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.newGoal' },
          },
          {
            path: ':id/allocate',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
                m => m.FinancialGoalAllocationFormPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goalAllocate', goalLedgerMode: 'allocate' },
          },
          {
            path: ':id/reverse',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
                m => m.FinancialGoalAllocationFormPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goalReverse', goalLedgerMode: 'deallocate' },
          },
          {
            path: ':id/schedule',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-schedule-page.component').then(
                m => m.FinancialGoalSchedulePageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goalSchedule' },
          },
          {
            path: ':id/schedules/:scheduleId/edit',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-edit-schedule-page.component').then(
                m => m.FinancialGoalEditSchedulePageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goalEditSchedule' },
          },
          {
            path: ':id/movements/:movementId/edit',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-edit-ledger-movement-page.component').then(
                m => m.FinancialGoalEditLedgerMovementPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.goalEditLedgerMovement' },
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
                m => m.FinancialGoalUpsertPageComponent,
              ),
            data: { pageTitleKey: 'pageTitle.editGoal' },
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
        path: 'simulations',
        loadComponent: () =>
          import('./pages/finances/financial-simulations-page/simulation-jobs-page.component').then(m => m.SimulationJobsPageComponent),
        data: { pageTitleKey: 'pageTitle.simulationJobs' },
      },
      {
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
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
                    m => m.FinancialGoalUpsertPageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.groupNewGoal' },
              },
              {
                path: ':id/allocate',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
                    m => m.FinancialGoalAllocationFormPageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.goalAllocate', goalLedgerMode: 'allocate' },
              },
              {
                path: ':id/reverse',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-allocation-form-page.component').then(
                    m => m.FinancialGoalAllocationFormPageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.goalReverse', goalLedgerMode: 'deallocate' },
              },
              {
                path: ':id/schedule',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-schedule-page.component').then(
                    m => m.FinancialGoalSchedulePageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.goalSchedule' },
              },
              {
                path: ':id/schedules/:scheduleId/edit',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-edit-schedule-page.component').then(
                    m => m.FinancialGoalEditSchedulePageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.goalEditSchedule' },
              },
              {
                path: ':id/movements/:movementId/edit',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-edit-ledger-movement-page.component').then(
                    m => m.FinancialGoalEditLedgerMovementPageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.goalEditLedgerMovement' },
              },
              {
                path: ':id/edit',
                loadComponent: () =>
                  import('./pages/finances/financial-goals-page/financial-goal-upsert-page.component').then(
                    m => m.FinancialGoalUpsertPageComponent,
                  ),
                data: { pageTitleKey: 'pageTitle.editGoal' },
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
              import('./pages/finances/groups-page/overview-group-page/overview-group-page.component').then(
                m => m.OverviewGroupPageComponent,
              ),
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
                loadComponent: () =>
                  import('./pages/finances/groups-page/associate-bank-account-group-page/associate-bank-account-group-page.component').then(
                    m => m.AssociateBankAccountGroupPageComponent,
                  ),
                data: {
                  pageTitleKey: 'pageTitle.associateBankAccountGroup',
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
                loadComponent: () =>
                  import('./pages/finances/groups-page/associate-credit-card-group-page/associate-credit-card-group-page.component').then(
                    m => m.AssociateCreditCardGroupPageComponent,
                  ),
                data: {
                  pageTitleKey: 'pageTitle.associateCreditCardGroup',
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
                loadComponent: () =>
                  import('./pages/finances/groups-page/new-group-category-page/new-group-category-page.component').then(
                    m => m.NewGroupCategoryPageComponent,
                  ),
                data: {
                  pageTitleKey: 'pageTitle.newCategory',
                },
              },
              {
                path: 'edit/:categoryId',
                loadComponent: () =>
                  import('./pages/finances/groups-page/edit-group-category-page/edit-group-category-page.component').then(
                    m => m.EditGroupCategoryPageComponent,
                  ),
                data: {
                  pageTitleKey: 'pageTitle.editCategory',
                },
              },
            ],
          },
        ],
      },
    ],
  },
  {
    path: 'invite/:id',
    loadComponent: () => import('./pages/accept-invite-page/accept-invite-page.component').then(m => m.AcceptInvitePageComponent),
    data: {
      pageTitleKey: 'pageTitle.acceptInvite',
    },
  },
  {
    'path': 'not-found',
    loadComponent: () => import('./pages/not-found-page/not-found-page.component').then(m => m.NotFoundPageComponent),
    data: {
      pageTitleKey: 'pageTitle.notFound',
    },
  },
  {
    path: '**',
    redirectTo: 'not-found',
  },
];
