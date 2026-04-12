import { Component, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';

import { DatePickerComponent } from '../../../components/date-picker/date-picker.component';
import { RequiredFieldAsteriskComponent } from '../../../components/required-field-asterisk/required-field-asterisk.component';
import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import {
  WalletItemPickerComponent,
  WalletItemSearchResponseDtoWithIcon,
} from '../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

function toLocalIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

@Component({
  selector: 'app-financial-goal-allocate-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    InputNumber,
    WalletItemPickerComponent,
    DatePickerComponent,
    InputText,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './financial-goal-allocate-page.component.html',
})
export class FinancialGoalAllocatePageComponent {
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  readonly groupId = signal<string | null>(null);
  readonly selectedGroupForPicker = computed((): GroupDto | undefined => {
    const gid = this.groupId();
    return gid != null && gid !== '' ? { id: gid, name: '' } : undefined;
  });
  readonly goalName = signal('');
  initialLoading = true;
  loading = false;
  goalId = '';

  readonly form: ReturnType<FormBuilder['group']>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private financialGoalService: FinancialGoalService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
  ) {
    this.form = this.fb.group({
      walletItem: [null as WalletItemSearchResponseDtoWithIcon | null, Validators.required],
      amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
      allocationDate: [new Date() as Date | null, Validators.required],
      note: [''],
    });
    this.goalId = this.route.snapshot.paramMap.get('id') ?? '';
    void this.bootstrap();
  }

  amountCurrency(): string {
    const w = this.form.get('walletItem')?.value as WalletItemSearchResponseDtoWithIcon | null | undefined;
    return w?.currency ?? 'BRL';
  }

  get closeRouterLink(): string[] {
    return [...this.workspace.goalsRoot, this.goalId];
  }

  private async bootstrap() {
    try {
      const d = await this.financialGoalService.getGoal(this.goalId);
      this.goalName.set(d.goal.name);
      this.groupId.set(d.goal.groupId ?? this.workspace.groupId ?? null);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.initialLoading = false;
    }
  }

  async save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const wallet = v.walletItem as WalletItemSearchResponseDtoWithIcon;
    const allocDate = v.allocationDate as Date | null;
    if (!(allocDate instanceof Date) || Number.isNaN(allocDate.getTime())) {
      this.form.get('allocationDate')?.markAsTouched();
      return;
    }
    this.loading = true;
    try {
      const noteTrimmed = typeof v.note === 'string' ? v.note.trim() : '';
      await this.financialGoalService.allocate(this.goalId, {
        walletItemId: wallet.id,
        amount: v.amount!,
        allocationDate: toLocalIsoDate(allocDate),
        ...(noteTrimmed !== '' ? { note: noteTrimmed } : {}),
      });
      await this.router.navigate(this.closeRouterLink);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}
