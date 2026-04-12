import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';

import { CurrencySelectorComponent } from '../../../components/currency-selector/currency-selector.component';
import { DatePickerComponent } from '../../../components/date-picker/date-picker.component';
import { RequiredFieldAsteriskComponent } from '../../../components/required-field-asterisk/required-field-asterisk.component';
import { FinancialGoalTargetDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/goals';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

@Component({
  selector: 'app-financial-goal-upsert-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    InputText,
    ButtonDirective,
    DatePickerComponent,
    RequiredFieldAsteriskComponent,
    InputNumber,
    CurrencySelectorComponent,
    ConfirmDialog,
  ],
  templateUrl: './financial-goal-upsert-page.component.html',
})
export class FinancialGoalUpsertPageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly form: FormGroup;
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  goalId: string | undefined;
  loading = false;
  deleting = false;
  initialLoading = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private financialGoalService: FinancialGoalService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private confirmationService: ConfirmationService,
    private translateService: TranslateService,
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      deadline: [null as Date | null],
      targets: this.fb.array([this.targetRow()]),
    });
    void this.bootstrap();
  }

  get targets(): FormArray {
    return this.form.get('targets') as FormArray;
  }

  private targetRow(): FormGroup {
    return this.fb.group({
      currency: ['BRL', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
      targetAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    });
  }

  addTarget() {
    this.targets.push(this.targetRow());
  }

  removeTarget(i: number) {
    if (this.targets.length > 1) {
      this.targets.removeAt(i);
    }
  }

  get closeRouterLink(): string[] {
    if (this.goalId != null) {
      return [...this.workspace.goalsRoot, this.goalId];
    }
    return this.workspace.goalsRoot;
  }

  private async bootstrap() {
    this.goalId = this.route.snapshot.paramMap.get('id') ?? undefined;
    if (this.goalId != null) {
      this.initialLoading = true;
      try {
        const detail = await this.financialGoalService.getGoal(this.goalId);
        this.form.patchValue({
          name: detail.goal.name,
          description: detail.goal.description ?? '',
          deadline: detail.goal.deadline ? new Date(`${detail.goal.deadline}T12:00:00`) : null,
        });
        this.targets.clear();
        for (const t of detail.targets) {
          this.targets.push(
            this.fb.group({
              currency: [t.currency, [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
              targetAmount: [t.targetAmount, [Validators.required, Validators.min(0.01)]],
            }),
          );
        }
        if (this.targets.length === 0) {
          this.targets.push(this.targetRow());
        }
      } catch (e) {
        this.errorMessageService.handleError(e, this.messageService);
      } finally {
        this.initialLoading = false;
      }
    }
  }

  confirmDelete() {
    if (this.goalId == null || this.deleting) return;
    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.goalsPage.deleteGoalConfirm'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary' },
      accept: () => void this.deleteGoal(),
    });
  }

  private async deleteGoal() {
    if (this.goalId == null) return;
    this.deleting = true;
    try {
      await this.financialGoalService.deleteGoal(this.goalId);
      await this.router.navigate(this.workspace.goalsRoot);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.deleting = false;
    }
  }

  async save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const deadlineRaw = v.deadline as Date | null | undefined;
    const deadline: string | undefined =
      deadlineRaw instanceof Date && !Number.isNaN(deadlineRaw.getTime()) ? deadlineRaw.toISOString().slice(0, 10) : undefined;
    const targetDtos: FinancialGoalTargetDto[] = (v.targets as { currency: string; targetAmount: number }[]).map(t => ({
      currency: String(t.currency).toUpperCase(),
      targetAmount: t.targetAmount,
    }));

    this.loading = true;
    try {
      if (this.goalId == null) {
        const body = {
          name: v.name,
          description: v.description || undefined,
          deadline,
          targets: targetDtos,
        };
        const created =
          this.workspace.scope === 'group' && this.workspace.groupId != null
            ? await this.financialGoalService.createGroupGoal(this.workspace.groupId, body)
            : await this.financialGoalService.createIndividualGoal(body);
        await this.router.navigate([...this.workspace.goalsRoot, created.id]);
      } else {
        await this.financialGoalService.updateGoal(this.goalId, {
          name: v.name,
          description: v.description || undefined,
          deadline,
          targets: targetDtos,
        });
        await this.router.navigate([...this.workspace.goalsRoot, this.goalId]);
      }
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}
