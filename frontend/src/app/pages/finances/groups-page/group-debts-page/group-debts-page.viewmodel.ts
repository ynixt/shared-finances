import {
  GroupDebtMonthlyCompositionDto,
  GroupDebtMovementDto,
  GroupDebtPairBalanceDto,
  GroupDebtPairHistoryDto,
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
  pair: GroupDebtPairBalanceDto;
  selectedMonthComposition: GroupDebtMonthlyCompositionGridItem | undefined;
}

export interface GroupDebtHistoryGridItem {
  directionalLabel: string;
  displayName: string;
  linkedWalletEvent: EventForListDto | undefined;
  linkedWalletEventName: string | undefined;
  movement: GroupDebtMovementDto;
}

export interface GroupDebtPairHistoryGridItem {
  chargeDelta: number;
  currency: string;
  firstUserId: string;
  id: string;
  lines: GroupDebtHistoryGridItem[];
  manualAdjustmentDelta: number;
  month: string;
  netAmount: number;
  netPayerId?: string | null;
  netReceiverId?: string | null;
  pairLabel: string;
  secondUserId: string;
  settlementDelta: number;
}

export function mapGroupDebtPairHistoryToGridItems(
  history: GroupDebtPairHistoryDto[],
  options: {
    resolveMemberName: (userId: string) => string;
    resolveLineName: (movement: GroupDebtMovementDto, linkedWalletEventName: string | undefined) => string;
    resolveTransactionDate: (movement: GroupDebtMovementDto) => string | null | undefined;
  },
): GroupDebtPairHistoryGridItem[] {
  return history
    .map(item => {
      const pairNames = [options.resolveMemberName(item.firstUserId), options.resolveMemberName(item.secondUserId)].sort((left, right) =>
        left.localeCompare(right),
      );
      const lines = item.lines.map(movement => {
        const linkedWalletEvent = movement.sourceWalletEvent ?? undefined;
        const linkedWalletEventName = resolveLinkedWalletEventName(linkedWalletEvent);

        return {
          movement,
          linkedWalletEvent,
          linkedWalletEventName,
          directionalLabel: `${options.resolveMemberName(movement.payerId)} -> ${options.resolveMemberName(movement.receiverId)}`,
          displayName: options.resolveLineName(movement, linkedWalletEventName),
        };
      });

      lines.sort((left, right) => {
        const leftDate = options.resolveTransactionDate(left.movement) ?? '';
        const rightDate = options.resolveTransactionDate(right.movement) ?? '';
        const dateComparison = rightDate.localeCompare(leftDate);
        if (dateComparison !== 0) {
          return dateComparison;
        }

        const nameComparison = left.displayName.localeCompare(right.displayName);
        if (nameComparison !== 0) {
          return nameComparison;
        }

        return left.movement.id.localeCompare(right.movement.id);
      });

      return {
        id: `${item.firstUserId}-${item.secondUserId}-${item.currency}-${item.month}`,
        firstUserId: item.firstUserId,
        secondUserId: item.secondUserId,
        pairLabel: pairNames.join(' -> '),
        currency: item.currency,
        month: item.month,
        netPayerId: item.netPayerId,
        netReceiverId: item.netReceiverId,
        netAmount: item.netAmount,
        chargeDelta: item.chargeDelta,
        settlementDelta: item.settlementDelta,
        manualAdjustmentDelta: item.manualAdjustmentDelta,
        lines,
      };
    })
    .sort((left, right) => left.pairLabel.localeCompare(right.pairLabel));
}

export function mapOutstandingBalancesToGridItems(balances: GroupDebtPairBalanceDto[]): GroupDebtOutstandingBalanceGridItem[] {
  return balances.map(pair => {
    const pairId = `${pair.payerId}-${pair.receiverId}-${pair.currency}`;

    return {
      id: pairId,
      pair,
      selectedMonthComposition: mapMonthlyCompositionToGridItems(pair.monthlyComposition, pair.currency, pairId)[0],
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
