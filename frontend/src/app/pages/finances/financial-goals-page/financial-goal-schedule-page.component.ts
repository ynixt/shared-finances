import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { DatePickerComponent } from '../../../components/date-picker/date-picker.component';
import { RequiredFieldAsteriskComponent } from '../../../components/required-field-asterisk/required-field-asterisk.component';
import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { RecurrenceType__Options } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums/recurrence-type';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import {
  WalletItemPickerComponent,
  WalletItemSearchResponseDtoWithIcon,
} from '../components/item-picker/wallet-item-picker/wallet-item-picker.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

@UntilDestroy()
@Component({
  selector: 'app-financial-goal-schedule-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    InputNumber,
    Select,
    ToggleSwitch,
    WalletItemPickerComponent,
    DatePickerComponent,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './financial-goal-schedule-page.component.html',
})
export class FinancialGoalSchedulePageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  private readonly recurrenceI18nTick = signal(0);
  readonly recurrenceOptions = computed(() => {
    this.recurrenceI18nTick();
    return RecurrenceType__Options.map(v => ({
      value: v,
      label: this.translateService.instant(`financesPage.goalsPage.recurrence.${v}`),
    }));
  });
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
    private router: Router,
    private fb: FormBuilder,
    private financialGoalService: FinancialGoalService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private translateService: TranslateService,
  ) {
    this.translateService.onLangChange.pipe(untilDestroyed(this)).subscribe(() => this.recurrenceI18nTick.update(n => n + 1));
    this.form = this.fb.group({
      walletItem: [null as WalletItemSearchResponseDtoWithIcon | null, Validators.required],
      amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
      periodicity: [RecurrenceType__Options[0], Validators.required],
      firstExecution: [null as Date | null, Validators.required],
      qtyLimit: [null as number | null],
      removesAllocation: [false],
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
      this.groupId.set(this.workspace.groupId ?? d.goal.groupId ?? null);
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
    const first = v.firstExecution as Date | null;
    const firstExecution = first instanceof Date && !Number.isNaN(first.getTime()) ? first.toISOString().slice(0, 10) : '';
    if (!firstExecution) {
      this.form.get('firstExecution')?.markAsTouched();
      return;
    }
    this.loading = true;
    try {
      await this.financialGoalService.createSchedule(this.goalId, {
        walletItemId: wallet.id,
        amount: v.amount!,
        periodicity: v.periodicity!,
        firstExecution,
        qtyLimit: v.qtyLimit ?? undefined,
        removesAllocation: !!v.removesAllocation,
      });
      await this.router.navigate(this.closeRouterLink);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}
