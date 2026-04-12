import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { FinancialGoalAllocationFormPageComponent } from './financial-goal-allocation-form-page.component';

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}

function routeStub(params: { goalId: string; groupId?: string; mode: 'allocate' | 'deallocate' }): ActivatedRoute {
  const pathFromRoot =
    params.groupId == null
      ? [{ paramMap: convertToParamMap({}) }]
      : [{ paramMap: convertToParamMap({}) }, { paramMap: convertToParamMap({ groupId: params.groupId }) }];
  return {
    snapshot: {
      paramMap: convertToParamMap({ id: params.goalId }),
      data: { goalLedgerMode: params.mode },
      pathFromRoot,
    },
  } as unknown as ActivatedRoute;
}

describe('FinancialGoalAllocationFormPageComponent', () => {
  const financialGoalServiceMock = {
    getGoal: vi.fn(),
    allocate: vi.fn(),
    reverse: vi.fn(),
  };
  const routerMock = {
    navigate: vi.fn(),
  };
  const errorMessageServiceMock = {
    handleError: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    financialGoalServiceMock.getGoal.mockResolvedValue({
      goal: { id: 'goal-1', name: 'Emergency', groupId: null },
      targets: [],
      committedByCurrency: {},
      commitmentChart: [],
    });
    financialGoalServiceMock.allocate.mockResolvedValue(undefined);
    financialGoalServiceMock.reverse.mockResolvedValue(undefined);
    routerMock.navigate.mockResolvedValue(true);
  });

  it('submits allocation payload with allocation date in allocate mode', async () => {
    const injector = Injector.create({
      providers: [{ provide: ActivatedRoute, useValue: routeStub({ goalId: 'goal-1', mode: 'allocate' }) }],
    });
    const component = runInInjectionContext(
      injector,
      () =>
        new FinancialGoalAllocationFormPageComponent(
          routerMock as never,
          new FormBuilder(),
          financialGoalServiceMock as never,
          {} as never,
          errorMessageServiceMock as never,
        ),
    );
    await flushPromises();

    component.form.patchValue({
      walletItem: { id: 'wallet-1', currency: 'BRL' },
      amount: 50,
      allocationDate: new Date('2026-04-10T12:00:00Z'),
      note: '  aporte ',
    });
    await component.save();

    expect(financialGoalServiceMock.allocate).toHaveBeenCalledWith('goal-1', {
      walletItemId: 'wallet-1',
      amount: 50,
      allocationDate: '2026-04-10',
      note: 'aporte',
    });
    expect(financialGoalServiceMock.reverse).not.toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/app/goals', 'goal-1']);
  });

  it('submits deallocation payload without date in deallocate mode', async () => {
    const injector = Injector.create({
      providers: [{ provide: ActivatedRoute, useValue: routeStub({ goalId: 'goal-1', groupId: 'group-5', mode: 'deallocate' }) }],
    });
    const component = runInInjectionContext(
      injector,
      () =>
        new FinancialGoalAllocationFormPageComponent(
          routerMock as never,
          new FormBuilder(),
          financialGoalServiceMock as never,
          {} as never,
          errorMessageServiceMock as never,
        ),
    );
    await flushPromises();

    component.form.patchValue({
      walletItem: { id: 'wallet-1', currency: 'BRL' },
      amount: 30,
      note: '  ajuste ',
    });
    await component.save();

    expect(financialGoalServiceMock.reverse).toHaveBeenCalledWith('goal-1', {
      walletItemId: 'wallet-1',
      amount: 30,
      note: 'ajuste',
    });
    expect(financialGoalServiceMock.allocate).not.toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/app/groups', 'group-5', 'goals', 'goal-1']);
  });
});
