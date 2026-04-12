import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { FinancialGoalEditLedgerMovementPageComponent } from './financial-goal-edit-ledger-movement-page.component';

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}

function routeStub(groupId?: string): ActivatedRoute {
  const pathFromRoot =
    groupId == null
      ? [{ paramMap: convertToParamMap({}) }]
      : [{ paramMap: convertToParamMap({}) }, { paramMap: convertToParamMap({ groupId }) }];

  return {
    snapshot: {
      paramMap: convertToParamMap({ id: 'goal-1', movementId: 'movement-1' }),
      pathFromRoot,
    },
  } as unknown as ActivatedRoute;
}

describe('FinancialGoalEditLedgerMovementPageComponent', () => {
  const financialGoalServiceMock = {
    getGoal: vi.fn(),
    getLedgerMovement: vi.fn(),
    editMovement: vi.fn(),
    deleteMovement: vi.fn(),
  };
  const confirmationServiceMock = {
    confirm: vi.fn(),
  };
  const translateServiceMock = {
    instant: vi.fn((key: string) => `translated:${key}`),
  };
  const routerMock = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    financialGoalServiceMock.getGoal.mockResolvedValue({
      goal: { id: 'goal-1', name: 'Emergency', groupId: 'group-8' },
      targets: [],
      committedByCurrency: {},
      commitmentChart: [],
    });
    financialGoalServiceMock.getLedgerMovement.mockResolvedValue({
      id: 'movement-1',
      walletItemId: 'wallet-1',
      walletItemName: 'Main account',
      currency: 'BRL',
      signedAmount: 25,
      note: '',
      movementKind: 'IMMEDIATE',
      scheduleId: null,
      movementDate: '2026-04-10',
      createdAt: null,
    });
    financialGoalServiceMock.editMovement.mockResolvedValue(undefined);
    financialGoalServiceMock.deleteMovement.mockResolvedValue(undefined);
    routerMock.navigate.mockResolvedValue(true);
  });

  it('confirms and deletes movement from edit workflow', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub('group-8') }] });
    const component = runInInjectionContext(
      injector,
      () =>
        new FinancialGoalEditLedgerMovementPageComponent(
          routerMock as never,
          new FormBuilder(),
          financialGoalServiceMock as never,
          {} as never,
          { handleError: vi.fn() } as never,
          confirmationServiceMock as never,
          translateServiceMock as never,
        ),
    );
    await flushPromises();

    component.confirmDelete();
    expect(confirmationServiceMock.confirm).toHaveBeenCalledTimes(1);
    const confirmConfig = confirmationServiceMock.confirm.mock.calls[0]?.[0];

    confirmConfig?.accept?.();
    await flushPromises();

    expect(financialGoalServiceMock.deleteMovement).toHaveBeenCalledWith('goal-1', 'movement-1');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/app/groups', 'group-8', 'goals', 'goal-1']);
  });
});
