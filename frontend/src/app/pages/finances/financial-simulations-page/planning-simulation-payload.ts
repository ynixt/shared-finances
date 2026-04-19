import dayjs from 'dayjs';

import { UserResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { PlanningSimulationOutcomeBand } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ONLY_DATE_FORMAT } from '../../../util/date-util';

export type PlanningExpenseInputForm = {
  amount: number | null;
  installments: number;
  firstPaymentDate: dayjs.Dayjs;
  currency: string;
};

export type PlanningSimulationCurrencyPoint = {
  openingBalance: number;
  projectedCashFlow: number;
  creditCardBillOutflow: number;
  simulatedExpenseOutflow: number;
  debtInflow?: number;
  debtOutflow?: number;
  closingBalance: number;
  scheduledGoalContribution: number;
  closingBalanceWithGoalContributions: number;
};

export type PlanningSimulationResultPayload = {
  scopeType: 'USER' | 'GROUP';
  outcomeBand: PlanningSimulationOutcomeBand;
  timeline: Array<{
    month: string;
    byCurrency: Record<string, PlanningSimulationCurrencyPoint>;
  }>;
  goalTrack?: {
    canSustainScheduledContributions: boolean;
    canSustainScheduledContributionsIfAllocationsAreFreed: boolean;
    canFitIfAllocationsAreFreed: boolean;
    committedAllocationsByCurrency?: Record<string, number>;
  };
  groupContext?: {
    incompleteSimulation: boolean;
    includedMembers: number;
    excludedMembers: number;
    privacyLabels: string[];
  };
};

type PlanningSimulationRequestPayload = {
  horizonMonths: number;
  expenses: Array<{
    amount: number;
    installments: number;
    firstPaymentDate?: string;
    currency?: string;
  }>;
};

const OUTCOME_BANDS = new Set<PlanningSimulationOutcomeBand>([
  'FITS',
  'FITS_BUT_CANNOT_SUSTAIN_SCHEDULED_GOAL_CONTRIBUTIONS',
  'FITS_IF_GOAL_ALLOCATIONS_ARE_REDUCED',
  'DOES_NOT_FIT',
]);

export function defaultPlanningExpenseForm(user: UserResponseDto, referenceDate = dayjs()): PlanningExpenseInputForm {
  return {
    amount: null,
    installments: 1,
    firstPaymentDate: dayjs(referenceDate),
    currency: user.defaultCurrency,
  };
}

export function normalizeHorizonMonths(value: number): number {
  return Math.min(36, Math.max(1, value || 6));
}

export function buildPlanningSimulationRequestPayload(params: {
  horizonMonths: number;
  expenses: PlanningExpenseInputForm[];
  pendingExpense: PlanningExpenseInputForm;
}): PlanningSimulationRequestPayload {
  const normalizedHorizon = normalizeHorizonMonths(params.horizonMonths);
  const allExpenses = [...params.expenses];
  if ((params.pendingExpense.amount ?? 0) > 0) {
    allExpenses.push(params.pendingExpense);
  }

  return {
    horizonMonths: normalizedHorizon,
    expenses: allExpenses
      .map(expense => ({
        amount: Number(expense.amount ?? 0),
        installments: Math.max(1, expense.installments || 1),
        firstPaymentDate: expense.firstPaymentDate?.format(ONLY_DATE_FORMAT) || undefined,
        currency: expense.currency.trim() === '' ? undefined : expense.currency.trim().toUpperCase(),
      }))
      .filter(expense => expense.amount > 0),
  };
}

export function parsePlanningSimulationResultPayload(resultPayload?: string | null): PlanningSimulationResultPayload | null {
  if (resultPayload == null || resultPayload === '') return null;

  try {
    const parsed = JSON.parse(resultPayload) as Partial<PlanningSimulationResultPayload>;
    if (parsed == null || typeof parsed !== 'object') {
      return null;
    }
    if (parsed.scopeType !== 'USER' && parsed.scopeType !== 'GROUP') {
      return null;
    }
    if (parsed.outcomeBand == null || !OUTCOME_BANDS.has(parsed.outcomeBand)) {
      return null;
    }
    if (!Array.isArray(parsed.timeline)) {
      return null;
    }

    const hasInvalidTimelineRow = parsed.timeline.some(row => {
      if (row == null || typeof row !== 'object') {
        return true;
      }
      const typedRow = row as { month?: unknown; byCurrency?: unknown };
      return typeof typedRow.month !== 'string' || typedRow.byCurrency == null || typeof typedRow.byCurrency !== 'object';
    });

    if (hasInvalidTimelineRow) {
      return null;
    }

    return parsed as PlanningSimulationResultPayload;
  } catch {
    return null;
  }
}
