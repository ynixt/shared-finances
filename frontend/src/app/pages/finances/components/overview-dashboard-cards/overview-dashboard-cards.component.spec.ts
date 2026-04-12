import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OverviewDashboardCardDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { OverviewDashboardCardsComponent } from './overview-dashboard-cards.component';

describe('OverviewDashboardCardsComponent', () => {
  const confirmationServiceMock = {
    confirm: vi.fn(),
  };

  const translateServiceMock = {
    instant: vi.fn((key: string) => `translated:${key}`),
  };

  let component: OverviewDashboardCardsComponent;
  const injector = Injector.create({ providers: [] });

  beforeEach(() => {
    vi.clearAllMocks();
    component = runInInjectionContext(
      injector,
      () => new OverviewDashboardCardsComponent(confirmationServiceMock as never, translateServiceMock as never),
    );
  });

  it('maps the new cash-flow cards to translation keys and value classes', () => {
    expect(component.cardTitleKey('GOAL_COMMITTED')).toBe('financesPage.overviewPage.cards.goalCommitted');
    expect(component.cardTitleKey('GOAL_FREE_BALANCE')).toBe('financesPage.overviewPage.cards.goalFreeBalance');
    expect(component.cardTitleKey('PERIOD_CASH_IN')).toBe('financesPage.overviewPage.cards.periodCashIn');
    expect(component.cardTitleKey('PERIOD_CASH_OUT')).toBe('financesPage.overviewPage.cards.periodCashOut');
    expect(component.cardTitleKey('END_OF_PERIOD_NET_CASH_FLOW')).toBe('financesPage.overviewPage.cards.endOfPeriodNetCashFlow');

    expect(component.cardValueClass({ key: 'GOAL_COMMITTED', value: 10 } as OverviewDashboardCardDto)).toBe(
      'text-surface-900 dark:text-surface-0',
    );
    expect(component.cardValueClass({ key: 'PERIOD_CASH_IN', value: 10 } as OverviewDashboardCardDto)).toBe('text-green-700');
    expect(component.cardValueClass({ key: 'PROJECTED_CASH_OUT', value: 10 } as OverviewDashboardCardDto)).toBe('text-red-700');
    expect(component.cardValueClass({ key: 'BALANCE', value: -1 } as OverviewDashboardCardDto)).toBe('text-red-700');
  });

  it('maps detail source types to translated sublabels', () => {
    expect(component.detailSublabel({ sourceType: 'BANK_ACCOUNT' } as never)).toBe(
      'financesPage.overviewPage.detail.sourceType.bankAccount',
    );
    expect(component.detailSublabel({ sourceType: 'CREDIT_CARD_BILL' } as never)).toBe(
      'financesPage.overviewPage.detail.sourceType.creditCardBill',
    );
    expect(component.detailSublabel({ sourceType: 'GOAL' } as never)).toBe('financesPage.overviewPage.detail.sourceType.goal');
    expect(component.detailSublabel({ sourceType: 'FORMULA' } as never)).toBe('financesPage.overviewPage.detail.sourceType.formula');
    expect(component.detailSublabel({ sourceType: 'SOMETHING_ELSE' } as never)).toBe('financesPage.overviewPage.detail.sourceType.unknown');
  });

  it('opens the details dialog using the translated card title and clears selection on accept', () => {
    const card = {
      key: 'PROJECTED_CASH_OUT',
      value: 120,
      details: [],
    } as OverviewDashboardCardDto;

    component.showDetails(card);

    expect(component.selectedCard).toBe(card);
    expect(confirmationServiceMock.confirm).toHaveBeenCalledTimes(1);

    const dialogConfig = confirmationServiceMock.confirm.mock.calls[0]?.[0];
    expect(dialogConfig?.header).toBe('translated:financesPage.overviewPage.cards.projectedCashOut');
    expect(dialogConfig?.acceptLabel).toBe('translated:financesPage.overviewPage.actions.close');

    dialogConfig?.accept?.();

    expect(component.selectedCard).toBeUndefined();
  });
});
