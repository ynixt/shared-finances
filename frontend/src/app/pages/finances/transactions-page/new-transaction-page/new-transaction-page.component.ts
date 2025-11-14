import { Component, Signal, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { DatePickerComponent } from '../../../../components/date-picker/date-picker.component';
import { PagedSelectComponent } from '../../../../components/paged-select/paged-select.component';
import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { WalletItemSearchResponseDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet';
import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
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

type NewTransactionForm = FormGroup<{
  type: FormControl<TransactionType | undefined>;
  group: FormControl<GroupWithRoleDto | undefined>;
  origin: FormControl<WalletItemSearchResponseDto | undefined>;
  target: FormControl<WalletItemSearchResponseDto | undefined>;
  name: FormControl<string | undefined>;
  category: FormControl<CategoryDto | undefined>;
  date: FormControl<Date | undefined>;
  value: FormControl<string | undefined>;
  confirmed: FormControl<boolean | undefined>;
  observations: FormControl<string | undefined>;
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
  ],
  templateUrl: './new-transaction-page.component.html',
  styleUrl: './new-transaction-page.component.scss',
})
@UntilDestroy()
export class NewTransactionPageComponent {
  readonly TransactionType = TransactionType;

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
      },
      { validators: this.requireTargetOnTransfer },
    ) as NewTransactionForm;

    this.groupControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.form.get('origin')?.reset();
      this.form.get('target')?.reset();
    });

    this.typeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      this.form.get('target')?.reset();
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

  async loadGroups(page = 0, query: string | undefined): Promise<GroupWithRoleDto[]> {
    return await this.groupService.getAllGroups();
  }

  submit() {
    throw new Error('Method not implemented.');
  }

  private requireTargetOnTransfer = (group: AbstractControl): ValidationErrors | null => {
    const fg = group as FormGroup;
    const type = fg.get('type')?.value;
    const target = fg.get('target');

    if (!target) return null;

    const isTransfer = type === TransactionType.TRANSFER;
    const hasValue = target.value !== undefined && target.value !== null && target.value !== '';

    if (isTransfer && !hasValue) {
      const nextErrors = { ...(target.errors ?? {}), required: true };
      target.setErrors(nextErrors);
    } else if (!isTransfer && target.errors) {
      const { required, ...rest } = target.errors;
      target.setErrors(Object.keys(rest).length ? rest : null);
    } else if (isTransfer && hasValue && target.errors) {
      const { required, ...rest } = target.errors;
      target.setErrors(Object.keys(rest).length ? rest : null);
    }

    return null;
  };
}
