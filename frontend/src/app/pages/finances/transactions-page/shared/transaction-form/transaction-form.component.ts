import { Component, Signal, computed, effect, inject, input, output } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { combineLatest, startWith } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { SelectButton } from 'primeng/selectbutton';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { ChipEditorComponent } from '../../../../../components/chip-editor/chip-editor.component';
import { DatePickerComponent } from '../../../../../components/date-picker/date-picker.component';
import { I18nSelectComponent } from '../../../../../components/i18n-select/i18n-select.component';
import { PagedSelectComponent } from '../../../../../components/paged-select/paged-select.component';
import { RequiredFieldAsteriskComponent } from '../../../../../components/required-field-asterisk/required-field-asterisk.component';
import { TextareaComponent } from '../../../../../components/textarea/textarea.component';
import { GroupWithRoleDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletItemSearchResponseDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { NewEntryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType,
  PaymentType__Obj,
  RecurrenceType,
  RecurrenceType__Obj,
  RecurrenceType__Options,
  WalletEntryType,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { SimpleMenuItem } from '../../../../../models/simple-menu-item';
import { UserService } from '../../../../../services/user.service';
import { CategoryPickerComponent } from '../../../components/item-picker/category-picker/category-picker.component';
import { WalletItemPickerComponent } from '../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { CreditCardBillService } from '../../../services/credit-card-bill.service';
import { GroupService } from '../../../services/group.service';
import { mapEventToTransactionFormPatch, mapTransactionFormToNewEntryDto } from './transaction-form.mapper';
import { NewTransactionForm, TransactionFormInitialData, TransactionFormMode, ValueType } from './transaction-form.types';

@Component({
  selector: 'app-transaction-form',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    PagedSelectComponent,
    WalletItemPickerComponent,
    InputText,
    CategoryPickerComponent,
    InputNumber,
    RequiredFieldAsteriskComponent,
    DatePickerComponent,
    ToggleSwitch,
    SelectButton,
    I18nSelectComponent,
    TextareaComponent,
    ChipEditorComponent,
    RouterLink,
  ],
  templateUrl: './transaction-form.component.html',
})
@UntilDestroy()
export class TransactionFormComponent {
  readonly mode = input<TransactionFormMode>('create');
  readonly submitting = input(false);
  readonly submitLabel = input('general.save');
  readonly initialEntry = input<TransactionFormInitialData | undefined>(undefined);
  readonly withFuture = input(false);
  readonly showRecurringScopeHint = computed(() => this.mode() === 'edit' && this.initialEntry()?.recurrenceConfigId != null);
  readonly isRecurringWithFuture = computed(() => this.showRecurringScopeHint() && this.withFuture());

  readonly formSubmitted = output<NewEntryDto>();

  readonly WalletEntryType = WalletEntryType__Obj;
  readonly PaymentType = PaymentType__Obj;
  readonly ValueType = ValueType;

  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);
  private readonly userService = inject(UserService);
  private readonly creditCardBillService = inject(CreditCardBillService);

  readonly form: NewTransactionForm;
  readonly user: Signal<UserResponseDto | null> = this.userService.user;

  private hydratedEntryKey: string | undefined;
  private isHydrating = false;

  get selectedGroup() {
    return this.form.get('group')!!.value;
  }

  get groupControl() {
    return this.form.get('group')!!;
  }

  get typeControl() {
    return this.form.get('type')!!;
  }

  get originControl() {
    return this.form.get('origin')!!;
  }

  get targetControl() {
    return this.form.get('target')!!;
  }

  get dateControl() {
    return this.form.get('date')!!;
  }

  get currentOrigin() {
    return this.originControl.value;
  }

  get currentTarget() {
    return this.targetControl.value;
  }

  get currentOriginIsCreditCard(): boolean {
    return this.currentOrigin?.type === 'CREDIT_CARD';
  }

  get currentTargetIsCreditCard(): boolean {
    return this.currentTarget?.type === 'CREDIT_CARD';
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
    if (this.currentPaymentType === PaymentType__Obj.INSTALLMENTS) {
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
    if (this.valueTypeControl.value === ValueType.TOTAL) {
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
        type: [WalletEntryType__Obj.EXPENSE, [Validators.required]],
        group: [undefined],
        origin: [undefined, [Validators.required]],
        target: [undefined],
        name: [undefined, [Validators.maxLength(255)]],
        category: [undefined],
        value: [undefined, [Validators.required, Validators.min(0.01)]],
        date: [new Date(), [Validators.required]],
        confirmed: [false, [Validators.required]],
        observations: [undefined, [Validators.maxLength(512)]],
        paymentType: [PaymentType__Obj.UNIQUE, [Validators.required]],
        installments: [undefined, [Validators.min(2), Validators.max(720)]],
        periodicity: [RecurrenceType__Obj.MONTHLY, []],
        valueType: [ValueType.TOTAL, []],
        calculatedValue: [0, []],
        periodicityQtyLimit: [undefined, []],
        originBill: [undefined, []],
        targetBill: [undefined, []],
        tags: [undefined, []],
      },
      {
        validators: [
          this.requiredOnlyOnTransfer,
          this.requiredOnlyIfInstallment,
          this.requiredOnlyOnOriginCreditCard,
          this.requiredOnlyOnTargetCreditCard,
        ],
      },
    ) as NewTransactionForm;

    this.calculatedValueControl.disable();

    effect(() => {
      if (this.mode() === 'edit') {
        this.groupControl.disable({ emitEvent: false });
      } else {
        this.groupControl.enable({ emitEvent: false });
      }

      const entry = this.initialEntry();

      if (entry == null) {
        return;
      }

      const entryKey = entry.id ?? `${entry.recurrenceConfigId ?? 'single'}:${entry.date}`;
      if (this.hydratedEntryKey === entryKey) {
        return;
      }

      this.hydratedEntryKey = entryKey;
      this.isHydrating = true;
      this.form.patchValue(mapEventToTransactionFormPatch(entry));
      this.isHydrating = false;

      this.typeControl.enable({ emitEvent: false });

      if (entry.recurrenceConfigId != null) {
        this.paymentTypeControl.disable({ emitEvent: false });
      } else {
        this.paymentTypeControl.enable({ emitEvent: false });
      }

      this.form.updateValueAndValidity({ emitEvent: false });
    });

    this.groupControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      if (this.isHydrating) return;
      this.form.get('origin')?.reset();
      this.form.get('target')?.reset();
    });

    this.typeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      if (this.isHydrating) return;
      this.form.get('target')?.reset();
    });

    this.paymentTypeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(newPaymentType => {
      if (newPaymentType !== PaymentType__Obj.INSTALLMENTS) {
        this.valueTypeControl.setValue(ValueType.TOTAL);
        this.installmentsControl.reset();
      }

      if (newPaymentType !== PaymentType__Obj.INSTALLMENTS && newPaymentType !== PaymentType__Obj.RECURRING) {
        this.periodicityControl.setValue(RecurrenceType__Obj.MONTHLY);
      }

      if (newPaymentType !== PaymentType__Obj.RECURRING) {
        this.form.get('periodicityQtyLimit')?.reset();
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

    combineLatest([
      this.dateControl.valueChanges.pipe(startWith(this.dateControl.value)),
      this.originControl.valueChanges.pipe(startWith(this.originControl.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(([date, origin]) => {
        if (
          origin?.type === 'CREDIT_CARD' &&
          origin.dueDay != null &&
          origin.dueOnNextBusinessDay != null &&
          origin.daysBetweenDueAndClosing != null
        ) {
          this.form
            .get('originBill')!!
            .setValue(
              this.creditCardBillService
                .getBestBill(date ?? new Date(), origin.dueDay, origin.dueOnNextBusinessDay, origin.daysBetweenDueAndClosing)
                .toDate(),
            );
        } else if (origin?.type !== 'CREDIT_CARD') {
          this.form.get('originBill')?.reset();
        }
      });

    combineLatest([
      this.dateControl.valueChanges.pipe(startWith(this.dateControl.value)),
      this.targetControl.valueChanges.pipe(startWith(this.targetControl.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(([date, target]) => {
        if (
          target?.type === 'CREDIT_CARD' &&
          target.dueDay != null &&
          target.dueOnNextBusinessDay != null &&
          target.daysBetweenDueAndClosing != null
        ) {
          this.form
            .get('targetBill')!!
            .setValue(
              this.creditCardBillService
                .getBestBill(date ?? new Date(), target.dueDay, target.dueOnNextBusinessDay, target.daysBetweenDueAndClosing)
                .toDate(),
            );
        } else if (target?.type !== 'CREDIT_CARD') {
          this.form.get('targetBill')?.reset();
        }
      });
  }

  transactionTypeOptions: any[] = [
    {
      label: 'enums.walletEntryType.REVENUE',
      value: WalletEntryType__Obj.REVENUE,
      styleClass: 'rounded-none md:rounded-l-md',
      severity: 'success',
    },
    {
      label: 'enums.walletEntryType.EXPENSE',
      value: WalletEntryType__Obj.EXPENSE,
      styleClass: 'rounded-none',
      severity: 'danger',
    },
    {
      label: 'enums.walletEntryType.TRANSFER',
      value: WalletEntryType__Obj.TRANSFER,
      styleClass: 'rounded-none md:rounded-r-md',
      severity: 'info',
    },
  ];

  paymentTypeOptions: SimpleMenuItem<PaymentType>[] = [
    {
      label: 'enums.paymentType.UNIQUE',
      value: PaymentType__Obj.UNIQUE,
    },
    {
      label: 'enums.paymentType.RECURRING',
      value: PaymentType__Obj.RECURRING,
    },
    {
      label: 'enums.paymentType.INSTALLMENTS',
      value: PaymentType__Obj.INSTALLMENTS,
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

  recurrenceTypeOptions: SimpleMenuItem<RecurrenceType>[] = RecurrenceType__Options.filter(
    recurrence => recurrence !== RecurrenceType__Obj.SINGLE,
  ).map(recurrence => ({
    label: `enums.recurrenceType.${recurrence}`,
    value: recurrence,
  }));

  onTypeOptionClick(type: WalletEntryType) {
    if (this.typeControl.disabled || this.isTypeOptionDisabled(type)) {
      return;
    }

    this.typeControl.setValue(type);
  }

  isTypeOptionDisabled(type: WalletEntryType): boolean {
    if (this.mode() !== 'edit') {
      return false;
    }

    const initialType = this.initialEntry()?.type;
    if (initialType == null) {
      return false;
    }

    return this.isTransferType(initialType) !== this.isTransferType(type);
  }

  async loadGroups(page = 0, query: string | undefined): Promise<GroupWithRoleDto[]> {
    return await this.groupService.getAllGroups();
  }

  submit() {
    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.formSubmitted.emit(mapTransactionFormToNewEntryDto(this.form, this.calculatedValueControl.value));
  }

  shouldRequireOriginBill(): boolean {
    return this.currentOriginIsCreditCard;
  }

  shouldRequireTargetBill(): boolean {
    return this.currentTargetIsCreditCard;
  }

  private requiredOnlyOnTransfer = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    const targetControl = group.get('target')!!;

    const isTransfer = type === WalletEntryType__Obj.TRANSFER;
    return this.requireFieldsIf(targetControl, isTransfer);
  };

  private requiredOnlyIfInstallment = (group: NewTransactionForm): ValidationErrors | null => {
    const paymentType = group.get('paymentType')?.value;
    const isInstallments = paymentType === PaymentType__Obj.INSTALLMENTS;

    const errors = [
      this.requireFieldsIf(group.get('installments')!!, isInstallments),
      this.requireFieldsIf(group.get('periodicity')!!, isInstallments),
    ].filter(v => v != null);

    if (errors.length == 0) return null;

    return errors.reduce((acc, curr) => ({ ...acc, ...curr }), {});
  };

  private requiredOnlyOnOriginCreditCard = (group: NewTransactionForm): ValidationErrors | null => {
    const origin = group.get('origin')?.value;
    const originBillControl = group.get('originBill')!!;

    const isCreditCard = origin?.type === 'CREDIT_CARD';
    return this.requireFieldsIf(originBillControl, isCreditCard);
  };

  private requiredOnlyOnTargetCreditCard = (group: NewTransactionForm): ValidationErrors | null => {
    const target = group.get('target')?.value;
    const targetBillControl = group.get('targetBill')!!;

    const isCreditCard = target?.type === 'CREDIT_CARD';
    return this.requireFieldsIf(targetBillControl, isCreditCard);
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

  private isTransferType(type: WalletEntryType): boolean {
    return type === WalletEntryType__Obj.TRANSFER;
  }
}
