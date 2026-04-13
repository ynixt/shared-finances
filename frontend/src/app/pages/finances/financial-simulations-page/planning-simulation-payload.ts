import dayjs, { Dayjs } from 'dayjs';

import { UserResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { PlanningSimulationOutcomeBand } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ONLY_DATE_FORMAT } from '../../../util/date-util';

export type PlanningDebtInputForm = {
  amount: number | null;
  installments: number;
  firstPaymentDate: dayjs.Dayjs;
  currency: string;
};

export type PlanningSimulationCurrencyPoint = {
  openingBalance: number;
  projectedCashFlow: number;
  creditCardBillOutflow: number;
  debtOutflow: number;
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
  debts: Array<{
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

export function defaultPlanningDebtForm(user: UserResponseDto, referenceDate = dayjs()): PlanningDebtInputForm {
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
  debts: PlanningDebtInputForm[];
  pendingDebt: PlanningDebtInputForm;
}): PlanningSimulationRequestPayload {
  const normalizedHorizon = normalizeHorizonMonths(params.horizonMonths);
  const allDebts = [...params.debts];
  if ((params.pendingDebt.amount ?? 0) > 0) {
    allDebts.push(params.pendingDebt);
  }

  return {
    horizonMonths: normalizedHorizon,
    debts: allDebts
      .map(debt => ({
        amount: Number(debt.amount ?? 0),
        installments: Math.max(1, debt.installments || 1),
        firstPaymentDate: debt.firstPaymentDate?.format(ONLY_DATE_FORMAT) || undefined,
        currency: debt.currency.trim() === '' ? undefined : debt.currency.trim().toUpperCase(),
      }))
      .filter(debt => debt.amount > 0),
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
