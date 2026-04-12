import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { NEVER } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createEmptyPage } from '../../../services/pagination.service';
import { FinancialGoalDetailPageComponent } from './financial-goal-detail-page.component';

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
      paramMap: convertToParamMap({ id: 'goal-1' }),
      pathFromRoot,
    },
  } as unknown as ActivatedRoute;
}

describe('FinancialGoalDetailPageComponent', () => {
  const financialGoalServiceMock = {
    getGoal: vi.fn(),
    listMovements: vi.fn(),
    listSchedules: vi.fn(),
    deleteSchedule: vi.fn(),
  };

  const translateServiceMock = {
    instant: vi.fn((key: string) => key),
    onLangChange: NEVER,
  };

  const localeServiceMock = {
    locale$: NEVER,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    financialGoalServiceMock.getGoal.mockResolvedValue({
      goal: { id: 'goal-1', name: 'Emergency', groupId: null },
      targets: [],
      committedByCurrency: {},
      commitmentChart: [],
    });
    financialGoalServiceMock.listMovements.mockResolvedValue(createEmptyPage());
    financialGoalServiceMock.listSchedules.mockResolvedValue(createEmptyPage());
    financialGoalServiceMock.deleteSchedule.mockResolvedValue(undefined);
  });

  it('keeps movement and schedule pagination requests independent', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub('group-8') }] });
    const component = runInInjectionContext(
      injector,
      () =>
        new FinancialGoalDetailPageComponent(
          financialGoalServiceMock as never,
          {} as never,
          { handleError: vi.fn() } as never,
          translateServiceMock as never,
          { transform: vi.fn(() => '') } as never,
          { transform: vi.fn(() => '') } as never,
          localeServiceMock as never,
        ),
    );
    await flushPromises();

    financialGoalServiceMock.listMovements.mockClear();
    financialGoalServiceMock.listSchedules.mockClear();

    component.onMovementLazyLoad({ first: 10 });
    await flushPromises();

    expect(financialGoalServiceMock.listMovements).toHaveBeenCalledWith('goal-1', {
      page: 1,
      size: 10,
      sort: [
        { property: 'movementDate', direction: 'DESC' },
        { property: 'id', direction: 'DESC' },
      ],
    });
    expect(financialGoalServiceMock.listSchedules).not.toHaveBeenCalled();

    component.onScheduleLazyLoad({ first: 20 });
    await flushPromises();

    expect(financialGoalServiceMock.listSchedules).toHaveBeenCalledWith('goal-1', {
      page: 2,
      size: 10,
      sort: [
        { property: 'nextExecution', direction: 'ASC' },
        { property: 'id', direction: 'ASC' },
      ],
    });
  });

  it('exposes only the edit action in title extras (no delete action on detail)', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub() }] });
    const component = runInInjectionContext(
      injector,
      () =>
        new FinancialGoalDetailPageComponent(
          financialGoalServiceMock as never,
          {} as never,
          { handleError: vi.fn() } as never,
          translateServiceMock as never,
          { transform: vi.fn(() => '') } as never,
          { transform: vi.fn(() => '') } as never,
          localeServiceMock as never,
        ),
    );
    await flushPromises();

    const extras = component.editExtras();
    expect(extras).toHaveLength(1);
    expect(extras[0]?.routerLink).toEqual(['/app/goals', 'goal-1', 'edit']);
    expect(extras[0]?.click).toBeUndefined();
  });
});
