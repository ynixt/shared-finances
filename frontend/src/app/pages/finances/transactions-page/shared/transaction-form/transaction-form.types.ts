import { FormControl, FormGroup } from '@angular/forms';

import { GroupDto, GroupWithRoleDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { EventForListDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { PaymentType, RecurrenceType, WalletEntryType } from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';

export enum ValueType {
  TOTAL = 'TOTAL',
  INSTALLMENT = 'INSTALLMENT',
}

export type SelectableGroup = GroupDto | GroupWithRoleDto;

export type NewTransactionForm = FormGroup<{
  type: FormControl<WalletEntryType | undefined>;
  group: FormControl<SelectableGroup | undefined>;
  origin: FormControl<WalletItemSearchResponseDto | undefined>;
  target: FormControl<WalletItemSearchResponseDto | undefined>;
  name: FormControl<string | undefined>;
  category: FormControl<CategoryDto | undefined>;
  date: FormControl<Date | undefined>;
  value: FormControl<number | undefined>;
  targetValue: FormControl<number | undefined>;
  confirmed: FormControl<boolean | undefined>;
  observations: FormControl<string | undefined>;
  paymentType: FormControl<PaymentType | undefined>;
  installments: FormControl<number | undefined>;
  periodicity: FormControl<RecurrenceType | undefined>;
  valueType: FormControl<ValueType | undefined>;
  calculatedValue: FormControl<number | undefined>;
  periodicityQtyLimit: FormControl<number | undefined>;
  originBill: FormControl<Date | undefined>;
  targetBill: FormControl<Date | undefined>;
  tags: FormControl<string[] | undefined>;
}>;

export type TransactionFormMode = 'create' | 'edit';

export type TransactionFormInitialData = EventForListDto;
