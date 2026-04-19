import dayjs from 'dayjs';

import { GroupDebtMovementDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { NewEntryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType__Obj,
  TransferPurpose__Obj,
  WalletEntryType__Obj,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';

export function manualAdjustmentNetAmountForRoot(history: GroupDebtMovementDto[], rootMovementId: string): number {
  return history
    .filter(movement => movement.id === rootMovementId || movement.sourceMovementId === rootMovementId)
    .reduce((sum, movement) => sum + movement.deltaSigned, 0);
}

export function buildDebtSettlementEntry(params: {
  amount: number;
  date: Date;
  groupId: string;
  name: string;
  originId: string;
  targetId: string;
}): NewEntryDto {
  return {
    beneficiaries: null,
    categoryId: null,
    confirmed: true,
    date: dayjs(params.date).format('YYYY-MM-DD'),
    groupId: params.groupId,
    installments: null,
    name: params.name,
    observations: null,
    originBillDate: null,
    originId: params.originId,
    originValue: params.amount,
    paymentType: PaymentType__Obj.UNIQUE,
    periodicity: null,
    periodicityQtyLimit: null,
    sources: null,
    tags: null,
    targetBillDate: null,
    targetId: params.targetId,
    targetValue: params.amount,
    transferPurpose: TransferPurpose__Obj.DEBT_SETTLEMENT,
    type: WalletEntryType__Obj.TRANSFER,
    value: null,
  };
}
