import { Component, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';

import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import {
  WalletItemPickerComponent,
  WalletItemSearchResponseDtoWithIcon,
} from '../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { FinancialGoalService } from '../services/financial-goal.service';

@Component({
  selector: 'app-financial-goal-reverse-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    InputNumber,
    InputText,
    WalletItemPickerComponent,
  ],
  templateUrl: './financial-goal-reverse-page.component.html',
})
export class FinancialGoalReversePageComponent {
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
    return ['/app/goals', this.goalId];
  }

  private async bootstrap() {
    try {
      const d = await this.financialGoalService.getGoal(this.goalId);
      this.goalName.set(d.goal.name);
      this.groupId.set(d.goal.groupId ?? null);
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
    this.loading = true;
    try {
      await this.financialGoalService.reverse(this.goalId, {
        walletItemId: wallet.id,
        amount: v.amount!,
        note: v.note || undefined,
      });
      await this.router.navigate(this.closeRouterLink);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}
