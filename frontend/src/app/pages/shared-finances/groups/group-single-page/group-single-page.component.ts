import { Component, Inject, OnDestroy, OnInit, Renderer2 } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { TdDialogService } from '@covalent/core/dialogs';
import { Group } from 'src/app/@core/models/group';
import { TranslocoService } from '@ngneat/transloco';
import { TitleService, GroupsService, ErrorService, TransactionService } from 'src/app/@core/services';
import moment, { Moment } from 'moment';
import { GroupSummary, Page, Transaction } from 'src/app/@core/models';
import { DOCUMENT } from '@angular/common';
import { HotToastService } from '@ngneat/hot-toast';
import { take } from 'rxjs/operators';
import { NewTransactionDialogService } from 'src/app/components/new-transaction/new-transaction-dialog.service';

@Component({
  selector: 'app-group-single-page',
  templateUrl: './group-single-page.component.html',
  styleUrls: ['./group-single-page.component.scss'],
})
export class GroupSinglePageComponent implements OnInit, OnDestroy {
  public group: Group;
  public sharedLinkLoading = false;
  public transactionsPage$: Observable<Page<Transaction>>;
  public pageSize = 20;

  groupSummaryState: { isLoading: boolean; summary?: GroupSummary } = {
    isLoading: true,
  };

  private activatedRouteSubscription: Subscription;
  private groupId: string;
  private monthDate: Moment;

  constructor(
    private groupsService: GroupsService,
    private activatedRoute: ActivatedRoute,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private router: Router,
    private titleService: TitleService,
    private transactionService: TransactionService,
    private toast: HotToastService,
    private errorService: ErrorService,
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

  public async deleteTransaction(transaction: Transaction) {
    await this.transactionService
      .deleteTransaction(transaction.id)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('deleting'),
          success: this.translocoService.translate('deleting-successful', { name: transaction.description ?? '' }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
              name: transaction.description,
            }),
        }),
      )
      .toPromise();
  }

  private async loadGroup(groupId: string): Promise<void> {
    this.groupId = groupId;

    const group = await this.groupsService.getGroup(groupId);

    if (group) {
      this.titleService.changeTitle('group-name', {
        name: group.name,
      });

      this.group = group;
    } else {
      this.router.navigateByUrl('/404');
    }
  }

  private async getInfoBasedOnGroupAndDate() {
    this.getTransactions();
    // this.transactionsChangeObserver();
    return Promise.all([this.getGroupSummary()]);
    // return Promise.all([this.getGroupSummary(), this.getChart()]);
  }

  private async getGroupSummary() {
    this.groupSummaryState.isLoading = true;

    this.groupSummaryState.summary = {
      ...(await this.groupsService.getGroupSummary(
        this.group.id,
        moment(this.monthDate).startOf('month'),
        moment(this.monthDate).endOf('month'),
      )),
    };

    this.groupSummaryState.summary.expenses = [...this.groupSummaryState.summary.expenses].sort((expenseA, expenseB) =>
      expenseA.user.name.localeCompare(expenseB.user.name),
    );

    this.groupSummaryState.isLoading = false;
  }
}
