import { TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';

import { UserSimpleDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { EventForListDto, NewEntryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { EntryResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry/EventForListDto/entry-response-dto';
import {
  PaymentType__Obj,
  RecurrenceType__Obj,
  TransferPurpose__Obj,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ONLY_DATE_FORMAT } from '../../../../../util/date-util';
import { NewTransactionForm, UserForBeneficiary, ValueType } from './transaction-form.types';

function getTransferOriginEntry(entry: EventForListDto) {
  return entry.entries.find(item => item.value <= 0) ?? entry.entries[0];
}

function getTransferTargetEntry(entry: EventForListDto) {
  return entry.entries.find(item => item.value > 0) ?? entry.entries[1] ?? entry.entries[0];
}

function getTransferOriginValue(entry: EventForListDto): number {
  return entry.originValue ?? Math.abs(getTransferOriginEntry(entry).value ?? 0);
}

function getTransferTargetValue(entry: EventForListDto): number | undefined {
  return entry.targetValue ?? undefined;
}

function inferPercentsFromEntryValues(entries: EntryResponseDto[]): number[] {
  const abs = entries.map(e => Math.abs(e.value));
  const total = abs.reduce((a, b) => a + b, 0);
  if (total <= 0) {
    const even = Math.floor((100 / entries.length) * 100) / 100;
    const out = entries.map(() => even);
    out[out.length - 1] = Math.round((100 - even * (entries.length - 1)) * 100) / 100;
    return out;
  }
  const rounded: number[] = [];
  let allocated = 0;
  for (let i = 0; i < entries.length - 1; i++) {
    const p = Math.round(((abs[i]! / total) * 100 + Number.EPSILON) * 100) / 100;
    rounded.push(p);
    allocated += p;
  }
  rounded.push(Math.round((100 - allocated) * 100) / 100);
  return rounded;
}

function resolveContributionPercents(entries: EntryResponseDto[]): number[] {
  if (entries.every(e => e.contributionPercent != null)) {
    return entries.map(e => e.contributionPercent as number);
  }
  return inferPercentsFromEntryValues(entries);
}

export interface ExtraSourceLegInit {
  walletItem: WalletItemSearchResponseDto;
  contributionPercent: number;
  bill?: Date;
}

export interface ExtraBeneficiaryLegInit {
  benefitPercent: number;
  userId: string;
}

export interface TransactionFormHydration {
  patch: Partial<NewTransactionForm['value']>;
  primaryOriginContributionPercent: number;
  extraSourceLegs: ExtraSourceLegInit[];
  beneficiaryLegs: ExtraBeneficiaryLegInit[];
}

export function getEditableValueFromEvent(entry: EventForListDto): number {
  if (entry.type === WalletEntryType__Obj.TRANSFER) {
    return getTransferOriginValue(entry);
  }

  if (entry.type !== WalletEntryType__Obj.TRANSFER && entry.entries.length > 1) {
    const sum = entry.entries.reduce((acc, e) => acc + Math.abs(e.value), 0);
    return Math.round(sum * 100) / 100;
  }

  return Math.abs(entry.entries[0]?.value ?? 0);
}

export function mapEventToTransactionFormPatch(entry: EventForListDto): TransactionFormHydration {
  const recurrence = entry.recurrenceConfig;
  const paymentType = recurrence?.paymentType ?? PaymentType__Obj.UNIQUE;
  const isInstallment = paymentType === PaymentType__Obj.INSTALLMENTS;
  const isRecurring = paymentType === PaymentType__Obj.RECURRING;
  const transferOrigin = getTransferOriginEntry(entry);
  const transferTarget = getTransferTargetEntry(entry);

  if (entry.type === WalletEntryType__Obj.TRANSFER) {
    return {
      patch: {
        type: entry.type,
        group: entry.group == null ? undefined : { id: entry.group.id, name: entry.group.name },
        origin: transferOrigin.walletItem as any,
        target: transferTarget.walletItem as any,
        name: entry.name ?? undefined,
        category: entry.category ?? undefined,
        date: dayjs(entry.date).toDate(),
        value: getTransferOriginValue(entry),
        targetValue: getTransferTargetValue(entry),
        confirmed: entry.confirmed,
        observations: entry.observations ?? undefined,
        paymentType: paymentType,
        transferPurpose: entry.transferPurpose ?? TransferPurpose__Obj.GENERAL,
        installments: isInstallment ? (recurrence?.qtyLimit ?? entry.installment ?? undefined) : undefined,
        periodicity: recurrence?.periodicity ?? RecurrenceType__Obj.MONTHLY,
        valueType: isInstallment ? ValueType.INSTALLMENT : ValueType.TOTAL,
        periodicityQtyLimit: isRecurring ? (recurrence?.qtyLimit ?? undefined) : undefined,
        originBill: transferOrigin.billDate == null ? undefined : dayjs(transferOrigin.billDate).toDate(),
        targetBill: transferTarget.billDate == null ? undefined : dayjs(transferTarget.billDate).toDate(),
        tags: entry.tags == null ? undefined : [...entry.tags],
      },
      primaryOriginContributionPercent: 100,
      extraSourceLegs: [],
      beneficiaryLegs: [],
    };
  }

  const percents = resolveContributionPercents(entry.entries);
  const first = entry.entries[0]!;
  const rest = entry.entries.slice(1);

  const extraSourceLegs: ExtraSourceLegInit[] = rest.map((e, i) => ({
    walletItem: e.walletItem as WalletItemSearchResponseDto,
    contributionPercent: percents[i + 1]!,
    bill: e.billDate == null ? undefined : dayjs(e.billDate).toDate(),
  }));

  const beneficiaries =
    entry.beneficiaries.length > 0 ? entry.beneficiaries : entry.user?.id != null ? [{ userId: entry.user.id, benefitPercent: 100 }] : [];

  const beneficiaryLegs: ExtraBeneficiaryLegInit[] = beneficiaries.map(beneficiary => ({
    userId: beneficiary.userId,
    benefitPercent: beneficiary.benefitPercent,
  }));

  return {
    patch: {
      type: entry.type,
      group: entry.group == null ? undefined : { id: entry.group.id, name: entry.group.name },
      origin: first.walletItem as any,
      target: undefined,
      name: entry.name ?? undefined,
      category: entry.category ?? undefined,
      date: dayjs(entry.date).toDate(),
      value: getEditableValueFromEvent(entry),
      targetValue: undefined,
      confirmed: entry.confirmed,
      observations: entry.observations ?? undefined,
      paymentType: paymentType,
      transferPurpose: entry.transferPurpose ?? TransferPurpose__Obj.GENERAL,
      installments: isInstallment ? (recurrence?.qtyLimit ?? entry.installment ?? undefined) : undefined,
      periodicity: recurrence?.periodicity ?? RecurrenceType__Obj.MONTHLY,
      valueType: isInstallment ? ValueType.INSTALLMENT : ValueType.TOTAL,
      periodicityQtyLimit: isRecurring ? (recurrence?.qtyLimit ?? undefined) : undefined,
      originBill: first.billDate == null ? undefined : dayjs(first.billDate).toDate(),
      targetBill: undefined,
      tags: entry.tags == null ? undefined : [...entry.tags],
    },
    primaryOriginContributionPercent: percents[0]!,
    extraSourceLegs,
    beneficiaryLegs,
  };
}

export function mapTransactionFormToNewEntryDto(
  form: NewTransactionForm,
  calculatedValue: number | undefined,
  includeTransferTargetValue: boolean,
): NewEntryDto {
  const formValue = form.getRawValue();

  let value = formValue.value ?? 0;
  if (formValue.paymentType === PaymentType__Obj.INSTALLMENTS && formValue.valueType === ValueType.TOTAL) {
    value = calculatedValue ?? value;
  }

  const normalizedValue = parseFloat(value.toFixed(2));
  const normalizedTargetValue = formValue.targetValue == null ? null : parseFloat(formValue.targetValue.toFixed(2));

  if (formValue.type === WalletEntryType__Obj.TRANSFER) {
    return {
      beneficiaries: null,
      categoryId: formValue.category?.id,
      confirmed: formValue.confirmed ?? false,
      date: dayjs(formValue.date!!).format(ONLY_DATE_FORMAT),
      groupId: formValue.group?.id,
      installments: formValue.installments,
      name: formValue.name,
      observations: formValue.observations,
      originBillDate: formValue.originBill == null ? null : dayjs(formValue.originBill).format(ONLY_DATE_FORMAT),
      originId: formValue.origin!!.id,
      paymentType: formValue.paymentType!!,
      transferPurpose: formValue.transferPurpose ?? TransferPurpose__Obj.GENERAL,
      periodicity: formValue.paymentType == PaymentType__Obj.UNIQUE ? RecurrenceType__Obj.SINGLE : formValue.periodicity,
      periodicityQtyLimit: formValue.periodicityQtyLimit,
      tags: formValue.tags,
      targetBillDate: formValue.targetBill == null ? null : dayjs(formValue.targetBill).format(ONLY_DATE_FORMAT),
      targetId: formValue.target?.id,
      type: formValue.type,
      value: null,
      originValue: normalizedValue,
      targetValue: includeTransferTargetValue ? normalizedTargetValue : null,
    };
  }

  const extras = formValue.extraSourceLegs ?? [];
  const primaryPct = extras.length === 0 ? 100 : parseFloat(Number(formValue.primaryOriginContributionPercent ?? 100).toFixed(2));

  const sources = [
    {
      walletItemId: formValue.origin!!.id,
      contributionPercent: primaryPct,
      billDate: formValue.originBill == null ? null : dayjs(formValue.originBill).format(ONLY_DATE_FORMAT),
    },
    ...extras.map(leg => ({
      walletItemId: leg.walletItem!.id,
      contributionPercent: parseFloat(Number(leg.contributionPercent ?? 0).toFixed(2)),
      billDate: leg.bill == null ? null : dayjs(leg.bill).format(ONLY_DATE_FORMAT),
    })),
  ];
  const beneficiaries =
    formValue.group?.id == null || formValue.primaryBeneficiaryUser == null
      ? null
      : [
          {
            userId: formValue.primaryBeneficiaryUser?.id,
            benefitPercent: parseFloat(Number(formValue.primaryBeneficiaryPercent ?? 100).toFixed(2)),
          },
          ...((formValue.extraBeneficiaryLegs ?? []).map(leg => ({
            userId: leg.user!!.id!!,
            benefitPercent: parseFloat(Number(leg.benefitPercent ?? 0).toFixed(2)),
          })) ?? []),
        ];

  return {
    beneficiaries,
    categoryId: formValue.category?.id,
    confirmed: formValue.confirmed ?? false,
    date: dayjs(formValue.date!!).format(ONLY_DATE_FORMAT),
    groupId: formValue.group?.id,
    installments: formValue.installments,
    name: formValue.name,
    observations: formValue.observations,
    originBillDate: null,
    originId: null,
    paymentType: formValue.paymentType!!,
    transferPurpose: formValue.transferPurpose ?? TransferPurpose__Obj.GENERAL,
    periodicity: formValue.paymentType == PaymentType__Obj.UNIQUE ? RecurrenceType__Obj.SINGLE : formValue.periodicity,
    periodicityQtyLimit: formValue.periodicityQtyLimit,
    sources,
    tags: formValue.tags,
    targetBillDate: null,
    targetId: formValue.target?.id,
    type: formValue.type!!,
    value: normalizedValue,
    originValue: null,
    targetValue: null,
  };
}

export function convertUserToUserForBeneficiary(user: UserSimpleDto, translateService: TranslateService): UserForBeneficiary {
  return {
    ...user,
    label: translateService.instant('name.fullName', {
      lastName: user.lastName,
      firstName: user.firstName,
    }),
  };
}
