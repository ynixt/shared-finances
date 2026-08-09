import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import {
  GroupDebtMovementDto,
  GroupDebtPairBalanceDto,
  GroupDebtPairHistoryDto,
  GroupDebtWorkspaceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { GroupDebtMovementReasonKind__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { EntryDescriptionComponent } from '../../components/wallet-entry-table/components/entry-description/entry-description.component';
import { MONTH_QUERY_PARAM_FORMAT, readDateRangeFromQueryParams, syncDateQueryParams } from '../../services/date-query-params.util';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';
import {
  GroupDebtHistoryGridItem,
  GroupDebtOutstandingBalanceGridItem,
  GroupDebtPairHistoryGridItem,
  mapGroupDebtPairHistoryToGridItems,
  mapOutstandingBalancesToGridItems,
} from './group-debts-page.viewmodel';

interface MemberOption {
  label: string;
  userId: string;
}

@Component({
  selector: 'app-group-debts-page',
  imports: [
    ButtonDirective,
    EntryDescriptionComponent,
    FinancesTitleBarComponent,
    LocalCurrencyPipe,
    LocalDatePipe,
    ProgressSpinner,
    TranslatePipe,
    AdvancedDatePickerComponent,
    ReactiveFormsModule,
    ConfirmDialog,
  ],
  templateUrl: './group-debts-page.component.html',
  providers: [ConfirmationService],
})
@UntilDestroy()
export class GroupDebtsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly groupService = inject(GroupService);
  private readonly groupDebtService = inject(GroupDebtService);
  private readonly messageService = inject(MessageService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly translateService = inject(TranslateService);
  private readonly confirmationService = inject(ConfirmationService);

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly group = signal<GroupWithRoleDto | undefined>(undefined);
  readonly members = signal<GroupUserDto[]>([]);
  readonly workspace = signal<GroupDebtWorkspaceDto | undefined>(undefined);
  readonly pairHistory = signal<GroupDebtPairHistoryDto[]>([]);
  readonly loading = signal(true);
  readonly workspaceLoading = signal(false);
  readonly historyLoading = signal(false);
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);
  readonly canMutate = computed(() => this.group()?.permissions?.includes('SEND_ENTRIES') === true);
  readonly outstandingBalanceGridItems = computed<GroupDebtOutstandingBalanceGridItem[]>(() =>
    mapOutstandingBalancesToGridItems(this.workspace()?.balances ?? []),
  );
  readonly pairHistoryGridItems = computed<GroupDebtPairHistoryGridItem[]>(() =>
    mapGroupDebtPairHistoryToGridItems(this.pairHistory(), {
      resolveMemberName: userId => this.memberName(userId),
      resolveLineName: (movement, linkedWalletEventName) => this.historyLineName(movement, linkedWalletEventName),
      resolveTransactionDate: movement => this.transactionDate(movement),
    }),
  );

  readonly memberOptions = computed<MemberOption[]>(() =>
    this.members().map(member => ({
      userId: member.user.id,
      label: `${member.user.firstName} ${member.user.lastName}`.trim(),
    })),
  );

  constructor() {
    if (this.groupId) {
      const defaultRange = this.getDefaultDateRange();
      const initialDateRange = readDateRangeFromQueryParams(this.route.snapshot.queryParamMap, 'day_only') ?? defaultRange;
      this.dateControl.setValue(initialDateRange, { emitEvent: false });
      void this.reload();
    } else {
      this.loading.set(false);
    }

    this.dateControl.valueChanges.pipe(untilDestroyed(this)).subscribe(date => this.onSelectedMonthChange(date ?? undefined, true));
  }

  async reload() {
    this.loading.set(true);

    try {
      const [group, members] = await Promise.all([
        this.groupService.getGroup(this.groupId),
        this.groupService.findAllMembers(this.groupId),
      ]);

      this.group.set(group);
      this.members.set(members);
      await this.reloadVisibleData();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.loading.set(false);
    }
  }

  async onSelectedMonthChange(value: DateRange | undefined, syncUrl: boolean) {
    if (value == null) {
      return;
    }

    if (syncUrl) {
      void syncDateQueryParams(this.route, this.router, value, 'day_only');
    }

    try {
      await this.reloadVisibleData();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    }
  }

  memberName(userId: string): string {
    return (
      this.memberOptions().find(option => option.userId === userId)?.label ??
      this.translateService.instant('financesPage.groupsPage.unknownParticipant')
    );
  }

  pairLabel(pair: GroupDebtPairBalanceDto): string {
    return `${this.memberName(pair.payerId)} -> ${this.memberName(pair.receiverId)}`;
  }

  reasonLabel(reason: string): string {
    return `financesPage.groupsPage.debtsPage.reason.${reason}`;
  }

  sourceReferenceLabel(movement: GroupDebtMovementDto): string {
    if (movement.carriedOver) {
      return this.translateService.instant('financesPage.groupsPage.debtsPage.carryoverSource');
    }

    if (movement.projected) {
      return this.translateService.instant('financesPage.groupsPage.debtsPage.projectedSource');
    }

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

  historyBlockTrack(item: GroupDebtPairHistoryGridItem): string {
    return item.id;
  }

  historyLineTrack(item: GroupDebtHistoryGridItem): string {
    return item.movement.id;
  }

  outstandingBalanceTrack(item: GroupDebtOutstandingBalanceGridItem): string {
    return item.id;
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

  openSettleValuePage(movement: GroupDebtMovementDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'settlements', 'new'], {
      queryParams: {
        payerId: movement.payerId,
        receiverId: movement.receiverId,
        amount: Math.abs(movement.deltaSigned),
      },
    });
  }

  openEditSettlementPage(movement: GroupDebtMovementDto) {
    if (!this.canEditSettlement(movement)) {
      return;
    }

    void this.router.navigate(['/app/transactions/edit', movement.sourceWalletEventId], {
      queryParams: {
        returnTo: this.router.url,
      },
    });
  }

  openAdjustmentPage(movement: GroupDebtMovementDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'adjustments', movement.id]);
  }

  openCreateAdjustmentPage(pair: GroupDebtPairBalanceDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'adjustments', 'new'], {
      queryParams: {
        payerId: pair.payerId,
        receiverId: pair.receiverId,
        month: this.selectedMonth(),
        currency: pair.currency,
      },
    });
  }

  isAdjustableMovement(movement: GroupDebtMovementDto): boolean {
    return (
      !movement.projected &&
      !movement.carriedOver &&
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT_COMPENSATION &&
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT &&
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT_REVERSAL
    );
  }

  adjustmentActionLabel(movement: GroupDebtMovementDto): string {
    return movement.reasonKind === GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT
      ? 'financesPage.groupsPage.debtsPage.editAdjustmentAction'
      : 'financesPage.groupsPage.debtsPage.addAdjustmentAction';
  }

  isSettleValueMovement(movement: GroupDebtMovementDto): boolean {
    return (
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT &&
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT_REVERSAL
    );
  }

  canDeleteManualAdjustment(movement: GroupDebtMovementDto): boolean {
    return !movement.projected && !movement.carriedOver && movement.reasonKind === GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT;
  }

  canDeleteSettlement(movement: GroupDebtMovementDto): boolean {
    return (
      !movement.projected &&
      !movement.carriedOver &&
      movement.reasonKind === GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT &&
      movement.sourceWalletEventId != null
    );
  }

  canEditSettlement(movement: GroupDebtMovementDto): boolean {
    return (
      !movement.projected &&
      !movement.carriedOver &&
      movement.reasonKind === GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT &&
      movement.sourceWalletEventId != null
    );
  }

  monthDate(month: string): Date {
    return this.yearMonthToDate(month);
  }

  transactionDate(movement: GroupDebtMovementDto): string | null | undefined {
    return movement.transactionDate ?? movement.sourceWalletEvent?.date;
  }

  movementStateLabel(movement: GroupDebtMovementDto): string {
    return movement.projected ? 'financesPage.groupsPage.debtsPage.projectedState' : 'financesPage.groupsPage.debtsPage.executedState';
  }

  historyLineName(movement: GroupDebtMovementDto, linkedWalletEventName: string | undefined): string {
    const normalizedNote = movement.note?.trim();
    if (linkedWalletEventName != null) {
      return linkedWalletEventName;
    }
    if (normalizedNote != null && normalizedNote.length > 0) {
      return normalizedNote;
    }

    return `${this.memberName(movement.payerId)} -> ${this.memberName(movement.receiverId)}`;
  }

  selectedMonth(): string {
    return this.dateControl.value?.startDate?.format(MONTH_QUERY_PARAM_FORMAT) ?? dayjs().format(MONTH_QUERY_PARAM_FORMAT);
  }

  confirmDeleteManualAdjustment(movement: GroupDebtMovementDto) {
    if (!this.canMutate() || !this.canDeleteManualAdjustment(movement)) {
      return;
    }

    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.groupsPage.debtsPage.deleteAdjustmentConfirm'),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        void this.deleteManualAdjustment(movement.id);
      },
    });
  }

  confirmDeleteSettlement(movement: GroupDebtMovementDto) {
    if (!this.canMutate() || !this.canDeleteSettlement(movement)) {
      return;
    }

    const settlementSummary = this.settlementDeletionSummary(movement);

    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.groupsPage.debtsPage.deleteSettlementConfirm', {
        totalAmount: settlementSummary.totalAmountFormatted,
        fragmentCount: settlementSummary.fragmentCount,
      }),
      header: this.translateService.instant('general.confirmation'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('general.delete'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        void this.deleteSettlement(movement.id);
      },
    });
  }

  private async reloadVisibleData() {
    if (!this.groupId) {
      return;
    }

    this.workspaceLoading.set(true);
    this.historyLoading.set(true);

    try {
      const selectedMonth = this.selectedMonth();
      const [workspace, pairHistory] = await Promise.all([
        this.groupDebtService.getWorkspace(this.groupId, selectedMonth),
        this.groupDebtService.listPairHistory(this.groupId, { selectedMonth }),
      ]);

      this.workspace.set(workspace);
      this.pairHistory.set(pairHistory);
    } finally {
      this.workspaceLoading.set(false);
      this.historyLoading.set(false);
    }
  }

  private yearMonthToDate(month: string): Date {
    return dayjs(`${month}-01`).toDate();
  }

  private getDefaultDateRange(): DateRange {
    const currentDate = dayjs().startOf('month');

    return {
      startDate: currentDate,
      endDate: currentDate,
      sameMonth: true,
    };
  }

  private async deleteManualAdjustment(movementId: string) {
    try {
      await this.groupDebtService.deleteAdjustment(this.groupId, movementId);
      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.debtsPage.adjustmentDeleted'),
        life: DEFAULT_SUCCESS_LIFE,
      });
      await this.reloadVisibleData();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    }
  }

  private async deleteSettlement(movementId: string) {
    try {
      await this.groupDebtService.deleteSettlement(this.groupId, movementId);
      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.debtsPage.settlementDeleted'),
        life: DEFAULT_SUCCESS_LIFE,
      });
      await this.reloadVisibleData();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
    }
  }

  private settlementDeletionSummary(movement: GroupDebtMovementDto): { fragmentCount: number; totalAmountFormatted: string } {
    const relatedMovements =
      movement.sourceWalletEventId == null
        ? []
        : this.pairHistory()
            .flatMap(pair => pair.lines)
            .filter(
              line =>
                line.reasonKind === GroupDebtMovementReasonKind__Obj.DEBT_SETTLEMENT &&
                line.sourceWalletEventId === movement.sourceWalletEventId,
            );

    const fragmentCount = Math.max(relatedMovements.length, 1);
    const totalAmount = (relatedMovements.length > 0 ? relatedMovements : [movement]).reduce(
      (acc, line) => acc + Math.abs(line.deltaSigned),
      0,
    );

    return {
      fragmentCount,
      totalAmountFormatted: new Intl.NumberFormat(undefined, { style: 'currency', currency: movement.currency }).format(totalAmount),
    };
  }
}
