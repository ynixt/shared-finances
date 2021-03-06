import { Component, Inject, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { merge, Observable, Subscription } from 'rxjs';
import { TdDialogService } from '@covalent/core/dialogs';
import { Group } from 'src/app/@core/models/group';
import { TranslocoService } from '@ngneat/transloco';
import { TitleService, GroupsService, TransactionService } from 'src/app/@core/services';
import moment, { Moment } from 'moment';
import { GroupSummary, Page, Transaction } from 'src/app/@core/models';
import { DOCUMENT } from '@angular/common';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { Chart } from 'src/app/@core/models/chart';
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from 'src/app/@core/constants';

@UntilDestroy()
@Component({
  selector: 'app-group-single-page',
  templateUrl: './group-single-page.component.html',
  styleUrls: ['./group-single-page.component.scss'],
})
export class GroupSinglePageComponent implements OnInit, OnDestroy {
  group: Group;
  sharedLinkLoading = false;
  transactionsPage$: Observable<Page<Transaction>>;
  pageSize = DEFAULT_PAGE_SIZE;
  transactionsGroupedYearMonth: Chart[];

  groupSummaryState: { isLoading: boolean; summary?: GroupSummary } = {
    isLoading: true,
  };

  disallowFutureOnSameMonth = true;

  legend: boolean = true;
  showLabels: boolean = true;
  animations: boolean = true;
  showYAxisLabel: boolean = true;
  showXAxisLabel: boolean = true;

  colorScheme = {
    domain: ['#5AA454'],
  };

  private activatedRouteSubscription: Subscription;
  private groupId: string;
  private monthDate: Moment;
  private transactionsChangeSubscription: Subscription;

  constructor(
    private groupsService: GroupsService,
    private activatedRoute: ActivatedRoute,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private router: Router,
    private titleService: TitleService,
    private transactionService: TransactionService,
    private newTransactionDialogService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2,
  ) {}

  ngOnInit(): void {
    this.activatedRouteSubscription = this.activatedRoute.params.subscribe(params => this.loadGroup(params.id));
  }

  ngOnDestroy(): void {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

  async createInvite(): Promise<void> {
    this.sharedLinkLoading = true;

    try {
      const sharedLink = await this.groupsService.generateShareLink(this.groupId);

      this.dialogService.openPrompt({
        title: this.translocoService.translate('link-created'),
        message: this.translocoService.translate('link-created-message'),
        value: `${window.location.origin}/finances/shared/invite/${sharedLink}`,
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('ok'),
      });
    } finally {
      this.sharedLinkLoading = false;
    }
  }

  async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnGroupAndDate();
  }

  getTransactions(page = 1): void {
    this.transactionsPage$ = this.groupsService.getTransactions(
      this.group.id,
      { maxDate: moment(this.monthDate).endOf('month'), minDate: moment(this.monthDate).startOf('month') },
      { page, pageSize: this.pageSize },
    );
  }

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  public async getChart(): Promise<void> {
    this.transactionsGroupedYearMonth = undefined;

    const groupNamesById = new Map<string, string>();
    groupNamesById.set(this.group.id, this.group.name);

    const charts = await this.groupsService.getTransactionsChart(groupNamesById, this.getMaxDate().add(1), {
      groupId: this.group.id,
      maxDate: this.getMaxDate(),
      minDate: moment(this.getMaxDate(false)).subtract(CHART_DEFAULT_MINIMUM_MONTHS, 'month'),
    });

    this.transactionsGroupedYearMonth = charts;
  }

  public async toggleDisallowFutureOnSameMonth(): Promise<void> {
    this.disallowFutureOnSameMonth = !this.disallowFutureOnSameMonth;
    await this.getInfoBasedOnGroupAndDate();
  }

  private async loadGroup(groupId: string): Promise<void> {
    this.groupId = groupId;

    const group = await this.groupsService.getGroup(groupId);

    if (group) {
      this.titleService.changeTitle('group-name', {
        name: group.name,
      });

      this.group = group;

      this.transactionsChangeObserver();
    } else {
      this.router.navigateByUrl('/404');
    }
  }

  private async getInfoBasedOnGroupAndDate() {
    this.getTransactions();
    return Promise.all([this.getGroupSummary(), this.getChart()]);
  }

  private async getGroupSummary() {
    this.groupSummaryState = {
      isLoading: true,
    };

    const summary = await this.groupsService.getGroupSummary(
      this.group.id,
      moment(this.monthDate).startOf('month'),
      moment(this.monthDate).endOf('month'),
    );

    this.groupSummaryState = {
      isLoading: false,
      summary,
    };
  }

  private transactionsChangeObserver(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.transactionService.onTransactionCreated(this.group.id),
      this.transactionService.onTransactionUpdated(this.group.id),
      this.transactionService.onTransactionDeleted(this.group.id),
    )
      .pipe(untilDestroyed(this))
      .subscribe(() => Promise.all([this.getGroupSummary(), this.getChart()]));
  }

  private getMaxDate(disallowFutureOnSameMonth = this.disallowFutureOnSameMonth) {
    return this.transactionService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }
}
