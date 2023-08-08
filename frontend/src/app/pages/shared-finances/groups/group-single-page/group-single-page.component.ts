import { Component, Inject, OnDestroy, OnInit, Renderer2 } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { lastValueFrom, merge, Observable, Subscription } from "rxjs";
import { TdDialogService } from "@covalent/core/dialogs";
import { Group } from "src/app/@core/models/group";
import { TranslocoService } from "@ngneat/transloco";
import { TitleService, GroupsService, TransactionService } from "src/app/@core/services";
import moment, { Moment } from "moment";
import { GroupSummary, Page, Transaction } from "src/app/@core/models";
import { DOCUMENT } from "@angular/common";
import { NewTransactionDialogService } from "src/app/components/new-transaction/new-transaction-dialog.service";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { Chart } from "src/app/@core/models/chart";
import { CHART_DEFAULT_MINIMUM_MONTHS, DEFAULT_PAGE_SIZE } from "src/app/@core/constants";
import { Color } from "@swimlane/ngx-charts/lib/utils/color-sets";
import { take } from "rxjs/operators";
import { UntypedFormControl, UntypedFormGroup } from "@angular/forms";
import { BreakpointObserver, Breakpoints, BreakpointState } from "@angular/cdk/layout";

@UntilDestroy()
@Component({
  selector: "app-group-single-page",
  templateUrl: "./group-single-page.component.html",
  styleUrls: ["./group-single-page.component.scss"]
})
export class GroupSinglePageComponent implements OnInit {
  group: Group;
  sharedLinkLoading = false;
  transactionsPage$: Observable<Page<Transaction>>;
  pageSize = DEFAULT_PAGE_SIZE;
  transactionsGroupedYearMonth: Chart[];

  groupSummaryState: { isLoading: boolean; summary?: GroupSummary } = {
    isLoading: true
  };

  disallowFutureOnSameMonth = true;

  legend: boolean = false;
  showLabels: boolean = true;
  animations: boolean = true;
  showYAxisLabel: boolean = true;
  showXAxisLabel: boolean = true;

  colorScheme: Color;

  private groupSubscription: Subscription;
  private groupId: string;
  private monthDate: Moment;
  private transactionsChangeSubscription: Subscription;

  filterFormGroup = new UntypedFormGroup({
    categories: new UntypedFormControl()
  });

  get filterCategories() {
    return this.filterFormGroup.value.categories?.map(category => category.id);
  }

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
    private breakpointObserver: BreakpointObserver,
  ) {
    this.breakpointObserver
      .observe([Breakpoints.Medium])
      .pipe(untilDestroyed(this))
      .subscribe((state: BreakpointState) => {
        this.legend = state.matches;
      });

  }

  ngOnInit(): void {
    this.activatedRoute.params.pipe(untilDestroyed(this)).subscribe(
      params => this.loadGroup(params.id)
    );
  }

  public filter() {
    return this.getInfoBasedOnFilters();
  }

  public clearFilters() {
    this.filterFormGroup.reset();
    return this.filter();
  }

  async createInvite(): Promise<void> {
    this.sharedLinkLoading = true;

    try {
      const sharedLink = await this.groupsService.generateShareLink(this.groupId);

      this.dialogService.openPrompt({
        title: this.translocoService.translate("link-created"),
        message: this.translocoService.translate(
          "link-created-message", { expiresOn: sharedLink.expiresOn }
        ),
        value: `${window.location.origin}/finances/shared/invite/${sharedLink.code}`,
        cancelButton: this.translocoService.translate("cancel"),
        acceptButton: this.translocoService.translate("ok")
      });
    } finally {
      this.sharedLinkLoading = false;
    }
  }

  async dateChanged(newDate: Moment): Promise<void> {
    this.monthDate = newDate;
    await this.getInfoBasedOnFilters();
  }

  getTransactions(page = 0): void {
    this.transactionsPage$ = this.groupsService.getTransactions(
      this.group.id,
      {
        maxDate: moment(this.monthDate).endOf("month"),
        minDate: moment(this.monthDate).startOf("month"),
        categoriesId: this.filterCategories
      },
      { page, size: this.pageSize }
    );
  }

  public async editTransaction(transaction: Transaction) {
    this.newTransactionDialogService.openDialog(this.document, this.renderer2, transaction.group != null, transaction);
  }

  public async getChart(): Promise<void> {
    this.transactionsGroupedYearMonth = undefined;

    const charts = await this.groupsService.getTransactionsChart(this.group, this.getMaxDate().add(1), {
      groupId: this.group.id,
      maxDate: this.getMaxDate(),
      minDate: moment(this.getMaxDate(false)).subtract(CHART_DEFAULT_MINIMUM_MONTHS, "month"),
      categoriesId: this.filterCategories
    });

    this.transactionsGroupedYearMonth = charts;
  }

  public async toggleDisallowFutureOnSameMonth(): Promise<void> {
    this.disallowFutureOnSameMonth = !this.disallowFutureOnSameMonth;
    await this.getInfoBasedOnFilters();
  }

  private loadGroup(groupId: string) {
    this.groupId = groupId;

    this.groupSubscription?.unsubscribe();
    this.groupSubscription = this.groupsService.getGroup(groupId).pipe(untilDestroyed(this)).subscribe(group => {
      if (group) {
        this.titleService.changeTitle("group-name", {
          name: group.name
        });

        this.group = group;

        this.colorScheme = {
          name: "expense",
          selectable: true,
          group: 0 as any,
          domain: [
            "#E44D25",
            ...(this.group.users.map(() => this.randomColor()))
          ]
        };

        this.transactionsChangeObserver();
      } else {
        this.router.navigateByUrl("/404");
      }
    });
  }

  private async getInfoBasedOnFilters() {
    this.getTransactions();
    return Promise.all([this.getGroupSummary(), this.getChart()]);
  }

  private async getGroupSummary() {
    this.groupSummaryState = {
      isLoading: true
    };

    const summary = await this.groupsService.getGroupSummary(
      this.group.id,
      moment(this.monthDate).startOf("month"),
      moment(this.monthDate).endOf("month"),
      this.filterCategories
    );

    this.groupSummaryState = {
      isLoading: false,
      summary
    };
  }

  private transactionsChangeObserver(): void {
    this.transactionsChangeSubscription?.unsubscribe();

    this.transactionsChangeSubscription = merge(
      this.transactionService.onTransactionCreated(this.group.id),
      this.transactionService.onTransactionUpdated(this.group.id),
      this.transactionService.onTransactionDeleted(this.group.id)
    )
      .pipe(untilDestroyed(this))
      .subscribe(async () => {
          const transactionsPage = await lastValueFrom(this.transactionsPage$.pipe(take(1)));

          if (transactionsPage.number == 0) {
            this.getTransactions();
          } else {
            // TODO: ask to user if he wants update
          }

          await Promise.all([
            this.getGroupSummary(), this.getChart()
          ]);
        }
      );
  }

  private getMaxDate(disallowFutureOnSameMonth = this.disallowFutureOnSameMonth) {
    return this.transactionService.getMaxDate(this.monthDate, disallowFutureOnSameMonth);
  }

  private randomColor(): string {
    return `#${(0x1000000 + Math.random() * 0xffffff).toString(16).substr(1, 6)}`;
  }
}
