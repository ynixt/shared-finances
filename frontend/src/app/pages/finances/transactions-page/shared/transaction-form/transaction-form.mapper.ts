import dayjs from 'dayjs';

import { EventForListDto, NewEntryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType__Obj,
  RecurrenceType__Obj,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { ONLY_DATE_FORMAT } from '../../../../../util/date-util';
import { NewTransactionForm, ValueType } from './transaction-form.types';

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

export function getEditableValueFromEvent(entry: EventForListDto): number {
  if (entry.type === WalletEntryType__Obj.TRANSFER) {
    return getTransferOriginValue(entry);
  }

  return Math.abs(entry.entries[0]?.value ?? 0);
}

export function mapEventToTransactionFormPatch(entry: EventForListDto): Partial<NewTransactionForm['value']> {
  const recurrence = entry.recurrenceConfig;
  const paymentType = recurrence?.paymentType ?? PaymentType__Obj.UNIQUE;
  const isInstallment = paymentType === PaymentType__Obj.INSTALLMENTS;
  const isRecurring = paymentType === PaymentType__Obj.RECURRING;
  const transferOrigin = getTransferOriginEntry(entry);
  const transferTarget = getTransferTargetEntry(entry);
  const origin = entry.type === WalletEntryType__Obj.TRANSFER ? transferOrigin : entry.entries[0];
  const target = entry.type === WalletEntryType__Obj.TRANSFER ? transferTarget : undefined;

  return {
    type: entry.type,
    group: entry.group == null ? undefined : { id: entry.group.id, name: entry.group.name },
    origin: origin?.walletItem as any,
    target: target?.walletItem as any,
    name: entry.name ?? undefined,
    category: entry.category ?? undefined,
    date: dayjs(entry.date).toDate(),
    value: getEditableValueFromEvent(entry),
    targetValue: entry.type === WalletEntryType__Obj.TRANSFER ? getTransferTargetValue(entry) : undefined,
    confirmed: entry.confirmed,
    observations: entry.observations ?? undefined,
    paymentType: paymentType,
    installments: isInstallment ? (recurrence?.qtyLimit ?? entry.installment ?? undefined) : undefined,
    periodicity: recurrence?.periodicity ?? RecurrenceType__Obj.MONTHLY,
    valueType: isInstallment ? ValueType.INSTALLMENT : ValueType.TOTAL,
    periodicityQtyLimit: isRecurring ? (recurrence?.qtyLimit ?? undefined) : undefined,
    originBill: origin?.billDate == null ? undefined : dayjs(origin.billDate).toDate(),
    targetBill: target?.billDate == null ? undefined : dayjs(target.billDate).toDate(),
    tags: entry.tags == null ? undefined : [...entry.tags],
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

  return {
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
    periodicity: formValue.paymentType == PaymentType__Obj.UNIQUE ? RecurrenceType__Obj.SINGLE : formValue.periodicity,
    periodicityQtyLimit: formValue.periodicityQtyLimit,
    tags: formValue.tags,
    targetBillDate: formValue.targetBill == null ? null : dayjs(formValue.targetBill).format(ONLY_DATE_FORMAT),
    targetId: formValue.target?.id,
    type: formValue.type!!,
    value: formValue.type === WalletEntryType__Obj.TRANSFER ? null : normalizedValue,
    originValue: formValue.type === WalletEntryType__Obj.TRANSFER ? normalizedValue : null,
    targetValue: formValue.type === WalletEntryType__Obj.TRANSFER && includeTransferTargetValue ? normalizedTargetValue : null,
  };
}
