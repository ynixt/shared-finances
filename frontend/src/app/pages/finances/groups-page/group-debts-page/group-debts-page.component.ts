import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';

import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import {
  GroupDebtMovementDto,
  GroupDebtPairBalanceDto,
  GroupDebtWorkspaceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { GroupDebtMovementReasonKind__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';

interface MemberOption {
  label: string;
  userId: string;
}

@Component({
  selector: 'app-group-debts-page',
  imports: [ButtonDirective, FinancesTitleBarComponent, LocalCurrencyPipe, LocalDatePipe, ProgressSpinner, TableModule, TranslatePipe],
  templateUrl: './group-debts-page.component.html',
})
export class GroupDebtsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly groupService = inject(GroupService);
  private readonly groupDebtService = inject(GroupDebtService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly translateService = inject(TranslateService);

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly group = signal<GroupWithRoleDto | undefined>(undefined);
  readonly members = signal<GroupUserDto[]>([]);
  readonly workspace = signal<GroupDebtWorkspaceDto | undefined>(undefined);
  readonly history = signal<GroupDebtMovementDto[]>([]);
  readonly loading = signal(true);
  readonly canMutate = computed(() => this.group()?.permissions?.includes('SEND_ENTRIES') === true);

  readonly memberOptions = computed<MemberOption[]>(() =>
    this.members().map(member => ({
      userId: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    })),
  );

  constructor() {
    if (this.groupId) {
      void this.reload();
    } else {
      this.loading.set(false);
    }
  }

  async reload() {
    this.loading.set(true);

    try {
      const [group, members, workspace, history] = await Promise.all([
        this.groupService.getGroup(this.groupId),
        this.groupService.findAllMembers(this.groupId),
        this.groupDebtService.getWorkspace(this.groupId),
        this.groupDebtService.listHistory(this.groupId),
      ]);

      this.group.set(group);
      this.members.set(members);
      this.workspace.set(workspace);
      this.history.set(history);
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.loading.set(false);
    }
  }

  memberName(userId: string): string {
    return this.memberOptions().find(option => option.userId === userId)?.label ?? userId;
  }

  pairLabel(pair: GroupDebtPairBalanceDto): string {
    return `${this.memberName(pair.payerId)} -> ${this.memberName(pair.receiverId)}`;
  }

  reasonLabel(reason: string): string {
    return `financesPage.groupsPage.debtsPage.reason.${reason}`;
  }

  sourceReferenceLabel(movement: GroupDebtMovementDto): string {
    if (movement.sourceWalletEventId) {
      return this.translateService.instant('financesPage.groupsPage.debtsPage.sourceWalletEvent', {
        id: movement.sourceWalletEventId.slice(0, 8),
      });
    }

    if (movement.sourceMovementId) {
      return this.translateService.instant('financesPage.groupsPage.debtsPage.sourceMovement', {
        id: movement.sourceMovementId.slice(0, 8),
      });
    }

    return this.translateService.instant('financesPage.groupsPage.debtsPage.noSource');
  }

  openSettlementPage(pair: GroupDebtPairBalanceDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'settlements', 'new'], {
      queryParams: {
        payerId: pair.payerId,
        receiverId: pair.receiverId,
        amount: Math.abs(pair.outstandingAmount),
      },
    });
  }

  openAdjustmentPage(movement: GroupDebtMovementDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'adjustments', movement.id]);
  }

  isAdjustableMovement(movement: GroupDebtMovementDto): boolean {
    return movement.reasonKind !== GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT_COMPENSATION;
  }

  monthDate(month: string): Date {
    return this.yearMonthToDate(month);
  }

  private yearMonthToDate(month: string): Date {
    return dayjs(`${month}-01`).toDate();
  }
}
