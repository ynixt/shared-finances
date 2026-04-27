import {
  GroupDebtMonthlyCompositionDto,
  GroupDebtMovementDto,
  GroupDebtPairBalanceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';

export interface GroupDebtMonthlyCompositionGridItem {
  chargeDelta: number;
  currency: string;
  id: string;
  manualAdjustmentDelta: number;
  month: string;
  netAmount: number;
  settlementDelta: number;
}

export interface GroupDebtOutstandingBalanceGridItem {
  id: string;
  monthlyCompositionItems: GroupDebtMonthlyCompositionGridItem[];
  pair: GroupDebtPairBalanceDto;
}

export interface GroupDebtHistoryGridItem {
  linkedWalletEvent: EventForListDto | undefined;
  linkedWalletEventName: string | undefined;
  movement: GroupDebtMovementDto;
}

export function mapGroupDebtHistoryToGridItems(history: GroupDebtMovementDto[]): GroupDebtHistoryGridItem[] {
  return history.map(movement => {
    const linkedWalletEvent = movement.sourceWalletEvent ?? undefined;

    return {
      movement,
      linkedWalletEvent,
      linkedWalletEventName: resolveLinkedWalletEventName(linkedWalletEvent),
    };
  });
}

export function mapOutstandingBalancesToGridItems(balances: GroupDebtPairBalanceDto[]): GroupDebtOutstandingBalanceGridItem[] {
  return balances.map(pair => {
    const pairId = `${pair.payerId}-${pair.receiverId}-${pair.currency}`;

    return {
      id: pairId,
      pair,
      monthlyCompositionItems: mapMonthlyCompositionToGridItems(pair.monthlyComposition, pair.currency, pairId),
    };
  });
}

export function resolveLinkedWalletEventName(event: EventForListDto | undefined): string | undefined {
  const normalizedName = event?.name?.trim();
  if (normalizedName == null || normalizedName.length === 0) {
    return undefined;
  }

  return normalizedName;
}

function mapMonthlyCompositionToGridItems(
  monthlyComposition: GroupDebtMonthlyCompositionDto[],
  currency: string,
  pairId: string,
): GroupDebtMonthlyCompositionGridItem[] {
  return monthlyComposition.map(row => ({
    id: `${pairId}-${row.month}`,
    currency,
    month: row.month,
    netAmount: row.netAmount,
    chargeDelta: row.chargeDelta,
    settlementDelta: row.settlementDelta,
    manualAdjustmentDelta: row.manualAdjustmentDelta,
  }));
}
