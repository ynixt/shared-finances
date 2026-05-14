import { NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { DataView } from 'primeng/dataview';
import { Dialog } from 'primeng/dialog';
import { ProgressSpinner } from 'primeng/progressspinner';

import { GroupUserDto, GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import {
  GroupDebtMonthlyDrilldownDto,
  GroupDebtMovementDto,
  GroupDebtPairBalanceDto,
  GroupDebtWorkspaceDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/debts';
import { GroupDebtMovementReasonKind__Obj } from '../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { EntryDescriptionComponent } from '../../components/wallet-entry-table/components/entry-description/entry-description.component';
import { MONTH_QUERY_PARAM_FORMAT_V2, readDateRangeFromQueryParams, syncDateQueryParams } from '../../services/date-query-params.util';
import { GroupDebtService } from '../../services/group-debt.service';
import { GroupService } from '../../services/group.service';
import {
  GroupDebtHistoryGridItem,
  GroupDebtOutstandingBalanceGridItem,
  mapGroupDebtHistoryToGridItems,
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
    DataView,
    Dialog,
    EntryDescriptionComponent,
    FinancesTitleBarComponent,
    LocalCurrencyPipe,
    LocalDatePipe,
    NgClass,
    ProgressSpinner,
    TranslatePipe,
    AdvancedDatePickerComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './group-debts-page.component.html',
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

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly group = signal<GroupWithRoleDto | undefined>(undefined);
  readonly members = signal<GroupUserDto[]>([]);
  readonly workspace = signal<GroupDebtWorkspaceDto | undefined>(undefined);
  readonly history = signal<GroupDebtMovementDto[]>([]);
  readonly loading = signal(true);
  readonly workspaceLoading = signal(false);
  readonly historyLoading = signal(false);
  readonly drilldownVisible = signal(false);
  readonly drilldownLoading = signal(false);
  readonly monthlyDrilldown = signal<GroupDebtMonthlyDrilldownDto | undefined>(undefined);
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);
  readonly canMutate = computed(() => this.group()?.permissions?.includes('SEND_ENTRIES') === true);
  readonly outstandingBalanceGridItems = computed<GroupDebtOutstandingBalanceGridItem[]>(() =>
    mapOutstandingBalancesToGridItems(this.workspace()?.balances ?? []),
  );
  readonly historyGridItems = computed<GroupDebtHistoryGridItem[]>(() => mapGroupDebtHistoryToGridItems(this.history()));

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
      this.closeMonthlyDrilldown();
      await this.reloadVisibleData();
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
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

  historyTrack(item: GroupDebtHistoryGridItem): string {
    return item.movement.id;
  }

  outstandingBalanceTrack(item: GroupDebtOutstandingBalanceGridItem): string {
    return item.id;
  }

  drilldownTrack(movement: GroupDebtMovementDto): string {
    return movement.id;
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

  async openMonthlyDrilldown(pair: GroupDebtPairBalanceDto) {
    this.drilldownVisible.set(true);
    this.drilldownLoading.set(true);
    this.monthlyDrilldown.set(undefined);

    try {
      this.monthlyDrilldown.set(
        await this.groupDebtService.getMonthlyDrilldown(this.groupId, {
          payerId: pair.payerId,
          receiverId: pair.receiverId,
          currency: pair.currency,
          selectedMonth: this.selectedMonth(),
        }),
      );
    } catch (error) {
      this.drilldownVisible.set(false);
      this.errorMessageService.handleError(error, this.messageService);
    } finally {
      this.drilldownLoading.set(false);
    }
  }

  closeMonthlyDrilldown() {
    this.drilldownVisible.set(false);
    this.drilldownLoading.set(false);
    this.monthlyDrilldown.set(undefined);
  }

  openAdjustmentPage(movement: GroupDebtMovementDto) {
    void this.router.navigate(['/app/groups', this.groupId, 'debts', 'adjustments', movement.id]);
  }

  isAdjustableMovement(movement: GroupDebtMovementDto): boolean {
    return (
      !movement.projected &&
      !movement.carriedOver &&
      movement.reasonKind !== GroupDebtMovementReasonKind__Obj.MANUAL_ADJUSTMENT_COMPENSATION
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

  drilldownHeader(): string {
    const drilldown = this.monthlyDrilldown();

    return this.translateService.instant('financesPage.groupsPage.debtsPage.drilldownTitle', {
      payer: this.memberName(drilldown?.payerId ?? ''),
      receiver: this.memberName(drilldown?.receiverId ?? ''),
      month: drilldown?.month != null ? dayjs(`${drilldown.month}-01`).format('MMM YYYY') : '',
    });
  }

  selectedMonth(): string {
    return this.dateControl.value?.startDate?.format(MONTH_QUERY_PARAM_FORMAT_V2) ?? dayjs().format(MONTH_QUERY_PARAM_FORMAT_V2);
  }

  private async reloadVisibleData() {
    if (!this.groupId) {
      return;
    }

    this.workspaceLoading.set(true);
    this.historyLoading.set(true);

    try {
      const selectedMonth = this.selectedMonth();
      const [workspace, history] = await Promise.all([
        this.groupDebtService.getWorkspace(this.groupId, selectedMonth),
        this.groupDebtService.listHistory(this.groupId, { selectedMonth }),
      ]);

      this.workspace.set(workspace);
      this.history.set(history);
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
}
