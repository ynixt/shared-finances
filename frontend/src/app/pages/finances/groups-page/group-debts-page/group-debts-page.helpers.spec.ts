import { describe, expect, it } from 'vitest';

import { TransferPurpose__Obj, WalletEntryType__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { buildDebtSettlementEntry, manualAdjustmentNetAmountForRoot } from './group-debts-page.helpers';

describe('group-debts-page helpers', () => {
  it('builds settlement transfers with DEBT_SETTLEMENT purpose', () => {
    const dto = buildDebtSettlementEntry({
      amount: 42.5,
      date: new Date('2026-04-21T00:00:00Z'),
      groupId: 'group-1',
      name: 'Debt settlement',
      originId: 'wallet-1',
      targetId: 'wallet-2',
    });

    expect(dto.transferPurpose).toBe(TransferPurpose__Obj.DEBT_SETTLEMENT);
    expect(dto.type).toBe(WalletEntryType__Obj.TRANSFER);
    expect(dto.originValue).toBe(42.5);
    expect(dto.targetValue).toBe(42.5);
    expect(dto.groupId).toBe('group-1');
  });

  it('keeps signed manual adjustment net amounts when compensation movements exist', () => {
    const amount = manualAdjustmentNetAmountForRoot(
      [
        {
          id: 'movement-1',
          payerId: 'user-1',
          receiverId: 'user-2',
          month: '2026-04',
          currency: 'BRL',
          deltaSigned: 15,
          reasonKind: 'MANUAL_ADJUSTMENT',
          createdByUserId: 'user-1',
        },
        {
          id: 'movement-2',
          payerId: 'user-1',
          receiverId: 'user-2',
          month: '2026-04',
          currency: 'BRL',
          deltaSigned: -24,
          reasonKind: 'MANUAL_ADJUSTMENT_COMPENSATION',
          createdByUserId: 'user-1',
          sourceMovementId: 'movement-1',
        },
      ] as any,
      'movement-1',
    );

    expect(amount).toBe(-9);
  });
});
