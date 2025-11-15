import { Component, Signal, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { combineLatest, startWith } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { DatePickerComponent } from '../../../../components/date-picker/date-picker.component';
import { I18nSelectComponent } from '../../../../components/i18n-select/i18n-select.component';
import { PagedSelectComponent } from '../../../../components/paged-select/paged-select.component';
import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import {
  RecurrenceType,
  RecurrenceType__Obj,
  RecurrenceType__Options,
} from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { SimpleMenuItem } from '../../../../models/simple-menu-item';
import { UserService } from '../../../../services/user.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { CategoryPickerComponent } from '../../components/item-picker/category-picker/category-picker.component';
import { WalletItemPickerComponent } from '../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { GroupService } from '../../services/group.service';

enum TransactionType {
  REVENUE,
  EXPENSE,
  TRANSFER,
}

enum PaymentType {
  UNIQUE,
  RECURRING,
  INSTALLMENTS,
}

enum ValueType {
  TOTAL = 'TOTAL',
  INSTALLMENT = 'INSTALLMENT',
}

type NewTransactionForm = FormGroup<{
  type: FormControl<TransactionType | undefined>;
  group: FormControl<GroupWithRoleDto | undefined>;
  origin: FormControl<WalletItemSearchResponseDto | undefined>;
  target: FormControl<WalletItemSearchResponseDto | undefined>;
  name: FormControl<string | undefined>;
  category: FormControl<CategoryDto | undefined>;
  date: FormControl<Date | undefined>;
  value: FormControl<number | undefined>;
  confirmed: FormControl<boolean | undefined>;
  observations: FormControl<string | undefined>;
  paymentType: FormControl<PaymentType | undefined>;
  installments: FormControl<number | undefined>;
  periodicity: FormControl<RecurrenceType | undefined>;
  valueType: FormControl<ValueType | undefined>;
  calculatedValue: FormControl<number | undefined>;
}>;

@Component({
  selector: 'app-new-transaction-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ButtonDirective,
    ReactiveFormsModule,
    PagedSelectComponent,
    WalletItemPickerComponent,
    InputText,
    CategoryPickerComponent,
    Textarea,
    InputNumber,
    RequiredFieldAsteriskComponent,
    DatePickerComponent,
    ToggleSwitch,
    SelectButton,
    I18nSelectComponent,
  ],
  templateUrl: './new-transaction-page.component.html',
  styleUrl: './new-transaction-page.component.scss',
})
@UntilDestroy()
export class NewTransactionPageComponent {
  readonly TransactionType = TransactionType;
  readonly PaymentType = PaymentType;

  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);
  private readonly userService = inject(UserService);

  readonly form: NewTransactionForm;

  user: Signal<UserResponseDto | null> = this.userService.user;
  submitting = false;

  get selectedGroup() {
    return this.form.get('group')!!.value;
  }

  get groupControl() {
    return this.form.get('group')!!;
  }

  get typeControl() {
    return this.form.get('type')!!;
  }

  get currentOrigin() {
    return this.form.get('origin')!!.value;
  }

  get currentPaymentType() {
    return this.form.get('paymentType')!!.value;
  }

  get valueControl() {
    return this.form.get('value')!!;
  }

  get installmentsControl() {
    return this.form.get('installments')!!;
  }

  get valueTypeControl() {
    return this.form.get('valueType')!!;
  }

  get calculatedValueControl() {
    return this.form.get('calculatedValue')!!;
  }

  get periodicityControl() {
    return this.form.get('periodicity')!!;
  }

  get paymentTypeControl() {
    return this.form.get('paymentType')!!;
  }

  get calculatedValueType(): ValueType {
    return this.valueTypeControl.value === ValueType.TOTAL ? ValueType.INSTALLMENT : ValueType.TOTAL;
  }

  get valueSublabel(): string {
    if (this.currentPaymentType === PaymentType.INSTALLMENTS) {
      if (this.valueTypeControl.value === ValueType.TOTAL) {
        return 'financesPage.transactionsPage.valueTypes.TOTAL';
      } else {
        return this.currentPeriodicityLabel;
      }
    } else {
      return '';
    }
  }

  get calculatedValueSublabel(): string {
    if (this.currentPaymentType === PaymentType.INSTALLMENTS && this.valueTypeControl.value === ValueType.TOTAL) {
      return this.currentPeriodicityLabel;
    } else {
      return '';
    }
  }

  get currentPeriodicityLabel(): string {
    if (this.periodicityControl.value == null) return '';

    return `enums.recurrenceType.${this.periodicityControl.value}`;
  }

  constructor() {
    this.form = this.formBuilder.group(
      {
        type: [TransactionType.REVENUE, [Validators.required]],
        group: [undefined],
        origin: [undefined, [Validators.required]],
        target: [undefined],
        name: [undefined, [Validators.maxLength(255)]],
        category: [undefined],
        value: [undefined, [Validators.required, Validators.min(0.01)]],
        date: [new Date(), [Validators.required]],
        confirmed: [false, [Validators.required]],
        observations: [undefined, [Validators.maxLength(512)]],
        paymentType: [PaymentType.UNIQUE, [Validators.required]],
        installments: [undefined, [Validators.min(2), Validators.max(720)]],
        periodicity: [RecurrenceType__Obj.MONTHLY, []],
        valueType: [ValueType.TOTAL, []],
        calculatedValue: [0, []],
      },
      { validators: [this.requiredOnlyOnTransfer, this.requiredOnlyIfInstallment] },
    ) as NewTransactionForm;

    this.calculatedValueControl.disable();

    this.groupControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.form.get('origin')?.reset();
      this.form.get('target')?.reset();
    });

    this.typeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.form.get('target')?.reset();
    });

    this.paymentTypeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(newPaymentType => {
      if (newPaymentType !== PaymentType.INSTALLMENTS) {
        this.installmentsControl.reset();
      }

      if (newPaymentType !== PaymentType.INSTALLMENTS && newPaymentType !== PaymentType.RECURRING) {
        this.periodicityControl.reset();
      }
    });

    combineLatest([
      this.valueControl.valueChanges.pipe(startWith(this.valueControl.value)),
      this.installmentsControl.valueChanges.pipe(startWith(this.installmentsControl.value)),
      this.valueTypeControl.valueChanges.pipe(startWith(this.valueTypeControl.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(([value, installments, valueType]) => {
        const currentValueSafe = value ?? 0;
        const currentInstallmentsSafe = Math.max(installments ?? 1, 1);

        if (valueType === ValueType.TOTAL) {
          this.calculatedValueControl.setValue(currentValueSafe / currentInstallmentsSafe);
        } else {
          this.calculatedValueControl.setValue(currentValueSafe * currentInstallmentsSafe);
        }
      });
  }

  transactionTypeOptions: any[] = [
    {
      label: 'financesPage.transactionsPage.types.revenue',
      value: TransactionType.REVENUE,
      styleClass: 'rounded-none md:rounded-l-md',
      severity: 'success',
    },
    {
      label: 'financesPage.transactionsPage.types.expense',
      value: TransactionType.EXPENSE,
      styleClass: 'rounded-none',
      severity: 'danger',
    },
    {
      label: 'financesPage.transactionsPage.types.transfer',
      value: TransactionType.TRANSFER,
      styleClass: 'rounded-none md:rounded-r-md',
      severity: 'info',
    },
  ];

  paymentTypeOptions: SimpleMenuItem<PaymentType>[] = [
    {
      label: 'financesPage.transactionsPage.paymentTypes.unique',
      value: PaymentType.UNIQUE,
    },
    {
      label: 'financesPage.transactionsPage.paymentTypes.recurring',
      value: PaymentType.RECURRING,
    },
    {
      label: 'financesPage.transactionsPage.paymentTypes.installments',
      value: PaymentType.INSTALLMENTS,
    },
  ];

  valueTypeOptions: SimpleMenuItem<ValueType>[] = [
    {
      label: 'financesPage.transactionsPage.valueTypes.TOTAL',
      value: ValueType.TOTAL,
    },
    {
      label: 'financesPage.transactionsPage.valueTypes.INSTALLMENT',
      value: ValueType.INSTALLMENT,
    },
  ];

  recurrenceTypeOptions: SimpleMenuItem<RecurrenceType>[] = RecurrenceType__Options.map(recurrence => ({
    label: `enums.recurrenceType.${recurrence}`,
    value: recurrence,
  }));

  async loadGroups(page = 0, query: string | undefined): Promise<GroupWithRoleDto[]> {
    return await this.groupService.getAllGroups();
  }

  submit() {
    throw new Error('Method not implemented.');
  }

  private requiredOnlyOnTransfer = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    const targetControl = group.get('target')!!;

    const isTransfer = type === TransactionType.TRANSFER;
    return this.requireFieldsIf(targetControl, isTransfer);
  };

  private requiredOnlyIfInstallment = (group: NewTransactionForm): ValidationErrors | null => {
    const paymentType = group.get('paymentType')?.value;
    const isInstallments = paymentType === PaymentType.INSTALLMENTS;

    const errors = [
      this.requireFieldsIf(group.get('installments')!!, isInstallments),
      this.requireFieldsIf(group.get('periodicity')!!, isInstallments),
    ].filter(v => v != null);

    if (errors.length == 0) return null;

    return errors.reduce((acc, curr) => ({ ...acc, ...curr }), {});
  };

  private requireFieldsIf(control: AbstractControl, required: boolean): ValidationErrors | null {
    const hasTargetValue = control.value !== undefined && control.value !== null;

    if (required && !hasTargetValue) {
      const nextErrors = { ...(control.errors ?? {}), required: true };
      control.setErrors(nextErrors);
    } else if (!required && control.errors) {
      const { required, ...rest } = control.errors;
      control.setErrors(Object.keys(rest).length ? rest : null);
    } else if (required && hasTargetValue && control.errors) {
      const { required, ...rest } = control.errors;
      control.setErrors(Object.keys(rest).length ? rest : null);
    }

    return control.errors;
  }
}
