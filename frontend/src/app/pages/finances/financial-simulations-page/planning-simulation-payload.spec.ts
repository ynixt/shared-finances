import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';

import {
  buildPlanningSimulationRequestPayload,
  defaultPlanningExpenseForm,
  normalizeHorizonMonths,
  parsePlanningSimulationResultPayload,
} from './planning-simulation-payload';

describe('planning-simulation-payload helpers', () => {
  it('builds default expense form for month start', () => {
    const expense = defaultPlanningExpenseForm(
      {
        id: 'user-1',
        firstName: 'Test',
        lastName: 'User',
        email: 'test@example.com',
        lang: 'pt-BR',
        tmz: 'America/Sao_Paulo',
        defaultCurrency: 'BRL',
        emailVerified: true,
        mfaEnabled: false,
        onboardingDone: true,
        photoUrl: null,
      },
      dayjs('2026-04-13T10:00:00Z'),
    );

    expect(expense.firstPaymentDate.format('YYYY-MM-DD')).toBe('2026-04-13');
    expect(expense.installments).toBe(1);
    expect(expense.currency).toBe('BRL');
  });

  it('normalizes horizon months to supported limits', () => {
    expect(normalizeHorizonMonths(0)).toBe(6);
    expect(normalizeHorizonMonths(1)).toBe(1);
    expect(normalizeHorizonMonths(60)).toBe(36);
  });

  it('builds planning payload with normalized expenses and currency', () => {
    const payload = buildPlanningSimulationRequestPayload({
      horizonMonths: 48,
      expenses: [
        {
          amount: 1200,
          installments: 12,
          firstPaymentDate: dayjs('2026-05-01'),
          currency: 'brl',
        },
      ],
      pendingExpense: {
        amount: 300,
        installments: 3,
        firstPaymentDate: dayjs('2026-06-01'),
        currency: '',
      },
    });

    expect(payload.horizonMonths).toBe(36);
    expect(payload.expenses).toEqual([
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
                simulatedExpenseOutflow: 0,
                debtInflow: 25,
                debtOutflow: 40,
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
    expect(parsed?.timeline[0]?.byCurrency['USD']?.debtInflow).toBe(25);
    expect(parsed?.timeline[0]?.byCurrency['USD']?.debtOutflow).toBe(40);
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
