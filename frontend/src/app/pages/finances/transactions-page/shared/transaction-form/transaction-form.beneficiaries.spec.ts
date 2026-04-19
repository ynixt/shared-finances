import { describe, expect, it } from 'vitest';

import { WalletEntryType__Obj } from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { defaultBeneficiaryState, validateBeneficiarySplit } from './transaction-form.beneficiaries';

describe('transaction-form beneficiaries helpers', () => {
  it('defaults new group non-transfer entries to the current user with 100%', () => {
    expect(
      defaultBeneficiaryState({
        currentUserId: 'user-1',
        groupId: 'group-1',
        type: WalletEntryType__Obj.EXPENSE,
      }),
    ).toEqual({
      primaryBeneficiaryUserId: 'user-1',
      primaryBeneficiaryPercent: 100,
      extraBeneficiaryLegs: [],
    });
  });

  it('rejects duplicate beneficiaries', () => {
    expect(
      validateBeneficiarySplit({
        groupId: 'group-1',
        type: WalletEntryType__Obj.REVENUE,
        primaryBeneficiaryUserId: 'user-1',
        primaryBeneficiaryPercent: 50,
        extraBeneficiaryLegs: [{ userId: 'user-1', benefitPercent: 50 }],
      }),
    ).toEqual({ duplicateBeneficiaries: true });
  });

  it('requires beneficiary percentages to total 100', () => {
    expect(
      validateBeneficiarySplit({
        groupId: 'group-1',
        type: WalletEntryType__Obj.EXPENSE,
        primaryBeneficiaryUserId: 'user-1',
        primaryBeneficiaryPercent: 60,
        extraBeneficiaryLegs: [{ userId: 'user-2', benefitPercent: 30 }],
      }),
    ).toEqual({ beneficiaryPercentsSum: { sum: 90 } });
  });
});
