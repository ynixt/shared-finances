import { Component, computed, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { Subscription } from 'rxjs';

import { ButtonDirective } from 'primeng/button';

import { PagedSelectComponent } from '../../../../components/paged-select/paged-select.component';
import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { WalletItemPickerComponent } from '../../components/wallet-item-picker/wallet-item-picker.component';
import { GroupService } from '../../services/group.service';

enum TransactionType {
  REVENUE,
  EXPENSE,
  TRANSFER,
}

@Component({
  selector: 'app-new-transaction-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ButtonDirective,
    ReactiveFormsModule,
    PagedSelectComponent,
    WalletItemPickerComponent,
  ],
  templateUrl: './new-transaction-page.component.html',
  styleUrl: './new-transaction-page.component.scss',
})
@UntilDestroy()
export class NewTransactionPageComponent {
  readonly TransactionType = TransactionType;

  get selectedGroup() {
    return this.form().get('group')!!.value;
  }

  get groupControl() {
    return this.form().get('group')!!;
  }

  get typeControl() {
    return this.form().get('type')!!;
  }

  constructor() {
    const formGroup = this.form();

    this.groupControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      formGroup.get('origin')?.reset();
      formGroup.get('target')?.reset();
    });

    this.typeControl.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      formGroup.get('target')?.reset();
    });
  }

  submit() {
    throw new Error('Method not implemented.');
  }

  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);

  transactionTypeOptions: any[] = [
    { label: 'Receita', value: TransactionType.REVENUE, styleClass: 'rounded-r-none', severity: 'success' },
    { label: 'Despesa', value: TransactionType.EXPENSE, styleClass: 'rounded-none', severity: 'danger' },
    { label: 'Transferência', value: TransactionType.TRANSFER, styleClass: 'rounded-l-none', severity: 'info' },
  ];

  readonly form = computed<FormGroup>(() =>
    this.formBuilder.group(
      {
        type: [TransactionType.REVENUE, [Validators.required]],
        group: [undefined, []],
        origin: [undefined, [Validators.required]],
        target: [undefined],
      },
      { validators: this.requireTargetOnTransfer },
    ),
  );

  async loadGroups(page = 0, query: string | undefined): Promise<GroupWithRoleDto[]> {
    return await this.groupService.getAllGroups();
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
