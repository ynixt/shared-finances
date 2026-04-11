import '@angular/compiler';
import { FormControl, FormGroup } from '@angular/forms';

import { describe, expect, it } from 'vitest';

import { EventForListDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType__Obj,
  RecurrenceType__Obj,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { mapEventToTransactionFormPatch, mapTransactionFormToNewEntryDto } from './transaction-form.mapper';
import { NewTransactionForm, ValueType } from './transaction-form.types';

function walletItem(id: string, currency: string) {
  return {
    id,
    name: `Wallet ${id}`,
    type: 'BANK_ACCOUNT',
    enabled: true,
    currency,
    user: null,
  } as any;
}

function buildTransferEvent(): EventForListDto {
  return {
    id: 'event-1',
    type: 'TRANSFER',
    name: 'Wire transfer',
    category: null,
    user: null,
    group: null,
    tags: ['fx'],
    observations: null,
    date: '2026-04-10',
    confirmed: true,
    installment: null,
    recurrenceConfigId: null,
    recurrenceConfig: null,
    currency: 'USD',
    originValue: 100,
    targetValue: 540.25,
    entries: [
      {
        value: -100,
        walletItemId: 'origin-wallet',
        billDate: null,
        billId: null,
        walletItem: walletItem('origin-wallet', 'USD'),
      },
      {
        value: 540.25,
        walletItemId: 'target-wallet',
        billDate: null,
        billId: null,
        walletItem: walletItem('target-wallet', 'BRL'),
      },
    ],
  };
}

function buildForm(): NewTransactionForm {
  return new FormGroup({
    type: new FormControl(WalletEntryType__Obj.TRANSFER),
    group: new FormControl(undefined),
    origin: new FormControl(walletItem('origin-wallet', 'USD')),
    target: new FormControl(walletItem('target-wallet', 'BRL')),
    name: new FormControl('Wire transfer'),
    category: new FormControl(undefined),
    date: new FormControl(new Date('2026-04-10T00:00:00.000Z')),
    value: new FormControl(100),
    targetValue: new FormControl(540.25),
    confirmed: new FormControl(true),
    observations: new FormControl(undefined),
    paymentType: new FormControl(PaymentType__Obj.UNIQUE),
    installments: new FormControl(undefined),
    periodicity: new FormControl(RecurrenceType__Obj.SINGLE),
    valueType: new FormControl(ValueType.TOTAL),
    calculatedValue: new FormControl(undefined),
    periodicityQtyLimit: new FormControl(undefined),
    originBill: new FormControl(undefined),
    targetBill: new FormControl(undefined),
    tags: new FormControl(['fx']),
  }) as NewTransactionForm;
}

describe('transaction-form.mapper', () => {
  it('maps explicit transfer origin and target amounts into the edit form patch', () => {
    const patch = mapEventToTransactionFormPatch(buildTransferEvent());

    expect(patch.value).toBe(100);
    expect(patch.targetValue).toBe(540.25);
    expect(patch.origin?.id).toBe('origin-wallet');
    expect(patch.target?.id).toBe('target-wallet');
  });

  it('serializes explicit transfer amounts into the new entry dto', () => {
    const dto = mapTransactionFormToNewEntryDto(buildForm(), undefined, true);

    expect(dto.type).toBe('TRANSFER');
    expect(dto.value).toBeNull();
    expect(dto.originValue).toBe(100);
    expect(dto.targetValue).toBe(540.25);
  });

  it('omits targetValue when transfer is still only a schedule template', () => {
    const dto = mapTransactionFormToNewEntryDto(buildForm(), undefined, false);

    expect(dto.originValue).toBe(100);
    expect(dto.targetValue).toBeNull();
  });
});
