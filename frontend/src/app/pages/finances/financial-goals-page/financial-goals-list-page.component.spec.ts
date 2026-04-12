import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { createEmptyPage } from '../../../services/pagination.service';
import { FinancialGoalsListPageComponent } from './financial-goals-list-page.component';

async function flushPromises() {
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
      pathFromRoot,
    },
  } as unknown as ActivatedRoute;
}

describe('FinancialGoalsListPageComponent', () => {
  const financialGoalServiceMock = {
    listIndividualGoals: vi.fn(),
    listGroupGoals: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    financialGoalServiceMock.listIndividualGoals.mockResolvedValue(createEmptyPage());
    financialGoalServiceMock.listGroupGoals.mockResolvedValue(createEmptyPage());
  });

  it('loads first page from individual endpoint when outside group workspace', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub() }] });
    const component = runInInjectionContext(injector, () => new FinancialGoalsListPageComponent(financialGoalServiceMock as never));

    await flushPromises();

    expect(financialGoalServiceMock.listIndividualGoals).toHaveBeenCalledTimes(1);
    expect(financialGoalServiceMock.listIndividualGoals).toHaveBeenCalledWith({
      page: 0,
      size: 12,
      sort: [{ property: 'name', direction: 'ASC' }],
    });
    expect(financialGoalServiceMock.listGroupGoals).not.toHaveBeenCalled();
    expect(component.newGoalRouterLink).toEqual(['/app/goals', 'new']);
  });

  it('ignores duplicate lazy load requests for already loaded page', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub() }] });
    const component = runInInjectionContext(injector, () => new FinancialGoalsListPageComponent(financialGoalServiceMock as never));

    await flushPromises();
    financialGoalServiceMock.listIndividualGoals.mockClear();

    component.onLazyLoad({ first: 0, rows: 12, sortField: 'name', sortOrder: 1 });
    await flushPromises();

    expect(financialGoalServiceMock.listIndividualGoals).not.toHaveBeenCalled();
  });

  it('loads first page from group endpoint when inside group workspace', async () => {
    const injector = Injector.create({ providers: [{ provide: ActivatedRoute, useValue: routeStub('group-7') }] });
    const component = runInInjectionContext(injector, () => new FinancialGoalsListPageComponent(financialGoalServiceMock as never));

    await flushPromises();

    expect(financialGoalServiceMock.listGroupGoals).toHaveBeenCalledTimes(1);
    expect(financialGoalServiceMock.listGroupGoals).toHaveBeenCalledWith('group-7', {
      page: 0,
      size: 12,
      sort: [{ property: 'name', direction: 'ASC' }],
    });
    expect(financialGoalServiceMock.listIndividualGoals).not.toHaveBeenCalled();
    expect(component.newGoalRouterLink).toEqual(['/app/groups', 'group-7', 'goals', 'new']);
    expect(component.closeRouterLink).toEqual(['/app/groups', 'group-7']);
  });
});
