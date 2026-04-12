import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';

import { DatePickerComponent } from '../../../components/date-picker/date-picker.component';
import { GoalLedgerMovementDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/goals/goal-ledger-movement-dto';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { FinancialGoalService } from '../services/financial-goal.service';
import { resolveGoalWorkspaceContext } from './goal-workspace-context';

function toLocalIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** API sends `YYYY-MM-DD` (no time); build a local calendar date. */
function parseMovementDateToLocalDate(iso: string | undefined | null): Date {
  if (iso == null || iso === '') {
    return new Date();
  }
  const parts = iso.split('-').map(Number);
  const y = parts[0];
  const mo = parts[1];
  const d = parts[2];
  if (y == null || mo == null || d == null || Number.isNaN(y) || Number.isNaN(mo) || Number.isNaN(d)) {
    return new Date();
  }
  return new Date(y, mo - 1, d);
}

@Component({
  selector: 'app-financial-goal-edit-ledger-movement-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ReactiveFormsModule,
    ButtonDirective,
    InputNumber,
    InputText,
    DatePickerComponent,
    ConfirmDialog,
  ],
  templateUrl: './financial-goal-edit-ledger-movement-page.component.html',
})
export class FinancialGoalEditLedgerMovementPageComponent {
  private readonly route = inject(ActivatedRoute);
  readonly workspace = resolveGoalWorkspaceContext(this.route);
  readonly movement = signal<GoalLedgerMovementDto | undefined>(undefined);
  readonly goalName = signal('');
  initialLoading = true;
  loading = false;
  deleting = false;
  goalId = '';
  movementId = '';

  readonly form: ReturnType<FormBuilder['group']>;

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private financialGoalService: FinancialGoalService,
    private messageService: MessageService,
    private errorMessageService: ErrorMessageService,
    private confirmationService: ConfirmationService,
    private translateService: TranslateService,
  ) {
    this.form = this.fb.group({
      newSignedAmount: [null as number | null, Validators.required],
      allocationDate: [null as Date | null, Validators.required],
      note: [''],
    });
    this.goalId = this.route.snapshot.paramMap.get('id') ?? '';
    this.movementId = this.route.snapshot.paramMap.get('movementId') ?? '';
    void this.bootstrap();
  }

  movementCurrency(): string {
    return this.movement()?.currency ?? 'BRL';
  }

  get closeRouterLink(): string[] {
    return [...this.workspace.goalsRoot, this.goalId];
  }

  private async bootstrap() {
    try {
      const [detail, mov] = await Promise.all([
        this.financialGoalService.getGoal(this.goalId),
        this.financialGoalService.getLedgerMovement(this.goalId, this.movementId),
      ]);
      this.goalName.set(detail.goal.name);
      this.movement.set(mov);
      const allocationDate = parseMovementDateToLocalDate(mov.movementDate);
      this.form.patchValue({ newSignedAmount: mov.signedAmount, allocationDate, note: mov.note ?? '' });
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.initialLoading = false;
    }
  }

  confirmDelete() {
    if (this.deleting) return;
    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.goalsPage.deleteMovementConfirm'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary' },
      accept: () => void this.deleteMovement(),
    });
  }

  private async deleteMovement() {
    this.deleting = true;
    try {
      await this.financialGoalService.deleteMovement(this.goalId, this.movementId);
      await this.router.navigate(this.closeRouterLink);
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
    const allocDate = this.form.value.allocationDate as Date | null;
    if (!(allocDate instanceof Date) || Number.isNaN(allocDate.getTime())) {
      this.form.get('allocationDate')?.markAsTouched();
      return;
    }
    this.loading = true;
    try {
      const noteRaw = (this.form.value.note as string | null | undefined) ?? '';
      await this.financialGoalService.editMovement(this.goalId, this.movementId, {
        newSignedAmount: this.form.value.newSignedAmount!,
        allocationDate: toLocalIsoDate(allocDate),
        note: noteRaw.trim(),
      });
      await this.router.navigate(this.closeRouterLink);
    } catch (e) {
      this.errorMessageService.handleError(e, this.messageService);
    } finally {
      this.loading = false;
    }
  }
}
