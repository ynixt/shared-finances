import { Component, Signal, computed, effect, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { combineLatest, debounceTime, startWith } from 'rxjs';

import dayjs from 'dayjs';
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
import { NewEntryDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import {
  PaymentType,
  PaymentType__Obj,
  RecurrenceType,
  RecurrenceType__Obj,
  RecurrenceType__Options,
  TransferPurpose__Obj,
  WalletEntryType,
  WalletEntryType__Obj,
} from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { SimpleMenuItem } from '../../../../../models/simple-menu-item';
import { LocalDatePipeService } from '../../../../../pipes/local-date.pipe';
import { LocalNumberPipeService } from '../../../../../pipes/local-number.pipe';
import { UserService } from '../../../../../services/user.service';
import { ONLY_DATE_FORMAT } from '../../../../../util/date-util';
import { CategoryPickerComponent } from '../../../components/item-picker/category-picker/category-picker.component';
import { WalletItemPickerComponent } from '../../../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { CreditCardBillService } from '../../../services/credit-card-bill.service';
import { GroupService } from '../../../services/group.service';
import { WalletEntryService } from '../../../services/wallet-entry.service';
import { defaultBeneficiaryState, validateBeneficiarySplit } from './transaction-form.beneficiaries';
import {
  ExtraBeneficiaryLegInit,
  ExtraSourceLegInit,
  convertUserToUserForBeneficiary,
  mapEventToTransactionFormPatch,
  mapTransactionFormToNewEntryDto,
} from './transaction-form.mapper';
import {
  BeneficiaryLegForm,
  ExtraSourceLegForm,
  NewTransactionForm,
  TransactionFormInitialData,
  TransactionFormMode,
  UserForBeneficiary,
  ValueType,
} from './transaction-form.types';

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
  private readonly walletEntryService = inject(WalletEntryService);
  private readonly translateService = inject(TranslateService);
  private readonly localDatePipeService = inject(LocalDatePipeService);
  private readonly localNumberPipeService = inject(LocalNumberPipeService);

  readonly form: NewTransactionForm;
  readonly user: Signal<UserResponseDto | null> = this.userService.user;
  readonly transferQuoteLoading = signal(false);
  readonly transferQuoteError = signal<string | null>(null);
  /** Stored quote used for cross-currency transfer (read-only display + client-side conversion). */
  readonly transferRateDisplay = signal<{
    rate: number;
    quoteDate: string;
    baseCurrency: string;
    quoteCurrency: string;
  } | null>(null);

  private hydratedEntryKey: string | undefined;
  private isHydrating = false;
  private transferRateRequestId = 0;
  private isUpdatingTargetValueProgrammatically = false;
  /** After loading a concrete transfer for edit, first rate fetch must not overwrite targetValue from the server. */
  private suppressNextCrossCurrencyTargetFromRate = false;
  private lastTransferRateSnapshot: TransferRateSnapshot | null = null;
  /** Same-currency only: user edited target so it should not follow origin amount. */
  private sameCurrencyTargetUserOverridden = false;
  private beneficiaryHydrationRequestId = 0;
  private readonly groupMembersCache = new Map<string, UserForBeneficiary[]>();
  private readonly groupMembersRequestCache = new Map<string, Promise<UserForBeneficiary[]>>();

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

  get extraSourceLegs(): FormArray<ExtraSourceLegForm> {
    return this.form.get('extraSourceLegs') as FormArray<ExtraSourceLegForm>;
  }

  get extraBeneficiaryLegs(): FormArray<BeneficiaryLegForm> {
    return this.form.get('extraBeneficiaryLegs') as FormArray<BeneficiaryLegForm>;
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

  get targetValueControl() {
    return this.form.get('targetValue')!!;
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

  get shouldShowTargetValueField(): boolean {
    if (this.typeControl.value !== WalletEntryType__Obj.TRANSFER) {
      return false;
    }

    if (this.mode() === 'edit') {
      return this.initialEntry()?.id != null;
    }

    return this.currentPaymentType === PaymentType__Obj.UNIQUE && !this.isFutureDate(this.dateControl.value);
  }

  get shouldShowDeferredTargetHint(): boolean {
    return this.typeControl.value === WalletEntryType__Obj.TRANSFER && !this.shouldShowTargetValueField;
  }

  get shouldRequireTargetValueField(): boolean {
    return this.shouldShowTargetValueField;
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
        targetValue: [undefined, [Validators.min(0.01)]],
        date: [new Date(), [Validators.required]],
        confirmed: [false, [Validators.required]],
        observations: [undefined, [Validators.maxLength(512)]],
        paymentType: [PaymentType__Obj.UNIQUE, [Validators.required]],
        transferPurpose: [TransferPurpose__Obj.GENERAL, [Validators.required]],
        installments: [undefined, [Validators.min(2), Validators.max(720)]],
        periodicity: [RecurrenceType__Obj.MONTHLY, []],
        valueType: [ValueType.TOTAL, []],
        calculatedValue: [0, []],
        periodicityQtyLimit: [undefined, []],
        originBill: [undefined, []],
        targetBill: [undefined, []],
        tags: [undefined, []],
        primaryOriginContributionPercent: [100, [Validators.min(0.01), Validators.max(100)]],
        extraSourceLegs: this.formBuilder.array<ExtraSourceLegForm>([]),
        primaryBeneficiaryUser: [undefined],
        primaryBeneficiaryPercent: [100, [Validators.min(0.01), Validators.max(100)]],
        extraBeneficiaryLegs: this.formBuilder.array<BeneficiaryLegForm>([]),
      },
      {
        validators: [
          this.requiredOnlyOnTransfer,
          this.requiredOnlyOnConcreteTransferTargetValue,
          this.requiredOnlyIfInstallment,
          this.requiredOnlyOnOriginCreditCard,
          this.requiredOnlyOnTargetCreditCard,
          this.sourcePercentsMustSumTo100,
          this.requiredExtraSourceLegFields,
          this.beneficiaryPercentsMustSumTo100,
          this.requiredBeneficiaryFields,
          this.beneficiaryUsersMustBeUnique,
        ],
      },
    ) as NewTransactionForm;

    this.calculatedValueControl.disable();

    combineLatest([
      this.form
        .get('primaryOriginContributionPercent')!
        .valueChanges.pipe(startWith(this.form.get('primaryOriginContributionPercent')!.value)),
      this.extraSourceLegs.valueChanges.pipe(startWith(this.extraSourceLegs.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(() => {
        this.form.updateValueAndValidity({ emitEvent: false });
      });

    combineLatest([
      this.form.get('primaryBeneficiaryUser')!.valueChanges.pipe(startWith(this.form.get('primaryBeneficiaryUser')!.value)),
      this.form.get('primaryBeneficiaryPercent')!.valueChanges.pipe(startWith(this.form.get('primaryBeneficiaryPercent')!.value)),
      this.extraBeneficiaryLegs.valueChanges.pipe(startWith(this.extraBeneficiaryLegs.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(() => {
        this.form.updateValueAndValidity({ emitEvent: false });
      });

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
      const hydration = mapEventToTransactionFormPatch(entry);
      this.form.patchValue(hydration.patch);
      this.form.get('primaryOriginContributionPercent')?.setValue(hydration.primaryOriginContributionPercent);
      this.resetExtraSourceLegs(hydration.extraSourceLegs);
      this.form.get('primaryBeneficiaryUser')?.setValue(undefined);
      this.form.get('primaryBeneficiaryPercent')?.setValue(100);
      this.resetExtraBeneficiaryLegs([]);
      this.isHydrating = false;
      this.lastTransferRateSnapshot = null;
      this.transferRateDisplay.set(null);
      this.suppressNextCrossCurrencyTargetFromRate =
        this.mode() === 'edit' &&
        entry.id != null &&
        entry.type === WalletEntryType__Obj.TRANSFER &&
        this.shouldShowTargetValueField &&
        this.hasDifferentTransferCurrencies();
      this.syncSameCurrencyTargetOverrideState();

      this.typeControl.enable({ emitEvent: false });

      if (entry.recurrenceConfigId != null) {
        this.paymentTypeControl.disable({ emitEvent: false });
      } else {
        this.paymentTypeControl.enable({ emitEvent: false });
      }

      this.form.updateValueAndValidity({ emitEvent: false });
      void this.hydrateBeneficiariesFromMembers(entryKey, entry.group?.id ?? undefined, hydration.beneficiaryLegs);
    });

    this.groupControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.beneficiaryHydrationRequestId++;
      if (this.isHydrating) return;
      this.form.get('origin')?.reset();
      this.form.get('target')?.reset();
      this.resetExtraSourceLegs([]);
      this.resetBeneficiariesForContext();
      this.form.get('primaryOriginContributionPercent')?.setValue(100);
      this.resetTargetValueControl();
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
      this.transferQuoteError.set(null);
    });

    this.typeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.beneficiaryHydrationRequestId++;
      if (this.isHydrating) return;
      this.form.get('target')?.reset();
      this.resetExtraSourceLegs([]);
      this.resetBeneficiariesForContext();
      this.form.get('primaryOriginContributionPercent')?.setValue(100);
      this.resetTargetValueControl();
      this.transferQuoteError.set(null);
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
    });

    this.targetValueControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      if (this.isHydrating || this.isUpdatingTargetValueProgrammatically) {
        return;
      }

      this.syncSameCurrencyTargetOverrideState();
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

    combineLatest([
      this.typeControl.valueChanges.pipe(startWith(this.typeControl.value)),
      this.paymentTypeControl.valueChanges.pipe(startWith(this.paymentTypeControl.value)),
      this.dateControl.valueChanges.pipe(startWith(this.dateControl.value)),
      this.originControl.valueChanges.pipe(startWith(this.originControl.value)),
      this.targetControl.valueChanges.pipe(startWith(this.targetControl.value)),
    ])
      .pipe(debounceTime(150), untilDestroyed(this))
      .subscribe(() => {
        if (this.isHydrating) return;
        void this.refreshTransferRateContext();
      });

    this.valueControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      if (this.isHydrating) return;
      this.syncTransferTargetFromOriginValue();
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

  addExtraSourceLeg(): void {
    const arr = this.extraSourceLegs;
    if (arr.length === 0) {
      this.form.get('primaryOriginContributionPercent')?.setValue(50);
      const g = this.createExtraSourceLegGroup({ contributionPercent: 50 });
      arr.push(g);
      this.attachExtraLegBillSync(g);
    } else {
      const g = this.createExtraSourceLegGroup({});
      arr.push(g);
      this.attachExtraLegBillSync(g);
    }
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  removeExtraSourceLeg(index: number): void {
    this.extraSourceLegs.removeAt(index);
    if (this.extraSourceLegs.length === 0) {
      this.form.get('primaryOriginContributionPercent')?.setValue(100);
    }
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  addExtraBeneficiaryLeg(): void {
    const arr = this.extraBeneficiaryLegs;
    if (arr.length === 0) {
      this.form.get('primaryBeneficiaryPercent')?.setValue(50);
      arr.push(this.createExtraBeneficiaryLegGroup({ benefitPercent: 50 }));
    } else {
      arr.push(this.createExtraBeneficiaryLegGroup({}));
    }
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  removeExtraBeneficiaryLeg(index: number): void {
    this.extraBeneficiaryLegs.removeAt(index);
    if (this.extraBeneficiaryLegs.length === 0) {
      this.form.get('primaryBeneficiaryPercent')?.setValue(100);
    }
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  private createExtraSourceLegGroup(init: Partial<ExtraSourceLegInit> = {}): ExtraSourceLegForm {
    return this.formBuilder.group({
      walletItem: [init.walletItem, [Validators.required]],
      contributionPercent: [init.contributionPercent, [Validators.required, Validators.min(0.01), Validators.max(100)]],
      bill: [init.bill],
    }) as ExtraSourceLegForm;
  }

  private createExtraBeneficiaryLegGroup(
    init: Partial<{
      benefitPercent: number;
      user: UserForBeneficiary;
    }> = {},
  ): BeneficiaryLegForm {
    return this.formBuilder.group({
      user: [init.user, [Validators.required]],
      benefitPercent: [init.benefitPercent, [Validators.required, Validators.min(0.01), Validators.max(100)]],
    }) as BeneficiaryLegForm;
  }

  private resetExtraSourceLegs(legs: ExtraSourceLegInit[]): void {
    const arr = this.extraSourceLegs;
    while (arr.length) {
      arr.removeAt(0);
    }
    for (const leg of legs) {
      const g = this.createExtraSourceLegGroup(leg);
      arr.push(g);
      this.attachExtraLegBillSync(g);
    }
  }

  private resetExtraBeneficiaryLegs(
    legs: Array<{
      benefitPercent: number;
      userId: string;
    }>,
  ): void {
    const arr = this.extraBeneficiaryLegs;
    while (arr.length) {
      arr.removeAt(0);
    }
    for (const leg of legs) {
      arr.push(this.createExtraBeneficiaryLegGroup(leg));
    }
  }

  private attachExtraLegBillSync(group: ExtraSourceLegForm): void {
    combineLatest([
      this.dateControl.valueChanges.pipe(startWith(this.dateControl.value)),
      group.get('walletItem')!.valueChanges.pipe(startWith(group.get('walletItem')!.value)),
    ])
      .pipe(untilDestroyed(this))
      .subscribe(([date, w]) => {
        if (this.isHydrating) {
          return;
        }
        if (w?.type === 'CREDIT_CARD' && w.dueDay != null && w.dueOnNextBusinessDay != null && w.daysBetweenDueAndClosing != null) {
          group
            .get('bill')!
            .setValue(
              this.creditCardBillService
                .getBestBill(date ?? new Date(), w.dueDay, w.dueOnNextBusinessDay, w.daysBetweenDueAndClosing)
                .toDate(),
            );
        } else if (w?.type !== 'CREDIT_CARD') {
          group.get('bill')?.reset();
        }
      });
  }

  private sourcePercentsMustSumTo100 = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    if (type === WalletEntryType__Obj.TRANSFER) {
      return null;
    }
    // Use `group`, not `this.form`: validators run while `group()` is still constructing and `this.form` is not assigned yet.
    const arr = group.get('extraSourceLegs') as FormArray<ExtraSourceLegForm> | null;
    if (!arr) {
      return null;
    }
    const primary = arr.length === 0 ? 100 : Number(group.get('primaryOriginContributionPercent')?.value ?? 0);
    let sum = primary;
    for (let i = 0; i < arr.length; i++) {
      sum += Number(arr.at(i).get('contributionPercent')?.value ?? 0);
    }
    const rounded = Math.round(sum * 100) / 100;
    if (rounded !== 100) {
      return { sourcePercentsSum: { sum: rounded } };
    }
    return null;
  };

  private requiredExtraSourceLegFields = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    if (type === WalletEntryType__Obj.TRANSFER) {
      return null;
    }
    const arr = group.get('extraSourceLegs') as FormArray<ExtraSourceLegForm> | null;
    if (!arr) {
      return null;
    }
    for (let i = 0; i < arr.length; i++) {
      const g = arr.at(i) as FormGroup;
      const w = g.get('walletItem')?.value;
      const billCtrl = g.get('bill')!!;
      this.requireFieldsIf(billCtrl, w?.type === 'CREDIT_CARD');
    }
    return null;
  };

  private beneficiaryPercentsMustSumTo100 = (group: NewTransactionForm): ValidationErrors | null => {
    const arr = group.get('extraBeneficiaryLegs') as FormArray<BeneficiaryLegForm> | null;
    return (
      validateBeneficiarySplit({
        groupId: group.get('group')?.value?.id,
        type: group.get('type')?.value,
        primaryBeneficiaryUser: group.get('primaryBeneficiaryUser')?.value,
        primaryBeneficiaryPercent: group.get('primaryBeneficiaryPercent')?.value,
        extraBeneficiaryLegs: arr?.getRawValue() ?? [],
      })?.beneficiaryPercentsSum ?? null
    );
  };

  private requiredBeneficiaryFields = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    const selectedGroup = group.get('group')?.value;
    const isRequired = type !== WalletEntryType__Obj.TRANSFER && selectedGroup != null;

    this.requireFieldsIf(group.get('primaryBeneficiaryUser')!, isRequired);
    this.requireFieldsIf(group.get('primaryBeneficiaryPercent')!, isRequired);

    const arr = group.get('extraBeneficiaryLegs') as FormArray<BeneficiaryLegForm> | null;
    if (!arr) {
      return null;
    }

    for (let i = 0; i < arr.length; i++) {
      const g = arr.at(i) as FormGroup;
      this.requireFieldsIf(g.get('user')!, isRequired);
      this.requireFieldsIf(g.get('benefitPercent')!, isRequired);
    }

    return null;
  };

  private beneficiaryUsersMustBeUnique = (group: NewTransactionForm): ValidationErrors | null => {
    const arr = group.get('extraBeneficiaryLegs') as FormArray<BeneficiaryLegForm> | null;
    return validateBeneficiarySplit({
      groupId: group.get('group')?.value?.id,
      type: group.get('type')?.value,
      primaryBeneficiaryUser: group.get('primaryBeneficiaryUser')?.value,
      primaryBeneficiaryPercent: group.get('primaryBeneficiaryPercent')?.value,
      extraBeneficiaryLegs: arr?.getRawValue() ?? [],
    })?.duplicateBeneficiaries
      ? { duplicateBeneficiaries: true }
      : null;
  };

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

  async loadGroupMembers(page = 0, query: string | undefined): Promise<UserForBeneficiary[]> {
    const groupId = this.selectedGroup?.id;
    if (groupId == null) {
      return [];
    }

    return this.filterGroupMembers(await this.getGroupMembers(groupId), query);
  }

  submit() {
    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.formSubmitted.emit(mapTransactionFormToNewEntryDto(this.form, this.calculatedValueControl.value, this.shouldShowTargetValueField));
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

  private requiredOnlyOnConcreteTransferTargetValue = (group: NewTransactionForm): ValidationErrors | null => {
    const type = group.get('type')?.value;
    const paymentType = group.get('paymentType')?.value;
    const date = group.get('date')?.value;
    const targetValueControl = group.get('targetValue')!!;

    const shouldShowTargetValue =
      type === WalletEntryType__Obj.TRANSFER &&
      (this.mode() === 'edit'
        ? this.initialEntry()?.id != null
        : paymentType === PaymentType__Obj.UNIQUE && !this.isFutureDate(date ?? undefined));

    return this.requireFieldsIf(targetValueControl, shouldShowTargetValue);
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

  private resetBeneficiariesForContext() {
    const user = this.user();

    if (this.isHydrating || !user) {
      return;
    }

    const defaults = defaultBeneficiaryState({
      currentUser: convertUserToUserForBeneficiary(user, this.translateService),
      groupId: this.selectedGroup?.id,
      type: this.typeControl.value,
    });

    this.form.get('primaryBeneficiaryUser')?.setValue(defaults.primaryBeneficiaryUser, { emitEvent: false });
    this.form.get('primaryBeneficiaryPercent')?.setValue(defaults.primaryBeneficiaryPercent, { emitEvent: false });
    this.resetExtraBeneficiaryLegs(
      defaults.extraBeneficiaryLegs.filter((leg): leg is { userId: string; benefitPercent: number } => {
        return leg.userId != null && leg.benefitPercent != null;
      }),
    );
  }

  private async getGroupMembers(groupId: string): Promise<UserForBeneficiary[]> {
    const cached = this.groupMembersCache.get(groupId);
    if (cached != null) {
      return cached;
    }

    const cachedRequest = this.groupMembersRequestCache.get(groupId);
    if (cachedRequest != null) {
      return cachedRequest;
    }

    const request = this.groupService
      .findAllMembers(groupId)
      .then(list => list.map(member => convertUserToUserForBeneficiary(member.user, this.translateService)))
      .then(members => {
        this.groupMembersCache.set(groupId, members);
        this.groupMembersRequestCache.delete(groupId);
        return members;
      })
      .catch(error => {
        this.groupMembersRequestCache.delete(groupId);
        throw error;
      });

    this.groupMembersRequestCache.set(groupId, request);
    return request;
  }

  private filterGroupMembers(members: UserForBeneficiary[], query: string | undefined): UserForBeneficiary[] {
    const normalizedQuery = query?.trim().toLowerCase();
    if (normalizedQuery == null || normalizedQuery.length === 0) {
      return members;
    }

    return members.filter(member => {
      const fullName = `${member.firstName} ${member.lastName}`.trim().toLowerCase();
      return (
        member.label.toLowerCase().includes(normalizedQuery) ||
        fullName.includes(normalizedQuery) ||
        member.email.toLowerCase().includes(normalizedQuery)
      );
    });
  }

  private async hydrateBeneficiariesFromMembers(
    entryKey: string,
    groupId: string | undefined,
    beneficiaryLegs: ExtraBeneficiaryLegInit[],
  ): Promise<void> {
    if (groupId == null) {
      return;
    }

    if (beneficiaryLegs.length === 0) {
      this.form.get('primaryBeneficiaryUser')?.setValue(undefined, { emitEvent: false });
      this.form.get('primaryBeneficiaryPercent')?.setValue(100, { emitEvent: false });
      this.resetExtraBeneficiaryLegs([]);
      this.form.updateValueAndValidity({ emitEvent: false });
      return;
    }

    const requestId = ++this.beneficiaryHydrationRequestId;
    const members = await this.getGroupMembers(groupId);

    if (requestId !== this.beneficiaryHydrationRequestId || this.hydratedEntryKey !== entryKey) {
      return;
    }

    const usersById = new Map(members.map(member => [member.id, member] as const));
    // Beneficiaries are a flat list in the domain model. The form requires one object-based
    // control plus id-based extra rows, so we use the first item only as a UI anchor.
    const [primaryLeg, ...extraLegs] = beneficiaryLegs;
    const primaryBeneficiaryUser = primaryLeg == null ? undefined : usersById.get(primaryLeg.userId);

    this.form.get('primaryBeneficiaryUser')?.setValue(primaryBeneficiaryUser, { emitEvent: false });
    this.form.get('primaryBeneficiaryPercent')?.setValue(primaryLeg?.benefitPercent ?? 100, { emitEvent: false });
    this.resetExtraBeneficiaryLegs(extraLegs);
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  private isTransferType(type: WalletEntryType): boolean {
    return type === WalletEntryType__Obj.TRANSFER;
  }

  hasDifferentTransferCurrencies(): boolean {
    const originCurrency = this.currentOrigin?.currency;
    const targetCurrency = this.currentTarget?.currency;
    return originCurrency != null && targetCurrency != null && originCurrency !== targetCurrency;
  }

  selectedTransferDateIso(): string {
    const d = this.dateControl.value;
    return d ? dayjs(d).format(ONLY_DATE_FORMAT) : '';
  }

  transferRateReadonlyLabel(tr: { rate: number; quoteDate: string; baseCurrency: string; quoteCurrency: string }): string {
    return this.translateService.instant('financesPage.transactionsPage.transferExchangeRateLine', {
      base: tr.baseCurrency,
      rate: this.localNumberPipeService.transform(tr.rate, {
        maximumFractionDigits: 8,
      }),
      quote: tr.quoteCurrency,
    });
  }

  transferRateNearestHint(tr: { quoteDate: string }): string {
    const date = dayjs(tr.quoteDate);

    return this.translateService.instant('financesPage.transactionsPage.transferExchangeRateNearestHint', {
      date: this.localDatePipeService.transform(date, 'shortDate'),
    });
  }

  private isFutureDate(date: Date | undefined): boolean {
    if (date == null) {
      return false;
    }

    const selectedDate = new Date(date);
    selectedDate.setHours(0, 0, 0, 0);

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selectedDate.getTime() > today.getTime();
  }

  private async refreshTransferRateContext() {
    if (this.typeControl.value !== WalletEntryType__Obj.TRANSFER) {
      this.transferRateRequestId++;
      this.transferQuoteLoading.set(false);
      this.transferQuoteError.set(null);
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
      this.resetTargetValueControl();
      this.form.updateValueAndValidity({ emitEvent: false });
      return;
    }

    if (!this.shouldShowTargetValueField) {
      this.transferRateRequestId++;
      this.transferQuoteLoading.set(false);
      this.transferQuoteError.set(null);
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
      this.resetTargetValueControl();
      this.form.updateValueAndValidity({ emitEvent: false });
      return;
    }

    const origin = this.currentOrigin;
    const target = this.currentTarget;
    const date = this.dateControl.value;

    if (origin == null || target == null || date == null) {
      this.transferQuoteLoading.set(false);
      this.transferQuoteError.set(null);
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
      this.form.updateValueAndValidity({ emitEvent: false });
      return;
    }

    const originValue = this.valueControl.value;

    if (origin.currency === target.currency) {
      this.transferRateRequestId++;
      this.transferQuoteLoading.set(false);
      this.transferQuoteError.set(null);
      this.transferRateDisplay.set(null);
      this.lastTransferRateSnapshot = null;
      this.syncSameCurrencyTargetOverrideState();
      if (originValue != null && originValue > 0 && !this.sameCurrencyTargetUserOverridden) {
        this.setTargetValueControlValue(originValue);
      }
      this.form.updateValueAndValidity({ emitEvent: false });
      return;
    }

    const prevSnapshot = this.lastTransferRateSnapshot;
    const currentSnapshot = this.buildTransferRateSnapshot();

    const requestId = ++this.transferRateRequestId;
    this.transferQuoteLoading.set(true);
    this.transferQuoteError.set(null);

    try {
      const dto = await this.walletEntryService.fetchTransferRate({
        groupId: this.groupControl.value?.id ?? null,
        originId: origin.id,
        targetId: target.id,
        date: dayjs(date).format(ONLY_DATE_FORMAT),
      });

      if (requestId !== this.transferRateRequestId) {
        return;
      }

      const rate = Number(dto.rate);
      this.transferRateDisplay.set({
        rate,
        quoteDate: dto.quoteDate,
        baseCurrency: dto.baseCurrency,
        quoteCurrency: dto.quoteCurrency,
      });
      this.transferQuoteError.set(null);

      const targetHasValue = this.targetValueControl.value != null && this.targetValueControl.value > 0;

      if (this.suppressNextCrossCurrencyTargetFromRate) {
        this.suppressNextCrossCurrencyTargetFromRate = false;
        this.lastTransferRateSnapshot = currentSnapshot;
        return;
      }

      const shouldUpdateTarget = this.computeShouldUpdateTargetAfterRateFetch(
        prevSnapshot,
        currentSnapshot,
        targetHasValue,
        originValue ?? null,
      );

      if (shouldUpdateTarget && originValue != null && originValue > 0) {
        this.setTargetValueControlValue(this.roundMoney(originValue * rate));
      }

      this.lastTransferRateSnapshot = currentSnapshot;
    } catch {
      if (requestId !== this.transferRateRequestId) {
        return;
      }

      this.suppressNextCrossCurrencyTargetFromRate = false;
      this.transferRateDisplay.set(null);
      this.transferQuoteError.set('financesPage.transactionsPage.targetValueQuoteUnavailable');
      const keepTargetOnRateFailure = this.mode() === 'edit' && this.initialEntry()?.id != null;
      if (!keepTargetOnRateFailure) {
        this.isUpdatingTargetValueProgrammatically = true;
        this.targetValueControl.reset(undefined, { emitEvent: false });
        this.isUpdatingTargetValueProgrammatically = false;
        this.sameCurrencyTargetUserOverridden = false;
      }
    } finally {
      if (requestId === this.transferRateRequestId) {
        this.transferQuoteLoading.set(false);
        this.form.updateValueAndValidity({ emitEvent: false });
      }
    }
  }

  private syncTransferTargetFromOriginValue() {
    if (this.typeControl.value !== WalletEntryType__Obj.TRANSFER || !this.shouldShowTargetValueField) {
      return;
    }

    const originValue = this.valueControl.value;
    if (originValue == null || originValue <= 0) {
      return;
    }

    if (!this.hasDifferentTransferCurrencies()) {
      if (!this.sameCurrencyTargetUserOverridden) {
        this.setTargetValueControlValue(originValue);
      }
      return;
    }

    const meta = this.transferRateDisplay();
    if (meta == null) {
      return;
    }

    this.setTargetValueControlValue(this.roundMoney(originValue * meta.rate));
  }

  private buildTransferRateSnapshot(): TransferRateSnapshot {
    const o = this.currentOrigin;
    const t = this.currentTarget;
    const d = this.dateControl.value;
    return {
      originId: o?.id,
      targetId: t?.id,
      dateStr: d ? dayjs(d).format(ONLY_DATE_FORMAT) : undefined,
      pairKey: o?.currency != null && t?.currency != null ? `${o.currency}|${t.currency}` : undefined,
    };
  }

  private computeShouldUpdateTargetAfterRateFetch(
    prev: TransferRateSnapshot | null,
    current: TransferRateSnapshot,
    targetHasValue: boolean,
    originValue: number | null,
  ): boolean {
    if (originValue == null || originValue <= 0) {
      return false;
    }

    if (prev?.pairKey == null) {
      return true;
    }

    const onlyDateChanged =
      prev.originId === current.originId &&
      prev.targetId === current.targetId &&
      prev.pairKey === current.pairKey &&
      prev.dateStr !== current.dateStr;

    if (onlyDateChanged && targetHasValue) {
      return false;
    }

    const onlyAccountSwapSamePair =
      prev.pairKey === current.pairKey &&
      prev.dateStr === current.dateStr &&
      (prev.originId !== current.originId || prev.targetId !== current.targetId);

    if (onlyAccountSwapSamePair) {
      return false;
    }

    return true;
  }

  private roundMoney(amount: number): number {
    return Math.round((amount + Number.EPSILON) * 100) / 100;
  }

  private setTargetValueControlValue(value: number | undefined) {
    this.isUpdatingTargetValueProgrammatically = true;
    this.targetValueControl.setValue(value, { emitEvent: false });
    this.isUpdatingTargetValueProgrammatically = false;
  }

  private resetTargetValueControl() {
    this.isUpdatingTargetValueProgrammatically = true;
    this.targetValueControl.reset(undefined, { emitEvent: false });
    this.isUpdatingTargetValueProgrammatically = false;
    this.sameCurrencyTargetUserOverridden = false;
  }

  private syncSameCurrencyTargetOverrideState() {
    const originCurrency = this.currentOrigin?.currency;
    const targetCurrency = this.currentTarget?.currency;

    if (
      this.typeControl.value !== WalletEntryType__Obj.TRANSFER ||
      originCurrency == null ||
      targetCurrency == null ||
      originCurrency !== targetCurrency
    ) {
      this.sameCurrencyTargetUserOverridden = false;
      return;
    }

    const originAmount = this.valueControl.value;
    const targetAmount = this.targetValueControl.value;
    if (originAmount == null || targetAmount == null) {
      this.sameCurrencyTargetUserOverridden = false;
      return;
    }
    this.sameCurrencyTargetUserOverridden = Math.abs(this.roundMoney(originAmount) - this.roundMoney(targetAmount)) > 0.001;
  }
}

interface TransferRateSnapshot {
  originId?: string;
  targetId?: string;
  dateStr?: string;
  pairKey?: string;
}
