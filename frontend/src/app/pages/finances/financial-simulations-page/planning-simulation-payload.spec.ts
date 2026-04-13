import { describe, expect, it } from 'vitest';

import {
  buildPlanningSimulationRequestPayload,
  defaultPlanningDebtForm,
  normalizeHorizonMonths,
  parsePlanningSimulationResultPayload,
} from './planning-simulation-payload';

describe('planning-simulation-payload helpers', () => {
  it('builds default debt form for month start', () => {
    const debt = defaultPlanningDebtForm(new Date('2026-04-13T10:00:00Z'));

    expect(debt.firstPaymentDate).toBe('2026-04-01');
    expect(debt.installments).toBe(1);
    expect(debt.currency).toBe('');
  });

  it('normalizes horizon months to supported limits', () => {
    expect(normalizeHorizonMonths(0)).toBe(6);
    expect(normalizeHorizonMonths(1)).toBe(1);
    expect(normalizeHorizonMonths(60)).toBe(36);
  });

  it('builds planning payload with normalized debts and currency', () => {
    const payload = buildPlanningSimulationRequestPayload({
      horizonMonths: 48,
      debts: [
        {
          amount: 1200,
          installments: 12,
          firstPaymentDate: '2026-05-01',
          currency: 'brl',
        },
      ],
      pendingDebt: {
        amount: 300,
        installments: 3,
        firstPaymentDate: '2026-06-01',
        currency: '',
      },
    });

    expect(payload.horizonMonths).toBe(36);
    expect(payload.debts).toEqual([
      {
        amount: 1200,
        installments: 12,
        firstPaymentDate: '2026-05-01',
        currency: 'BRL',
      },
      {
        amount: 300,
        installments: 3,
        firstPaymentDate: '2026-06-01',
        currency: undefined,
      },
    ]);
  });

  it('parses a valid planning simulation result payload', () => {
    const parsed = parsePlanningSimulationResultPayload(
      JSON.stringify({
        scopeType: 'USER',
        outcomeBand: 'FITS',
        timeline: [
          {
            month: '2026-04',
            byCurrency: {
              USD: {
                openingBalance: 1000,
                projectedCashFlow: 100,
                creditCardBillOutflow: 50,
                debtOutflow: 0,
                closingBalance: 1050,
                scheduledGoalContribution: 20,
                closingBalanceWithGoalContributions: 1030,
              },
            },
          },
        ],
      }),
    );

    expect(parsed?.outcomeBand).toBe('FITS');
    expect(parsed?.timeline).toHaveLength(1);
  });

  it('returns null for invalid planning simulation result payload', () => {
    const parsed = parsePlanningSimulationResultPayload(
      JSON.stringify({
        scopeType: 'USER',
        outcomeBand: 'INVALID',
      }),
    );

    expect(parsed).toBeNull();
  });
});
