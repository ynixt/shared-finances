import { UserSimpleDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletEntryType, WalletEntryType__Obj } from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { UserForBeneficiary } from './transaction-form.types';

export interface BeneficiaryLegValue {
  benefitPercent?: number | null;
  user?: UserForBeneficiary | null;
}

export interface BeneficiaryDefaults {
  extraBeneficiaryLegs: BeneficiaryLegValue[];
  primaryBeneficiaryPercent: number;
  primaryBeneficiaryUser?: UserForBeneficiary;
}

export interface HydratedBeneficiaryState {
  extraBeneficiaryLegs: Array<{
    benefitPercent: number;
    user?: UserForBeneficiary;
  }>;
  primaryBeneficiaryPercent: number;
  primaryBeneficiaryUser?: UserForBeneficiary;
}

export function hydrateBeneficiaryState(
  beneficiaryLegs: Array<{ benefitPercent: number; userId: string }>,
  members: UserForBeneficiary[],
): HydratedBeneficiaryState {
  const usersById = new Map(members.map(member => [member.id, member] as const));
  const [primaryLeg, ...extraLegs] = beneficiaryLegs;

  return {
    primaryBeneficiaryUser: primaryLeg == null ? undefined : usersById.get(primaryLeg.userId),
    primaryBeneficiaryPercent: primaryLeg?.benefitPercent ?? 100,
    extraBeneficiaryLegs: extraLegs.map(leg => ({
      user: usersById.get(leg.userId),
      benefitPercent: leg.benefitPercent,
    })),
  };
}

export function defaultBeneficiaryState(params: {
  currentUser?: UserForBeneficiary | null;
  groupId?: string | null;
  type?: WalletEntryType;
}): BeneficiaryDefaults {
  if (params.type === WalletEntryType__Obj.TRANSFER || params.groupId == null) {
    return {
      primaryBeneficiaryUser: undefined,
      primaryBeneficiaryPercent: 100,
      extraBeneficiaryLegs: [],
    };
  }

  return {
    primaryBeneficiaryUser: params.currentUser ?? undefined,
    primaryBeneficiaryPercent: 100,
    extraBeneficiaryLegs: [],
  };
}

export function validateBeneficiarySplit(params: {
  extraBeneficiaryLegs: BeneficiaryLegValue[];
  groupId?: string | null;
  primaryBeneficiaryPercent?: number | null;
  primaryBeneficiaryUser?: UserSimpleDto | null;
  type?: WalletEntryType;
}): { beneficiaryPercentsSum?: { sum: number }; duplicateBeneficiaries?: true } | null {
  if (params.type === WalletEntryType__Obj.TRANSFER || params.groupId == null) {
    return null;
  }

  const errors: { beneficiaryPercentsSum?: { sum: number }; duplicateBeneficiaries?: true } = {};

  const sum = params.extraBeneficiaryLegs.reduce(
    (total, leg) => total + Number(leg.benefitPercent ?? 0),
    Number(params.primaryBeneficiaryPercent ?? 0),
  );

  const rounded = Math.round(sum * 100) / 100;

  if (rounded !== 100) {
    errors.beneficiaryPercentsSum = { sum: rounded };
  }

  const ids = new Set<string>();

  if (params.primaryBeneficiaryUser) {
    ids.add(params.primaryBeneficiaryUser.id);
  }

  for (const leg of params.extraBeneficiaryLegs) {
    const userId = leg.user?.id;
    if (!userId) {
      continue;
    }
    if (ids.has(userId)) {
      errors.duplicateBeneficiaries = true;
      break;
    }
    ids.add(userId);
  }

  return Object.keys(errors).length === 0 ? null : errors;
}
