import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupDebtMovementDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { GroupDebtMovementReasonKind__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';
import { manualAdjustmentNetAmountForRoot } from '../group-debts-page/group-debts-page.helpers';

function nonZeroNumberValidator(control: AbstractControl): ValidationErrors | null {
  if (control.value == null) {
    return null;
  }

  const value = Number(control.value);
  if (!Number.isFinite(value) || value === 0) {
    return { nonZero: true };
  }

  return null;
}

interface MemberOption {
  label: string;
  userId: string;
}

@Component({
  selector: 'app-group-debt-adjustment-page',
  imports: [
    ButtonDirective,
    FinancesTitleBarComponent,
    InputNumber,
    InputText,
    LocalDatePipe,
    ProgressSpinner,
    ReactiveFormsModule,
    TranslatePipe,
  ],
  templateUrl: './group-debt-adjustment-page.component.html',
})
export class GroupDebtAdjustmentPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly groupService = inject(GroupService);
  private readonly groupDebtService = inject(GroupDebtService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly translateService = inject(TranslateService);

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly debtId = this.route.snapshot.paramMap.get('debtId') ?? '';

  readonly group = signal<GroupWithRoleDto | undefined>(undefined);
  readonly members = signal<GroupUserDto[]>([]);
  readonly debtMovement = signal<GroupDebtMovementDto | undefined>(undefined);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly mode = signal<'create' | 'edit'>('create');
  readonly canMutate = computed(() => this.group()?.permissions?.includes('SEND_ENTRIES') === true);

  readonly memberOptions = computed<MemberOption[]>(() =>
    this.members().map(member => ({
      userId: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    })),
  );

  readonly adjustmentForm = this.formBuilder.group({
    amountDelta: [undefined as number | undefined, [Validators.required, nonZeroNumberValidator]],
    note: [''],
  });

  constructor() {
    if (this.groupId && this.debtId) {
      void this.load();
    } else {
      this.loading.set(false);
    }
  }

  memberName(userId: string): string {
    return this.memberOptions().find(option => option.userId === userId)?.label ?? userId;
  }

  modeIsEdit(): boolean {
    return this.mode() === 'edit';
  }

  monthDate(month: string): Date {
    return dayjs(`${month}-01`).toDate();
  }

  async submit() {
    if (this.adjustmentForm.invalid || this.saving() || !this.canMutate()) {
      return;
    }

    const movement = this.debtMovement();
    if (movement == null) {
      return;
    }

    this.saving.set(true);
    const raw = this.adjustmentForm.getRawValue();

    try {
      if (this.mode() === 'edit') {
        await this.groupDebtService.editAdjustment(this.groupId, movement.id, {
          amountDelta: raw.amountDelta!,
          note: raw.note || null,
        });
      } else {
        await this.groupDebtService.createAdjustment(this.groupId, {
          payerId: movement.payerId,
          receiverId: movement.receiverId,
          month: movement.month,
          currency: movement.currency,
          amountDelta: raw.amountDelta!,
          note: raw.note || null,
        });
      }

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.debtsPage.adjustmentSaved'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['/app/groups', this.groupId, 'debts']);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.saving.set(false);
    }
  }

  private async load() {
    this.loading.set(true);

    try {
      const [group, members, movement, history] = await Promise.all([
        this.groupService.getGroup(this.groupId),
        this.groupService.findAllMembers(this.groupId),
        this.groupDebtService.getMovement(this.groupId, this.debtId),
        this.groupDebtService.listHistory(this.groupId),
      ]);

      this.group.set(group);
      this.members.set(members);
      this.debtMovement.set(movement);

      if (movement.reasonKind === GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT) {
        this.mode.set('edit');
        this.adjustmentForm.patchValue({
          amountDelta: manualAdjustmentNetAmountForRoot(history, movement.id),
          note: movement.note ?? '',
        });
      } else {
        this.mode.set('create');
        this.adjustmentForm.patchValue({
          amountDelta: Math.abs(movement.deltaSigned),
          note: '',
        });
      }
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.loading.set(false);
    }
  }
}
