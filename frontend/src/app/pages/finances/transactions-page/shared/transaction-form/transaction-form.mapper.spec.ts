import '@angular/compiler';
import { FormArray, FormControl, FormGroup } from '@angular/forms';

import { describe, expect, it } from 'vitest';

import { EventForListDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType__Obj,
  RecurrenceType__Obj,
  TransferPurpose__Obj,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { mapEventToTransactionFormPatch, mapTransactionFormToNewEntryDto } from './transaction-form.mapper';
import { NewTransactionForm, UserForBeneficiary, ValueType } from './transaction-form.types';

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
    beneficiaries: [],
    type: 'TRANSFER',
    transferPurpose: TransferPurpose__Obj.GENERAL,
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
    transferPurpose: new FormControl(TransferPurpose__Obj.GENERAL),
    installments: new FormControl(undefined),
    periodicity: new FormControl(RecurrenceType__Obj.SINGLE),
    valueType: new FormControl(ValueType.TOTAL),
    calculatedValue: new FormControl(undefined),
    periodicityQtyLimit: new FormControl(undefined),
    originBill: new FormControl(undefined),
    targetBill: new FormControl(undefined),
    tags: new FormControl(['fx']),
    primaryOriginContributionPercent: new FormControl(100),
    extraSourceLegs: new FormArray([]),
    primaryBeneficiaryUser: new FormControl(undefined),
    primaryBeneficiaryPercent: new FormControl(100),
    extraBeneficiaryLegs: new FormArray([]),
  }) as unknown as NewTransactionForm;
}

describe('transaction-form.mapper', () => {
  it('maps explicit transfer origin and target amounts into the edit form patch', () => {
    const hydration = mapEventToTransactionFormPatch(buildTransferEvent());

    expect(hydration.patch.value).toBe(100);
    expect(hydration.patch.targetValue).toBe(540.25);
    expect(hydration.patch.origin?.id).toBe('origin-wallet');
    expect(hydration.patch.target?.id).toBe('target-wallet');
    expect(hydration.primaryOriginContributionPercent).toBe(100);
    expect(hydration.extraSourceLegs).toEqual([]);
  });

  it('serializes explicit transfer amounts into the new entry dto', () => {
    const dto = mapTransactionFormToNewEntryDto(buildForm(), undefined, true);

    expect(dto.type).toBe('TRANSFER');
    expect(dto.value).toBeNull();
    expect(dto.originValue).toBe(100);
    expect(dto.targetValue).toBe(540.25);
    expect(dto.transferPurpose).toBe('GENERAL');
  });

  it('omits targetValue when transfer is still only a schedule template', () => {
    const dto = mapTransactionFormToNewEntryDto(buildForm(), undefined, false);

    expect(dto.originValue).toBe(100);
    expect(dto.targetValue).toBeNull();
    expect(dto.transferPurpose).toBe('GENERAL');
  });

  it('hydrates and preserves transfer purpose for settlement transfers', () => {
    const hydration = mapEventToTransactionFormPatch({
      ...buildTransferEvent(),
      transferPurpose: TransferPurpose__Obj.DEBT_SETTLEMENT,
    });
    const form = buildForm();

    form.patchValue(hydration.patch);
    const dto = mapTransactionFormToNewEntryDto(form, undefined, true);

    expect(hydration.patch.transferPurpose).toBe('DEBT_SETTLEMENT');
    expect(dto.transferPurpose).toBe('DEBT_SETTLEMENT');
  });

  it('hydrates and serializes group beneficiaries for non-transfer entries', () => {
    const hydration = mapEventToTransactionFormPatch({
      ...buildTransferEvent(),
      type: WalletEntryType__Obj.EXPENSE,
      group: { id: 'group-1', name: 'Trip fund' } as any,
      transferPurpose: TransferPurpose__Obj.GENERAL,
      originValue: undefined,
      targetValue: undefined,
      beneficiaries: [
        { userId: 'user-a', benefitPercent: 70 },
        { userId: 'user-b', benefitPercent: 30 },
      ],
      entries: [
        {
          value: -100,
          walletItemId: 'origin-wallet',
          billDate: null,
          billId: null,
          contributionPercent: 100,
          walletItem: walletItem('origin-wallet', 'USD'),
        },
      ],
    });
    const form = buildForm();

    form.patchValue({
      ...hydration.patch,
      type: WalletEntryType__Obj.EXPENSE,
      group: { id: 'group-1', name: 'Trip fund' } as any,
      origin: walletItem('origin-wallet', 'USD'),
      target: undefined,
      value: 100,
    });
    const [primaryLeg, ...extraLegs] = hydration.beneficiaryLegs;
    const primaryUser: UserForBeneficiary | undefined =
      primaryLeg == null
        ? undefined
        : ({
            id: primaryLeg.userId,
            firstName: 'User',
            lastName: primaryLeg.userId,
            email: `${primaryLeg.userId}@example.com`,
            label: primaryLeg.userId,
          } as UserForBeneficiary);
    form.get('primaryBeneficiaryUser')?.setValue(primaryUser);
    form.get('primaryBeneficiaryPercent')?.setValue(primaryLeg?.benefitPercent);
    (form.get('extraBeneficiaryLegs') as FormArray).push(
      new FormGroup({
        userId: new FormControl(extraLegs[0]?.userId),
        benefitPercent: new FormControl(extraLegs[0]?.benefitPercent),
      }),
    );

    const dto = mapTransactionFormToNewEntryDto(form, undefined, false);

    expect(hydration.beneficiaryLegs).toEqual([
      { userId: 'user-a', benefitPercent: 70 },
      { userId: 'user-b', benefitPercent: 30 },
    ]);
    expect(dto.beneficiaries).toEqual([
      { userId: 'user-a', benefitPercent: 70 },
      { userId: 'user-b', benefitPercent: 30 },
    ]);
  });
});
